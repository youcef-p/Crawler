package com.example.reelscraper.data.extractor

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Data container for captured network/DOM streams, safe across threads.
 */
data class DiscoveredStreamItem(
    val url: String,
    val source: String,
    val mimeType: String = "",
    val pageUrl: String = "",
    val requestHeaders: Map<String, String> = emptyMap(),
    val referer: String? = null,
    val userAgent: String? = null
)

/**
 * Centralized, thread-safe WebView session manager.
 *
 * Guarantees:
 * 1. WebView creation, navigation, and destruction run EXCLUSIVELY on the main thread.
 * 2. Background callbacks (shouldInterceptRequest, onLoadResource) NEVER touch the WebView instance.
 * 3. Current page URL is tracked via an AtomicReference updated ONLY from main-thread callbacks
 *    (onPageStarted, onPageFinished, doUpdateVisitedHistory).
 * 4. Interceptions are wrapped in try-catch to prevent Chromium thread exceptions from crashing the app.
 * 5. Safe evaluateJavascript with timeout and deferred completion.
 * 6. Idempotent, safe disposal on main thread with state guard.
 */
class WebViewSession(
    private val context: Context,
    private val initialUrl: String,
    private val customUserAgent: String? = null,
    private val onStreamDiscovered: ((DiscoveredStreamItem) -> Unit)? = null
) {
    companion object {
        private const val TAG = "WebViewSession"
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val isDisposed = AtomicBoolean(false)
    private val currentPageUrl = AtomicReference<String>(initialUrl)
    private val detectedQueue = ConcurrentLinkedQueue<DiscoveredStreamItem>()

    @Volatile
    private var webView: WebView? = null

    val currentUrl: String
        get() = currentPageUrl.get() ?: initialUrl

    val capturedItems: List<DiscoveredStreamItem>
        get() = detectedQueue.toList()

    /**
     * Initializes the WebView strictly on the main thread.
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun initialize(
        enableJs: Boolean = true,
        enableDomStorage: Boolean = true,
        blockImages: Boolean = false,
        injectedHookScript: String? = null,
        onPageLoadFinished: ((String) -> Unit)? = null
    ): Unit = withContext(Dispatchers.Main.immediate) {
        if (isDisposed.get()) return@withContext

        try {
            val wv = WebView(context.applicationContext)
            val settings = wv.settings
            settings.javaScriptEnabled = enableJs
            settings.domStorageEnabled = enableDomStorage
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            if (blockImages) {
                settings.loadsImagesAutomatically = false
                settings.blockNetworkImage = false
            }

            customUserAgent?.let {
                settings.userAgentString = it
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)

            // Javascript interface for DOM/Fetch/XHR hooks
            class Bridge {
                @JavascriptInterface
                fun onMediaFound(url: String, type: String, contextStr: String) {
                    if (isDisposed.get()) return
                    handleMediaDetection(
                        url = url,
                        source = type.ifBlank { "DOM_HOOK" },
                        pageUrl = currentPageUrl.get() ?: initialUrl
                    )
                }
            }

            wv.addJavascriptInterface(Bridge(), "ReelScraperBridge")

            wv.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (url != null && url.isNotBlank()) {
                        currentPageUrl.set(url)
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (isDisposed.get()) return
                    if (url != null && url.isNotBlank()) {
                        currentPageUrl.set(url)
                    }

                    if (injectedHookScript != null && !isDisposed.get()) {
                        try {
                            view?.evaluateJavascript(injectedHookScript, null)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to inject hook script", e)
                        }
                    }

                    onPageLoadFinished?.invoke(currentPageUrl.get() ?: initialUrl)
                }

                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    if (url != null && url.isNotBlank()) {
                        currentPageUrl.set(url)
                    }
                }

                /**
                 * Chromium background thread callback ('ThreadPoolForeg').
                 * NEVER call WebView methods inside this callback.
                 */
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    if (isDisposed.get() || request == null) return null

                    try {
                        val requestUri: Uri = request.url ?: return null
                        val reqUrl = requestUri.toString()
                        if (isMediaUrl(reqUrl)) {
                            val headers = request.requestHeaders ?: emptyMap()
                            val page = currentPageUrl.get() ?: initialUrl
                            val referer = headers["Referer"] ?: headers["referer"] ?: page
                            val ua = headers["User-Agent"] ?: headers["user-agent"] ?: customUserAgent

                            handleMediaDetection(
                                url = reqUrl,
                                source = "NETWORK",
                                mimeType = determineMime(reqUrl),
                                pageUrl = page,
                                requestHeaders = headers,
                                referer = referer,
                                userAgent = ua
                            )
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Safe intercept exception handled cleanly: ${e.message}")
                    }

                    return null // Proceed with normal request loading
                }
            }

            webView = wv
            wv.loadUrl(initialUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WebViewSession", e)
        }
    }

    private fun handleMediaDetection(
        url: String,
        source: String,
        mimeType: String = "",
        pageUrl: String,
        requestHeaders: Map<String, String> = emptyMap(),
        referer: String? = null,
        userAgent: String? = null
    ) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank() || cleanUrl.startsWith("blob:") || cleanUrl.startsWith("data:")) return

        val item = DiscoveredStreamItem(
            url = cleanUrl,
            source = source,
            mimeType = mimeType.ifBlank { determineMime(cleanUrl) },
            pageUrl = pageUrl,
            requestHeaders = requestHeaders,
            referer = referer ?: pageUrl,
            userAgent = userAgent ?: customUserAgent
        )

        detectedQueue.add(item)
        onStreamDiscovered?.invoke(item)
    }

    /**
     * Safely evaluates JavaScript on the main thread with a coroutine timeout.
     */
    suspend fun evaluateJs(script: String, timeoutMs: Long = 3000L): String? = withContext(Dispatchers.Main.immediate) {
        if (isDisposed.get() || webView == null) return@withContext null

        val deferred = CompletableDeferred<String?>()
        try {
            webView?.evaluateJavascript(script) { result ->
                if (!deferred.isCompleted) {
                    deferred.complete(result)
                }
            }
        } catch (e: Exception) {
            if (!deferred.isCompleted) {
                deferred.complete(null)
            }
        }

        withTimeoutOrNull(timeoutMs) {
            deferred.await()
        }
    }

    /**
     * Safely navigates to a new URL on the main thread.
     */
    fun loadUrl(url: String) {
        if (isDisposed.get()) return
        currentPageUrl.set(url)
        mainHandler.post {
            if (!isDisposed.get()) {
                try {
                    webView?.loadUrl(url)
                } catch (e: Exception) {
                    Log.w(TAG, "Error loading URL in WebViewSession", e)
                }
            }
        }
    }

    /**
     * Safely reloads on the main thread.
     */
    fun reload() {
        if (isDisposed.get()) return
        mainHandler.post {
            if (!isDisposed.get()) {
                try {
                    webView?.reload()
                } catch (e: Exception) {
                    Log.w(TAG, "Error reloading WebViewSession", e)
                }
            }
        }
    }

    /**
     * Safely updates the user agent on the main thread.
     */
    fun setUserAgent(ua: String?) {
        if (isDisposed.get()) return
        mainHandler.post {
            if (!isDisposed.get()) {
                try {
                    webView?.settings?.userAgentString = ua
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating User-Agent", e)
                }
            }
        }
    }

    /**
     * Idempotent disposal. Cleans up all resources strictly on the main thread.
     */
    fun dispose() {
        if (isDisposed.compareAndSet(false, true)) {
            mainHandler.post {
                try {
                    val wv = webView
                    webView = null
                    if (wv != null) {
                        wv.removeJavascriptInterface("ReelScraperBridge")
                        wv.webViewClient = object : WebViewClient() {}
                        wv.stopLoading()
                        wv.loadUrl("about:blank")
                        wv.clearHistory()
                        wv.destroy()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error during WebViewSession disposal", e)
                }
            }
        }
    }

    private fun isMediaUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".mp4") || lower.contains(".webm") ||
                lower.contains(".m3u8") || lower.contains(".mpd") ||
                lower.contains(".gif") || lower.contains("/hls/") ||
                lower.contains("/manifest") || lower.contains("master.m3u8")
    }

    private fun determineMime(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".m3u8") || lower.contains("/hls/") -> "application/x-mpegURL"
            lower.contains(".mpd") || lower.contains("/dash/") -> "application/dash+xml"
            lower.contains(".gif") -> "image/gif"
            lower.contains(".webm") -> "video/webm"
            else -> "video/mp4"
        }
    }
}
