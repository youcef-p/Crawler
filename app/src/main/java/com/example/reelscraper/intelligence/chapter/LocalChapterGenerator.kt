package com.example.reelscraper.intelligence.chapter

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.example.reelscraper.data.local.ChapterDao
import com.example.reelscraper.data.model.Chapter
import com.example.reelscraper.data.model.ChapterSource
import com.example.reelscraper.data.model.ScrapedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

class LocalChapterGenerator(
    private val chapterDao: ChapterDao
) {
    /**
     * Analyzes video duration and generates scene-change chapters locally.
     * Skips videos under 45 seconds.
     */
    suspend fun generateChaptersIfNeeded(media: ScrapedMedia): List<Chapter> = withContext(Dispatchers.IO) {
        val existing = chapterDao.getChaptersForMediaDirect(media.id)
        if (existing.isNotEmpty()) return@withContext existing

        val durationMs = media.durationMillis ?: 0L
        if (durationMs < 45_000L) {
            // Short clip: single Intro chapter
            val intro = Chapter(
                mediaId = media.id,
                title = "Intro",
                positionMillis = 0L,
                source = ChapterSource.SCENE_DETECTION
            )
            chapterDao.insertChapter(intro)
            return@withContext listOf(intro)
        }

        // Lightweight scene change sampling at intervals
        val chapters = mutableListOf<Chapter>()
        chapters.add(Chapter(mediaId = media.id, title = "Introduction", positionMillis = 0L, source = ChapterSource.SCENE_DETECTION))

        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(media.url, HashMap())

            val sampleIntervalMs = if (durationMs > 600_000L) 60_000L else 30_000L
            var prevFrame: Bitmap? = null
            var chapterIndex = 1

            var currentSampleMs = sampleIntervalMs
            while (currentSampleMs < durationMs - 10_000L) {
                val frame = retriever.getScaledFrameAtTime(
                    currentSampleMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    32,
                    18
                )

                if (frame != null && prevFrame != null) {
                    val diff = calculateHistogramDiff(prevFrame, frame)
                    // High scene change threshold
                    if (diff > 0.35f) {
                        chapters.add(
                            Chapter(
                                mediaId = media.id,
                                title = "Scene $chapterIndex",
                                positionMillis = currentSampleMs,
                                source = ChapterSource.SCENE_DETECTION
                            )
                        )
                        chapterIndex++
                    }
                }
                prevFrame = frame
                currentSampleMs += sampleIntervalMs
            }
        } catch (_: Exception) {
            // If retriever fails, generate regular thematic timeline intervals
            val step = max(30_000L, durationMs / 4)
            var pos = step
            var idx = 1
            while (pos < durationMs - 15_000L) {
                chapters.add(
                    Chapter(
                        mediaId = media.id,
                        title = "Part $idx",
                        positionMillis = pos,
                        source = ChapterSource.SCENE_DETECTION
                    )
                )
                pos += step
                idx++
            }
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {}
        }

        chapterDao.insertChapters(chapters)
        chapters
    }

    suspend fun addManualChapter(mediaId: Long, title: String, positionMs: Long): Chapter = withContext(Dispatchers.IO) {
        val chapter = Chapter(
            mediaId = mediaId,
            title = title.ifBlank { "Marker" },
            positionMillis = max(0L, positionMs),
            source = ChapterSource.MANUAL
        )
        val id = chapterDao.insertChapter(chapter)
        chapter.copy(id = id)
    }

    suspend fun deleteChapter(id: Long) = withContext(Dispatchers.IO) {
        chapterDao.deleteById(id)
    }

    private fun calculateHistogramDiff(b1: Bitmap, b2: Bitmap): Float {
        var totalDiff = 0f
        val w = minOf(b1.width, b2.width)
        val h = minOf(b1.height, b2.height)
        val total = w * h

        for (y in 0 until h) {
            for (x in 0 until w) {
                val p1 = b1.getPixel(x, y)
                val p2 = b2.getPixel(x, y)

                val r1 = (p1 shr 16) and 0xFF
                val g1 = (p1 shr 8) and 0xFF
                val b1v = p1 and 0xFF

                val r2 = (p2 shr 16) and 0xFF
                val g2 = (p2 shr 8) and 0xFF
                val b2v = p2 and 0xFF

                val lum1 = (0.299f * r1 + 0.587f * g1 + 0.114f * b1v) / 255f
                val lum2 = (0.299f * r2 + 0.587f * g2 + 0.114f * b2v) / 255f

                totalDiff += abs(lum1 - lum2)
            }
        }
        return if (total > 0) totalDiff / total else 0f
    }
}
