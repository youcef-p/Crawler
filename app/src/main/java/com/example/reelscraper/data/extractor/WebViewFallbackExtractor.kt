package com.example.reelscraper.data.extractor

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections

class WebViewFallbackExtractor {

    suspend fun extractFromPage(
        context: Context,
        pageUrl: String,
        settings: AppSettings
    ): List<ExtractedMediaCandidate> = withContext(Dispatchers.Main) {
        val discoveredUrls = Collections.synchronizedSet(mutableSetOf<String>())
        val completionDeferred = CompletableDeferred<Unit>()
        var webView: WebView? = null

        val timeoutMillis = (settings.maxWebViewWaitSeconds.coerceIn(2, 15) * 1000).toLong()

        try {
            webView = WebView(context.applicationContext).apply {
                val webSettings = this.settings
                webSettings.javaScriptEnabled = true
                webSettings.domStorageEnabled = true
                webSettings.mediaPlaybackRequiresUserGesture = false
                webSettings.loadsImagesAutomatically = false // Keep light
                webSettings.blockNetworkImage = false // Allow gif detection if needed
                webSettings.cacheMode = WebSettings.LOAD_NO_CACHE

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val requestUrl = request?.url?.toString()
                        if (requestUrl != null && isPlayableUrl(requestUrl)) {
                            discoveredUrls.add(requestUrl)
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Run JS queries to extract video/player properties
                        val jsExtractionCode = """
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
                        """.trimIndent()

                        view?.evaluateJavascript(jsExtractionCode) { result ->
                            try {
                                if (!result.isNullOrBlank() && result != "null" && result != "\"[]\"") {
                                    // Strip outer quotes and unescape
                                    val cleaned = result.removeSurrounding("\"")
                                        .replace("\\\"", "\"")
                                        .replace("\\\\", "\\")
                                    val array = org.json.JSONArray(cleaned)
                                    for (i in 0 until array.length()) {
                                        val u = array.optString(i)
                                        if (u.isNotBlank() && isPlayableUrl(u)) {
                                            discoveredUrls.add(u)
                                        }
                                    }
                                }
                            } catch (_: Exception) {
                                // Ignore json parsing errors
                            }
                            // Allow network requests 1 second grace period to settle
                            Handler(Looper.getMainLooper()).postDelayed({
                                completionDeferred.complete(Unit)
                            }, 1000)
                        }
                    }
                }

                loadUrl(pageUrl)
            }

            withTimeoutOrNull(timeoutMillis) {
                completionDeferred.await()
            }
        } catch (_: Exception) {
            // Ignore webview errors
        } finally {
            try {
                webView?.stopLoading()
                webView?.loadUrl("about:blank")
                webView?.clearHistory()
                webView?.destroy()
            } catch (_: Exception) {
                // Ignore cleanup error
            }
        }

        // Convert discovered URLs into candidates
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
        return@withContext candidates
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
