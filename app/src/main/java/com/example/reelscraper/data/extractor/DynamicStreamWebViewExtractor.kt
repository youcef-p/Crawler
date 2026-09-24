package com.example.reelscraper.data.extractor

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.StreamSession
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.Collections
import java.util.Locale

data class DynamicStreamCaptureResult(
    val candidate: ExtractedMediaCandidate,
    val session: StreamSession
)

/**
 * Thread-safe offscreen dynamic stream capture engine.
 * Uses WebViewSession to safely observe network requests, DOM video tags, and player objects
 * without thread violations or lifecycle leaks.
 */
class DynamicStreamWebViewExtractor {

    companion object {
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

        private const val INJECTED_HOOK_JS = """
            (function() {
                if (window.__reelscraper_hooked) return;
                window.__reelscraper_hooked = true;

                function reportMedia(url, type, context) {
                    if (!url || typeof url !== 'string') return;
                    if (url.startsWith('blob:') || url.startsWith('data:')) return;
                    try {
                        if (window.ReelScraperBridge && window.ReelScraperBridge.onMediaFound) {
                            window.ReelScraperBridge.onMediaFound(url, type, context || '');
                        }
                    } catch(e) {}
                }

                // Hook fetch
                const origFetch = window.fetch;
                if (origFetch) {
                    window.fetch = function(input, init) {
                        const url = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
                        if (url && (url.includes('.m3u8') || url.includes('.mpd') || url.includes('.mp4') || url.includes('.webm') || url.includes('.gif'))) {
                            reportMedia(url, 'FETCH', 'network_fetch');
                        }
                        return origFetch.apply(this, arguments);
                    };
                }

                // Hook XMLHttpRequest
                const origOpen = XMLHttpRequest.prototype.open;
                if (origOpen) {
                    XMLHttpRequest.prototype.open = function(method, url) {
                        if (typeof url === 'string' && (url.includes('.m3u8') || url.includes('.mpd') || url.includes('.mp4') || url.includes('.webm') || url.includes('.gif'))) {
                            reportMedia(url, 'XHR', method);
                        }
                        return origOpen.apply(this, arguments);
                    };
                }

                // Hook HTMLMediaElement src assignment
                const desc = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'src');
                if (desc && desc.set) {
                    const origSet = desc.set;
                    Object.defineProperty(HTMLMediaElement.prototype, 'src', {
                        set: function(val) {
                            reportMedia(val, 'MEDIA_SRC', 'html5_media');
                            return origSet.call(this, val);
                        },
                        get: desc.get
                    });
                }

                // Periodic player inspection
                setInterval(function() {
                    try {
                        const videos = document.querySelectorAll('video');
                        videos.forEach(v => {
                            if (v.src) reportMedia(v.src, 'DOM_VIDEO', v.title || document.title);
                            if (v.currentSrc) reportMedia(v.currentSrc, 'DOM_VIDEO', v.title || document.title);
                            v.querySelectorAll('source').forEach(s => {
                                if (s.src) reportMedia(s.src, 'DOM_SOURCE', s.type || '');
                            });
                        });

                        if (window.jwplayer && typeof window.jwplayer === 'function') {
                            try {
                                const p = window.jwplayer();
                                if (p && p.getPlaylist) {
                                    const pl = p.getPlaylist();
                                    if (Array.isArray(pl) && pl[0] && pl[0].file) {
                                        reportMedia(pl[0].file, 'JWPLAYER', pl[0].title || '');
                                    }
                                }
                            } catch(e) {}
                        }

                        if (window.videojs && typeof window.videojs.getAllPlayers === 'function') {
                            try {
                                const players = window.videojs.getAllPlayers();
                                players.forEach(p => {
                                    const src = p.src();
                                    if (src) reportMedia(src, 'VIDEOJS', '');
                                });
                            } catch(e) {}
                        }

                        if (window.hls && window.hls.url) {
                            reportMedia(window.hls.url, 'HLS_JS', 'hls.js');
                        }
                    } catch(e) {}
                }, 1000);
            })();
        """
    }

    suspend fun captureDynamicStreams(
        context: Context,
        pageUrl: String,
        settings: AppSettings,
        userAgent: String = DESKTOP_USER_AGENT
    ): List<DynamicStreamCaptureResult> = withContext(Dispatchers.IO) {
        val results = Collections.synchronizedList(mutableListOf<DynamicStreamCaptureResult>())
        val seenUrls = Collections.synchronizedSet(mutableSetOf<String>())
        val completion = CompletableDeferred<Unit>()

        var session: WebViewSession? = null

        withContext(Dispatchers.Main.immediate) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)

