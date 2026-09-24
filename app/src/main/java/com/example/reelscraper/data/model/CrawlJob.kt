package com.example.reelscraper.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CrawlJobStatus {
    RUNNING,
    COMPLETED,
    CANCELLED,
    FAILED
}

@Entity(
    tableName = "crawl_jobs",
    indices = [
        Index(value = ["status"]),
        Index(value = ["startedAt"])
    ]
)
data class CrawlJob(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seedUrl: String,
    val maxDepth: Int,
    val status: CrawlJobStatus = CrawlJobStatus.RUNNING,
    val pagesVisited: Int = 0,
    val linksDiscovered: Int = 0,
    val mediaDiscovered: Int = 0,
    val mediaInserted: Int = 0,
    val duplicatesSkipped: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null
)
