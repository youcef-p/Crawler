package com.example.reelscraper.data.extractor

import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.settings.AppSettings
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale

class ContentTypeSniffer(private val client: OkHttpClient) {

    suspend fun sniffMediaUrl(url: String, settings: AppSettings): Pair<MediaType, String>? {
        if (!settings.enableContentTypeSniffing) return null

        try {
            // First try lightweight HEAD request
            val headRequest = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) Chrome/126.0.0.0")
                .build()

            val headResponse = client.newCall(headRequest).execute()
            var contentType = headResponse.header("Content-Type")?.lowercase(Locale.US)
            headResponse.close()

            // If HEAD failed or gave no content-type, try Range GET bytes=0-0
            if (contentType.isNullOrBlank() || contentType.contains("text/html")) {
                val rangeRequest = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) Chrome/126.0.0.0")
                    .header("Range", "bytes=0-0")
                    .build()

                val rangeResponse = client.newCall(rangeRequest).execute()
                contentType = rangeResponse.header("Content-Type")?.lowercase(Locale.US)
                rangeResponse.close()
            }

            if (!contentType.isNullOrBlank()) {
                val mediaType = when {
                    contentType.contains("application/vnd.apple.mpegurl") || contentType.contains("audio/mpegurl") -> MediaType.HLS
                    contentType.contains("application/dash+xml") -> MediaType.DASH
                    contentType.contains("image/gif") -> MediaType.GIF
                    contentType.contains("video/mp4") -> MediaType.VIDEO
                    contentType.contains("video/webm") -> MediaType.VIDEO
                    contentType.contains("video/") -> MediaType.VIDEO
                    else -> null
                }

                if (mediaType != null) {
                    val ext = when (mediaType) {
                        MediaType.HLS -> "m3u8"
                        MediaType.DASH -> "mpd"
                        MediaType.GIF -> "gif"
                        MediaType.VIDEO -> if (contentType.contains("webm")) "webm" else "mp4"
                    }
                    return Pair(mediaType, ext)
                }
            }
        } catch (_: Exception) {
            // Sniff failed
        }
        return null
    }
}
