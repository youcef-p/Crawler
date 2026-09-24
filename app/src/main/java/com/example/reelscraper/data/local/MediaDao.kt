package com.example.reelscraper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("""
        SELECT * FROM scraped_media
        ORDER BY
            CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END,
            CASE WHEN isBroken = 1 THEN 1 ELSE 0 END,
            CASE WHEN lastPlayedTimestamp IS NOT NULL THEN 0 ELSE 1 END,
            discoveredTimestamp DESC
    """)
    fun getAllMedia(): Flow<List<ScrapedMedia>>

    @Query("SELECT * FROM scraped_media ORDER BY discoveredTimestamp DESC")
    suspend fun getAllMediaList(): List<ScrapedMedia>

    @Query("SELECT * FROM scraped_media WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' OR normalizedName LIKE '%' || :query || '%' ORDER BY discoveredTimestamp DESC")
    fun searchMedia(query: String): Flow<List<ScrapedMedia>>

    @Query("SELECT * FROM scraped_media WHERE mediaType = :type ORDER BY discoveredTimestamp DESC")
    fun getMediaByType(type: MediaType): Flow<List<ScrapedMedia>>

    @Query("SELECT * FROM scraped_media WHERE (title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%') AND mediaType = :type ORDER BY discoveredTimestamp DESC")
    fun searchMediaByType(query: String, type: MediaType): Flow<List<ScrapedMedia>>

    @Query("SELECT * FROM scraped_media WHERE isFavorite = 1 ORDER BY discoveredTimestamp DESC")
    fun getFavorites(): Flow<List<ScrapedMedia>>

    @Query("SELECT * FROM scraped_media WHERE isDynamic = 1 ORDER BY discoveredTimestamp DESC")
    fun getDynamicStreams(): Flow<List<ScrapedMedia>>

    @Query("SELECT * FROM scraped_media WHERE isBroken = 1 ORDER BY discoveredTimestamp DESC")
    fun getBrokenMedia(): Flow<List<ScrapedMedia>>

    @Query("SELECT * FROM scraped_media WHERE id = :id LIMIT 1")
    fun getMediaById(id: Long): Flow<ScrapedMedia?>

    @Query("SELECT * FROM scraped_media WHERE id = :id LIMIT 1")
    suspend fun getMediaByIdDirect(id: Long): ScrapedMedia?

    @Query("SELECT COUNT(*) FROM scraped_media")
    fun getCount(): Flow<Int>

    @Query("SELECT DISTINCT sourceDomain FROM scraped_media WHERE sourceDomain != '' ORDER BY sourceDomain ASC")
    fun getDistinctSourceDomains(): Flow<List<String>>

    @Query("""
        SELECT * FROM scraped_media
        WHERE (:keyword IS NULL OR :keyword = '' OR 
               title LIKE '%' || :keyword || '%' OR 
               url LIKE '%' || :keyword || '%' OR 
               sourceDomain LIKE '%' || :keyword || '%' OR 
               sourcePageUrl LIKE '%' || :keyword || '%' OR 
               normalizedName LIKE '%' || :keyword || '%')
          AND (:filterDomains = 0 OR sourceDomain IN (:domains))
          AND (:filterFormat = 0 OR fileExtension = :format OR mediaType = :mediaTypeStr)
          AND (:onlyFavorites = 0 OR isFavorite = 1)
          AND (:onlyDynamic = 0 OR isDynamic = 1)
          AND (:hideBroken = 0 OR isBroken = 0)
        ORDER BY
            CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END,
            CASE WHEN isBroken = 1 THEN 1 ELSE 0 END,
            discoveredTimestamp DESC
    """)
    fun getFilteredMediaAdvanced(
        keyword: String?,
        filterDomains: Int,
        domains: List<String>,
        filterFormat: Int,
        format: String,
        mediaTypeStr: String,
        onlyFavorites: Int,
        onlyDynamic: Int,
        hideBroken: Int
    ): Flow<List<ScrapedMedia>>

    @Query("""
        SELECT * FROM scraped_media
        WHERE (:keyword IS NULL OR :keyword = '' OR 
               title LIKE '%' || :keyword || '%' OR 
               url LIKE '%' || :keyword || '%' OR 
               sourceDomain LIKE '%' || :keyword || '%' OR 
               sourcePageUrl LIKE '%' || :keyword || '%' OR 
               normalizedName LIKE '%' || :keyword || '%')
          AND (:filterDomains = 0 OR sourceDomain IN (:domains))
        ORDER BY discoveredTimestamp DESC
    """)
    fun getFilteredMedia(keyword: String?, filterDomains: Int, domains: List<String>): Flow<List<ScrapedMedia>>

    @Query("SELECT url FROM scraped_media")
    suspend fun getAllExistingUrls(): List<String>

    @Query("DELETE FROM scraped_media WHERE url LIKE '%commondatastorage.googleapis.com/gtv-videos-bucket%'")
    suspend fun removeInvalidGoogleStorageSamples(): Int

    @Query("SELECT COUNT(*) FROM scraped_media WHERE normalizedName = :normalizedName AND sourceDomain = :sourceDomain")
    suspend fun existsByNormalizedNameAndDomain(normalizedName: String, sourceDomain: String): Int

    @Query("UPDATE scraped_media SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE scraped_media SET isBroken = :isBroken WHERE id = :id")
    suspend fun updateBrokenStatus(id: Long, isBroken: Boolean)

    @Query("UPDATE scraped_media SET url = :newUrl, playbackHeadersJson = :newHeaders, isBroken = 0 WHERE id = :id")
    suspend fun updateRefreshedStreamUrl(id: Long, newUrl: String, newHeaders: String?)

    @Query("UPDATE scraped_media SET playCount = playCount + 1, lastPositionMs = :positionMs, lastPlayedTimestamp = :timestamp WHERE id = :id")
    suspend fun recordPlaybackState(id: Long, positionMs: Long, timestamp: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMedia(media: ScrapedMedia): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMediaList(mediaList: List<ScrapedMedia>): List<Long>

    @Update
    suspend fun updateMedia(media: ScrapedMedia)

    @Delete
    suspend fun deleteMedia(media: ScrapedMedia)

    @Query("DELETE FROM scraped_media WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scraped_media")
    suspend fun clearAll()

    @Query("DELETE FROM scraped_media WHERE isBroken = 1")
    suspend fun clearBrokenMedia(): Int

    @Query("DELETE FROM scraped_media WHERE id NOT IN (SELECT MIN(id) FROM scraped_media GROUP BY url)")
    suspend fun clearDuplicates(): Int
}
