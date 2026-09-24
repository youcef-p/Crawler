package com.example.reelscraper.data.scraper

import com.example.reelscraper.data.model.CrawlJobStatus

data class CrawlProgressState(
    val jobId: Long = 0L,
    val pagesScanned: Int = 0,
    val linksDiscovered: Int = 0,
    val mediaFound: Int = 0,
    val duplicatesSkipped: Int = 0,
    val rowsInserted: Int = 0,
    val currentDepth: Int = 1,
    val maxDepth: Int = 1,
    val currentUrl: String = "",
    val statusMessage: String = "Idle",
    val jobStatus: CrawlJobStatus = CrawlJobStatus.RUNNING,
    val isFinished: Boolean = false,
    val itemsKeptMessage: String? = null
)
