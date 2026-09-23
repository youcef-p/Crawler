package com.example.reelscraper.data.repository

import com.example.reelscraper.data.local.MediaDao
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.scraper.ScrapeProgress
import com.example.reelscraper.data.scraper.WebScraperEngine
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface MediaRepository {
    val allMedia: Flow<List<ScrapedMedia>>
    val mediaCount: Flow<Int>
    val distinctSourceDomains: Flow<List<String>>

    fun getFilteredMedia(keyword: String?, selectedDomains: Set<String>): Flow<List<ScrapedMedia>>
    fun searchMedia(query: String, filterType: MediaType?): Flow<List<ScrapedMedia>>
    fun getFavorites(): Flow<List<ScrapedMedia>>
    fun getMediaById(id: Long): Flow<ScrapedMedia?>

    suspend fun scrapeAndIndex(
        url: String,
        depth: Int = 2,
        settings: AppSettings,
        onProgress: (ScrapeProgress) -> Unit = {}
    ): Result<List<ScrapedMedia>>

    suspend fun insertMedia(media: ScrapedMedia): Long
    suspend fun toggleFavorite(mediaId: Long, isFavorite: Boolean)
    suspend fun deleteMedia(mediaId: Long)
    suspend fun clearAll()
    suspend fun clearDuplicates(): Int
    suspend fun exportJson(): String
    suspend fun importJson(json: String): Int
    suspend fun seedStarterSamplesIfEmpty()
}

