package com.example.reelscraper.player

import com.example.reelscraper.data.local.HeatmapDao
import com.example.reelscraper.data.model.HeatmapBucket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Tracks and aggregates replay and seek activity into 5-second normalized buckets in Room.
 */
class HeatmapTracker(
    private val heatmapDao: HeatmapDao,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        const val BUCKET_INTERVAL_MS = 5000L // 5-second buckets
    }

    fun recordWatchSegment(mediaId: Long, currentPosMs: Long, isReplay: Boolean, isSeek: Boolean) {
        if (mediaId <= 0 || currentPosMs < 0) return
        val bucketStart = (currentPosMs / BUCKET_INTERVAL_MS) * BUCKET_INTERVAL_MS
        val bucketEnd = bucketStart + BUCKET_INTERVAL_MS

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val existing = heatmapDao.getBucket(mediaId, bucketStart)
                val newReplay = (existing?.replayCount ?: 0) + (if (isReplay) 1 else 0)
                val newSeek = (existing?.seekCount ?: 0) + (if (isSeek) 1 else 0)

                val bucket = HeatmapBucket(
                    id = existing?.id ?: 0,
                    mediaId = mediaId,
                    bucketStartMillis = bucketStart,
                    bucketEndMillis = bucketEnd,
                    replayCount = newReplay,
                    seekCount = newSeek,
                    updatedAt = System.currentTimeMillis()
                )
                heatmapDao.insertOrUpdateBucket(bucket)
            } catch (_: Exception) {}
        }
    }

    suspend fun getNormalizedHeatmap(mediaId: Long, totalDurationMs: Long): List<Float> = withContext(Dispatchers.IO) {
        if (totalDurationMs <= 0) return@withContext emptyList()
        val buckets = heatmapDao.getHeatmapForMediaDirect(mediaId)
        if (buckets.isEmpty()) return@withContext emptyList()

        val numSlices = 50
        val sliceDuration = max(1000L, totalDurationMs / numSlices)
        val sliceValues = FloatArray(numSlices) { 0f }

        for (bucket in buckets) {
            val sliceIdx = ((bucket.bucketStartMillis / sliceDuration)).toInt().coerceIn(0, numSlices - 1)
            val score = (bucket.replayCount * 2f) + bucket.seekCount.toFloat()
            sliceValues[sliceIdx] += score
        }

        val maxVal = sliceValues.maxOrNull() ?: 0f
        if (maxVal > 0f) {
            sliceValues.map { (it / maxVal).coerceIn(0f, 1f) }
        } else {
            emptyList()
        }
    }

    suspend fun clearHeatmap(mediaId: Long) = withContext(Dispatchers.IO) {
        heatmapDao.deleteForMedia(mediaId)
    }
}
