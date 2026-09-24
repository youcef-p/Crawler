package com.example.reelscraper.data.repository

import com.example.reelscraper.data.local.CrawlJobDao
import com.example.reelscraper.data.local.MediaDao
import com.example.reelscraper.data.model.CrawlJobStatus
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.scraper.CrawlProgressState
import com.example.reelscraper.data.scraper.MediaPersister
import com.example.reelscraper.data.scraper.WebScraperEngine
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.util.MediaNormalizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface MediaRepository {
    val allMedia: Flow<List<ScrapedMedia>>
    val mediaCount: Flow<Int>
    val distinctSourceDomains: Flow<List<String>>
    val crawlProgress: StateFlow<CrawlProgressState>

    fun getFilteredMedia(keyword: String?, selectedDomains: Set<String>): Flow<List<ScrapedMedia>>
    fun getFilteredMediaAdvanced(
        keyword: String?,
        selectedDomains: Set<String>,
        selectedFormat: String?,
        onlyFavorites: Boolean,
        onlyDynamic: Boolean,
        hideBroken: Boolean
    ): Flow<List<ScrapedMedia>>

    fun searchMedia(query: String, filterType: MediaType?): Flow<List<ScrapedMedia>>
    fun getFavorites(): Flow<List<ScrapedMedia>>
    fun getDynamicStreams(): Flow<List<ScrapedMedia>>
    fun getBrokenMedia(): Flow<List<ScrapedMedia>>
    fun getMediaById(id: Long): Flow<ScrapedMedia?>
    suspend fun getMediaByIdDirect(id: Long): ScrapedMedia?

    suspend fun scrapeAndIndex(
        url: String,
        depth: Int = 2,
        settings: AppSettings,
        onProgress: (CrawlProgressState) -> Unit = {}
    ): Result<CrawlProgressState>

    suspend fun insertMedia(media: ScrapedMedia): Long
    suspend fun toggleFavorite(mediaId: Long, isFavorite: Boolean)
    suspend fun markBroken(mediaId: Long, isBroken: Boolean)
    suspend fun recordPlayback(mediaId: Long, positionMs: Long)
    suspend fun deleteMedia(mediaId: Long)
    suspend fun clearAll()
    suspend fun clearDuplicates(): Int
    suspend fun clearBrokenMedia(): Int
    suspend fun exportJson(): String
    suspend fun importJson(json: String): Int
    suspend fun seedStarterSamplesIfEmpty()
    suspend fun cleanupOrphanJobs(): Int
}

