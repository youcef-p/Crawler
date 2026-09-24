package com.example.reelscraper.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "site_profiles",
    indices = [
        Index(value = ["domain"], unique = true)
    ]
)
data class SiteProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val isEnabled: Boolean = true,
    val defaultDepth: Int = 2,
    val maxLinksPerPage: Int = 100,
    val maxPagesPerCrawl: Int = 50,
    val requestDelayMillis: Long = 100L,
    val timeoutMillis: Long = 8000L,
    val retryCount: Int = 2,
    val userAgentMode: String = "DEFAULT", // "DEFAULT", "DESKTOP", "MOBILE", "CUSTOM"
    val customUserAgent: String? = null,
    val customHeadersJson: String? = null,
    val allowedUrlPatterns: String? = null,
    val blockedUrlPatterns: String? = null,
    val allowedMediaTypes: String? = null,
    val disabledMediaTypes: String? = null,
    val enableHtmlExtraction: Boolean = true,
    val enableMetaExtraction: Boolean = true,
    val enableJsonLdExtraction: Boolean = true,
    val enableScriptExtraction: Boolean = true,
    val enableRegexExtraction: Boolean = true,
    val enableAttributeExtraction: Boolean = true,
    val enableIframeExtraction: Boolean = true,
    val enableFeedExtraction: Boolean = true,
    val enableSitemapExtraction: Boolean = true,
    val enableJsonApiExtraction: Boolean = true,
    val enableDynamicStreaming: Boolean = true,
    val dynamicMode: String = "AUTOMATIC", // "AUTOMATIC", "MANUAL_ONLY", "OFF"
    val enableWebViewFallback: Boolean = true,
    val enableLocalProxy: Boolean = false,
    val playbackHeadersJson: String? = null,
    val paginationMode: String = "NONE", // "NONE", "NEXT_LINK", "URL_TEMPLATE"
    val nextpageSelector: String? = null,
    val paginationUrlTemplate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
