package com.example.reelscraper.data.settings

data class AppSettings(
    // Crawling settings
    val defaultScanLevels: Int = 2,
    val maxLinksPerPage: Int = 100,
    val maxConcurrentRequests: Int = 3,
    val pageTimeoutSeconds: Int = 8,
    val requestDelayMs: Long = 100L,
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
    val maxWebViewWaitSeconds: Int = 5,
    val maxScriptScanSizeBytes: Int = 500_000,
    val maxJsonScanSizeBytes: Int = 500_000,
    val enableContentTypeSniffing: Boolean = true,
    val maxSniffingRequestsPerPage: Int = 10,

    // Playback settings
    val autoplayNext: Boolean = true,
    val muteByDefault: Boolean = false,
    val preloadAdjacentCount: Int = 1,
    val pauseOnBackground: Boolean = true,

    // Appearance settings
    val themeMode: String = "dark", // "dark", "light", "system"
    val showFormatBadges: Boolean = true,
    val showSourceDomainLabels: Boolean = true
)
