package com.example.reelscraper.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ChapterSource {
    MANUAL,
    SCENE_DETECTION,
    METADATA
}

@Entity(
    tableName = "chapters",
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["mediaId", "positionMillis"])
    ]
)
data class Chapter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Long,
    val title: String,
    val positionMillis: Long,
    val source: ChapterSource = ChapterSource.METADATA,
    val createdAt: Long = System.currentTimeMillis()
)
