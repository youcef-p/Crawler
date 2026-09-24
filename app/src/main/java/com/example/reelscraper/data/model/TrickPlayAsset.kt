package com.example.reelscraper.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TrickPlayType {
    SPRITE_SHEET,
    WEBVTT_MAP,
    LOCAL_FRAMES
}

@Entity(
    tableName = "trick_play_assets",
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["type"])
    ]
)
data class TrickPlayAsset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Long,
    val type: TrickPlayType,
    val imageUrl: String? = null,
    val vttUrl: String? = null,
    val tileWidth: Int = 160,
    val tileHeight: Int = 90,
    val columns: Int = 1,
    val rows: Int = 1,
    val intervalMillis: Long = 5000L,
    val createdAt: Long = System.currentTimeMillis()
)
