package com.example.reelscraper.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.reelscraper.data.model.TrickPlayAsset
import kotlinx.coroutines.flow.Flow

@Dao
interface TrickPlayDao {
    @Query("SELECT * FROM trick_play_assets WHERE mediaId = :mediaId LIMIT 1")
    fun getTrickPlayForMedia(mediaId: Long): Flow<TrickPlayAsset?>

    @Query("SELECT * FROM trick_play_assets WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getTrickPlayForMediaDirect(mediaId: Long): TrickPlayAsset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrickPlay(asset: TrickPlayAsset): Long

    @Query("DELETE FROM trick_play_assets WHERE mediaId = :mediaId")
    suspend fun deleteForMedia(mediaId: Long)
}
