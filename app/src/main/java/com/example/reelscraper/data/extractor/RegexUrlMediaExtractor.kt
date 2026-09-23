package com.example.reelscraper.data.extractor

import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer
import java.util.regex.Pattern

class RegexUrlMediaExtractor : MediaExtractor {
    override val name: String = "REGEX_SCAN"

    private val mediaUrlPattern = Pattern.compile(
        """(?:https?:\\/\\/|https?:\/\/|https?%3A%2F%2F|\/\/)[^\s"'<>\)\{\}\[\]\\]+\.(?:mp4|m3u8|mpd|webm|gif)(?:\?[^\s"'<>\)\{\}\[\]\\]*)?""",
        Pattern.CASE_INSENSITIVE
    )

    override suspend fun extract(context: ExtractionContext): List<ExtractedMediaCandidate> {
        val html = context.html
        if (html.isBlank()) return emptyList()

        val results = mutableListOf<ExtractedMediaCandidate>()
        val pageUrl = context.pageUrl
        val settings = context.settings

        // Limit scanning to max size to prevent performance lag
        val textToScan = if (html.length > settings.maxScriptScanSizeBytes * 2) {
            html.substring(0, settings.maxScriptScanSizeBytes * 2)
        } else {
            html
        }

        val matcher = mediaUrlPattern.matcher(textToScan)
        while (matcher.find()) {
            val rawUrl = matcher.group()
            val normalizedUrl = MediaNormalizer.normalizeUrl(rawUrl, pageUrl) ?: continue

            val type = determineType(normalizedUrl)
            if (isTypeAllowed(type, settings)) {
                results.add(
                    ExtractedMediaCandidate(
                        url = normalizedUrl,
                        mediaType = type,
                        sourcePageUrl = pageUrl,
                        fileExtension = MediaNormalizer.extractExtension(normalizedUrl),
                        extractorType = name,
                        priority = 4
                    )
                )
            }
        }

        return results
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
