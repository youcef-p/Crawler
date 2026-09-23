package com.example.reelscraper.data.extractor

import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer

class HtmlTagMediaExtractor : MediaExtractor {
    override val name: String = "HTML_TAGS"

    override suspend fun extract(context: ExtractionContext): List<ExtractedMediaCandidate> {
        val doc = context.document ?: return emptyList()
        val results = mutableListOf<ExtractedMediaCandidate>()
        val pageUrl = context.pageUrl
        val settings = context.settings

        // 1. <video> elements
        for (video in doc.select("video")) {
            val videoPoster = video.attr("abs:poster").ifBlank { video.attr("poster") }
            val videoTitle = video.attr("title").ifBlank { video.attr("aria-label") }

            // video direct src
            val directSrc = video.attr("abs:src").ifBlank { video.attr("src") }
            if (directSrc.isNotBlank()) {
                val normalizedUrl = MediaNormalizer.normalizeUrl(directSrc, pageUrl)
                if (normalizedUrl != null) {
                    val type = determineType(normalizedUrl, null)
                    if (isTypeAllowed(type, settings)) {
                        results.add(
                            ExtractedMediaCandidate(
                                url = normalizedUrl,
                                mediaType = type,
                                title = videoTitle.ifBlank { null },
                                posterUrl = MediaNormalizer.normalizeUrl(videoPoster, pageUrl),
                                sourcePageUrl = pageUrl,
                                fileExtension = MediaNormalizer.extractExtension(normalizedUrl),
                                extractorType = name,
                                priority = 10
                            )
                        )
                    }
                }
            }

            // <source> children inside video
            for (source in video.select("source")) {
                val src = source.attr("abs:src").ifBlank { source.attr("src") }
                val mimeType = source.attr("type")
                if (src.isNotBlank()) {
                    val normalizedUrl = MediaNormalizer.normalizeUrl(src, pageUrl)
                    if (normalizedUrl != null) {
                        val type = determineType(normalizedUrl, mimeType)
                        if (isTypeAllowed(type, settings)) {
                            results.add(
                                ExtractedMediaCandidate(
                                    url = normalizedUrl,
                                    mediaType = type,
                                    title = videoTitle.ifBlank { null },
                                    posterUrl = MediaNormalizer.normalizeUrl(videoPoster, pageUrl),
                                    sourcePageUrl = pageUrl,
                                    fileExtension = MediaNormalizer.extractExtension(normalizedUrl),
                                    extractorType = name,
                                    priority = 9
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Standalone <source> tags
        for (source in doc.select("source")) {
            if (source.parent()?.tagName() == "video") continue
            val src = source.attr("abs:src").ifBlank { source.attr("src") }
            val mimeType = source.attr("type")
            if (src.isNotBlank()) {
                val normalizedUrl = MediaNormalizer.normalizeUrl(src, pageUrl)
                if (normalizedUrl != null) {
                    val type = determineType(normalizedUrl, mimeType)
                    if (isTypeAllowed(type, settings)) {
                        results.add(
                            ExtractedMediaCandidate(
                                url = normalizedUrl,
                                mediaType = type,
                                title = source.attr("title").ifBlank { null },
                                sourcePageUrl = pageUrl,
                                fileExtension = MediaNormalizer.extractExtension(normalizedUrl),
                                extractorType = name,
                                priority = 8
                            )
                        )
                    }
                }
            }
        }

        // 3. <img> tags ending with .gif
        if (settings.extractGif) {
            for (img in doc.select("img")) {
                val src = img.attr("abs:src").ifBlank { img.attr("src") }
                if (src.contains(".gif", ignoreCase = true)) {
                    val normalizedUrl = MediaNormalizer.normalizeUrl(src, pageUrl)
                    if (normalizedUrl != null && normalizedUrl.contains(".gif", ignoreCase = true)) {
                        val altTitle = img.attr("alt").ifBlank { img.attr("title") }
                        results.add(
                            ExtractedMediaCandidate(
                                url = normalizedUrl,
                                mediaType = MediaType.GIF,
                                title = altTitle.ifBlank { null },
                                posterUrl = normalizedUrl,
                                sourcePageUrl = pageUrl,
                                fileExtension = "gif",
                                extractorType = name,
                                priority = 6
                            )
                        )
                    }
                }
            }

            // <picture><source srcset="..."> for gif
            for (source in doc.select("picture source")) {
                val srcset = source.attr("abs:srcset").ifBlank { source.attr("srcset") }
                if (srcset.contains(".gif", ignoreCase = true)) {
                    val firstUrl = srcset.split(',').firstOrNull()?.trim()?.split(' ')?.firstOrNull() ?: ""
                    val normalized = MediaNormalizer.normalizeUrl(firstUrl, pageUrl)
                    if (normalized != null && normalized.contains(".gif", ignoreCase = true)) {
                        results.add(
                            ExtractedMediaCandidate(
                                url = normalized,
                                mediaType = MediaType.GIF,
                                sourcePageUrl = pageUrl,
                                fileExtension = "gif",
                                extractorType = name,
                                priority = 5
                            )
                        )
                    }
                }
            }
        }

        // 4. <a> and <a download="..."> tags pointing to playable media
        for (a in doc.select("a[href]")) {
            val href = a.attr("abs:href").ifBlank { a.attr("href") }
            val download = a.attr("download")
            val candidate = href.ifBlank { download }
            if (candidate.isNotBlank()) {
                val normalizedUrl = MediaNormalizer.normalizeUrl(candidate, pageUrl)
                if (normalizedUrl != null && isDirectMediaExtension(normalizedUrl)) {
                    val type = determineType(normalizedUrl, null)
                    if (isTypeAllowed(type, settings)) {
                        val anchorText = a.text().trim()
                        val anchorTitle = a.attr("title").ifBlank { anchorText }
                        results.add(
                            ExtractedMediaCandidate(
                                url = normalizedUrl,
                                mediaType = type,
                                title = anchorTitle.ifBlank { null },
                                sourcePageUrl = pageUrl,
                                fileExtension = MediaNormalizer.extractExtension(normalizedUrl),
                                extractorType = name,
                                priority = 7
                            )
                        )
                    }
                }
            }
        }

        // 5. <embed> and <object>
        for (embed in doc.select("embed[src]")) {
            val src = embed.attr("abs:src").ifBlank { embed.attr("src") }
            val normalizedUrl = MediaNormalizer.normalizeUrl(src, pageUrl)
            if (normalizedUrl != null && isDirectMediaExtension(normalizedUrl)) {
                val type = determineType(normalizedUrl, embed.attr("type"))
                if (isTypeAllowed(type, settings)) {
                    results.add(
                        ExtractedMediaCandidate(
                            url = normalizedUrl,
                            mediaType = type,
                            sourcePageUrl = pageUrl,
                            fileExtension = MediaNormalizer.extractExtension(normalizedUrl),
                            extractorType = name,
                            priority = 5
                        )
                    )
                }
            }
        }

        for (obj in doc.select("object[data]")) {
            val data = obj.attr("abs:data").ifBlank { obj.attr("data") }
            val normalizedUrl = MediaNormalizer.normalizeUrl(data, pageUrl)
            if (normalizedUrl != null && isDirectMediaExtension(normalizedUrl)) {
                val type = determineType(normalizedUrl, obj.attr("type"))
                if (isTypeAllowed(type, settings)) {
                    results.add(
                        ExtractedMediaCandidate(
                            url = normalizedUrl,
                            mediaType = type,
                            sourcePageUrl = pageUrl,
                            fileExtension = MediaNormalizer.extractExtension(normalizedUrl),
                            extractorType = name,
                            priority = 5
                        )
                    )
                }
            }
        }

        return results
    }

    private fun isDirectMediaExtension(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".mp4") || lower.contains(".webm") ||
                lower.contains(".m3u8") || lower.contains(".mpd") ||
                lower.contains(".gif")
    }

    private fun determineType(url: String, mimeType: String?): MediaType {
        val lowerMime = mimeType?.lowercase() ?: ""
        val lowerUrl = url.lowercase()
        return when {
            lowerMime.contains("mpegurl") || lowerMime.contains("hls") || lowerUrl.contains(".m3u8") -> MediaType.HLS
            lowerMime.contains("dash") || lowerUrl.contains(".mpd") -> MediaType.DASH
            lowerMime.contains("gif") || lowerUrl.contains(".gif") -> MediaType.GIF
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
