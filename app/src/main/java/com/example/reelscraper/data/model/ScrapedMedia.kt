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
        Index(value = ["discoveredTimestamp"])
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
    val isFavorite: Boolean = false
) {
    // Aliases matching prompt specifications
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
            MediaType.HLS -> "HLS / M3U8"
            MediaType.DASH -> "DASH"
            MediaType.GIF -> "GIF"
            MediaType.VIDEO -> if (fileExtension.isNotBlank()) fileExtension.uppercase() else "MP4"
        }
}
