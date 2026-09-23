package com.example.reelscraper.data.extractor

import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer
import org.json.JSONArray
import org.json.JSONObject

class JsonLdMediaExtractor : MediaExtractor {
    override val name: String = "JSON_LD"

    override suspend fun extract(context: ExtractionContext): List<ExtractedMediaCandidate> {
        val doc = context.document ?: return emptyList()
        val results = mutableListOf<ExtractedMediaCandidate>()
        val pageUrl = context.pageUrl
        val settings = context.settings

        for (script in doc.select("script[type=application/ld+json]")) {
            val jsonText = script.data().trim()
            if (jsonText.isBlank() || jsonText.length > settings.maxJsonScanSizeBytes) continue

            try {
                if (jsonText.startsWith("[")) {
                    val array = JSONArray(jsonText)
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i)
                        if (item != null) parseJsonLdObject(item, pageUrl, settings, results)
                    }
                } else if (jsonText.startsWith("{")) {
                    val obj = JSONObject(jsonText)
                    parseJsonLdObject(obj, pageUrl, settings, results)
                }
            } catch (_: Exception) {
                // Ignore parse errors on malformed JSON-LD
            }
        }

        return results
    }

    private fun parseJsonLdObject(
        obj: JSONObject,
        pageUrl: String,
        settings: AppSettings,
        outList: MutableList<ExtractedMediaCandidate>
    ) {
        val type = obj.optString("@type", "")
        val graph = obj.optJSONArray("@graph")
        if (graph != null) {
            for (i in 0 until graph.length()) {
                val subObj = graph.optJSONObject(i)
                if (subObj != null) parseJsonLdObject(subObj, pageUrl, settings, outList)
            }
        }

        val isVideoType = type.contains("Video", ignoreCase = true) ||
                type.contains("Media", ignoreCase = true)
        val isImageType = type.contains("Image", ignoreCase = true)

        val contentUrl = obj.optString("contentUrl").ifBlank {
            obj.optString("embedUrl").ifBlank {
                obj.optString("url")
            }
        }

        val name = obj.optString("name").ifBlank { obj.optString("headline") }
        val thumbnailUrl = obj.optString("thumbnailUrl").ifBlank {
            val imageObj = obj.opt("image")
            if (imageObj is String) imageObj else if (imageObj is JSONObject) imageObj.optString("url") else ""
        }

        if (contentUrl.isNotBlank()) {
            val normalizedUrl = MediaNormalizer.normalizeUrl(contentUrl, pageUrl)
            if (normalizedUrl != null && (isVideoType || isDirectMedia(normalizedUrl))) {
                val mediaType = determineType(normalizedUrl)
                if (isTypeAllowed(mediaType, settings)) {
                    outList.add(
                        ExtractedMediaCandidate(
                            url = normalizedUrl,
                            mediaType = mediaType,
                            title = name.ifBlank { null },
                            posterUrl = MediaNormalizer.normalizeUrl(thumbnailUrl, pageUrl),
                            sourcePageUrl = pageUrl,
                            fileExtension = MediaNormalizer.extractExtension(normalizedUrl),
                            extractorType = this.name,
                            priority = 10 // JSON-LD has highest priority for official structured titles
                        )
                    )
                }
            }
        }
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
