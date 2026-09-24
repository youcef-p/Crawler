package com.example.reelscraper.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MediaType {
    VIDEO,
    HLS,
    DASH,
    GIF
}

@Entity(
    tableName = "scraped_media",
    indices = [
        Index(value = ["normalizedName", "sourceDomain"], unique = true),
        Index(value = ["sourceDomain"]),
        Index(value = ["mediaType"]),
        Index(value = ["discoveredTimestamp"]),
        Index(value = ["isDynamic"]),
        Index(value = ["isBroken"]),
        Index(value = ["isFavorite"]),
        Index(value = ["lastPlayedTimestamp"])
    ]
)
data class ScrapedMedia(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val mediaType: MediaType,
    val thumbnailUrl: String? = null,
    val sourcePageUrl: String,
    val sourceDomain: String,
    val normalizedName: String = "",
    val fileExtension: String = "",
    val durationMillis: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val extractorType: String = "HTML",
    val discoveredTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isDynamic: Boolean = false,
    val streamSessionId: Long? = null,
    val playbackHeadersJson: String? = null,
    val isBroken: Boolean = false,
    val playCount: Int = 0,
    val lastPositionMs: Long = 0L,
    val lastPlayedTimestamp: Long? = null,

    // Advanced streaming & media intelligence additions
    val hdrType: String? = null, // SDR, HDR10, HLG
    val frameRate: Float? = null, // e.g. 30f, 60f
    val averageBitrate: Long? = null,
    val pHash: String? = null,
    val smartReframeEnabled: Boolean = false,
    val ambientPaletteUri: String? = null,
    val manifestTrackInfoJson: String? = null
) {
    val mediaUrl: String get() = url
    val pageUrl: String get() = sourcePageUrl
    val posterUrl: String? get() = thumbnailUrl
    val format: String get() = fileExtension
    val createdAt: Long get() = discoveredTimestamp

    val displayTitle: String
        get() = if (title.isNotBlank()) title else normalizedName.ifBlank { "Media #$id" }

    val isStream: Boolean
        get() = mediaType == MediaType.HLS || mediaType == MediaType.DASH

    val typeBadge: String
        get() = when (mediaType) {
            MediaType.HLS -> "HLS"
            MediaType.DASH -> "DASH"
            MediaType.GIF -> "GIF"
            MediaType.VIDEO -> if (fileExtension.isNotBlank()) fileExtension.uppercase() else "MP4"
        }

    val isHfr: Boolean get() = (frameRate ?: 0f) >= 55f
    val isHdr: Boolean get() = hdrType != null && hdrType != "SDR"
}
