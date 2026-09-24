package com.example.reelscraper.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "extraction_rules",
    indices = [
        Index(value = ["domain"])
    ]
)
data class ExtractionRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val ruleType: String, // CSS_SELECTOR, ATTRIBUTE, REGEX, JSON_PATH, SCRIPT_VARIABLE, IFRAME_SRC, META_TAG, FEED_URL, SITEMAP_URL
    val selector: String? = null,
    val attribute: String? = null,
    val regex: String? = null,
    val jsonPath: String? = null,
    val scriptPattern: String? = null,
    val targetType: String = "MEDIA_URL", // MEDIA_URL, POSTER_URL, TITLE, PAGE_LINK, DYNAMIC_CANDIDATE, IGNORE_URL
    val titleSource: String? = null,
    val posterSource: String? = null,
    val priority: Int = 10,
    val enabled: Boolean = true
)
