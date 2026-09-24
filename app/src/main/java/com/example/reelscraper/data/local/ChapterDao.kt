package com.example.reelscraper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.reelscraper.data.model.Chapter
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE mediaId = :mediaId ORDER BY positionMillis ASC")
    fun getChaptersForMedia(mediaId: Long): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE mediaId = :mediaId ORDER BY positionMillis ASC")
    suspend fun getChaptersForMediaDirect(mediaId: Long): List<Chapter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: Chapter): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<Chapter>): List<Long>

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM chapters WHERE mediaId = :mediaId")
    suspend fun deleteForMedia(mediaId: Long)
}
