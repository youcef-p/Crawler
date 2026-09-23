package com.example.reelscraper.data.scraper

import android.content.Context
import com.example.reelscraper.data.extractor.ContentTypeSniffer
import com.example.reelscraper.data.extractor.ExtractionContext
import com.example.reelscraper.data.extractor.MediaExtractionPipeline
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.ArrayDeque
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ScrapeProgress(
    val statusMessage: String,
    val currentUrl: String,
    val itemsFound: Int,
    val currentLevel: Int = 1,
    val maxLevel: Int = 1,
    val pagesVisited: Int = 0
)

data class CrawlNode(
    val url: String,
    val level: Int // 1-indexed (Level 1 = seed URL)
)

class WebScraperEngine(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val appContext: Context? = null
) {
    private val contentTypeSniffer = ContentTypeSniffer(okHttpClient)
    private val extractionPipeline = MediaExtractionPipeline(contentTypeSniffer)

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36 ReelScraper/1.0"
    }

    /**
     * Breadth-first crawl up to maxLevel (1 to 10).
     * Level 1: scan initial URL only.
     * Level 2: scan clickable links found on initial URL.
     * Level 3: scan clickable links found on Level 2 pages.
     */
    suspend fun crawlAndExtract(
        initialUrl: String,
        maxLevel: Int,
        settings: AppSettings,
        existingUrls: Set<String> = emptySet(),
        onProgress: (ScrapeProgress) -> Unit
    ): List<ScrapedMedia> = withContext(ioDispatcher) {
        val targetMaxLevel = maxLevel.coerceIn(1, 10)
        val normalizedSeed = MediaNormalizer.normalizeUrl(initialUrl)
            ?: throw IllegalArgumentException("Invalid URL: $initialUrl")

        val visitedUrls = Collections.synchronizedSet(mutableSetOf<String>())
        val discoveredMedia = mutableMapOf<Pair<String, String>, ScrapedMedia>()
        val queue = ArrayDeque<CrawlNode>()

        queue.add(CrawlNode(normalizedSeed, 1))
        visitedUrls.add(normalizedSeed)

        val semaphore = Semaphore(settings.maxConcurrentRequests.coerceIn(1, 10))
        var pagesVisited = 0

        onProgress(
            ScrapeProgress(
                statusMessage = "Starting Level 1 scan on $normalizedSeed",
                currentUrl = normalizedSeed,
                itemsFound = 0,
                currentLevel = 1,
                maxLevel = targetMaxLevel,
                pagesVisited = 0
            )
        )

        while (queue.isNotEmpty()) {
            val node = queue.poll() ?: break
            if (node.level > targetMaxLevel) continue

            // If node URL is a direct media file, don't crawl it as HTML page
            if (isDirectMediaUrl(node.url)) {
                val mediaDomain = MediaNormalizer.normalizeDomain(node.url)
                val mediaName = MediaNormalizer.normalizeMediaName(node.url)
                val key = Pair(mediaName, mediaDomain)
                if (!discoveredMedia.containsKey(key)) {
                    val ext = MediaNormalizer.extractExtension(node.url)
                    val candidate = ScrapedMedia(
                        url = node.url,
                        title = mediaName.replace('-', ' '),
                        mediaType = determineTypeFromUrl(node.url),
                        sourcePageUrl = node.url,
                        sourceDomain = mediaDomain,
                        normalizedName = mediaName,
                        fileExtension = ext
                    )
                    discoveredMedia[key] = candidate
                }
                continue
            }

            pagesVisited++
            onProgress(
                ScrapeProgress(
                    statusMessage = "Level ${node.level}/$targetMaxLevel: Scanning ${node.url}",
                    currentUrl = node.url,
                    itemsFound = discoveredMedia.size,
                    currentLevel = node.level,
                    maxLevel = targetMaxLevel,
                    pagesVisited = pagesVisited
                )
            )

            // Optional request delay
            if (settings.requestDelayMs > 0 && pagesVisited > 1) {
                delay(settings.requestDelayMs)
            }

            try {
                semaphore.withPermit {
                    val request = Request.Builder()
                        .url(node.url)
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8")
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    val finalUrl = response.request.url.toString()
                    val contentType = response.header("Content-Type") ?: ""
                    val responseHeaders = mutableMapOf<String, String>()
                    for (i in 0 until response.headers.size) {
                        responseHeaders[response.headers.name(i)] = response.headers.value(i)
                    }

                    val body = response.body?.string() ?: ""
                    response.close()

                    // Parse document if HTML
                    val doc = if (contentType.contains("text/html") || body.contains("<html", ignoreCase = true)) {
                        Jsoup.parse(body, finalUrl)
                    } else null

                    // Run extraction pipeline
                    val context = ExtractionContext(
                        pageUrl = finalUrl,
                        document = doc,
                        html = body,
                        contentType = contentType,
                        responseHeaders = responseHeaders,
                        client = okHttpClient,
                        settings = settings,
                        appContext = appContext
                    )

                    val extracted = extractionPipeline.execute(context)
                    for (item in extracted) {
                        val key = Pair(item.normalizedName, item.sourceDomain)
                        if (!discoveredMedia.containsKey(key)) {
                            discoveredMedia[key] = item
                        }
                    }

                    // If not at max level, extract clickable links to crawl on next level
                    if (node.level < targetMaxLevel && doc != null) {
                        val links = doc.select("a[href]")
                        var linkCount = 0

                        for (link in links) {
                            if (linkCount >= settings.maxLinksPerPage) break
                            val href = link.attr("abs:href").ifBlank { link.attr("href") }
                            val normalized = MediaNormalizer.normalizeUrl(href, finalUrl) ?: continue

                            // Only crawl HTTP/HTTPS, skip already visited
                            if (!visitedUrls.contains(normalized)) {
                                visitedUrls.add(normalized)
                                linkCount++
                                queue.add(CrawlNode(normalized, node.level + 1))
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Skip failed page and continue crawl
            }

            onProgress(
                ScrapeProgress(
                    statusMessage = "Found ${discoveredMedia.size} streams across $pagesVisited pages",
                    currentUrl = node.url,
                    itemsFound = discoveredMedia.size,
                    currentLevel = node.level,
                    maxLevel = targetMaxLevel,
                    pagesVisited = pagesVisited
                )
            )
        }

        return@withContext discoveredMedia.values.toList()
    }

    private fun isDirectMediaUrl(url: String): Boolean {
        val lower = url.lowercase(Locale.US)
        return lower.contains(".mp4") || lower.contains(".webm") ||
                lower.contains(".m3u8") || lower.contains(".mpd") ||
                lower.contains(".gif")
    }

    private fun determineTypeFromUrl(url: String): com.example.reelscraper.data.model.MediaType {
        val lower = url.lowercase(Locale.US)
        return when {
            lower.contains(".m3u8") -> com.example.reelscraper.data.model.MediaType.HLS
            lower.contains(".mpd") -> com.example.reelscraper.data.model.MediaType.DASH
            lower.contains(".gif") -> com.example.reelscraper.data.model.MediaType.GIF
            else -> com.example.reelscraper.data.model.MediaType.VIDEO
        }
    }
}
