package com.example.reelscraper.data.util

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

object MediaNormalizer {

    private val GENERIC_NAMES = setOf(
        "video", "master", "manifest", "stream", "index", "playlist", "source",
        "output", "chunk", "media", "play", "default", "movie", "clip", "preview",
        "segment", "dash", "hls", "prog", "track", "main", "hd", "sd", "720p", "1080p", "480p"
    )

    private val MEDIA_EXTENSIONS = setOf(
        "mp4", "webm", "m3u8", "mpd", "gif", "mov", "mkv", "avi", "flv", "m4v"
    )

    /**
     * Normalizes a media name from URL, fallback to page title or page URL.
     * Rules:
     * - last path segment of the media URL
     * - remove file extension
     * - decode URL encoding
     * - remove query parameters
     * - convert to lowercase
     * - trim whitespace
     * - collapse repeated spaces/underscores/hyphens
     */
    fun normalizeMediaName(
        mediaUrl: String,
        pageTitle: String? = null,
        pageUrl: String? = null
    ): String {
        var rawName = extractLastPathSegment(mediaUrl)

        // If rawName is empty or is purely generic, fallback to page title or page URL
        val baseCandidate = cleanNameString(rawName)
        if (baseCandidate.isBlank() || GENERIC_NAMES.contains(baseCandidate)) {
            if (!pageTitle.isNullOrBlank()) {
                val cleanedTitle = cleanNameString(pageTitle)
                if (cleanedTitle.isNotBlank()) {
                    return cleanedTitle
                }
            }
            if (!pageUrl.isNullOrBlank()) {
                val pageSegment = cleanNameString(extractLastPathSegment(pageUrl))
                if (pageSegment.isNotBlank() && !GENERIC_NAMES.contains(pageSegment)) {
                    return pageSegment
                }
            }
        }

        return if (baseCandidate.isNotBlank()) baseCandidate else "media_" + Math.abs(mediaUrl.hashCode())
    }

    private fun extractLastPathSegment(urlString: String): String {
        try {
            val uri = URI(urlString.trim())
            val path = uri.path ?: urlString.substringBefore('?').substringBefore('#')
            val segments = path.split('/').filter { it.isNotBlank() }
            if (segments.isNotEmpty()) {
                return segments.last()
            }
        } catch (_: Exception) {
            val withoutQuery = urlString.substringBefore('?').substringBefore('#')
            val lastSlash = withoutQuery.lastIndexOf('/')
            if (lastSlash != -1 && lastSlash < withoutQuery.length - 1) {
                return withoutQuery.substring(lastSlash + 1)
            }
        }
        return ""
    }

    fun cleanNameString(input: String): String {
        var result = input
        try {
            result = URLDecoder.decode(result, StandardCharsets.UTF_8.name())
        } catch (_: Exception) {
            // Ignore if decode fails
        }

        // Remove query parameters or fragment if present
        result = result.substringBefore('?').substringBefore('#')

        // Remove known file extensions
        val dotIndex = result.lastIndexOf('.')
        if (dotIndex != -1) {
            val ext = result.substring(dotIndex + 1).lowercase(Locale.US)
            if (MEDIA_EXTENSIONS.contains(ext) || ext.length in 2..5) {
                result = result.substring(0, dotIndex)
            }
        }

        // Lowercase, replace underscores/hyphens/dots/spaces with single hyphen
        result = result.lowercase(Locale.US)
            .replace(Regex("[_\\s\\.\\-]+"), "-")
            .replace(Regex("[^a-z0-9\\-]"), "")
            .trim('-')

        return result
    }

    /**
     * Normalizes a source website / domain:
     * - extracts host
     * - removes "www."
     * - lowercase
     */
    fun normalizeDomain(urlString: String): String {
        if (urlString.isBlank()) return "unknown"
        try {
            var url = urlString.trim()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            val uri = URI(url)
            val host = uri.host ?: return "unknown"
            var domain = host.lowercase(Locale.US)
            if (domain.startsWith("www.")) {
                domain = domain.substring(4)
            }
            return domain
        } catch (_: Exception) {
            val cleaned = urlString.removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .substringBefore('/')
                .substringBefore(':')
                .lowercase(Locale.US)
            return cleaned.ifBlank { "unknown" }
        }
    }

    /**
     * Normalizes and resolves a URL against a base URL.
     * Skips non-HTTP/HTTPS, javascript:, mailto:, tel:, etc.
     */
    fun normalizeUrl(rawUrl: String, baseUrl: String? = null): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return null

        // Check for unsupported schemes
        val lower = trimmed.lowercase(Locale.US)
        if (lower.startsWith("javascript:") || lower.startsWith("mailto:") ||
            lower.startsWith("tel:") || lower.startsWith("data:") ||
            lower.startsWith("blob:") || lower.startsWith("about:")
        ) {
            return null
        }

        var candidate = trimmed
        // Handle protocol-relative URL
        if (candidate.startsWith("//")) {
            candidate = "https:$candidate"
        }

        // Decode %2F or %3A in URL if accidentally double-encoded
        if (candidate.contains("%3A", ignoreCase = true) || candidate.contains("%2F", ignoreCase = true)) {
            try {
                if (candidate.startsWith("http%3A", ignoreCase = true) || candidate.startsWith("https%3A", ignoreCase = true)) {
                    candidate = URLDecoder.decode(candidate, StandardCharsets.UTF_8.name())
                }
            } catch (_: Exception) {
                // Keep candidate
            }
        }

        // Handle escaped slashes https:\/\/
        if (candidate.contains("\\/")) {
            candidate = candidate.replace("\\/", "/")
        }

        // If relative URL, resolve against baseUrl
        if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) {
            if (baseUrl.isNullOrBlank()) return null
            try {
                val baseUri = URI(baseUrl)
                val resolvedUri = baseUri.resolve(candidate)
                candidate = resolvedUri.toString()
            } catch (_: Exception) {
                return null
            }
        }

        return try {
            val uri = URI(candidate)
            if (uri.scheme != "http" && uri.scheme != "https") null else uri.normalize().toString()
        } catch (_: Exception) {
            if (candidate.startsWith("http://") || candidate.startsWith("https://")) candidate else null
        }
    }

    /**
     * Detects media file extension from URL or mime type.
     */
    fun extractExtension(url: String): String {
        val withoutQuery = url.substringBefore('?').substringBefore('#')
        val lastDot = withoutQuery.lastIndexOf('.')
        if (lastDot != -1 && lastDot < withoutQuery.length - 1) {
            val ext = withoutQuery.substring(lastDot + 1).lowercase(Locale.US)
            if (ext.length in 2..5) {
                return ext
            }
        }
        return when {
            url.contains(".m3u8", ignoreCase = true) -> "m3u8"
            url.contains(".mpd", ignoreCase = true) -> "mpd"
            url.contains(".mp4", ignoreCase = true) -> "mp4"
            url.contains(".webm", ignoreCase = true) -> "webm"
            url.contains(".gif", ignoreCase = true) -> "gif"
            else -> ""
        }
    }
}
