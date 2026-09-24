package com.example.reelscraper.intelligence.phash

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 64-bit perceptual hash generator using grayscale block luminance comparison.
 * Calculates Hamming distance to identify visual duplicates regardless of filename or bitrate.
 */
object PerceptualHashGenerator {

    /**
     * Generates a 64-bit hex perceptual hash string from a representative video or GIF frame.
     */
    suspend fun generateHash(
        context: Context,
        media: ScrapedMedia
    ): String? = withContext(Dispatchers.IO) {
        var retriever: MediaMetadataRetriever? = null
        var frame: Bitmap? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(media.url, HashMap())
            // Sample frame at 1s or middle of video
            val targetTimeUs = 1_000_000L
            frame = retriever.getScaledFrameAtTime(targetTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 9, 8)
                ?: retriever.getFrameAtTime(targetTimeUs)
        } catch (_: Exception) {
            // Extraction failed
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {}
        }

        val bitmap = frame ?: return@withContext null
        try {
            // Downscale to 9x8 for difference hash (dHash)
            val scaled = if (bitmap.width != 9 || bitmap.height != 8) {
                Bitmap.createScaledBitmap(bitmap, 9, 8, true)
            } else {
                bitmap
            }

            var hash = 0L
            var bitIndex = 0

            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val pLeft = scaled.getPixel(x, y)
                    val pRight = scaled.getPixel(x + 1, y)

                    val lumLeft = (0.299f * ((pLeft shr 16) and 0xFF) +
                            0.587f * ((pLeft shr 8) and 0xFF) +
                            0.114f * (pLeft and 0xFF))

                    val lumRight = (0.299f * ((pRight shr 16) and 0xFF) +
                            0.587f * ((pRight shr 8) and 0xFF) +
                            0.114f * (pRight and 0xFF))

                    if (lumLeft > lumRight) {
                        hash = hash or (1L shl bitIndex)
                    }
                    bitIndex++
                }
            }

            return@withContext String.format("%016x", hash)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calculates Hamming distance between two 64-bit hex hash strings.
     * Distance <= 10 indicates high visual similarity / perceptual duplicate.
     */
    fun hammingDistance(hash1: String?, hash2: String?): Int {
        if (hash1.isNullOrBlank() || hash2.isNullOrBlank()) return 64
        val l1 = hash1.toULongOrNull(16) ?: return 64
        val l2 = hash2.toULongOrNull(16) ?: return 64
        return java.lang.Long.bitCount((l1 xor l2).toLong())
    }

    fun isDuplicateCandidate(hash1: String?, hash2: String?, threshold: Int = 10): Boolean {
        return hammingDistance(hash1, hash2) <= threshold
    }

    fun shouldGenerateForMedia(media: ScrapedMedia, mode: String): Boolean {
        return when (mode) {
            "OFF" -> false
            "GIFS_ONLY" -> media.mediaType == MediaType.GIF
            "SHORT_VIDEOS_ONLY" -> (media.durationMillis ?: 0L) in 1..60_000L
            "ALL_MEDIA" -> true
            else -> false
        }
    }
}
