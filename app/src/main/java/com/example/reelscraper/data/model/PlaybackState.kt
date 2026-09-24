package com.example.reelscraper.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_states",
    indices = [
        Index(value = ["mediaId"], unique = true),
        Index(value = ["lastPlayedAt"])
    ]
)
data class PlaybackState(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Long,
    val positionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val watchedPercent: Float = 0f,
    val playCount: Int = 0,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val completed: Boolean = false,
    val preferredAudioTrack: String? = null,
    val preferredSubtitleTrack: String? = null,
    val preferredQuality: String? = null
)
