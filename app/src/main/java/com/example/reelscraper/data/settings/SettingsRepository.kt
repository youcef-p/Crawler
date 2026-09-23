package com.example.reelscraper.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reel_scraper_preferences")

open class SettingsRepository(
    private val context: Context,
    customSettingsFlow: Flow<AppSettings>? = null
) {

    private object PreferencesKeys {
        val DEFAULT_SCAN_LEVELS = intPreferencesKey("default_scan_levels")
        val MAX_LINKS_PER_PAGE = intPreferencesKey("max_links_per_page")
        val MAX_CONCURRENT_REQUESTS = intPreferencesKey("max_concurrent_requests")
        val PAGE_TIMEOUT_SECONDS = intPreferencesKey("page_timeout_seconds")
        val REQUEST_DELAY_MS = longPreferencesKey("request_delay_ms")

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

        val AUTOPLAY_NEXT = booleanPreferencesKey("autoplay_next")
        val MUTE_BY_DEFAULT = booleanPreferencesKey("mute_by_default")
        val PRELOAD_ADJACENT_COUNT = intPreferencesKey("preload_adjacent_count")
        val PAUSE_ON_BACKGROUND = booleanPreferencesKey("pause_on_background")

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_FORMAT_BADGES = booleanPreferencesKey("show_format_badges")
        val SHOW_SOURCE_DOMAIN_LABELS = booleanPreferencesKey("show_source_domain_labels")
    }

    open val settingsFlow: Flow<AppSettings> = customSettingsFlow ?: context.dataStore.data.map { preferences ->
        AppSettings(
            defaultScanLevels = preferences[PreferencesKeys.DEFAULT_SCAN_LEVELS] ?: 2,
            maxLinksPerPage = preferences[PreferencesKeys.MAX_LINKS_PER_PAGE] ?: 100,
            maxConcurrentRequests = preferences[PreferencesKeys.MAX_CONCURRENT_REQUESTS] ?: 3,
            pageTimeoutSeconds = preferences[PreferencesKeys.PAGE_TIMEOUT_SECONDS] ?: 8,
            requestDelayMs = preferences[PreferencesKeys.REQUEST_DELAY_MS] ?: 100L,
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
            maxWebViewWaitSeconds = preferences[PreferencesKeys.MAX_WEBVIEW_WAIT_SECONDS] ?: 5,
            maxScriptScanSizeBytes = preferences[PreferencesKeys.MAX_SCRIPT_SCAN_SIZE] ?: 500_000,
            maxJsonScanSizeBytes = preferences[PreferencesKeys.MAX_JSON_SCAN_SIZE] ?: 500_000,
            enableContentTypeSniffing = preferences[PreferencesKeys.ENABLE_CONTENT_TYPE_SNIFFING] ?: true,
            maxSniffingRequestsPerPage = preferences[PreferencesKeys.MAX_SNIFFING_REQUESTS] ?: 10,

            autoplayNext = preferences[PreferencesKeys.AUTOPLAY_NEXT] ?: true,
            muteByDefault = preferences[PreferencesKeys.MUTE_BY_DEFAULT] ?: false,
            preloadAdjacentCount = preferences[PreferencesKeys.PRELOAD_ADJACENT_COUNT] ?: 1,
            pauseOnBackground = preferences[PreferencesKeys.PAUSE_ON_BACKGROUND] ?: true,

            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "dark",
            showFormatBadges = preferences[PreferencesKeys.SHOW_FORMAT_BADGES] ?: true,
            showSourceDomainLabels = preferences[PreferencesKeys.SHOW_SOURCE_DOMAIN_LABELS] ?: true
        )
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

    suspend fun updatePlaybackSettings(autoplayNext: Boolean, muteByDefault: Boolean, preloadCount: Int, pauseOnBackground: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTOPLAY_NEXT] = autoplayNext
            preferences[PreferencesKeys.MUTE_BY_DEFAULT] = muteByDefault
            preferences[PreferencesKeys.PRELOAD_ADJACENT_COUNT] = preloadCount.coerceIn(1, 3)
            preferences[PreferencesKeys.PAUSE_ON_BACKGROUND] = pauseOnBackground
        }
    }

    suspend fun updateAppearance(themeMode: String, showBadges: Boolean, showDomainLabels: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode
            preferences[PreferencesKeys.SHOW_FORMAT_BADGES] = showBadges
            preferences[PreferencesKeys.SHOW_SOURCE_DOMAIN_LABELS] = showDomainLabels
        }
    }
}