class MediaRepositoryImpl(
    private val mediaDao: MediaDao,
    private val crawlJobDao: CrawlJobDao,
    private val scraperEngine: WebScraperEngine,
    private val mediaPersister: MediaPersister = MediaPersister(mediaDao, crawlJobDao),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MediaRepository {

    override val allMedia: Flow<List<ScrapedMedia>> = mediaDao.getAllMedia()
    override val mediaCount: Flow<Int> = mediaDao.getCount()
    override val distinctSourceDomains: Flow<List<String>> = mediaDao.getDistinctSourceDomains()
    override val crawlProgress: StateFlow<CrawlProgressState> = mediaPersister.progress

    override fun getFilteredMedia(keyword: String?, selectedDomains: Set<String>): Flow<List<ScrapedMedia>> {
        val cleanKeyword = keyword?.trim()?.ifBlank { null }
        val filterDomains = if (selectedDomains.isEmpty()) 0 else 1
        return mediaDao.getFilteredMedia(
            keyword = cleanKeyword,
            filterDomains = filterDomains,
            domains = selectedDomains.toList()
        )
    }

    override fun getFilteredMediaAdvanced(
        keyword: String?,
        selectedDomains: Set<String>,
        selectedFormat: String?,
        onlyFavorites: Boolean,
        onlyDynamic: Boolean,
        hideBroken: Boolean
    ): Flow<List<ScrapedMedia>> {
        val cleanKeyword = keyword?.trim()?.ifBlank { null }
        val filterDomains = if (selectedDomains.isEmpty()) 0 else 1
        val format = selectedFormat?.lowercase() ?: ""
        val filterFormat = if (format.isNotBlank() && format != "all") 1 else 0
        val mediaTypeStr = when (format) {
            "hls", "m3u8" -> MediaType.HLS.name
            "dash", "mpd" -> MediaType.DASH.name
            "gif" -> MediaType.GIF.name
            "mp4", "webm" -> MediaType.VIDEO.name
            else -> ""
        }

        return mediaDao.getFilteredMediaAdvanced(
            keyword = cleanKeyword,
            filterDomains = filterDomains,
            domains = selectedDomains.toList(),
            filterFormat = filterFormat,
            format = format,
            mediaTypeStr = mediaTypeStr,
            onlyFavorites = if (onlyFavorites) 1 else 0,
            onlyDynamic = if (onlyDynamic) 1 else 0,
            hideBroken = if (hideBroken) 1 else 0
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
    override fun getDynamicStreams(): Flow<List<ScrapedMedia>> = mediaDao.getDynamicStreams()
    override fun getBrokenMedia(): Flow<List<ScrapedMedia>> = mediaDao.getBrokenMedia()

    override fun getMediaById(id: Long): Flow<ScrapedMedia?> = mediaDao.getMediaById(id)
    override suspend fun getMediaByIdDirect(id: Long): ScrapedMedia? = mediaDao.getMediaByIdDirect(id)

    override suspend fun scrapeAndIndex(
        url: String,
        depth: Int,
        settings: AppSettings,
        onProgress: (CrawlProgressState) -> Unit
    ): Result<CrawlProgressState> = withContext(ioDispatcher) {
        try {
            val finalState = scraperEngine.crawlAndExtract(
                initialUrl = url,
                maxLevel = depth,
                settings = settings,
                persister = mediaPersister
            )
            onProgress(finalState)
            Result.success(finalState)
        } catch (e: CancellationException) {
            val finalState = mediaPersister.progress.value
            onProgress(finalState)
            Result.success(finalState)
        } catch (e: Exception) {
            val finalState = mediaPersister.progress.value
            onProgress(finalState)
            Result.failure(e)
        }
    }

    override suspend fun insertMedia(media: ScrapedMedia): Long = withContext(ioDispatcher) {
        mediaDao.insertMedia(media)
    }

    override suspend fun toggleFavorite(mediaId: Long, isFavorite: Boolean) = withContext(ioDispatcher) {
        mediaDao.updateFavoriteStatus(mediaId, isFavorite)
    }

    override suspend fun markBroken(mediaId: Long, isBroken: Boolean) = withContext(ioDispatcher) {
        mediaDao.updateBrokenStatus(mediaId, isBroken)
    }

    override suspend fun recordPlayback(mediaId: Long, positionMs: Long) = withContext(ioDispatcher) {
        mediaDao.recordPlaybackState(mediaId, positionMs)
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

    override suspend fun clearBrokenMedia(): Int = withContext(ioDispatcher) {
        mediaDao.clearBrokenMedia()
    }

    override suspend fun cleanupOrphanJobs(): Int = withContext(ioDispatcher) {
        crawlJobDao.markOrphanRunningJobsCancelled()
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
            obj.put("isDynamic", item.isDynamic)
            obj.put("durationMillis", item.durationMillis ?: JSONObject.NULL)
            obj.put("width", item.width ?: JSONObject.NULL)
            obj.put("height", item.height ?: JSONObject.NULL)
            obj.put("hdrType", item.hdrType ?: JSONObject.NULL)
            obj.put("frameRate", item.frameRate ?: JSONObject.NULL)
            obj.put("fileExtension", item.fileExtension)
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
                val isDynamic = obj.optBoolean("isDynamic", false)
                val durationMillis = if (obj.isNull("durationMillis")) null else obj.optLong("durationMillis")
                val width = if (obj.isNull("width")) null else obj.optInt("width")
                val height = if (obj.isNull("height")) null else obj.optInt("height")
                val hdrType = obj.optString("hdrType").ifBlank { null }
                val frameRate = if (obj.isNull("frameRate")) null else obj.optDouble("frameRate").toFloat()

                val media = ScrapedMedia(
                    url = url,
                    title = title,
                    mediaType = mediaType,
                    thumbnailUrl = thumb,
                    sourcePageUrl = sourcePageUrl,
                    sourceDomain = sourceDomain,
                    normalizedName = normalizedName,
                    fileExtension = fileExt,
                    isDynamic = isDynamic,
                    durationMillis = durationMillis,
                    width = width,
                    height = height,
                    hdrType = hdrType,
                    frameRate = frameRate
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
        mediaDao.removeInvalidGoogleStorageSamples()

        val existing = mediaDao.getAllExistingUrls()
        if (existing.isEmpty()) {
            val starterItems = listOf(
                ScrapedMedia(
                    url = "https://media.w3.org/2010/05/sintel/trailer.mp4",
                    title = "Sintel - Open Source CGI Film Trailer",
                    mediaType = MediaType.VIDEO,
                    thumbnailUrl = "https://media.w3.org/2010/05/sintel/poster.png",
                    sourcePageUrl = "https://durian.blender.org/",
                    sourceDomain = "durian.blender.org",
                    normalizedName = "sintel-trailer",
                    fileExtension = "mp4"
                ),
                ScrapedMedia(
                    url = "https://media.w3.org/2010/05/bunny/trailer.mp4",
                    title = "Big Buck Bunny - Open Source Animation",
                    mediaType = MediaType.VIDEO,
                    thumbnailUrl = "https://media.w3.org/2010/05/bunny/poster.png",
                    sourcePageUrl = "https://peach.blender.org/",
                    sourceDomain = "peach.blender.org",
                    normalizedName = "big-buck-bunny-trailer",
                    fileExtension = "mp4"
                ),
                ScrapedMedia(
                    url = "https://vjs.zencdn.net/v/oceans.mp4",
                    title = "Oceans - Marine Life Documentary",
                    mediaType = MediaType.VIDEO,
                    thumbnailUrl = "https://vjs.zencdn.net/v/oceans.png",
                    sourcePageUrl = "https://videojs.com",
                    sourceDomain = "videojs.com",
                    normalizedName = "oceans-marine-life",
                    fileExtension = "mp4"
                ),
                ScrapedMedia(
                    url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                    title = "Big Buck Bunny Multi-Rate HLS Stream",
                    mediaType = MediaType.HLS,
                    thumbnailUrl = "https://media.w3.org/2010/05/bunny/poster.png",
                    sourcePageUrl = "https://mux.com/test-streams",
                    sourceDomain = "mux.com",
                    normalizedName = "x36xhzz-hls",
                    fileExtension = "m3u8",
                    isDynamic = true
                ),
                ScrapedMedia(
                    url = "https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd",
                    title = "Big Buck Bunny Adaptive 30fps DASH Stream",
                    mediaType = MediaType.DASH,
                    thumbnailUrl = "https://media.w3.org/2010/05/bunny/poster.png",
                    sourcePageUrl = "https://dashif.org",
                    sourceDomain = "dashif.org",
                    normalizedName = "bbb-30fps-dash",
                    fileExtension = "mpd",
                    isDynamic = true
                ),
                ScrapedMedia(
                    url = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                    title = "Macro Blooming Flower - MDN CC0",
                    mediaType = MediaType.VIDEO,
                    thumbnailUrl = "https://media.w3.org/2010/05/bunny/poster.png",
                    sourcePageUrl = "https://developer.mozilla.org",
                    sourceDomain = "developer.mozilla.org",
                    normalizedName = "mdn-blooming-flower",
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
                )
            )
            mediaDao.insertMediaList(starterItems)
        }
    }
}
