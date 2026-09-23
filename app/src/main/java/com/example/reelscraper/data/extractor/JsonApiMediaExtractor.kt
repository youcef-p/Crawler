package com.example.reelscraper.data.extractor

import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer
import org.json.JSONArray
import org.json.JSONObject

class JsonApiMediaExtractor : MediaExtractor {
    override val name: String = "JSON_API"

    private val mediaKeys = setOf(
        "url", "src", "source", "file", "video", "videourl", "mediaurl",
        "stream", "hls", "dash", "mpd", "m3u8", "gif"
    )

    override suspend fun extract(context: ExtractionContext): List<ExtractedMediaCandidate> {
        val html = context.html.trim()
        val contentType = context.contentType?.lowercase() ?: ""
        val isJson = contentType.contains("application/json") ||
                contentType.contains("text/json") ||
                ((html.startsWith("{") && html.endsWith("}")) || (html.startsWith("[") && html.endsWith("]")))

        if (!isJson || html.length > context.settings.maxJsonScanSizeBytes) return emptyList()

        val results = mutableListOf<ExtractedMediaCandidate>()
        val pageUrl = context.pageUrl
        val settings = context.settings

        try {
            if (html.startsWith("[")) {
                val array = JSONArray(html)
                scanJsonArray(array, pageUrl, settings, results, 0)
            } else {
                val obj = JSONObject(html)
                scanJsonObject(obj, pageUrl, settings, results, 0)
            }
        } catch (_: Exception) {
            // Ignore parse errors
        }

        return results
    }

    private fun scanJsonObject(
        obj: JSONObject,
        pageUrl: String,
        settings: AppSettings,
        outList: MutableList<ExtractedMediaCandidate>,
        depth: Int
    ) {
        if (depth > 6 || outList.size >= 50) return

        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val lowerKey = key.lowercase()
            when (val value = obj.opt(key)) {
                is String -> {
                    if (mediaKeys.contains(lowerKey) || isDirectMedia(value)) {
                        val normalized = MediaNormalizer.normalizeUrl(value, pageUrl)
                        if (normalized != null && isDirectMedia(normalized)) {
                            val type = determineType(normalized)
                            if (isTypeAllowed(type, settings)) {
                                val title = obj.optString("title").ifBlank { obj.optString("name") }
                                val poster = obj.optString("poster").ifBlank { obj.optString("thumbnail") }
                                outList.add(
                                    ExtractedMediaCandidate(
                                        url = normalized,
                                        mediaType = type,
                                        title = title.ifBlank { null },
                                        posterUrl = MediaNormalizer.normalizeUrl(poster, pageUrl),
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
                is JSONObject -> scanJsonObject(value, pageUrl, settings, outList, depth + 1)
                is JSONArray -> scanJsonArray(value, pageUrl, settings, outList, depth + 1)
            }
        }
    }

    private fun scanJsonArray(
        array: JSONArray,
        pageUrl: String,
        settings: AppSettings,
        outList: MutableList<ExtractedMediaCandidate>,
        depth: Int
    ) {
        if (depth > 6 || outList.size >= 50) return
        for (i in 0 until array.length().coerceAtMost(50)) {
            when (val item = array.opt(i)) {
                is JSONObject -> scanJsonObject(item, pageUrl, settings, outList, depth + 1)
                is JSONArray -> scanJsonArray(item, pageUrl, settings, outList, depth + 1)
                is String -> {
                    if (isDirectMedia(item)) {
                        val normalized = MediaNormalizer.normalizeUrl(item, pageUrl)
                        if (normalized != null) {
                            val type = determineType(normalized)
                            if (isTypeAllowed(type, settings)) {
                                outList.add(
                                    ExtractedMediaCandidate(
                                        url = normalized,
                                        mediaType = type,
                                        sourcePageUrl = pageUrl,
                                        fileExtension = MediaNormalizer.extractExtension(normalized),
                                        extractorType = name,
                                        priority = 6
                                    )
                                )
                            }
                        }
                    }
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