class MediaRepositoryImpl(
    private val mediaDao: MediaDao,
    private val scraperEngine: WebScraperEngine,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MediaRepository {

    override val allMedia: Flow<List<ScrapedMedia>> = mediaDao.getAllMedia()
    override val mediaCount: Flow<Int> = mediaDao.getCount()
    override val distinctSourceDomains: Flow<List<String>> = mediaDao.getDistinctSourceDomains()

    override fun getFilteredMedia(keyword: String?, selectedDomains: Set<String>): Flow<List<ScrapedMedia>> {
        val cleanKeyword = keyword?.trim()?.ifBlank { null }
        val filterDomains = if (selectedDomains.isEmpty()) 0 else 1
        return mediaDao.getFilteredMedia(
            keyword = cleanKeyword,
            filterDomains = filterDomains,
            domains = selectedDomains.toList()
        )
    }

    override fun searchMedia(query: String, filterType: MediaType?): Flow<List<ScrapedMedia>> {
        val cleanQuery = query.trim()
        return if (filterType != null) {
            if (cleanQuery.isEmpty()) {
                mediaDao.getMediaByType(filterType)
            } else {
                mediaDao.searchMediaByType(cleanQuery, filterType)
            }
        } else {
            if (cleanQuery.isEmpty()) {
                mediaDao.getAllMedia()
            } else {
                mediaDao.searchMedia(cleanQuery)
            }
        }
    }

    override fun getFavorites(): Flow<List<ScrapedMedia>> = mediaDao.getFavorites()

    override fun getMediaById(id: Long): Flow<ScrapedMedia?> = mediaDao.getMediaById(id)

    override suspend fun scrapeAndIndex(
        url: String,
        depth: Int,
        settings: AppSettings,
        onProgress: (ScrapeProgress) -> Unit
    ): Result<List<ScrapedMedia>> = withContext(ioDispatcher) {
        try {
            val results = scraperEngine.crawlAndExtract(
                initialUrl = url,
                maxLevel = depth,
                settings = settings,
                onProgress = onProgress
            )

            // Deduplicate against database:
            // "If a matching item already exists in the database, skip the new item.
            // Keep the existing database entry. Do not insert a duplicate."
            val insertedList = mutableListOf<ScrapedMedia>()
            for (item in results) {
                val exists = mediaDao.existsByNormalizedNameAndDomain(item.normalizedName, item.sourceDomain) > 0
                if (!exists) {
                    val id = mediaDao.insertMedia(item)
                    if (id > 0) {
                        insertedList.add(item.copy(id = id))
                    }
                }
            }

            onProgress(
                ScrapeProgress(
                    statusMessage = "Crawl finished! Added ${insertedList.size} new items (${results.size - insertedList.size} duplicates skipped)",
                    currentUrl = url,
                    itemsFound = insertedList.size,
                    currentLevel = depth,
                    maxLevel = depth
                )
            )

            Result.success(insertedList)
        } catch (e: Exception) {
            onProgress(
                ScrapeProgress(
                    statusMessage = "Crawl failed: ${e.localizedMessage ?: "Unknown error"}",
                    currentUrl = url,
                    itemsFound = 0,
                    currentLevel = 1,
                    maxLevel = depth
                )
            )
            Result.failure(e)
        }
    }

    override suspend fun insertMedia(media: ScrapedMedia): Long = withContext(ioDispatcher) {
        mediaDao.insertMedia(media)
    }

    override suspend fun toggleFavorite(mediaId: Long, isFavorite: Boolean) = withContext(ioDispatcher) {
        mediaDao.updateFavoriteStatus(mediaId, isFavorite)
    }

    override suspend fun deleteMedia(mediaId: Long) = withContext(ioDispatcher) {
        mediaDao.deleteById(mediaId)
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        mediaDao.clearAll()
    }

    override suspend fun clearDuplicates(): Int = withContext(ioDispatcher) {
        mediaDao.clearDuplicates()
    }

    override suspend fun exportJson(): String = withContext(ioDispatcher) {
        val list = mediaDao.getAllMediaList()
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("url", item.url)
            obj.put("title", item.title)
            obj.put("mediaType", item.mediaType.name)
            obj.put("thumbnailUrl", item.thumbnailUrl ?: "")
            obj.put("sourcePageUrl", item.sourcePageUrl)
            obj.put("sourceDomain", item.sourceDomain)
            obj.put("normalizedName", item.normalizedName)
            obj.put("fileExtension", item.fileExtension)
            obj.put("discoveredTimestamp", item.discoveredTimestamp)
            array.put(obj)
        }
        array.toString(2)
    }

    override suspend fun importJson(json: String): Int = withContext(ioDispatcher) {
        try {
            val array = JSONArray(json)
            var count = 0
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val url = obj.getString("url")
                val title = obj.optString("title", "")
                val typeStr = obj.optString("mediaType", "VIDEO")
                val mediaType = try { MediaType.valueOf(typeStr) } catch (_: Exception) { MediaType.VIDEO }
                val thumb = obj.optString("thumbnailUrl").ifBlank { null }
                val sourcePageUrl = obj.optString("sourcePageUrl", url)
                val sourceDomain = obj.optString("sourceDomain", MediaNormalizer.normalizeDomain(sourcePageUrl))
                val normalizedName = obj.optString("normalizedName", MediaNormalizer.normalizeMediaName(url))
                val fileExt = obj.optString("fileExtension", MediaNormalizer.extractExtension(url))

                val media = ScrapedMedia(
                    url = url,
                    title = title,
                    mediaType = mediaType,
                    thumbnailUrl = thumb,
                    sourcePageUrl = sourcePageUrl,
                    sourceDomain = sourceDomain,
                    normalizedName = normalizedName,
                    fileExtension = fileExt
                )
                val res = mediaDao.insertMedia(media)
                if (res > 0) count++
            }
            count
        } catch (_: Exception) {
            0
        }
    }

    override suspend fun seedStarterSamplesIfEmpty() = withContext(ioDispatcher) {
        val existing = mediaDao.getAllExistingUrls()
        if (existing.isEmpty()) {
            val starterItems = listOf(
                ScrapedMedia(
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    title = "Big Buck Bunny - Open Source Film",
                    mediaType = MediaType.VIDEO,
                    thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                    sourcePageUrl = "https://peach.blender.org/",
                    sourceDomain = "peach.blender.org",
                    normalizedName = "bigbuckbunny",
                    fileExtension = "mp4"
                ),
                ScrapedMedia(
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    title = "Elephants Dream - 3D Animated Short",
                    mediaType = MediaType.VIDEO,
                    thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
                    sourcePageUrl = "https://orange.blender.org/",
                    sourceDomain = "orange.blender.org",
                    normalizedName = "elephantsdream",
                    fileExtension = "mp4"
                ),
                ScrapedMedia(
                    url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                    title = "Big Buck Bunny Multi-Rate HLS Stream",
                    mediaType = MediaType.HLS,
                    thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
                    sourcePageUrl = "https://mux.com/test-streams",
                    sourceDomain = "mux.com",
                    normalizedName = "x36xhzz",
                    fileExtension = "m3u8"
                ),
                ScrapedMedia(
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    title = "Chromecast - For Bigger Blazes",
                    mediaType = MediaType.VIDEO,
                    thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerBlazes.jpg",
                    sourcePageUrl = "https://google.com/chromecast",
                    sourceDomain = "google.com",
                    normalizedName = "forbiggerblazes",
                    fileExtension = "mp4"
                ),
                ScrapedMedia(
                    url = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExOHp1eDN2djE2eXFrc3R0N2t0cTlyYWl0OGNodnQ5M2p6bW1hZndnOSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/3o7TKSjRrfIPjeiVyM/giphy.gif",
                    title = "Retro Cyber Wave Animation",
                    mediaType = MediaType.GIF,
                    thumbnailUrl = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExOHp1eDN2djE2eXFrc3R0N2t0cTlyYWl0OGNodnQ5M2p6bW1hZndnOSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/3o7TKSjRrfIPjeiVyM/giphy.gif",
                    sourcePageUrl = "https://giphy.com",
                    sourceDomain = "giphy.com",
                    normalizedName = "retro-cyber-wave",
                    fileExtension = "gif"
                ),
                ScrapedMedia(
                    url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    title = "Tears of Steel - Sci-Fi VFX Showcase",
                    mediaType = MediaType.VIDEO,
                    thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
                    sourcePageUrl = "https://mango.blender.org/",
                    sourceDomain = "mango.blender.org",
                    normalizedName = "tearsofsteel",
                    fileExtension = "mp4"
                )
            )
            mediaDao.insertMediaList(starterItems)
        }
    }
}
