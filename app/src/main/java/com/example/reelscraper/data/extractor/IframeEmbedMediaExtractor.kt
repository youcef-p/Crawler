package com.example.reelscraper.data.extractor

import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.Collections
import java.util.concurrent.TimeUnit

class IframeEmbedMediaExtractor : MediaExtractor {
    override val name: String = "IFRAME_EMBED"

    private val visitedEmbedUrls = Collections.synchronizedSet(mutableSetOf<String>())

    override suspend fun extract(context: ExtractionContext): List<ExtractedMediaCandidate> {
        val doc = context.document ?: return emptyList()
        val results = mutableListOf<ExtractedMediaCandidate>()
        val pageUrl = context.pageUrl
        val settings = context.settings

        for (iframe in doc.select("iframe[src]")) {
            val src = iframe.attr("abs:src").ifBlank { iframe.attr("src") }
            val normalizedUrl = MediaNormalizer.normalizeUrl(src, pageUrl) ?: continue

            // 1. Direct media URL inside iframe
            if (isDirectMedia(normalizedUrl)) {
                val type = determineType(normalizedUrl)
                if (isTypeAllowed(type, settings)) {
                    results.add(
                        ExtractedMediaCandidate(
                            url = normalizedUrl,
                            mediaType = type,
                            title = iframe.attr("title").ifBlank { null },
                            sourcePageUrl = pageUrl,
                            fileExtension = MediaNormalizer.extractExtension(normalizedUrl),
                            extractorType = name,
                            priority = 7
                        )
                    )
                }
                continue
            }

            // 2. Embed page (e.g. /embed/..., player.html): fetch lightly once
            if (!visitedEmbedUrls.contains(normalizedUrl) && visitedEmbedUrls.size < 50) {
                visitedEmbedUrls.add(normalizedUrl)
                try {
                    val request = Request.Builder()
                        .url(normalizedUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) Chrome/126.0.0.0")
                        .header("Referer", pageUrl)
                        .build()

                    val response = context.client.newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    response.close()

                    if (body.isNotBlank() && body.length < settings.maxScriptScanSizeBytes * 2) {
                        val embedDoc = Jsoup.parse(body, normalizedUrl)
                        // Scan for <video>, <source>
                        for (video in embedDoc.select("video, source")) {
                            val vSrc = video.attr("abs:src").ifBlank { video.attr("src") }
                            val vNorm = MediaNormalizer.normalizeUrl(vSrc, normalizedUrl)
                            if (vNorm != null) {
                                val type = determineType(vNorm)
                                if (isTypeAllowed(type, settings)) {
                                    results.add(
                                        ExtractedMediaCandidate(
                                            url = vNorm,
                                            mediaType = type,
                                            title = iframe.attr("title").ifBlank { null },
                                            sourcePageUrl = pageUrl,
                                            fileExtension = MediaNormalizer.extractExtension(vNorm),
                                            extractorType = name,
                                            priority = 8
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Ignore iframe network errors
                }
            }
        }

        return results
    }

    private fun isDirectMedia(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".mp4") || lower.contains(".webm") ||
                lower.contains(".m3u8") || lower.contains(".mpd") ||
                lower.contains(".gif")
    }

    private fun determineType(url: String): MediaType {
        val lower = url.lowercase()
        return when {
            lower.contains(".m3u8") -> MediaType.HLS
            lower.contains(".mpd") -> MediaType.DASH
            lower.contains(".gif") -> MediaType.GIF
            else -> MediaType.VIDEO
        }
    }

    private fun isTypeAllowed(type: MediaType, settings: AppSettings): Boolean {
        return when (type) {
            MediaType.HLS -> settings.extractHls
            MediaType.DASH -> settings.extractDash
            MediaType.GIF -> settings.extractGif
            MediaType.VIDEO -> settings.extractMp4 || settings.extractWebm
        }
    }
}
