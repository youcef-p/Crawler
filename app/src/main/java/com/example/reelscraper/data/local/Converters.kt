package com.example.reelscraper.data.local

import androidx.room.TypeConverter
import com.example.reelscraper.data.model.CrawlJobStatus
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ChapterSource
import com.example.reelscraper.data.model.SubtitleFormat
import com.example.reelscraper.data.model.SubtitleSourceType
import com.example.reelscraper.data.model.TrickPlayType

class Converters {
    @TypeConverter
    fun fromMediaType(mediaType: MediaType): String {
        return mediaType.name
    }

    @TypeConverter
    fun toMediaType(value: String): MediaType {
        return try {
            MediaType.valueOf(value)
        } catch (_: Exception) {
            MediaType.VIDEO
        }
    }

    @TypeConverter
    fun fromCrawlJobStatus(status: CrawlJobStatus): String {
        return status.name
    }

    @TypeConverter
    fun toCrawlJobStatus(value: String): CrawlJobStatus {
        return try {
            CrawlJobStatus.valueOf(value)
        } catch (_: Exception) {
            CrawlJobStatus.COMPLETED
        }
    }
}

    @TypeConverter
    fun fromChapterSource(value: ChapterSource): String = value.name

    @TypeConverter
    fun toChapterSource(value: String): ChapterSource = try {
        ChapterSource.valueOf(value)
    } catch (_: Exception) {
        ChapterSource.METADATA
    }

    @TypeConverter
    fun fromSubtitleFormat(value: SubtitleFormat): String = value.name

    @TypeConverter
    fun toSubtitleFormat(value: String): SubtitleFormat = try {
        SubtitleFormat.valueOf(value)
    } catch (_: Exception) {
        SubtitleFormat.VTT
    }

    @TypeConverter
    fun fromSubtitleSourceType(value: SubtitleSourceType): String = value.name

    @TypeConverter
    fun toSubtitleSourceType(value: String): SubtitleSourceType = try {
        SubtitleSourceType.valueOf(value)
    } catch (_: Exception) {
        SubtitleSourceType.SCRAPED
    }

    @TypeConverter
    fun fromTrickPlayType(value: TrickPlayType): String = value.name

    @TypeConverter
    fun toTrickPlayType(value: String): TrickPlayType = try {
        TrickPlayType.valueOf(value)
    } catch (_: Exception) {
        TrickPlayType.LOCAL_FRAMES
    }
