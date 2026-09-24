package com.example.reelscraper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.reelscraper.data.model.StreamSession
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamSessionDao {
    @Query("SELECT * FROM stream_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): StreamSession?

    @Query("SELECT * FROM stream_sessions WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getSessionByMediaId(mediaId: Long): StreamSession?

    @Query("SELECT * FROM stream_sessions WHERE streamUrl = :streamUrl LIMIT 1")
    suspend fun getSessionByStreamUrl(streamUrl: String): StreamSession?

    @Query("SELECT * FROM stream_sessions ORDER BY capturedAt DESC")
    fun getAllSessions(): Flow<List<StreamSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StreamSession): Long

    @Update
    suspend fun updateSession(session: StreamSession)

    @Delete
    suspend fun deleteSession(session: StreamSession)

    @Query("UPDATE stream_sessions SET isActive = 0 WHERE mediaId = :mediaId")
    suspend fun deactivateSessionsForMedia(mediaId: Long)

    @Query("DELETE FROM stream_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM stream_sessions WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun deleteExpiredSessions(now: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM stream_sessions")
    suspend fun clearAll()
}
