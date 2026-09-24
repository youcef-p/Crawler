package com.example.reelscraper.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stream_sessions",
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["streamUrl"])
    ]
)
data class StreamSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Long = 0,
    val sourcePageUrl: String,
    val streamUrl: String,
    val streamType: String = "HLS",
    val httpMethod: String = "GET",
    val requestHeadersJson: String = "{}",
    val referer: String? = null,
    val userAgent: String? = null,
    val cookieHeader: String? = null,
    val capturedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val refreshPolicy: String = "ON_FAILURE", // ON_FAILURE, PERIODIC, ALWAYS
    val lastPlaybackFailure: String? = null,
    val lastRefreshResult: String? = null,
    val isActive: Boolean = true
)
