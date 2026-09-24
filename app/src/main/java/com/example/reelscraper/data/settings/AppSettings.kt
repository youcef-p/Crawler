package com.example.reelscraper.data.settings

data class AppSettings(
    // Crawling settings
    val defaultScanLevels: Int = 2,
    val maxLinksPerPage: Int = 100,
    val maxPagesPerCrawl: Int = 50,
    val maxConcurrentRequests: Int = 3,
    val pageTimeoutSeconds: Int = 8,
    val requestDelayMs: Long = 100L,
    val retryCount: Int = 2,
    val extractMp4: Boolean = true,
    val extractWebm: Boolean = true,
    val extractHls: Boolean = true,
    val extractDash: Boolean = true,
    val extractGif: Boolean = true,

    // Advanced extraction settings
    val enableHtmlTag: Boolean = true,
    val enableMetaTags: Boolean = true,
    val enableJsonLd: Boolean = true,
    val enableInlineScript: Boolean = true,
    val enableRegexScan: Boolean = true,
    val enableAttributeScan: Boolean = true,
    val enableIframeScan: Boolean = true,
    val enableFeedSitemapScan: Boolean = true,
    val enableJsonApiScan: Boolean = true,
    val enableWebViewFallback: Boolean = true,
    val webViewOnlyWhenNoMedia: Boolean = true,
    val maxWebViewWaitSeconds: Int = 8,
    val maxScriptScanSizeBytes: Int = 500_000,
    val maxJsonScanSizeBytes: Int = 500_000,
    val enableContentTypeSniffing: Boolean = true,
    val maxSniffingRequestsPerPage: Int = 10,

    // Dynamic streaming settings
    val enableDynamicStreaming: Boolean = true,
    val dynamicMode: String = "AUTOMATIC", // "AUTOMATIC", "MANUAL_ONLY", "OFF"
    val injectHooks: Boolean = true,
    val inspectPlayerObjects: Boolean = true,
    val useEphemeralCookies: Boolean = true,
    val enableLocalProxy: Boolean = false,
    val proxyCacheSizeBytes: Long = 50_000_000L,
    val refreshExpiredStreams: Boolean = true,
    val maxRefreshRetries: Int = 3,

    // Playback settings
    val autoplayNext: Boolean = true,
    val muteByDefault: Boolean = false,
    val preloadAdjacentCount: Int = 1,
    val pauseOnBackground: Boolean = true,
    val resumePlayback: Boolean = true,
    val showPlayerDebugStats: Boolean = false,

    // Appearance settings
    val themeMode: String = "dark", // "dark", "light", "system"
    val showFormatBadges: Boolean = true,
    val showSourceDomainLabels: Boolean = true,
    val showDynamicBadges: Boolean = true,

    // 1. Advanced Playback Engine
    val qualityPreference: String = "AUTO", // AUTO, LOWEST, HIGHEST, DATA_SAVER, 1080p, 720p, 480p
    val audioNormalization: Boolean = true,
    val normalizationStrength: String = "NORMAL", // OFF, LIGHT, NORMAL, STRONG
    val ambientMode: String = "POSTER_ONLY", // OFF, POSTER_ONLY, DYNAMIC_LOW_FREQ, DYNAMIC_HIGH_FREQ
    val smartReframeMode: String = "CENTER_CROP", // OFF, CENTER_CROP, MOTION_TRACKING, SUBJECT_TRACKING
    val enable60FpsConverter: Boolean = true,
    val preferSurfaceView: Boolean = true,
    val hdrEnabled: Boolean = true,
    val playerPoolSize: Int = 3, // 2, 3, 4
    val resumeBehavior: String = "ALWAYS", // ALWAYS, ASK, BEGINNING
    val markWatchedThreshold: Int = 90, // 80, 90, 95, 100
    val dataSaverMode: Boolean = false,

    // 2. Gesture Controls
    val gesturesEnabled: Boolean = true,
    val seekIncrementSeconds: Int = 10, // 5, 10, 15, 30
    val longPressSpeed: Float = 2.0f, // 1.5, 2.0, 3.0
    val brightnessGestureEnabled: Boolean = true,
    val volumeGestureEnabled: Boolean = true,

    // 3. Subtitles & Audio Multi-Track
    val preferredAudioLanguage: String = "auto",
    val preferredSubtitleLanguage: String = "en",
    val subtitleFontSizeSp: Int = 16,
    val subtitleTextColorHex: String = "#FFFFFF",
    val subtitleBgColorHex: String = "#80000000",
    val subtitleOutline: Boolean = true,
    val subtitleVerticalOffsetDp: Int = 24,

    // 4. On-Device Intelligence
    val enableAutoChapters: Boolean = true,
    val pHashMode: String = "ALL_MEDIA", // OFF, GIFS_ONLY, SHORT_VIDEOS_ONLY, ALL_MEDIA
    val enableLocalTranscription: Boolean = true,
    val transcriptionQuality: String = "BALANCED", // FAST, BALANCED, ACCURATE
    val transcriptionLanguage: String = "en-US",
    val enableHeatmapCollection: Boolean = true,
    val libraryPreviewMode: String = "LONG_PRESS" // OFF, LONG_PRESS, ON_FOCUS, WIFI_ONLY
)
