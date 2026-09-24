package com.example.reelscraper.data.scraper

import android.util.Log
import com.example.reelscraper.data.local.CrawlJobDao
import com.example.reelscraper.data.local.MediaDao
import com.example.reelscraper.data.model.CrawlJob
import com.example.reelscraper.data.model.CrawlJobStatus
import com.example.reelscraper.data.model.ScrapedMedia
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/**
 * Thread-safe live incremental media persister.
 * Inserts media into Room immediately per-page batch with OnConflictStrategy.IGNORE.
 * Never holds results in memory for end-of-crawl bulk insert.
 * Ensures mid-scan playability and full preservation of inserted items on stop/cancel/death.
 */
class MediaPersister(
    private val mediaDao: MediaDao,
    private val crawlJobDao: CrawlJobDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val TAG = "MediaPersister"
    }

    private val dbMutex = Mutex()
    private val sessionSeenKeys = Collections.synchronizedSet(mutableSetOf<Pair<String, String>>())

    private val _progress = MutableStateFlow(CrawlProgressState())
    val progress: StateFlow<CrawlProgressState> = _progress.asStateFlow()

    private val pagesScannedCounter = AtomicInteger(0)
    private val linksDiscoveredCounter = AtomicInteger(0)
    private val mediaFoundCounter = AtomicInteger(0)
    private val duplicatesSkippedCounter = AtomicInteger(0)
    private val rowsInsertedCounter = AtomicInteger(0)

    @Volatile
    private var currentJobId: Long = 0L

    /**
     * Initializes a new crawl session in the database.
     */
    suspend fun startJob(
        seedUrl: String,
        maxDepth: Int
    ): Long = withContext(ioDispatcher) {
        sessionSeenKeys.clear()
        pagesScannedCounter.set(0)
        linksDiscoveredCounter.set(0)
        mediaFoundCounter.set(0)
        duplicatesSkippedCounter.set(0)
        rowsInsertedCounter.set(0)

        val job = CrawlJob(
            seedUrl = seedUrl,
            maxDepth = maxDepth,
            status = CrawlJobStatus.RUNNING,
            startedAt = System.currentTimeMillis()
        )

        val id = crawlJobDao.insertJob(job)
        currentJobId = id

        _progress.value = CrawlProgressState(
            jobId = id,
            pagesScanned = 0,
            linksDiscovered = 0,
            mediaFound = 0,
            duplicatesSkipped = 0,
            rowsInserted = 0,
            currentDepth = 1,
            maxDepth = maxDepth,
            currentUrl = seedUrl,
            statusMessage = "Starting Level 1 scan on $seedUrl...",
            jobStatus = CrawlJobStatus.RUNNING,
            isFinished = false
        )

        id
    }

    /**
     * Called when new links are discovered on a page.
     */
    fun recordDiscoveredLinks(count: Int) {
        if (count > 0) {
            val total = linksDiscoveredCounter.addAndGet(count)
            _progress.update { it.copy(linksDiscovered = total) }
        }
    }

    /**
     * Called immediately when a page finishes extraction.
     * Persists the batch in a per-page transaction and updates live counters.
     */
    suspend fun persistPageBatch(
        pageUrl: String,
        depth: Int,
        maxDepth: Int,
        candidates: List<ScrapedMedia>
    ): List<ScrapedMedia> = withContext(ioDispatcher) {
        // NonCancellable ensures that an in-flight page batch is committed cleanly even if parent coroutine cancels
        withContext(NonCancellable) {
            val pCount = pagesScannedCounter.incrementAndGet()
            val insertedList = mutableListOf<ScrapedMedia>()

            if (candidates.isNotEmpty()) {
                val toInsert = mutableListOf<ScrapedMedia>()
                var sessionDuplicates = 0

                for (item in candidates) {
                    val key = Pair(item.normalizedName, item.sourceDomain)
                    if (sessionSeenKeys.add(key)) {
                        toInsert.add(item)
                    } else {
                        sessionDuplicates++
                    }
                }

                if (sessionDuplicates > 0) {
                    duplicatesSkippedCounter.addAndGet(sessionDuplicates)
                    mediaFoundCounter.addAndGet(sessionDuplicates)
                }

                if (toInsert.isNotEmpty()) {
                    dbMutex.withLock {
                        try {
                            val rowIds = mediaDao.insertMediaList(toInsert)
                            for (i in toInsert.indices) {
                                val rowId = rowIds.getOrNull(i) ?: -1L
                                if (rowId > 0) {
                                    rowsInsertedCounter.incrementAndGet()
                                    mediaFoundCounter.incrementAndGet()
                                    insertedList.add(toInsert[i].copy(id = rowId))
                                } else {
                                    duplicatesSkippedCounter.incrementAndGet()
                                    mediaFoundCounter.incrementAndGet()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error inserting page batch for $pageUrl", e)
                        }
                    }
                }
            }

            val curInserted = rowsInsertedCounter.get()
            val curFound = mediaFoundCounter.get()
            val curDupes = duplicatesSkippedCounter.get()
            val curLinks = linksDiscoveredCounter.get()

            // Update CrawlJob row in DB
            if (currentJobId > 0) {
                try {
                    crawlJobDao.updateProgress(
                        id = currentJobId,
                        pagesVisited = pCount,
                        linksDiscovered = curLinks,
                        mediaDiscovered = curFound,
                        mediaInserted = curInserted,
                        duplicatesSkipped = curDupes
                    )
                } catch (_: Exception) {}
            }

            val statusMsg = "Level $depth/$maxDepth: Scanned $pageUrl ($curInserted added, $curDupes dupes)"
            _progress.update {
                it.copy(
                    pagesScanned = pCount,
                    linksDiscovered = curLinks,
                    mediaFound = curFound,
                    duplicatesSkipped = curDupes,
                    rowsInserted = curInserted,
                    currentDepth = depth,
                    maxDepth = maxDepth,
                    currentUrl = pageUrl,
                    statusMessage = statusMsg
                )
            }

            insertedList
        }
    }

    /**
     * Finalizes the current job with status and message.
     * Safe across cancellations.
     */
    suspend fun finishJob(
        status: CrawlJobStatus,
        errorMessage: String? = null
    ): CrawlProgressState = withContext(ioDispatcher) {
        withContext(NonCancellable) {
            val finalInserted = rowsInsertedCounter.get()
            val finalFound = mediaFoundCounter.get()
            val finalDupes = duplicatesSkippedCounter.get()
            val finalPages = pagesScannedCounter.get()

            if (currentJobId > 0) {
                try {
                    crawlJobDao.updateStatus(
                        id = currentJobId,
                        status = status,
                        completedAt = System.currentTimeMillis(),
                        errorMessage = errorMessage
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating final job status", e)
                }
            }

            val itemsKeptMsg = when (status) {
                CrawlJobStatus.CANCELLED -> "Scan stopped. $finalInserted items kept in Library."
                CrawlJobStatus.COMPLETED -> "Scan complete! $finalInserted items saved, $finalDupes duplicates skipped."
                CrawlJobStatus.FAILED -> "Scan encountered an error. $finalInserted items kept."
                CrawlJobStatus.RUNNING -> null
            }

            val finalState = _progress.value.copy(
                pagesScanned = finalPages,
                mediaFound = finalFound,
                duplicatesSkipped = finalDupes,
                rowsInserted = finalInserted,
                jobStatus = status,
                isFinished = true,
                itemsKeptMessage = itemsKeptMsg,
                statusMessage = itemsKeptMsg ?: (errorMessage ?: "Scan finished")
            )

            _progress.value = finalState
            finalState
        }
    }
}
