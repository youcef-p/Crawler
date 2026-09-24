package com.example.reelscraper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.reelscraper.data.model.PlaybackState
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackStateDao {
    @Query("SELECT * FROM playback_states WHERE mediaId = :mediaId LIMIT 1")
    fun getPlaybackState(mediaId: Long): Flow<PlaybackState?>

    @Query("SELECT * FROM playback_states WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getPlaybackStateDirect(mediaId: Long): PlaybackState?

    @Query("SELECT * FROM playback_states ORDER BY lastPlayedAt DESC")
    fun getAllHistory(): Flow<List<PlaybackState>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaybackState(state: PlaybackState): Long

    @Query("UPDATE playback_states SET positionMillis = :posMs, durationMillis = :durMs, watchedPercent = :percent, lastPlayedAt = :timestamp, playCount = playCount + 1, completed = :completed WHERE mediaId = :mediaId")
    suspend fun updateProgress(
        mediaId: Long,
        posMs: Long,
        durMs: Long,
        percent: Float,
        completed: Boolean,
        timestamp: Long = System.currentTimeMillis()
    ): Int

    @Query("DELETE FROM playback_states WHERE mediaId = :mediaId")
    suspend fun deleteByMediaId(mediaId: Long)

    @Query("DELETE FROM playback_states")
    suspend fun clearHistory()
}
