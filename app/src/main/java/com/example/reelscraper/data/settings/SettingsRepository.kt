package com.example.reelscraper.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reel_scraper_preferences")

open class SettingsRepository(
    private val context: Context,
    customSettingsFlow: Flow<AppSettings>? = null
) {
    private object PreferencesKeys {
        val DEFAULT_SCAN_LEVELS = intPreferencesKey("default_scan_levels")
        val MAX_LINKS_PER_PAGE = intPreferencesKey("max_links_per_page")
        val MAX_PAGES_PER_CRAWL = intPreferencesKey("max_pages_per_crawl")
        val MAX_CONCURRENT_REQUESTS = intPreferencesKey("max_concurrent_requests")
        val PAGE_TIMEOUT_SECONDS = intPreferencesKey("page_timeout_seconds")
        val REQUEST_DELAY_MS = longPreferencesKey("request_delay_ms")
        val RETRY_COUNT = intPreferencesKey("retry_count")
        val SAME_DOMAIN_ONLY = booleanPreferencesKey("same_domain_only")
        val INCLUDE_SUBDOMAINS = booleanPreferencesKey("include_subdomains")
        val DISCOVER_SITEMAPS = booleanPreferencesKey("discover_sitemaps")
        val DISCOVER_MEDIA_LINK_PRELOADS = booleanPreferencesKey("discover_media_link_preloads")
        val FOLLOW_PAGINATION_LINKS = booleanPreferencesKey("follow_pagination_links")

        val EXTRACT_MP4 = booleanPreferencesKey("extract_mp4")
        val EXTRACT_WEBM = booleanPreferencesKey("extract_webm")
        val EXTRACT_HLS = booleanPreferencesKey("extract_hls")
        val EXTRACT_DASH = booleanPreferencesKey("extract_dash")
        val EXTRACT_GIF = booleanPreferencesKey("extract_gif")

        val ENABLE_HTML_TAG = booleanPreferencesKey("enable_html_tag")
        val ENABLE_META_TAGS = booleanPreferencesKey("enable_meta_tags")
        val ENABLE_JSON_LD = booleanPreferencesKey("enable_json_ld")
        val ENABLE_INLINE_SCRIPT = booleanPreferencesKey("enable_inline_script")
        val ENABLE_REGEX_SCAN = booleanPreferencesKey("enable_regex_scan")
        val ENABLE_ATTRIBUTE_SCAN = booleanPreferencesKey("enable_attribute_scan")
        val ENABLE_IFRAME_SCAN = booleanPreferencesKey("enable_iframe_scan")
        val ENABLE_FEED_SITEMAP_SCAN = booleanPreferencesKey("enable_feed_sitemap_scan")
        val ENABLE_JSON_API_SCAN = booleanPreferencesKey("enable_json_api_scan")
        val ENABLE_WEBVIEW_FALLBACK = booleanPreferencesKey("enable_webview_fallback")
        val WEBVIEW_ONLY_WHEN_NO_MEDIA = booleanPreferencesKey("webview_only_when_no_media")
        val MAX_WEBVIEW_WAIT_SECONDS = intPreferencesKey("max_webview_wait_seconds")
        val MAX_SCRIPT_SCAN_SIZE = intPreferencesKey("max_script_scan_size")
        val MAX_JSON_SCAN_SIZE = intPreferencesKey("max_json_scan_size")
        val ENABLE_CONTENT_TYPE_SNIFFING = booleanPreferencesKey("enable_content_type_sniffing")
        val MAX_SNIFFING_REQUESTS = intPreferencesKey("max_sniffing_requests")

        val ENABLE_DYNAMIC_STREAMING = booleanPreferencesKey("enable_dynamic_streaming")
        val DYNAMIC_MODE = stringPreferencesKey("dynamic_mode")
        val INJECT_HOOKS = booleanPreferencesKey("inject_hooks")
        val INSPECT_PLAYER_OBJECTS = booleanPreferencesKey("inspect_player_objects")
        val USE_EPHEMERAL_COOKIES = booleanPreferencesKey("use_ephemeral_cookies")
        val ENABLE_LOCAL_PROXY = booleanPreferencesKey("enable_local_proxy")
        val REFRESH_EXPIRED_STREAMS = booleanPreferencesKey("refresh_expired_streams")
        val MAX_REFRESH_RETRIES = intPreferencesKey("max_refresh_retries")

        val AUTOPLAY_NEXT = booleanPreferencesKey("autoplay_next")
        val MUTE_BY_DEFAULT = booleanPreferencesKey("mute_by_default")
        val PRELOAD_ADJACENT_COUNT = intPreferencesKey("preload_adjacent_count")
        val PAUSE_ON_BACKGROUND = booleanPreferencesKey("pause_on_background")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val SHOW_PLAYER_DEBUG_STATS = booleanPreferencesKey("show_player_debug_stats")

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_FORMAT_BADGES = booleanPreferencesKey("show_format_badges")
        val SHOW_SOURCE_DOMAIN_LABELS = booleanPreferencesKey("show_source_domain_labels")
        val SHOW_DYNAMIC_BADGES = booleanPreferencesKey("show_dynamic_badges")

        // Advanced streaming
        val QUALITY_PREFERENCE = stringPreferencesKey("quality_preference")
        val AUDIO_NORMALIZATION = booleanPreferencesKey("audio_normalization")
        val NORMALIZATION_STRENGTH = stringPreferencesKey("normalization_strength")
        val AMBIENT_MODE = stringPreferencesKey("ambient_mode")
        val SMART_REFRAME_MODE = stringPreferencesKey("smart_reframe_mode")
        val ENABLE_60FPS_CONVERTER = booleanPreferencesKey("enable_60fps_converter")
        val PREFER_SURFACE_VIEW = booleanPreferencesKey("prefer_surface_view")
        val HDR_ENABLED = booleanPreferencesKey("hdr_enabled")
        val PLAYER_POOL_SIZE = intPreferencesKey("player_pool_size")
        val RESUME_BEHAVIOR = stringPreferencesKey("resume_behavior")
        val MARK_WATCHED_THRESHOLD = intPreferencesKey("mark_watched_threshold")
        val DATA_SAVER_MODE = booleanPreferencesKey("data_saver_mode")

        // Gestures
        val GESTURES_ENABLED = booleanPreferencesKey("gestures_enabled")
        val SEEK_INCREMENT_SECONDS = intPreferencesKey("seek_increment_seconds")
        val LONG_PRESS_SPEED = floatPreferencesKey("long_press_speed")
        val BRIGHTNESS_GESTURE_ENABLED = booleanPreferencesKey("brightness_gesture_enabled")
        val VOLUME_GESTURE_ENABLED = booleanPreferencesKey("volume_gesture_enabled")

        // Subtitles & Audio Multi-Track
        val PREFERRED_AUDIO_LANGUAGE = stringPreferencesKey("preferred_audio_language")
        val PREFERRED_SUBTITLE_LANGUAGE = stringPreferencesKey("preferred_subtitle_language")
        val SUBTITLE_FONT_SIZE_SP = intPreferencesKey("subtitle_font_size_sp")
        val SUBTITLE_TEXT_COLOR_HEX = stringPreferencesKey("subtitle_text_color_hex")
        val SUBTITLE_BG_COLOR_HEX = stringPreferencesKey("subtitle_bg_color_hex")
        val SUBTITLE_OUTLINE = booleanPreferencesKey("subtitle_outline")
        val SUBTITLE_VERTICAL_OFFSET_DP = intPreferencesKey("subtitle_vertical_offset_dp")

        // On-Device Intelligence
        val ENABLE_AUTO_CHAPTERS = booleanPreferencesKey("enable_auto_chapters")
        val PHASH_MODE = stringPreferencesKey("phash_mode")
        val ENABLE_LOCAL_TRANSCRIPTION = booleanPreferencesKey("enable_local_transcription")
        val TRANSCRIPTION_QUALITY = stringPreferencesKey("transcription_quality")
        val TRANSCRIPTION_LANGUAGE = stringPreferencesKey("transcription_language")
        val ENABLE_HEATMAP_COLLECTION = booleanPreferencesKey("enable_heatmap_collection")
        val LIBRARY_PREVIEW_MODE = stringPreferencesKey("library_preview_mode")
    }

    open val settingsFlow: Flow<AppSettings> = customSettingsFlow ?: context.dataStore.data.map { preferences ->
        AppSettings(
            defaultScanLevels = preferences[PreferencesKeys.DEFAULT_SCAN_LEVELS] ?: 2,
            maxLinksPerPage = preferences[PreferencesKeys.MAX_LINKS_PER_PAGE] ?: 100,
            maxPagesPerCrawl = preferences[PreferencesKeys.MAX_PAGES_PER_CRAWL] ?: 50,
            maxConcurrentRequests = preferences[PreferencesKeys.MAX_CONCURRENT_REQUESTS] ?: 3,
            pageTimeoutSeconds = preferences[PreferencesKeys.PAGE_TIMEOUT_SECONDS] ?: 8,
            requestDelayMs = preferences[PreferencesKeys.REQUEST_DELAY_MS] ?: 100L,
            retryCount = preferences[PreferencesKeys.RETRY_COUNT] ?: 2,
            sameDomainOnly = preferences[PreferencesKeys.SAME_DOMAIN_ONLY] ?: true,
            includeSubdomains = preferences[PreferencesKeys.INCLUDE_SUBDOMAINS] ?: false,
            discoverSitemaps = preferences[PreferencesKeys.DISCOVER_SITEMAPS] ?: true,
            discoverMediaFromLinkPreloads = preferences[PreferencesKeys.DISCOVER_MEDIA_LINK_PRELOADS] ?: true,
            followPaginationLinks = preferences[PreferencesKeys.FOLLOW_PAGINATION_LINKS] ?: true,

            extractMp4 = preferences[PreferencesKeys.EXTRACT_MP4] ?: true,
            extractWebm = preferences[PreferencesKeys.EXTRACT_WEBM] ?: true,
            extractHls = preferences[PreferencesKeys.EXTRACT_HLS] ?: true,
            extractDash = preferences[PreferencesKeys.EXTRACT_DASH] ?: true,
            extractGif = preferences[PreferencesKeys.EXTRACT_GIF] ?: true,

            enableHtmlTag = preferences[PreferencesKeys.ENABLE_HTML_TAG] ?: true,
            enableMetaTags = preferences[PreferencesKeys.ENABLE_META_TAGS] ?: true,
            enableJsonLd = preferences[PreferencesKeys.ENABLE_JSON_LD] ?: true,
            enableInlineScript = preferences[PreferencesKeys.ENABLE_INLINE_SCRIPT] ?: true,
            enableRegexScan = preferences[PreferencesKeys.ENABLE_REGEX_SCAN] ?: true,
            enableAttributeScan = preferences[PreferencesKeys.ENABLE_ATTRIBUTE_SCAN] ?: true,
            enableIframeScan = preferences[PreferencesKeys.ENABLE_IFRAME_SCAN] ?: true,
            enableFeedSitemapScan = preferences[PreferencesKeys.ENABLE_FEED_SITEMAP_SCAN] ?: true,
            enableJsonApiScan = preferences[PreferencesKeys.ENABLE_JSON_API_SCAN] ?: true,
            enableWebViewFallback = preferences[PreferencesKeys.ENABLE_WEBVIEW_FALLBACK] ?: true,
            webViewOnlyWhenNoMedia = preferences[PreferencesKeys.WEBVIEW_ONLY_WHEN_NO_MEDIA] ?: true,
            maxWebViewWaitSeconds = preferences[PreferencesKeys.MAX_WEBVIEW_WAIT_SECONDS] ?: 8,
            maxScriptScanSizeBytes = preferences[PreferencesKeys.MAX_SCRIPT_SCAN_SIZE] ?: 500_000,
            maxJsonScanSizeBytes = preferences[PreferencesKeys.MAX_JSON_SCAN_SIZE] ?: 500_000,
            enableContentTypeSniffing = preferences[PreferencesKeys.ENABLE_CONTENT_TYPE_SNIFFING] ?: true,
            maxSniffingRequestsPerPage = preferences[PreferencesKeys.MAX_SNIFFING_REQUESTS] ?: 10,

            enableDynamicStreaming = preferences[PreferencesKeys.ENABLE_DYNAMIC_STREAMING] ?: true,
            dynamicMode = preferences[PreferencesKeys.DYNAMIC_MODE] ?: "AUTOMATIC",
            injectHooks = preferences[PreferencesKeys.INJECT_HOOKS] ?: true,
            inspectPlayerObjects = preferences[PreferencesKeys.INSPECT_PLAYER_OBJECTS] ?: true,
            useEphemeralCookies = preferences[PreferencesKeys.USE_EPHEMERAL_COOKIES] ?: true,
            enableLocalProxy = preferences[PreferencesKeys.ENABLE_LOCAL_PROXY] ?: false,
            refreshExpiredStreams = preferences[PreferencesKeys.REFRESH_EXPIRED_STREAMS] ?: true,
            maxRefreshRetries = preferences[PreferencesKeys.MAX_REFRESH_RETRIES] ?: 3,

            autoplayNext = preferences[PreferencesKeys.AUTOPLAY_NEXT] ?: true,
            muteByDefault = preferences[PreferencesKeys.MUTE_BY_DEFAULT] ?: false,
            preloadAdjacentCount = preferences[PreferencesKeys.PRELOAD_ADJACENT_COUNT] ?: 1,
            pauseOnBackground = preferences[PreferencesKeys.PAUSE_ON_BACKGROUND] ?: true,
            resumePlayback = preferences[PreferencesKeys.RESUME_PLAYBACK] ?: true,
            showPlayerDebugStats = preferences[PreferencesKeys.SHOW_PLAYER_DEBUG_STATS] ?: false,

            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "dark",
            showFormatBadges = preferences[PreferencesKeys.SHOW_FORMAT_BADGES] ?: true,
            showSourceDomainLabels = preferences[PreferencesKeys.SHOW_SOURCE_DOMAIN_LABELS] ?: true,
            showDynamicBadges = preferences[PreferencesKeys.SHOW_DYNAMIC_BADGES] ?: true,

            qualityPreference = preferences[PreferencesKeys.QUALITY_PREFERENCE] ?: "AUTO",
            audioNormalization = preferences[PreferencesKeys.AUDIO_NORMALIZATION] ?: true,
            normalizationStrength = preferences[PreferencesKeys.NORMALIZATION_STRENGTH] ?: "NORMAL",
            ambientMode = preferences[PreferencesKeys.AMBIENT_MODE] ?: "POSTER_ONLY",
            smartReframeMode = preferences[PreferencesKeys.SMART_REFRAME_MODE] ?: "CENTER_CROP",
            enable60FpsConverter = preferences[PreferencesKeys.ENABLE_60FPS_CONVERTER] ?: true,
            preferSurfaceView = preferences[PreferencesKeys.PREFER_SURFACE_VIEW] ?: true,
            hdrEnabled = preferences[PreferencesKeys.HDR_ENABLED] ?: true,
            playerPoolSize = preferences[PreferencesKeys.PLAYER_POOL_SIZE] ?: 3,
            resumeBehavior = preferences[PreferencesKeys.RESUME_BEHAVIOR] ?: "ALWAYS",
            markWatchedThreshold = preferences[PreferencesKeys.MARK_WATCHED_THRESHOLD] ?: 90,
            dataSaverMode = preferences[PreferencesKeys.DATA_SAVER_MODE] ?: false,

            gesturesEnabled = preferences[PreferencesKeys.GESTURES_ENABLED] ?: true,
            seekIncrementSeconds = preferences[PreferencesKeys.SEEK_INCREMENT_SECONDS] ?: 10,
            longPressSpeed = preferences[PreferencesKeys.LONG_PRESS_SPEED] ?: 2.0f,
            brightnessGestureEnabled = preferences[PreferencesKeys.BRIGHTNESS_GESTURE_ENABLED] ?: true,
            volumeGestureEnabled = preferences[PreferencesKeys.VOLUME_GESTURE_ENABLED] ?: true,

            preferredAudioLanguage = preferences[PreferencesKeys.PREFERRED_AUDIO_LANGUAGE] ?: "auto",
            preferredSubtitleLanguage = preferences[PreferencesKeys.PREFERRED_SUBTITLE_LANGUAGE] ?: "en",
            subtitleFontSizeSp = preferences[PreferencesKeys.SUBTITLE_FONT_SIZE_SP] ?: 16,
            subtitleTextColorHex = preferences[PreferencesKeys.SUBTITLE_TEXT_COLOR_HEX] ?: "#FFFFFF",
            subtitleBgColorHex = preferences[PreferencesKeys.SUBTITLE_BG_COLOR_HEX] ?: "#80000000",
            subtitleOutline = preferences[PreferencesKeys.SUBTITLE_OUTLINE] ?: true,
            subtitleVerticalOffsetDp = preferences[PreferencesKeys.SUBTITLE_VERTICAL_OFFSET_DP] ?: 24,

            enableAutoChapters = preferences[PreferencesKeys.ENABLE_AUTO_CHAPTERS] ?: true,
            pHashMode = preferences[PreferencesKeys.PHASH_MODE] ?: "ALL_MEDIA",
            enableLocalTranscription = preferences[PreferencesKeys.ENABLE_LOCAL_TRANSCRIPTION] ?: true,
            transcriptionQuality = preferences[PreferencesKeys.TRANSCRIPTION_QUALITY] ?: "BALANCED",
            transcriptionLanguage = preferences[PreferencesKeys.TRANSCRIPTION_LANGUAGE] ?: "en-US",
            enableHeatmapCollection = preferences[PreferencesKeys.ENABLE_HEATMAP_COLLECTION] ?: true,
            libraryPreviewMode = preferences[PreferencesKeys.LIBRARY_PREVIEW_MODE] ?: "LONG_PRESS"
        )
    }

    suspend fun updateDiscoverySettings(
        sameDomainOnly: Boolean,
        includeSubdomains: Boolean,
        discoverSitemaps: Boolean,
        discoverMediaFromLinkPreloads: Boolean,
        followPaginationLinks: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAME_DOMAIN_ONLY] = sameDomainOnly
            preferences[PreferencesKeys.INCLUDE_SUBDOMAINS] = includeSubdomains
            preferences[PreferencesKeys.DISCOVER_SITEMAPS] = discoverSitemaps
            preferences[PreferencesKeys.DISCOVER_MEDIA_LINK_PRELOADS] = discoverMediaFromLinkPreloads
            preferences[PreferencesKeys.FOLLOW_PAGINATION_LINKS] = followPaginationLinks
        }
    }

    suspend fun updateScanLevels(levels: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_SCAN_LEVELS] = levels.coerceIn(1, 10)
        }
    }

    suspend fun updateCrawlLimits(maxLinks: Int, concurrentRequests: Int, timeoutSec: Int, delayMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAX_LINKS_PER_PAGE] = maxLinks
            preferences[PreferencesKeys.MAX_CONCURRENT_REQUESTS] = concurrentRequests
            preferences[PreferencesKeys.PAGE_TIMEOUT_SECONDS] = timeoutSec
            preferences[PreferencesKeys.REQUEST_DELAY_MS] = delayMs
        }
    }

    suspend fun updateFormatToggles(mp4: Boolean, webm: Boolean, hls: Boolean, dash: Boolean, gif: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXTRACT_MP4] = mp4
            preferences[PreferencesKeys.EXTRACT_WEBM] = webm
            preferences[PreferencesKeys.EXTRACT_HLS] = hls
            preferences[PreferencesKeys.EXTRACT_DASH] = dash
            preferences[PreferencesKeys.EXTRACT_GIF] = gif
        }
    }

    suspend fun updateAdvancedExtraction(
        html: Boolean, meta: Boolean, jsonLd: Boolean, inlineScript: Boolean,
        regex: Boolean, attribute: Boolean, iframe: Boolean, feedSitemap: Boolean,
        jsonApi: Boolean, webView: Boolean, webViewOnlyWhenNoMedia: Boolean,
        contentTypeSniffing: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_HTML_TAG] = html
            preferences[PreferencesKeys.ENABLE_META_TAGS] = meta
            preferences[PreferencesKeys.ENABLE_JSON_LD] = jsonLd
            preferences[PreferencesKeys.ENABLE_INLINE_SCRIPT] = inlineScript
            preferences[PreferencesKeys.ENABLE_REGEX_SCAN] = regex
            preferences[PreferencesKeys.ENABLE_ATTRIBUTE_SCAN] = attribute
            preferences[PreferencesKeys.ENABLE_IFRAME_SCAN] = iframe
            preferences[PreferencesKeys.ENABLE_FEED_SITEMAP_SCAN] = feedSitemap
            preferences[PreferencesKeys.ENABLE_JSON_API_SCAN] = jsonApi
            preferences[PreferencesKeys.ENABLE_WEBVIEW_FALLBACK] = webView
            preferences[PreferencesKeys.WEBVIEW_ONLY_WHEN_NO_MEDIA] = webViewOnlyWhenNoMedia
            preferences[PreferencesKeys.ENABLE_CONTENT_TYPE_SNIFFING] = contentTypeSniffing
        }
    }

    suspend fun updateProxySettings(enableLocalProxy: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_LOCAL_PROXY] = enableLocalProxy
        }
    }

    suspend fun updateDynamicStreamSettings(
        enableDynamicStreaming: Boolean,
        enableLocalProxy: Boolean = false,
        refreshExpiredStreams: Boolean = true
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_DYNAMIC_STREAMING] = enableDynamicStreaming
            preferences[PreferencesKeys.ENABLE_LOCAL_PROXY] = enableLocalProxy
            preferences[PreferencesKeys.REFRESH_EXPIRED_STREAMS] = refreshExpiredStreams
        }
    }

    suspend fun updateDynamicStreamingSettings(
        enabled: Boolean,
        mode: String,
        injectHooks: Boolean,
        inspectPlayers: Boolean,
        ephemeralCookies: Boolean,
        localProxy: Boolean,
        refreshExpired: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_DYNAMIC_STREAMING] = enabled
            preferences[PreferencesKeys.DYNAMIC_MODE] = mode
            preferences[PreferencesKeys.INJECT_HOOKS] = injectHooks
            preferences[PreferencesKeys.INSPECT_PLAYER_OBJECTS] = inspectPlayers
            preferences[PreferencesKeys.USE_EPHEMERAL_COOKIES] = ephemeralCookies
            preferences[PreferencesKeys.ENABLE_LOCAL_PROXY] = localProxy
            preferences[PreferencesKeys.REFRESH_EXPIRED_STREAMS] = refreshExpired
        }
    }

    suspend fun updatePlaybackSettings(
        autoplayNext: Boolean,
        muteByDefault: Boolean,
        preloadCount: Int,
        pauseOnBackground: Boolean,
        resume: Boolean = true,
        debugStats: Boolean = false
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTOPLAY_NEXT] = autoplayNext
            preferences[PreferencesKeys.MUTE_BY_DEFAULT] = muteByDefault
            preferences[PreferencesKeys.PRELOAD_ADJACENT_COUNT] = preloadCount.coerceIn(0, 3)
            preferences[PreferencesKeys.PAUSE_ON_BACKGROUND] = pauseOnBackground
            preferences[PreferencesKeys.RESUME_PLAYBACK] = resume
            preferences[PreferencesKeys.SHOW_PLAYER_DEBUG_STATS] = debugStats
        }
    }

    suspend fun updateAdvancedPlaybackSettings(
        qualityPref: String,
        audioNorm: Boolean,
        normStrength: String,
        ambient: String,
        smartReframe: String,
        enable60Fps: Boolean,
        preferSurface: Boolean,
        hdr: Boolean,
        poolSize: Int,
        resumeBeh: String,
        watchedThresh: Int,
        dataSaver: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.QUALITY_PREFERENCE] = qualityPref
            preferences[PreferencesKeys.AUDIO_NORMALIZATION] = audioNorm
            preferences[PreferencesKeys.NORMALIZATION_STRENGTH] = normStrength
            preferences[PreferencesKeys.AMBIENT_MODE] = ambient
            preferences[PreferencesKeys.SMART_REFRAME_MODE] = smartReframe
            preferences[PreferencesKeys.ENABLE_60FPS_CONVERTER] = enable60Fps
            preferences[PreferencesKeys.PREFER_SURFACE_VIEW] = preferSurface
            preferences[PreferencesKeys.HDR_ENABLED] = hdr
            preferences[PreferencesKeys.PLAYER_POOL_SIZE] = poolSize.coerceIn(2, 4)
            preferences[PreferencesKeys.RESUME_BEHAVIOR] = resumeBeh
            preferences[PreferencesKeys.MARK_WATCHED_THRESHOLD] = watchedThresh
            preferences[PreferencesKeys.DATA_SAVER_MODE] = dataSaver
        }
    }

    suspend fun toggleDataSaver(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DATA_SAVER_MODE] = enabled
            if (enabled) {
                preferences[PreferencesKeys.PRELOAD_ADJACENT_COUNT] = 0
                preferences[PreferencesKeys.QUALITY_PREFERENCE] = "DATA_SAVER"
                preferences[PreferencesKeys.AMBIENT_MODE] = "OFF"
            }
        }
    }

    suspend fun updateGestureSettings(
        enabled: Boolean,
        seekSec: Int,
        speed: Float,
        brightness: Boolean,
        volume: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GESTURES_ENABLED] = enabled
            preferences[PreferencesKeys.SEEK_INCREMENT_SECONDS] = seekSec
            preferences[PreferencesKeys.LONG_PRESS_SPEED] = speed
            preferences[PreferencesKeys.BRIGHTNESS_GESTURE_ENABLED] = brightness
            preferences[PreferencesKeys.VOLUME_GESTURE_ENABLED] = volume
        }
    }

    suspend fun updateSubtitleSettings(
        audioLang: String,
        subtitleLang: String,
        fontSize: Int,
        textColor: String,
        bgColor: String,
        outline: Boolean,
        verticalOffset: Int
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_AUDIO_LANGUAGE] = audioLang
            preferences[PreferencesKeys.PREFERRED_SUBTITLE_LANGUAGE] = subtitleLang
            preferences[PreferencesKeys.SUBTITLE_FONT_SIZE_SP] = fontSize
            preferences[PreferencesKeys.SUBTITLE_TEXT_COLOR_HEX] = textColor
            preferences[PreferencesKeys.SUBTITLE_BG_COLOR_HEX] = bgColor
            preferences[PreferencesKeys.SUBTITLE_OUTLINE] = outline
            preferences[PreferencesKeys.SUBTITLE_VERTICAL_OFFSET_DP] = verticalOffset
        }
    }

    suspend fun updateIntelligenceSettings(
        autoChapters: Boolean,
        pHash: String,
        transcription: Boolean,
        transcriptionQuality: String,
        transcriptionLanguage: String,
        heatmap: Boolean,
        previewMode: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLE_AUTO_CHAPTERS] = autoChapters
            preferences[PreferencesKeys.PHASH_MODE] = pHash
            preferences[PreferencesKeys.ENABLE_LOCAL_TRANSCRIPTION] = transcription
            preferences[PreferencesKeys.TRANSCRIPTION_QUALITY] = transcriptionQuality
            preferences[PreferencesKeys.TRANSCRIPTION_LANGUAGE] = transcriptionLanguage
            preferences[PreferencesKeys.ENABLE_HEATMAP_COLLECTION] = heatmap
            preferences[PreferencesKeys.LIBRARY_PREVIEW_MODE] = previewMode
        }
    }

    suspend fun updateAppearance(
        themeMode: String,
        showBadges: Boolean,
        showDomainLabels: Boolean,
        showDynamicBadges: Boolean = true
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode
            preferences[PreferencesKeys.SHOW_FORMAT_BADGES] = showBadges
            preferences[PreferencesKeys.SHOW_SOURCE_DOMAIN_LABELS] = showDomainLabels
            preferences[PreferencesKeys.SHOW_DYNAMIC_BADGES] = showDynamicBadges
        }
    }

    open suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val current = settingsFlow.first()
        val updated = transform(current)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_NORMALIZATION] = updated.audioNormalization
            preferences[PreferencesKeys.NORMALIZATION_STRENGTH] = updated.normalizationStrength
            preferences[PreferencesKeys.ENABLE_60FPS_CONVERTER] = updated.enable60FpsConverter
            preferences[PreferencesKeys.AMBIENT_MODE] = updated.ambientMode
            preferences[PreferencesKeys.SMART_REFRAME_MODE] = updated.smartReframeMode
            preferences[PreferencesKeys.QUALITY_PREFERENCE] = updated.qualityPreference
            preferences[PreferencesKeys.DATA_SAVER_MODE] = updated.dataSaverMode
            preferences[PreferencesKeys.ENABLE_AUTO_CHAPTERS] = updated.enableAutoChapters
            preferences[PreferencesKeys.ENABLE_LOCAL_TRANSCRIPTION] = updated.enableLocalTranscription
            preferences[PreferencesKeys.TRANSCRIPTION_LANGUAGE] = updated.transcriptionLanguage
            preferences[PreferencesKeys.TRANSCRIPTION_QUALITY] = updated.transcriptionQuality
            preferences[PreferencesKeys.PHASH_MODE] = updated.pHashMode
            preferences[PreferencesKeys.SUBTITLE_FONT_SIZE_SP] = updated.subtitleFontSizeSp
            preferences[PreferencesKeys.SUBTITLE_TEXT_COLOR_HEX] = updated.subtitleTextColorHex
            preferences[PreferencesKeys.SUBTITLE_BG_COLOR_HEX] = updated.subtitleBgColorHex
            preferences[PreferencesKeys.SUBTITLE_VERTICAL_OFFSET_DP] = updated.subtitleVerticalOffsetDp
        }
    }
}
