package com.example.reelscraper.data.scraper

import android.content.Context
import android.util.Log
import com.example.reelscraper.data.extractor.ContentTypeSniffer
import com.example.reelscraper.data.extractor.ExtractionContext
import com.example.reelscraper.data.extractor.MediaExtractionPipeline
import com.example.reelscraper.data.model.CrawlJobStatus
import com.example.reelscraper.data.model.MediaType
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
    companion object {
        private const val TAG = "WebScraperEngine"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36 ReelScraper/1.0"
    }

    private val contentTypeSniffer = ContentTypeSniffer(okHttpClient)
    private val extractionPipeline = MediaExtractionPipeline(contentTypeSniffer)

    /**
     * Extracts media from a single page for diagnostic / testing purposes.
     */
    suspend fun extractSinglePage(
        url: String,
        settings: AppSettings = AppSettings()
    ): List<ScrapedMedia> = withContext(ioDispatcher) {
        val normalized = MediaNormalizer.normalizeUrl(url) ?: return@withContext emptyList()

        if (isDirectMediaUrl(normalized)) {
            val mediaDomain = MediaNormalizer.normalizeDomain(normalized)
            val mediaName = MediaNormalizer.normalizeMediaName(normalized)
            val ext = MediaNormalizer.extractExtension(normalized)
            return@withContext listOf(
                ScrapedMedia(
                    url = normalized,
                    title = mediaName.replace('-', ' '),
                    mediaType = determineTypeFromUrl(normalized),
                    sourcePageUrl = normalized,
                    sourceDomain = mediaDomain,
                    normalizedName = mediaName,
                    fileExtension = ext
                )
            )
        }

        try {
            val request = Request.Builder()
                .url(normalized)
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

            val doc = if (contentType.contains("text/html") || body.contains("<html", ignoreCase = true)) {
                Jsoup.parse(body, finalUrl)
            } else null

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

            extractionPipeline.execute(context)
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting single page $url: ${e.message}")
            emptyList()
        }
    }

    /**
     * Breadth-first crawl up to maxLevel (1 to 10).
     * Discovered media is inserted per-page immediately into Room via MediaPersister.
     * Cancellation stops remaining queue work and cleanly preserves all already-inserted items.
     */
    suspend fun crawlAndExtract(
        initialUrl: String,
        maxLevel: Int,
        settings: AppSettings,
        persister: MediaPersister
    ): CrawlProgressState = withContext(ioDispatcher) {
        val targetMaxLevel = maxLevel.coerceIn(1, 10)
        val normalizedSeed = MediaNormalizer.normalizeUrl(initialUrl)
            ?: throw IllegalArgumentException("Invalid URL: $initialUrl")

        persister.startJob(normalizedSeed, targetMaxLevel)

        val visitedUrls = Collections.synchronizedSet(mutableSetOf<String>())
        val queue = ArrayDeque<CrawlNode>()

        queue.add(CrawlNode(normalizedSeed, 1))
        visitedUrls.add(normalizedSeed)

        val semaphore = Semaphore(settings.maxConcurrentRequests.coerceIn(1, 10))
        var pagesVisitedCount = 0

        try {
            while (queue.isNotEmpty()) {
                val node = queue.poll() ?: break
                if (node.level > targetMaxLevel) continue

                // Direct media URL handling
                if (isDirectMediaUrl(node.url)) {
                    val mediaDomain = MediaNormalizer.normalizeDomain(node.url)
                    val mediaName = MediaNormalizer.normalizeMediaName(node.url)
                    val ext = MediaNormalizer.extractExtension(node.url)
                    val directItem = ScrapedMedia(
                        url = node.url,
                        title = mediaName.replace('-', ' '),
                        mediaType = determineTypeFromUrl(node.url),
                        sourcePageUrl = node.url,
                        sourceDomain = mediaDomain,
                        normalizedName = mediaName,
                        fileExtension = ext
                    )
                    persister.persistPageBatch(
                        pageUrl = node.url,
                        depth = node.level,
                        maxDepth = targetMaxLevel,
                        candidates = listOf(directItem)
                    )
                    continue
                }

                pagesVisitedCount++

                // Optional polite delay between page requests
                if (settings.requestDelayMs > 0 && pagesVisitedCount > 1) {
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

                        val doc = if (contentType.contains("text/html") || body.contains("<html", ignoreCase = true)) {
                            Jsoup.parse(body, finalUrl)
                        } else null

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

                        persister.persistPageBatch(
                            pageUrl = finalUrl,
                            depth = node.level,
                            maxDepth = targetMaxLevel,
                            candidates = extracted
                        )

                        if (node.level < targetMaxLevel && doc != null) {
                            val links = doc.select("a[href]")
                            var linkCount = 0

                            for (link in links) {
                                if (linkCount >= settings.maxLinksPerPage) break
                                val href = link.attr("abs:href").ifBlank { link.attr("href") }
                                val normalized = MediaNormalizer.normalizeUrl(href, finalUrl) ?: continue

                                if (visitedUrls.add(normalized)) {
                                    linkCount++
                                    queue.add(CrawlNode(normalized, node.level + 1))
                                }
                            }

                            persister.recordDiscoveredLinks(linkCount)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Error scraping page ${node.url}: ${e.message}")
                    persister.persistPageBatch(
                        pageUrl = node.url,
                        depth = node.level,
                        maxDepth = targetMaxLevel,
                        candidates = emptyList()
                    )
                }
            }

            persister.finishJob(CrawlJobStatus.COMPLETED)
        } catch (e: CancellationException) {
            Log.i(TAG, "Crawl cancelled by user. Preserving all inserted media.")
            persister.finishJob(CrawlJobStatus.CANCELLED)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Crawl failed with unrecoverable error", e)
            persister.finishJob(CrawlJobStatus.FAILED, e.localizedMessage)
        }
    }

    private fun isDirectMediaUrl(url: String): Boolean {
        val lower = url.lowercase(Locale.US)
        return lower.contains(".mp4") || lower.contains(".webm") ||
                lower.contains(".m3u8") || lower.contains(".mpd") ||
                lower.contains(".gif")
    }

    private fun determineTypeFromUrl(url: String): MediaType {
        val lower = url.lowercase(Locale.US)
        return when {
            lower.contains(".m3u8") -> MediaType.HLS
            lower.contains(".mpd") -> MediaType.DASH
            lower.contains(".gif") -> MediaType.GIF
            else -> MediaType.VIDEO
        }
    }
}
