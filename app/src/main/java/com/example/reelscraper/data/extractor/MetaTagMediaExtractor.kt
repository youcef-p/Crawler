package com.example.reelscraper.data.extractor

import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer

class MetaTagMediaExtractor : MediaExtractor {
    override val name: String = "META_TAGS"

    override suspend fun extract(context: ExtractionContext): List<ExtractedMediaCandidate> {
        val doc = context.document ?: return emptyList()
        val results = mutableListOf<ExtractedMediaCandidate>()
        val pageUrl = context.pageUrl
        val settings = context.settings

        // Extract potential global meta titles and posters
        val ogTitle = doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?: doc.selectFirst("meta[name=twitter:title]")?.attr("content")
        val ogPoster = doc.selectFirst("meta[property=og:image:secure_url]")?.attr("content")
            ?: doc.selectFirst("meta[property=og:image:url]")?.attr("content")
            ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: doc.selectFirst("meta[name=twitter:image:src]")?.attr("content")
            ?: doc.selectFirst("meta[name=twitter:image]")?.attr("content")

        val widthStr = doc.selectFirst("meta[property=og:video:width]")?.attr("content")
        val heightStr = doc.selectFirst("meta[property=og:video:height]")?.attr("content")
        val width = widthStr?.toIntOrNull()
        val height = heightStr?.toIntOrNull()

        // 1. og:video tags
        val videoMetaSelectors = listOf(
            "meta[property=og:video]",
            "meta[property=og:video:url]",
            "meta[property=og:video:secure_url]",
            "meta[name=twitter:player:stream]",
            "meta[name=twitter:player]"
        )

        for (selector in videoMetaSelectors) {
            for (element in doc.select(selector)) {
                val content = element.attr("content").trim()
                if (content.isNotBlank()) {
                    val normalizedUrl = MediaNormalizer.normalizeUrl(content, pageUrl)
                    if (normalizedUrl != null) {
                        val type = determineType(normalizedUrl)
                        if (isTypeAllowed(type, settings)) {
                            results.add(
                                ExtractedMediaCandidate(
                                    url = normalizedUrl,
                                    mediaType = type,
                                    title = ogTitle,
                                    posterUrl = MediaNormalizer.normalizeUrl(ogPoster ?: "", pageUrl),
                                    sourcePageUrl = pageUrl,
                                    fileExtension = MediaNormalizer.extractExtension(normalizedUrl),
                                    width = width,
                                    height = height,
                                    extractorType = name,
                                    priority = 8
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. og:image if it's a GIF
        if (settings.extractGif && ogPoster != null && ogPoster.contains(".gif", ignoreCase = true)) {
            val normalizedUrl = MediaNormalizer.normalizeUrl(ogPoster, pageUrl)
            if (normalizedUrl != null) {
                results.add(
                    ExtractedMediaCandidate(
                        url = normalizedUrl,
                        mediaType = MediaType.GIF,
                        title = ogTitle,
                        posterUrl = normalizedUrl,
                        sourcePageUrl = pageUrl,
                        fileExtension = "gif",
                        extractorType = name,
                        priority = 7
                    )
                )
            }
        }

        // 3. Microdata itemprop attributes
        val microdataSelectors = listOf(
            "[itemprop=contentUrl]",
            "[itemprop=embedUrl]",
            "[itemprop=video]"
        )
        for (selector in microdataSelectors) {
            for (el in doc.select(selector)) {
                val urlCandidate = el.attr("abs:src").ifBlank {
                    el.attr("src").ifBlank {
                        el.attr("abs:href").ifBlank {
                            el.attr("href").ifBlank { el.attr("content") }
                        }
                    }
                }
                if (urlCandidate.isNotBlank()) {
                    val normalized = MediaNormalizer.normalizeUrl(urlCandidate, pageUrl)
                    if (normalized != null && (isDirectMedia(normalized) || selector.contains("video", ignoreCase = true))) {
                        val type = determineType(normalized)
                        if (isTypeAllowed(type, settings)) {
                            results.add(
                                ExtractedMediaCandidate(
                                    url = normalized,
                                    mediaType = type,
                                    title = ogTitle,
                                    posterUrl = MediaNormalizer.normalizeUrl(ogPoster ?: "", pageUrl),
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
