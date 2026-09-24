package com.example.reelscraper.player

import java.io.BufferedReader
import java.io.StringReader

data class WebVttCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val linePosition: Float? = null,
    val alignment: String = "center" // "left", "center", "right"
)

object WebVttParser {

    /**
     * Parses WebVTT cue text into structured WebVttCue objects.
     */
    fun parse(vttContent: String): List<WebVttCue> {
        val cues = mutableListOf<WebVttCue>()
        val reader = BufferedReader(StringReader(vttContent))
        var line: String? = reader.readLine()

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.contains("-->")) {
                val timeParts = trimmed.split("-->")
                if (timeParts.size == 2) {
                    val startStr = timeParts[0].trim()
                    val endAndSettings = timeParts[1].trim().split(" ")
                    val endStr = endAndSettings[0].trim()

                    val startMs = parseTimestamp(startStr)
                    val endMs = parseTimestamp(endStr)

                    var alignment = "center"
                    for (setting in endAndSettings.drop(1)) {
                        if (setting.startsWith("align:")) {
                            alignment = setting.substringAfter("align:")
                        }
                    }

                    val textBuilder = StringBuilder()
                    line = reader.readLine()
                    while (line != null && line.isNotBlank()) {
                        if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                        textBuilder.append(line.trim())
                        line = reader.readLine()
                    }

                    val rawText = textBuilder.toString()
                    val cleanText = stripOrConvertTags(rawText)

                    if (startMs >= 0 && endMs > startMs && cleanText.isNotBlank()) {
                        cues.add(
                            WebVttCue(
                                startTimeMs = startMs,
                                endTimeMs = endMs,
                                text = cleanText,
                                alignment = alignment
                            )
                        )
                    }
                }
            }
            line = reader.readLine()
        }

        return cues
    }

    private fun stripOrConvertTags(text: String): String {
        return text.replace(Regex("<[^>]*>"), "")
    }

    private fun parseTimestamp(timeStr: String): Long {
        return try {
            val parts = timeStr.split(":")
            if (parts.size == 3) {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val secParts = parts[2].split(".")
                val seconds = secParts[0].toLong()
                val millis = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0L
                (hours * 3600_000L) + (minutes * 60_000L) + (seconds * 1000L) + millis
            } else if (parts.size == 2) {
                val minutes = parts[0].toLong()
                val secParts = parts[1].split(".")
                val seconds = secParts[0].toLong()
                val millis = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0L
                (minutes * 60_000L) + (seconds * 1000L) + millis
            } else {
                -1L
            }
        } catch (_: Exception) {
            -1L
        }
    }
}
