package com.example.reelscraper.data.local

import androidx.room.TypeConverter
import com.example.reelscraper.data.model.CrawlJobStatus
import com.example.reelscraper.data.model.MediaType

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
