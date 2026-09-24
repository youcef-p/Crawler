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
import java.util.PriorityQueue
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit

data class CrawlNode(
    val url: String,
    val level: Int,
    val priority: Int = 0
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
        val queue = PriorityQueue<CrawlNode>(compareByDescending<CrawlNode> { it.priority }.thenBy { it.level }.thenBy { it.url })

        queue.add(CrawlNode(normalizedSeed, 1, Int.MAX_VALUE))
        visitedUrls.add(normalizedSeed)

        if (settings.discoverSitemaps) {
            val seedUri = java.net.URI(normalizedSeed)
            val sitemapCandidates = listOf(
                seedUri.scheme + "://" + seedUri.authority + "/robots.txt",
                seedUri.scheme + "://" + seedUri.authority + "/sitemap.xml",
                seedUri.scheme + "://" + seedUri.authority + "/sitemap_index.xml"
            )
            sitemapCandidates.forEach { sitemap ->
                if (visitedUrls.add(sitemap)) queue.add(CrawlNode(sitemap, 1, 900))
            }
        }

        val semaphore = Semaphore(settings.maxConcurrentRequests.coerceIn(1, 10))
        val seedDomain = MediaNormalizer.normalizeDomain(normalizedSeed)
        var pagesVisitedCount = 0

        try {
            while (queue.isNotEmpty()) {
                val node = queue.poll() ?: break
                if (node.level > targetMaxLevel) continue
                if (pagesVisitedCount >= settings.maxPagesPerCrawl.coerceAtLeast(1)) break

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
                        val requestClient = okHttpClient.newBuilder()
                            .callTimeout(settings.pageTimeoutSeconds.coerceIn(2, 120).toLong(), TimeUnit.SECONDS)
                            .build()
                        var response: okhttp3.Response? = null
                        var lastError: Exception? = null
                        repeat((settings.retryCount.coerceIn(0, 5) + 1)) { attempt ->
                            if (response != null) return@repeat
                            try {
                                val request = Request.Builder()
                                    .url(node.url)
                                    .header("User-Agent", USER_AGENT)
                                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8")
                                    .build()
                                val candidateResponse = requestClient.newCall(request).execute()
                                if (candidateResponse.isSuccessful || candidateResponse.code in 300..399 || attempt == settings.retryCount.coerceIn(0, 5)) {
                                    response = candidateResponse
                                } else {
                                    candidateResponse.close()
                                    delay((200L * (attempt + 1)).coerceAtMost(1500L))
                                }
                            } catch (e: Exception) {
                                lastError = e
                                if (attempt < settings.retryCount.coerceIn(0, 5)) {
                                    delay((200L * (attempt + 1)).coerceAtMost(1500L))
                                }
                            }
                        }
                        val finalResponse = response ?: throw (lastError ?: IllegalStateException("Request failed"))
                        val finalUrl = finalResponse.request.url.toString()
                        val contentType = finalResponse.header("Content-Type") ?: ""
                        val responseHeaders = mutableMapOf<String, String>()
                        for (i in 0 until finalResponse.headers.size) {
                            responseHeaders[finalResponse.headers.name(i)] = finalResponse.headers.value(i)
                        }

                        val body = finalResponse.body?.string() ?: ""
                        finalResponse.close()

                        val doc = if (contentType.contains("text/html") || body.contains("<html", ignoreCase = true)) {
                            Jsoup.parse(body, finalUrl)
                        } else null

                        if (node.url.endsWith("/robots.txt", ignoreCase = true)) {
                            Regex("""(?im)^\s*Sitemap:\s*(https?://\S+)""")
                                .findAll(body)
                                .map { it.groupValues[1].trim() }
                                .forEach { sitemap ->
                                    if (visitedUrls.add(sitemap)) queue.add(CrawlNode(sitemap, node.level, 950))
                                }
                        }

                        if (node.url.contains("sitemap", ignoreCase = true) &&
                            (contentType.contains("xml", ignoreCase = true) || body.trimStart().startsWith("<?xml", ignoreCase = true))) {
                            Regex("""(?is)<loc>\s*(.*?)\s*</loc>""")
                                .findAll(body)
                                .map { it.groupValues[1].trim() }
                                .mapNotNull { MediaNormalizer.normalizeUrl(it, finalUrl) }
                                .take(settings.maxLinksPerPage.coerceAtLeast(1))
                                .forEach { sitemapUrl ->
                                    if (visitedUrls.add(sitemapUrl)) {
                                        queue.add(CrawlNode(sitemapUrl, (node.level + 1).coerceAtMost(targetMaxLevel), 800 + discoveryPriority(sitemapUrl)))
                                    }
                                }
                        }

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
                            val links = doc.select("a[href]").toMutableList()
                            if (settings.followPaginationLinks) {
                                links.addAll(
                                    doc.select("a[rel~=next], a[aria-label~=next i], a[aria-label~=older i], a[title~=next i]")
                                )
                            }
                            var linkCount = 0

                            for (link in links) {
                                if (linkCount >= settings.maxLinksPerPage) break
                                val href = link.attr("abs:href").ifBlank { link.attr("href") }
                                val normalized = MediaNormalizer.normalizeUrl(href, finalUrl) ?: continue
                                val linkDomain = MediaNormalizer.normalizeDomain(normalized)
                                val sameDomain = linkDomain == seedDomain
                                val subdomainAllowed = settings.includeSubdomains &&
                                    (linkDomain == seedDomain || linkDomain.endsWith("." + seedDomain))
                                if (settings.sameDomainOnly && !sameDomain && !subdomainAllowed) continue

                                if (visitedUrls.add(normalized)) {
                                    linkCount++
                                    queue.add(CrawlNode(normalized, node.level + 1, discoveryPriority(normalized)))
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

    private fun discoveryPriority(url: String): Int {
        val lower = url.lowercase(Locale.US)
        var score = 0
        listOf("video", "watch", "reel", "short", "player", "embed", "stream", "media", "gallery", "clip").forEach {
            if (lower.contains(it)) score += 35
        }
        listOf("article", "post", "story", "news", "episode", "movie").forEach {
            if (lower.contains(it)) score += 10
        }
        listOf("login", "logout", "signup", "register", "account", "cart", "privacy", "terms", "tag/", "category/").forEach {
            if (lower.contains(it)) score -= 30
        }
        if (isDirectMediaUrl(lower)) score += 250
        if (lower.contains("m3u8") || lower.contains("mpd")) score += 350
        return score.coerceIn(-200, 1000)
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
