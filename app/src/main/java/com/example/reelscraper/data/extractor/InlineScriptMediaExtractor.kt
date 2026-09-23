package com.example.reelscraper.data.extractor

import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer
import java.util.regex.Pattern

class InlineScriptMediaExtractor : MediaExtractor {
    override val name: String = "INLINE_SCRIPTS"

    private val playerKeyPatterns = listOf(
        Pattern.compile("""(?:file|source|sources|src|url|video|videoUrl|videoSrc|mediaUrl|mediaSrc|stream|streamUrl|hls|hlsUrl|dash|dashUrl|mpd|m3u8)\s*[:=]\s*["']([^"']+\.(?:mp4|m3u8|mpd|webm|gif)(?:\?[^"']*)?)["']""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:player\.src|player\.setup|video\.load)\s*\(\s*["']([^"']+\.(?:mp4|m3u8|mpd|webm|gif)(?:\?[^"']*)?)["']""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""["'](?:file|source|src|url)["']\s*:\s*["']([^"']+\.(?:mp4|m3u8|mpd|webm|gif)(?:\?[^"']*)?)["']""", Pattern.CASE_INSENSITIVE)
    )

    private val posterKeyPattern = Pattern.compile(
        """(?:poster|thumbnail|image|backgroundImage)\s*[:=]\s*["']([^"']+\.(?:jpg|jpeg|png|webp)(?:\?[^"']*)?)["']""",
        Pattern.CASE_INSENSITIVE
    )

    override suspend fun extract(context: ExtractionContext): List<ExtractedMediaCandidate> {
        val doc = context.document ?: return emptyList()
        val results = mutableListOf<ExtractedMediaCandidate>()
        val pageUrl = context.pageUrl
        val settings = context.settings

        for (script in doc.select("script")) {
            val scriptContent = script.data()
            if (scriptContent.isBlank() || scriptContent.length > settings.maxScriptScanSizeBytes) continue

            // Look for poster if present in same script
            var scriptPoster: String? = null
            val posterMatcher = posterKeyPattern.matcher(scriptContent)
            if (posterMatcher.find()) {
                val rawPoster = posterMatcher.group(1)?.replace("\\/", "/")
                if (!rawPoster.isNullOrBlank()) {
                    scriptPoster = MediaNormalizer.normalizeUrl(rawPoster, pageUrl)
                }
            }

            for (pattern in playerKeyPatterns) {
                val matcher = pattern.matcher(scriptContent)
                while (matcher.find()) {
                    val rawUrl = matcher.group(1)?.replace("\\/", "/") ?: continue
                    val normalizedUrl = MediaNormalizer.normalizeUrl(rawUrl, pageUrl) ?: continue

                    val type = determineType(normalizedUrl)
                    if (isTypeAllowed(type, settings)) {
                        results.add(
                            ExtractedMediaCandidate(
                                url = normalizedUrl,
                                mediaType = type,
                                posterUrl = scriptPoster,
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
