package com.example.reelscraper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.reelscraper.data.model.HeatmapBucket
import kotlinx.coroutines.flow.Flow

@Dao
interface HeatmapDao {
    @Query("SELECT * FROM heatmap_buckets WHERE mediaId = :mediaId ORDER BY bucketStartMillis ASC")
    fun getHeatmapForMedia(mediaId: Long): Flow<List<HeatmapBucket>>

    @Query("SELECT * FROM heatmap_buckets WHERE mediaId = :mediaId ORDER BY bucketStartMillis ASC")
    suspend fun getHeatmapForMediaDirect(mediaId: Long): List<HeatmapBucket>

    @Query("SELECT * FROM heatmap_buckets WHERE mediaId = :mediaId AND bucketStartMillis = :startMs LIMIT 1")
    suspend fun getBucket(mediaId: Long, startMs: Long): HeatmapBucket?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBucket(bucket: HeatmapBucket): Long

    @Query("DELETE FROM heatmap_buckets WHERE mediaId = :mediaId")
    suspend fun deleteForMedia(mediaId: Long)
}
