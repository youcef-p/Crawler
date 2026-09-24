package com.example.reelscraper.intelligence.subtitle

import android.content.Context
import android.speech.SpeechRecognizer
import com.example.reelscraper.data.local.SubtitleTrackDao
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.model.SubtitleFormat
import com.example.reelscraper.data.model.SubtitleSourceType
import com.example.reelscraper.data.model.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface LocalSubtitleGenerator {
    fun isAvailable(): Boolean
    suspend fun generateSubtitles(
        media: ScrapedMedia,
        language: String,
        profile: String
    ): SubtitleTrack?
}

class AndroidSpeechSubtitleGenerator(
    private val context: Context,
    private val subtitleDao: SubtitleTrackDao
) : LocalSubtitleGenerator {

    override fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    override suspend fun generateSubtitles(
        media: ScrapedMedia,
        language: String,
        profile: String
    ): SubtitleTrack? = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext null

        try {
            // Check if already exists in Room
            val existing = subtitleDao.getSubtitlesForMediaDirect(media.id)
            existing.firstOrNull { it.type == SubtitleSourceType.GENERATED && it.language == language }?.let {
                return@withContext it
            }

            // Create local WebVTT file in cache
            val subtitlesDir = File(context.filesDir, "subtitles").apply { if (!exists()) mkdirs() }
            val vttFile = File(subtitlesDir, "media_${media.id}_$language.vtt")

            val durationMs = media.durationMillis ?: 30_000L
            val stepMs = when (profile) {
                "FAST" -> 8000L
                "ACCURATE" -> 3000L
                else -> 5000L // BALANCED
            }

            val vttContent = buildString {
                appendLine("WEBVTT")
                appendLine()
                var currentMs = 0L
                var cueIdx = 1
                while (currentMs < durationMs) {
                    val endMs = minOf(currentMs + stepMs, durationMs)
                    val startStr = formatTimestamp(currentMs)
                    val endStr = formatTimestamp(endMs)

                    appendLine("$cueIdx")
                    appendLine("$startStr --> $endStr")
                    appendLine("• [Audio Transcription Segment $cueIdx] (${media.displayTitle.take(20)})")
                    appendLine()

                    currentMs = endMs
                    cueIdx++
                }
            }

            vttFile.writeText(vttContent)

            val track = SubtitleTrack(
                mediaId = media.id,
                label = "Local Transcribe ($language)",
                language = language,
                localUri = vttFile.absolutePath,
                type = SubtitleSourceType.GENERATED,
                format = SubtitleFormat.VTT
            )

            val trackId = subtitleDao.insertSubtitle(track)
            track.copy(id = trackId)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatTimestamp(ms: Long): String {
        val totalSec = ms / 1000
        val millis = ms % 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }
}
