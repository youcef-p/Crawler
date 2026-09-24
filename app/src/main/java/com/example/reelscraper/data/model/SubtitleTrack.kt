package com.example.reelscraper.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SubtitleSourceType {
    SCRAPED,
    GENERATED,
    MANUAL
}

enum class SubtitleFormat {
    VTT,
    SRT,
    ASS,
    SSA,
    DASH_TEXT,
    HLS_TEXT
}

@Entity(
    tableName = "subtitle_tracks",
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["language"])
    ]
)
data class SubtitleTrack(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Long,
    val label: String,
    val language: String,
    val sourceUrl: String? = null,
    val localUri: String? = null,
    val type: SubtitleSourceType = SubtitleSourceType.SCRAPED,
    val format: SubtitleFormat = SubtitleFormat.VTT,
    val createdAt: Long = System.currentTimeMillis()
)
