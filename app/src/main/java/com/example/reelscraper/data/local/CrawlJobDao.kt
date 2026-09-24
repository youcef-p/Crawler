package com.example.reelscraper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.reelscraper.data.model.CrawlJob
import com.example.reelscraper.data.model.CrawlJobStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CrawlJobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: CrawlJob): Long

    @Update
    suspend fun updateJob(job: CrawlJob)

    @Query("""
        UPDATE crawl_jobs 
        SET pagesVisited = :pagesVisited, 
            linksDiscovered = :linksDiscovered, 
            mediaDiscovered = :mediaDiscovered, 
            mediaInserted = :mediaInserted, 
            duplicatesSkipped = :duplicatesSkipped
        WHERE id = :id
    """)
    suspend fun updateProgress(
        id: Long,
        pagesVisited: Int,
        linksDiscovered: Int,
        mediaDiscovered: Int,
        mediaInserted: Int,
        duplicatesSkipped: Int
    )

    @Query("UPDATE crawl_jobs SET status = :status, completedAt = :completedAt, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(
        id: Long,
        status: CrawlJobStatus,
        completedAt: Long? = System.currentTimeMillis(),
        errorMessage: String? = null
    )

    @Query("""
        UPDATE crawl_jobs 
        SET status = 'CANCELLED', 
            completedAt = :completedAt, 
            errorMessage = 'App restarted / terminated while scan was running' 
        WHERE status = 'RUNNING'
    """)
    suspend fun markOrphanRunningJobsCancelled(completedAt: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM crawl_jobs WHERE id = :id LIMIT 1")
    suspend fun getJobById(id: Long): CrawlJob?

    @Query("SELECT * FROM crawl_jobs WHERE status = 'RUNNING' ORDER BY startedAt DESC")
    fun getActiveJobs(): Flow<List<CrawlJob>>

    @Query("SELECT * FROM crawl_jobs ORDER BY startedAt DESC")
    fun getAllJobs(): Flow<List<CrawlJob>>

    @Query("DELETE FROM crawl_jobs WHERE id = :id")
    suspend fun deleteJobById(id: Long)
}
