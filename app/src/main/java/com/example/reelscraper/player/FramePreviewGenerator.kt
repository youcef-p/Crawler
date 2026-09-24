package com.example.reelscraper.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Lazy local frame extractor using MediaMetadataRetriever with bounded in-memory LRU and disk cache.
 * Keeps memory footprint minimal using low-resolution (160x90) preview bitmaps.
 */
class FramePreviewGenerator(private val context: Context) {

    private val cacheDir = File(context.cacheDir, "trickplay_frames").apply {
        if (!exists()) mkdirs()
    }

    // 4MB in-memory LRU cache
    private val memoryCache = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    /**
     * Retrieves or generates low-resolution preview frame at specified position in milliseconds.
     */
    suspend fun getPreviewFrame(
        mediaId: Long,
        videoUrl: String,
        positionMs: Long,
        targetWidth: Int = 160,
        targetHeight: Int = 90
    ): Bitmap? = withContext(Dispatchers.IO) {
        // Round to nearest 2-second bucket to reuse generated frames
        val bucketedTimeMs = (max(0L, positionMs) / 2000L) * 2000L
        val cacheKey = "${mediaId}_${bucketedTimeMs}"

        // 1. Check Memory Cache
        memoryCache.get(cacheKey)?.let { return@withContext it }

        // 2. Check Disk Cache
        val diskFile = File(cacheDir, "${cacheKey}.jpg")
        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bitmap != null) {
                    memoryCache.put(cacheKey, bitmap)
                    return@withContext bitmap
                }
            } catch (_: Exception) {}
        }

        // 3. Generate lazily using MediaMetadataRetriever on background thread
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoUrl, HashMap())

            val timeMicros = bucketedTimeMs * 1000L
            val rawFrame = retriever.getScaledFrameAtTime(
                timeMicros,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                targetWidth,
                targetHeight
            ) ?: retriever.getFrameAtTime(timeMicros, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

            if (rawFrame != null) {
                val scaled = if (rawFrame.width != targetWidth || rawFrame.height != targetHeight) {
                    Bitmap.createScaledBitmap(rawFrame, targetWidth, targetHeight, true)
                } else {
                    rawFrame
                }

                // Save to disk cache
                FileOutputStream(diskFile).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
                }

                memoryCache.put(cacheKey, scaled)
                trimDiskCacheIfNeeded()
                return@withContext scaled
            }
        } catch (_: Exception) {
            // Extraction failed (e.g. unsupported stream format or network unreachable)
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {}
        }

        null
    }

    private fun trimDiskCacheIfNeeded() {
        try {
            val files = cacheDir.listFiles() ?: return
            // Keep max 150 frame files (~3MB)
            if (files.size > 150) {
                files.sortBy { it.lastModified() }
                for (i in 0 until (files.size - 100)) {
                    files[i].delete()
                }
            }
        } catch (_: Exception) {}
    }

    fun clearCache() {
        memoryCache.evictAll()
        try {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
        } catch (_: Exception) {}
    }
}
