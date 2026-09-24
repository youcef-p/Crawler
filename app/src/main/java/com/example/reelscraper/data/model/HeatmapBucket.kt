package com.example.reelscraper.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "heatmap_buckets",
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["mediaId", "bucketStartMillis"], unique = true)
    ]
)
data class HeatmapBucket(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Long,
    val bucketStartMillis: Long,
    val bucketEndMillis: Long,
    val replayCount: Int = 0,
    val seekCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
