package com.example.reelscraper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.reelscraper.data.model.SubtitleTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtitleTrackDao {
    @Query("SELECT * FROM subtitle_tracks WHERE mediaId = :mediaId ORDER BY language ASC")
    fun getSubtitlesForMedia(mediaId: Long): Flow<List<SubtitleTrack>>

    @Query("SELECT * FROM subtitle_tracks WHERE mediaId = :mediaId ORDER BY language ASC")
    suspend fun getSubtitlesForMediaDirect(mediaId: Long): List<SubtitleTrack>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitle(track: SubtitleTrack): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitles(tracks: List<SubtitleTrack>): List<Long>

    @Query("DELETE FROM subtitle_tracks WHERE mediaId = :mediaId")
    suspend fun deleteForMedia(mediaId: Long)
}
