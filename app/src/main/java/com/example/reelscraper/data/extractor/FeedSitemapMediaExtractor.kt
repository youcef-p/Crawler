package com.example.reelscraper.data.extractor

import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer

class FeedSitemapMediaExtractor : MediaExtractor {
    override val name: String = "FEED_SITEMAP"

    override suspend fun extract(context: ExtractionContext): List<ExtractedMediaCandidate> {
        val doc = context.document ?: return emptyList()
        val results = mutableListOf<ExtractedMediaCandidate>()
        val pageUrl = context.pageUrl
        val settings = context.settings

        // 1. RSS / Atom Enclosures
        for (enclosure in doc.select("enclosure")) {
            val url = enclosure.attr("url")
            val type = enclosure.attr("type")
            if (url.isNotBlank()) {
                val normalized = MediaNormalizer.normalizeUrl(url, pageUrl)
                if (normalized != null) {
                    val mediaType = determineType(normalized, type)
                    if (isTypeAllowed(mediaType, settings)) {
                        val title = enclosure.parent()?.selectFirst("title")?.text()
                        results.add(
                            ExtractedMediaCandidate(
                                url = normalized,
                                mediaType = mediaType,
                                title = title,
                                sourcePageUrl = pageUrl,
                                fileExtension = MediaNormalizer.extractExtension(normalized),
                                extractorType = name,
                                priority = 7
                            )
                        )
                    }
                }
            }
        }

        // 2. Media RSS (<media:content>, <media:player>)
        for (mediaContent in doc.select("media\\:content, content")) {
            val url = mediaContent.attr("url")
            if (url.isNotBlank()) {
                val normalized = MediaNormalizer.normalizeUrl(url, pageUrl)
                if (normalized != null && isDirectMedia(normalized)) {
                    val mediaType = determineType(normalized, mediaContent.attr("type"))
                    if (isTypeAllowed(mediaType, settings)) {
                        val thumb = mediaContent.parent()?.selectFirst("media\\:thumbnail")?.attr("url")
                        val title = mediaContent.parent()?.selectFirst("media\\:title, title")?.text()
                        results.add(
                            ExtractedMediaCandidate(
                                url = normalized,
                                mediaType = mediaType,
                                title = title,
                                posterUrl = MediaNormalizer.normalizeUrl(thumb ?: "", pageUrl),
                                sourcePageUrl = pageUrl,
                                fileExtension = MediaNormalizer.extractExtension(normalized),
                                extractorType = name,
                                priority = 8
                            )
                        )
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

    private fun determineType(url: String, mimeType: String?): MediaType {
        val lowerMime = mimeType?.lowercase() ?: ""
        val lower = url.lowercase()
        return when {
            lowerMime.contains("mpegurl") || lower.contains(".m3u8") -> MediaType.HLS
            lowerMime.contains("dash") || lower.contains(".mpd") -> MediaType.DASH
            lowerMime.contains("gif") || lower.contains(".gif") -> MediaType.GIF
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