            session = WebViewSession(
                context = context,
                initialUrl = pageUrl,
                customUserAgent = userAgent,
                onStreamDiscovered = { item ->
                    handleDiscoveredStream(
                        streamUrl = item.url,
                        detectionType = item.source,
                        pageUrl = pageUrl,
                        userAgent = item.userAgent ?: userAgent,
                        cookieManager = cookieManager,
                        results = results,
                        seenUrls = seenUrls,
                        refererOverride = item.referer,
                        explicitHeaders = item.requestHeaders
                    )
                }
            )

            session?.initialize(
                enableJs = true,
                enableDomStorage = true,
                blockImages = false,
                injectedHookScript = INJECTED_HOOK_JS,
                onPageLoadFinished = {
                    // Give streams 2 seconds to trigger before finishing early if found
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!completion.isCompleted) {
                            completion.complete(Unit)
                        }
                    }, 2500)
                }
            )

            val timeoutMs = (settings.maxWebViewWaitSeconds.coerceIn(3, 15) * 1000).toLong()
            Handler(Looper.getMainLooper()).postDelayed({
                if (!completion.isCompleted) {
                    completion.complete(Unit)
                }
            }, timeoutMs)
        }

        try {
            withTimeoutOrNull((settings.maxWebViewWaitSeconds + 3) * 1000L) {
                completion.await()
            }
        } finally {
            withContext(Dispatchers.Main.immediate) {
                session?.dispose()
            }
        }

        return@withContext results.toList()
    }

    private fun handleDiscoveredStream(
        streamUrl: String,
        detectionType: String,
        pageUrl: String,
        userAgent: String,
        cookieManager: CookieManager,
        results: MutableList<DynamicStreamCaptureResult>,
        seenUrls: MutableSet<String>,
        refererOverride: String? = null,
        explicitHeaders: Map<String, String>? = null
    ) {
        val cleanUrl = streamUrl.trim()
        if (cleanUrl.isBlank() || !seenUrls.add(cleanUrl)) return
        if (!isPlayableStream(cleanUrl)) return

        val mediaType = determineMediaType(cleanUrl)
        val ext = MediaNormalizer.extractExtension(cleanUrl)
        val normalizedName = MediaNormalizer.normalizeMediaName(cleanUrl, pageUrl = pageUrl)

        val headersMap = mutableMapOf<String, String>()
        val referer = refererOverride ?: pageUrl
        headersMap["Referer"] = referer
        headersMap["User-Agent"] = userAgent

        if (explicitHeaders != null) {
            for ((k, v) in explicitHeaders) {
                if (!k.equals("Host", ignoreCase = true) && !k.equals("Cookie", ignoreCase = true)) {
                    headersMap[k] = v
                }
            }
        }

        val cookie = try {
            cookieManager.getCookie(cleanUrl) ?: cookieManager.getCookie(pageUrl)
        } catch (_: Exception) {
            null
        }

        val headersJson = JSONObject(headersMap as Map<*, *>).toString()

        val candidate = ExtractedMediaCandidate(
            url = cleanUrl,
            mediaType = mediaType,
            title = normalizedName.replace('-', ' ').replace('_', ' ')
                .split(' ')
                .filter { it.isNotBlank() }
                .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } },
            posterUrl = null,
            sourcePageUrl = pageUrl,
            fileExtension = ext,
            extractorType = "DYNAMIC_$detectionType",
            priority = 10
        )

        val session = StreamSession(
            sourcePageUrl = pageUrl,
            streamUrl = cleanUrl,
            streamType = mediaType.name,
            requestHeadersJson = headersJson,
            referer = referer,
            userAgent = userAgent,
            cookieHeader = cookie,
            refreshPolicy = "ON_FAILURE"
        )

        results.add(DynamicStreamCaptureResult(candidate, session))
    }

    private fun isPlayableStream(url: String): Boolean {
        val lower = url.lowercase(Locale.US)
        if (lower.startsWith("blob:") || lower.startsWith("data:")) return false
        return lower.contains(".m3u8") || lower.contains(".mpd") ||
                lower.contains(".mp4") || lower.contains(".webm") ||
                lower.contains(".gif") || lower.contains("/hls/") ||
                lower.contains("/manifest") || lower.contains("master.m3u8")
    }

    private fun determineMediaType(url: String): MediaType {
        val lower = url.lowercase(Locale.US)
        return when {
            lower.contains(".m3u8") || lower.contains("/hls/") -> MediaType.HLS
            lower.contains(".mpd") || lower.contains("/dash/") -> MediaType.DASH
            lower.contains(".gif") -> MediaType.GIF
            else -> MediaType.VIDEO
        }
    }
}
