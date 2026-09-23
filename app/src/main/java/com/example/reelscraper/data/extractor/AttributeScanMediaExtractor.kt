package com.example.reelscraper.data.extractor

import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer
import org.json.JSONObject
import java.util.regex.Pattern

class AttributeScanMediaExtractor : MediaExtractor {
    override val name: String = "DATA_ATTRIBUTES"

    private val targetAttributeNames = setOf(
        "data-src", "data-lazy-src", "data-original", "data-video", "data-video-src",
        "data-file", "data-url", "data-hls", "data-dash", "data-mpd", "data-mp4",
        "data-webm", "data-gif", "data-media", "data-source", "data-stream",
        "data-playlist", "data-poster", "data-thumb", "data-thumbnail", "data-background",
        "data-bg", "data-image"
    )

    private val cssUrlPattern = Pattern.compile(
        """background(?:-image)?\s*:\s*url\(\s*["']?([^"')]+)["']?\s*\)""",
        Pattern.CASE_INSENSITIVE
    )

    override suspend fun extract(context: ExtractionContext): List<ExtractedMediaCandidate> {
        val doc = context.document ?: return emptyList()
        val results = mutableListOf<ExtractedMediaCandidate>()
        val pageUrl = context.pageUrl
        val settings = context.settings

        // 1. Scan elements with custom data attributes
        for (element in doc.select("*")) {
            val attributes = element.attributes()
            for (attr in attributes) {
                val key = attr.key.lowercase()
                val value = attr.value.trim()
                if (value.isBlank()) continue

                // Check known media data attributes
                if (targetAttributeNames.contains(key) || key.startsWith("data-video") || key.startsWith("data-stream")) {
                    val normalized = MediaNormalizer.normalizeUrl(value, pageUrl)
                    if (normalized != null && isDirectMedia(normalized)) {
                        val type = determineType(normalized)
                        if (isTypeAllowed(type, settings)) {
                            results.add(
                                ExtractedMediaCandidate(
                                    url = normalized,
                                    mediaType = type,
                                    title = element.attr("title").ifBlank { element.attr("alt") }.ifBlank { null },
                                    sourcePageUrl = pageUrl,
                                    fileExtension = MediaNormalizer.extractExtension(normalized),
                                    extractorType = name,
                                    priority = 6
                                )
                            )
                        }
                    }
                }

                // Check data-setup JSON attribute (common in Video.js)
                if (key == "data-setup" && value.startsWith("{")) {
                    try {
                        val json = JSONObject(value)
                        val sources = json.optJSONArray("sources")
                        if (sources != null) {
                            for (i in 0 until sources.length()) {
                                val sObj = sources.optJSONObject(i)
                                val src = sObj?.optString("src")
                                if (!src.isNullOrBlank()) {
                                    val norm = MediaNormalizer.normalizeUrl(src, pageUrl)
                                    if (norm != null) {
                                        val type = determineType(norm)
                                        if (isTypeAllowed(type, settings)) {
                                            results.add(
                                                ExtractedMediaCandidate(
                                                    url = norm,
                                                    mediaType = type,
                                                    sourcePageUrl = pageUrl,
                                                    fileExtension = MediaNormalizer.extractExtension(norm),
                                                    extractorType = name,
                                                    priority = 8
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore malformed JSON
                    }
                }

                // Check inline styles for background GIFs
                if (key == "style" && settings.extractGif && value.contains(".gif", ignoreCase = true)) {
                    val matcher = cssUrlPattern.matcher(value)
                    while (matcher.find()) {
                        val styleUrl = matcher.group(1)?.trim() ?: continue
                        if (styleUrl.contains(".gif", ignoreCase = true)) {
                            val norm = MediaNormalizer.normalizeUrl(styleUrl, pageUrl)
                            if (norm != null) {
                                results.add(
                                    ExtractedMediaCandidate(
                                        url = norm,
                                        mediaType = MediaType.GIF,
                                        sourcePageUrl = pageUrl,
                                        fileExtension = "gif",
                                        extractorType = name,
                                        priority = 4
                                    )
                                )
                            }
                        }
                    }
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
