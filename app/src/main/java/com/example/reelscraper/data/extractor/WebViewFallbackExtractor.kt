package com.example.reelscraper.data.extractor

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.util.Collections

/**
 * Thread-safe fallback extractor using WebViewSession to extract media tags, JS players, and network assets.
 */
class WebViewFallbackExtractor {

    companion object {
        private const val JS_EXTRACTION_CODE = """
            (function() {
                var urls = [];
                try {
                    // 1. Video elements & sources
                    document.querySelectorAll('video').forEach(function(v) {
                        if (v.currentSrc) urls.push(v.currentSrc);
                        if (v.src) urls.push(v.src);
                    });
                    document.querySelectorAll('video source').forEach(function(s) {
                        if (s.src) urls.push(s.src);
                    });
                    document.querySelectorAll('img[src*=".gif"]').forEach(function(g) {
                        if (g.src) urls.push(g.src);
                    });
                    // 2. Performance resource entries
                    if (window.performance && performance.getEntriesByType) {
                        performance.getEntriesByType('resource').forEach(function(r) {
                            if (r.name && (r.name.indexOf('.mp4') !== -1 || r.name.indexOf('.m3u8') !== -1 || r.name.indexOf('.mpd') !== -1 || r.name.indexOf('.webm') !== -1 || r.name.indexOf('.gif') !== -1)) {
                                urls.push(r.name);
                            }
                        });
                    }
                    // 3. Player instances
                    if (window.player && window.player.src) urls.push(window.player.src());
                    if (window.jwplayer && typeof window.jwplayer === 'function') {
                        try { var p = jwplayer(); if (p && p.getPlaylist) { var pl = p.getPlaylist(); if (pl && pl[0] && pl[0].file) urls.push(pl[0].file); } } catch(e){}
                    }
                } catch(e) {}
                return JSON.stringify(urls);
            })();
        """
    }

    suspend fun extractFromPage(
        context: Context,
        pageUrl: String,
        settings: AppSettings
    ): List<ExtractedMediaCandidate> = withContext(Dispatchers.IO) {
        val discoveredUrls = Collections.synchronizedSet(mutableSetOf<String>())
        val completionDeferred = CompletableDeferred<Unit>()
        var session: WebViewSession? = null

        val timeoutMillis = (settings.maxWebViewWaitSeconds.coerceIn(2, 15) * 1000).toLong()

        withContext(Dispatchers.Main.immediate) {
            session = WebViewSession(
                context = context,
                initialUrl = pageUrl,
                onStreamDiscovered = { item ->
                    if (isPlayableUrl(item.url)) {
                        discoveredUrls.add(item.url)
                    }
                }
            )

            session?.initialize(
                enableJs = true,
                enableDomStorage = true,
                blockImages = false,
                onPageLoadFinished = {
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Evaluate DOM scripts safely on the main thread
                        val mainScope = CoroutineScope(Dispatchers.Main.immediate)
                        mainScope.launch {
                            val result = session?.evaluateJs(JS_EXTRACTION_CODE, timeoutMs = 2000L)
                            if (!result.isNullOrBlank() && result != "null" && result != "\"[]\"") {
                                try {
                                    val cleaned = result.removeSurrounding("\"")
                                        .replace("\\\"", "\"")
                                        .replace("\\\\", "\\")
                                    val array = JSONArray(cleaned)
                                    for (i in 0 until array.length()) {
                                        val u = array.optString(i)
                                        if (u.isNotBlank() && isPlayableUrl(u)) {
                                            discoveredUrls.add(u)
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            if (!completionDeferred.isCompleted) {
                                completionDeferred.complete(Unit)
                            }
                        }
                    }, 1000)
                }
            )
        }

        try {
            withTimeoutOrNull(timeoutMillis) {
                completionDeferred.await()
            }
        } finally {
            withContext(Dispatchers.Main.immediate) {
                session?.dispose()
            }
        }

        val candidates = mutableListOf<ExtractedMediaCandidate>()
        for (url in discoveredUrls) {
            val normalized = MediaNormalizer.normalizeUrl(url, pageUrl) ?: continue
            val type = determineType(normalized)
            if (isTypeAllowed(type, settings)) {
                candidates.add(
                    ExtractedMediaCandidate(
                        url = normalized,
                        mediaType = type,
                        sourcePageUrl = pageUrl,
                        fileExtension = MediaNormalizer.extractExtension(normalized),
                        extractorType = "WEBVIEW_FALLBACK",
                        priority = 9
                    )
                )
            }
        }
        candidates
    }

    private fun isPlayableUrl(url: String): Boolean {
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
