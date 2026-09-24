package com.example.reelscraper.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.util.MediaNormalizer
import com.example.reelscraper.ui.viewmodel.SnifferCapturedItem
import com.example.reelscraper.ui.viewmodel.SnifferViewModel
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.CoralPink
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VividViolet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "SnifferScreen"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SnifferScreen(
    viewModel: SnifferViewModel,
    onNavigateBack: () -> Unit = {},
    onPlayMedia: (ScrapedMedia) -> Unit = {},
    onNavigateToFeed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentTargetUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val isDesktopMode by viewModel.isDesktopMode.collectAsStateWithLifecycle()
    val capturedItems by viewModel.capturedItems.collectAsStateWithLifecycle()
    val snackMessage by viewModel.snackMessage.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var inputUrl by remember { mutableStateOf(currentTargetUrl) }
    val isDisposed = remember { AtomicBoolean(false) }
    val currentPageUrl = remember { AtomicReference(currentTargetUrl) }

    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackMessage()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isDisposed.set(true)
            Handler(Looper.getMainLooper()).post {
                try {
                    webViewInstance?.apply {
                        removeJavascriptInterface("ReelScraperSnifferBridge")
                        webViewClient = object : WebViewClient() {}
                        stopLoading()
                        loadUrl("about:blank")
                        clearHistory()
                        destroy()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error disposing Sniffer WebView", e)
                } finally {
                    webViewInstance = null
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(CinemaBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = "Live Stream Sniffer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Browser with real-time media interception",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Desktop / Mobile Toggle
                    Surface(
                        color = if (isDesktopMode) VividViolet.copy(alpha = 0.3f) else CinemaSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            viewModel.toggleDesktopMode()
                            val newUa = if (!isDesktopMode) {
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
                            } else null
                            webViewInstance?.settings?.userAgentString = newUa
                            webViewInstance?.reload()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid,
                                contentDescription = "Toggle Desktop UA",
                                tint = if (isDesktopMode) NeonCyan else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDesktopMode) "Desktop" else "Mobile",
                                color = if (isDesktopMode) NeonCyan else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Count Badge
                    Surface(
                        color = NeonCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${capturedItems.size} found",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Navigation Address Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (webViewInstance?.canGoBack() == true) {
                            webViewInstance?.goBack()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.LightGray
                    )
                }

                IconButton(
                    onClick = {
                        if (webViewInstance?.canGoForward() == true) {
                            webViewInstance?.goForward()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = Color.LightGray
                    )
                }

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    singleLine = true,
                    placeholder = { Text("https://...", color = Color.Gray, fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            keyboardController?.hide()
                            val formatted = if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                                "https://$inputUrl"
                            } else inputUrl
                            inputUrl = formatted
                            viewModel.setUrl(formatted)
                            currentPageUrl.set(formatted)
                            webViewInstance?.loadUrl(formatted)
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CinemaSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sniffer_url_input")
                )

                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        val formatted = if (!inputUrl.startsWith("http://") && !inputUrl.startsWith("https://")) {
                            "https://$inputUrl"
                        } else inputUrl
                        inputUrl = formatted
                        viewModel.setUrl(formatted)
                        currentPageUrl.set(formatted)
                        webViewInstance?.loadUrl(formatted)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TravelExplore,
                        contentDescription = "Navigate",
                        tint = NeonCyan
                    )
                }

                IconButton(
                    onClick = {
                        webViewInstance?.reload()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload",
                        tint = Color.LightGray
                    )
                }
            }

            // Interactive WebView Component
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(CinemaSurface, RoundedCornerShape(12.dp))
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.databaseEnabled = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                            if (isDesktopMode) {
                                settings.userAgentString =
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
                            }

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)

                            class Bridge {
                                @JavascriptInterface
                                fun onStreamFound(url: String, type: String, contextStr: String) {
                                    if (isDisposed.get()) return
                                    Handler(Looper.getMainLooper()).post {
                                        if (!isDisposed.get()) {
                                            viewModel.onMediaIntercepted(
                                                url = url,
                                                source = type.ifBlank { "DOM_HOOK" },
                                                mimeType = "",
                                                pageUrl = currentPageUrl.get() ?: currentTargetUrl
                                            )
                                        }
                                    }
                                }
                            }

                            addJavascriptInterface(Bridge(), "ReelScraperSnifferBridge")

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    if (url != null && url.isNotBlank()) {
                                        currentPageUrl.set(url)
                                        inputUrl = url
                                        viewModel.setUrl(url)
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (isDisposed.get()) return
                                    if (url != null && url.isNotBlank()) {
                                        currentPageUrl.set(url)
                                        inputUrl = url
                                        viewModel.setUrl(url)
                                    }

                                    val hookScript = """
                                        (function() {
                                            function report(u, t) {
                                                if (!u || u.startsWith('blob:') || u.startsWith('data:')) return;
                                                try {
                                                    if (window.ReelScraperSnifferBridge) {
                                                        window.ReelScraperSnifferBridge.onStreamFound(u, t, '');
                                                    }
                                                } catch(e) {}
                                            }

                                            document.querySelectorAll('video').forEach(function(v) {
                                                if (v.src) report(v.src, 'DOM_VIDEO');
                                                if (v.currentSrc) report(v.currentSrc, 'DOM_VIDEO');
                                                v.querySelectorAll('source').forEach(function(s) {
                                                    if (s.src) report(s.src, 'DOM_SOURCE');
                                                });
                                            });

                                            if (window.jwplayer) {
                                                try {
                                                    var p = window.jwplayer();
                                                    if (p && p.getPlaylist) {
                                                        var pl = p.getPlaylist();
                                                        if (pl && pl[0] && pl[0].file) report(pl[0].file, 'JWPLAYER');
                                                    }
                                                } catch(e){}
                                            }

                                            if (window.videojs) {
                                                try {
                                                    window.videojs.getAllPlayers().forEach(function(p) {
                                                        if (p.src()) report(p.src(), 'VIDEOJS');
                                                    });
                                                } catch(e){}
                                            }
                                        })();
                                    """.trimIndent()

                                    try {
                                        view?.evaluateJavascript(hookScript, null)
                                    } catch (e: Exception) {
                                        Log.w(TAG, "Failed to inject sniffer hook", e)
                                    }
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
                                        val reqUri: Uri = request.url ?: return null
                                        val reqUrl = reqUri.toString()

                                        if (isStreamUrl(reqUrl)) {
                                            val page = currentPageUrl.get() ?: currentTargetUrl
                                            viewModel.onMediaIntercepted(
                                                url = reqUrl,
                                                source = "NETWORK",
                                                mimeType = determineStreamMime(reqUrl),
                                                pageUrl = page
                                            )
                                        }
                                    } catch (e: Throwable) {
                                        Log.w(TAG, "Interception handled safely: ${e.message}")
                                    }

                                    return null
                                }
                            }

                            webViewInstance = this
                            loadUrl(currentTargetUrl)
                        }
                    },
                    update = { wv ->
                        webViewInstance = wv
                    }
                )
            }

            // Bottom Section: Discovered Items List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(CinemaSurface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Detected Media Streams (${capturedItems.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    if (capturedItems.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { viewModel.clearCaptured() }
                            ) {
                                Text("Clear", color = CoralPink, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (capturedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Browse pages above to detect playable video streams, HLS manifests (.m3u8), and DASH (.mpd) in real-time.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(capturedItems, key = { it.url }) { item ->
                            SnifferCapturedItemCard(
                                item = item,
                                onSave = { viewModel.saveItem(item) },
                                onCreateRule = { viewModel.createRuleFromItem(item) },
                                onPlay = {
                                    viewModel.saveItem(item)
                                    val domain = MediaNormalizer.normalizeDomain(item.sourcePageUrl).ifBlank {
                                        MediaNormalizer.normalizeDomain(item.url)
                                    }
                                    val scraped = ScrapedMedia(
                                        url = item.url,
                                        title = item.title,
                                        mediaType = item.mediaType,
                                        thumbnailUrl = item.posterUrl,
                                        sourcePageUrl = item.sourcePageUrl,
                                        sourceDomain = domain,
                                        normalizedName = MediaNormalizer.normalizeMediaName(item.url, pageUrl = item.sourcePageUrl),
                                        fileExtension = MediaNormalizer.extractExtension(item.url),
                                        extractorType = "SNIFFER_${item.detectionSource}",
                                        isDynamic = item.mediaType == MediaType.HLS || item.mediaType == MediaType.DASH
                                    )
                                    onPlayMedia(scraped)
                                    onNavigateToFeed()
                                }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

@Composable
fun SnifferCapturedItemCard(
    item: SnifferCapturedItem,
    onSave: () -> Unit,
    onCreateRule: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CinemaSurfaceVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = when (item.mediaType) {
                            MediaType.HLS -> VividViolet.copy(alpha = 0.4f)
                            MediaType.DASH -> CoralPink.copy(alpha = 0.4f)
                            MediaType.GIF -> EmeraldGreen.copy(alpha = 0.4f)
                            else -> NeonCyan.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.mediaType.name,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = item.detectionSource,
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.url,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCreateRule) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Create Rule",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = if (item.isSaved) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "Save stream",
                        tint = if (item.isSaved) EmeraldGreen else NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onPlay) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play in feed",
                        tint = VividViolet,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

private fun isStreamUrl(url: String): Boolean {
    val lower = url.lowercase()
    return lower.contains(".m3u8") || lower.contains(".mpd") ||
            lower.contains(".mp4") || lower.contains(".webm") ||
            lower.contains(".gif") || lower.contains("/hls/") ||
            lower.contains("/manifest") || lower.contains("master.m3u8")
}

private fun determineStreamMime(url: String): String {
    val lower = url.lowercase()
    return when {
        lower.contains(".m3u8") || lower.contains("/hls/") -> "application/x-mpegURL"
        lower.contains(".mpd") || lower.contains("/dash/") -> "application/dash+xml"
        lower.contains(".gif") -> "image/gif"
        lower.contains(".webm") -> "video/webm"
        else -> "video/mp4"
    }
}
