package com.example.reelscraper

import com.example.reelscraper.data.local.MediaDao
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.repository.MediaRepositoryImpl
import com.example.reelscraper.data.scraper.WebScraperEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeMediaDao : MediaDao {
    val items = mutableListOf<ScrapedMedia>()
    private val stateFlow = MutableStateFlow<List<ScrapedMedia>>(emptyList())
    private var nextId = 1L

    private fun notifyChanges() {
        stateFlow.value = items.toList()
    }

    override fun getAllMedia(): Flow<List<ScrapedMedia>> = stateFlow

    override fun getFilteredMedia(keyword: String?, filterDomains: Int, domains: List<String>): Flow<List<ScrapedMedia>> = stateFlow.map { list ->
        list.filter { item ->
            val matchesDomain = filterDomains == 0 || domains.contains(item.sourceDomain)
            val matchesKeyword = keyword == null ||
                    item.title.contains(keyword, ignoreCase = true) ||
                    item.url.contains(keyword, ignoreCase = true) ||
                    item.sourceDomain.contains(keyword, ignoreCase = true) ||
                    item.sourcePageUrl.contains(keyword, ignoreCase = true) ||
                    item.normalizedName.contains(keyword, ignoreCase = true)
            matchesDomain && matchesKeyword
        }
    }

    override fun getDistinctSourceDomains(): Flow<List<String>> = stateFlow.map { list ->
        list.map { it.sourceDomain }.distinct()
    }

    override suspend fun existsByNormalizedNameAndDomain(normalizedName: String, sourceDomain: String): Int {
        return items.count { it.normalizedName == normalizedName && it.sourceDomain == sourceDomain }
    }

    override suspend fun clearDuplicates(): Int {
        val unique = items.distinctBy { Pair(it.normalizedName, it.sourceDomain) }
        val diff = items.size - unique.size
        items.clear()
        items.addAll(unique)
        notifyChanges()
        return diff
    }

    override fun searchMedia(query: String): Flow<List<ScrapedMedia>> = stateFlow.map { list ->
        list.filter { it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true) }
    }

    override fun getMediaByType(type: MediaType): Flow<List<ScrapedMedia>> = stateFlow.map { list ->
        list.filter { it.mediaType == type }
    }

    override fun searchMediaByType(query: String, type: MediaType): Flow<List<ScrapedMedia>> = stateFlow.map { list ->
        list.filter {
            it.mediaType == type && (it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true))
        }
    }

    override fun getFavorites(): Flow<List<ScrapedMedia>> = stateFlow.map { list ->
        list.filter { it.isFavorite }
    }

    override fun getMediaById(id: Long): Flow<ScrapedMedia?> = stateFlow.map { list ->
        list.find { it.id == id }
    }

    override fun getCount(): Flow<Int> = stateFlow.map { it.size }

    override suspend fun getAllExistingUrls(): List<String> = items.map { it.url }

    override suspend fun getAllMediaList(): List<ScrapedMedia> = items.toList()

    override suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean) {
        val index = items.indexOfFirst { it.id == id }
        if (index != -1) {
            items[index] = items[index].copy(isFavorite = isFavorite)
            notifyChanges()
        }
    }

    override suspend fun insertMedia(media: ScrapedMedia): Long {
        val id = if (media.id == 0L) nextId++ else media.id
        val newMedia = media.copy(id = id)
        items.add(newMedia)
        notifyChanges()
        return id
    }

    override suspend fun insertMediaList(mediaList: List<ScrapedMedia>): List<Long> {
        val ids = mutableListOf<Long>()
        for (item in mediaList) {
            ids.add(insertMedia(item))
        }
        return ids
    }

    override suspend fun updateMedia(media: ScrapedMedia) {
        val index = items.indexOfFirst { it.id == media.id }
        if (index != -1) {
            items[index] = media
            notifyChanges()
        }
    }

    override suspend fun deleteMedia(media: ScrapedMedia) {
        items.removeAll { it.id == media.id }
        notifyChanges()
    }

    override suspend fun deleteById(id: Long) {
        items.removeAll { it.id == id }
        notifyChanges()
    }

    override suspend fun clearAll() {
        items.clear()
        notifyChanges()
    }
}

class MediaRepositoryTest {

    private lateinit var fakeDao: FakeMediaDao
    private lateinit var scraperEngine: WebScraperEngine
    private lateinit var repository: MediaRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        fakeDao = FakeMediaDao()
        scraperEngine = WebScraperEngine()
        repository = MediaRepositoryImpl(
            mediaDao = fakeDao,
            scraperEngine = scraperEngine,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun insertAndQueryMedia() = runTest(testDispatcher) {
        val media = ScrapedMedia(
            url = "https://example.com/test.mp4",
            title = "Test Video",
            mediaType = MediaType.VIDEO,
            sourcePageUrl = "https://example.com",
            sourceDomain = "example.com",
            fileExtension = "mp4"
        )

        val id = repository.insertMedia(media)
        assertTrue(id > 0)

        val all = repository.allMedia.first()
        assertEquals(1, all.size)
        assertEquals("Test Video", all[0].title)
    }

    @Test
    fun searchAndFilterMedia() = runTest(testDispatcher) {
        repository.insertMedia(
            ScrapedMedia(
                url = "https://example.com/bunny.mp4",
                title = "Big Buck Bunny",
                mediaType = MediaType.VIDEO,
                sourcePageUrl = "https://example.com",
                sourceDomain = "example.com"
            )
        )
        repository.insertMedia(
            ScrapedMedia(
                url = "https://example.com/live.m3u8",
                title = "Live Stream",
                mediaType = MediaType.HLS,
                sourcePageUrl = "https://example.com",
                sourceDomain = "example.com"
            )
        )

        val searchResult = repository.searchMedia("Bunny", null).first()
        assertEquals(1, searchResult.size)
        assertEquals("Big Buck Bunny", searchResult[0].title)

        val hlsResult = repository.searchMedia("", MediaType.HLS).first()
        assertEquals(1, hlsResult.size)
        assertEquals(MediaType.HLS, hlsResult[0].mediaType)
    }

    @Test
    fun toggleFavoriteAndCount() = runTest(testDispatcher) {
        val id = repository.insertMedia(
            ScrapedMedia(
                url = "https://example.com/art.gif",
                title = "Pixel Art",
                mediaType = MediaType.GIF,
                sourcePageUrl = "https://example.com",
                sourceDomain = "example.com"
            )
        )

        repository.toggleFavorite(id, true)
        val favorites = repository.getFavorites().first()
        assertEquals(1, favorites.size)
        assertTrue(favorites[0].isFavorite)
    }

    @Test
    fun deleteMedia() = runTest(testDispatcher) {
        val id = repository.insertMedia(
            ScrapedMedia(
                url = "https://example.com/temp.mp4",
                title = "Temp",
                mediaType = MediaType.VIDEO,
                sourcePageUrl = "https://example.com",
                sourceDomain = "example.com"
            )
        )

        assertEquals(1, repository.mediaCount.first())
        repository.deleteMedia(id)
        assertEquals(0, repository.mediaCount.first())
    }

    @Test
    fun testFilteredMediaByDomainAndKeyword() = runTest(testDispatcher) {
        repository.insertMedia(
            ScrapedMedia(
                url = "https://peach.blender.org/bunny.mp4",
                title = "Big Buck Bunny",
                mediaType = MediaType.VIDEO,
                sourcePageUrl = "https://peach.blender.org",
                sourceDomain = "peach.blender.org",
                normalizedName = "bunny"
            )
        )
        repository.insertMedia(
            ScrapedMedia(
                url = "https://mux.com/stream.m3u8",
                title = "Mux Live Stream",
                mediaType = MediaType.HLS,
                sourcePageUrl = "https://mux.com",
                sourceDomain = "mux.com",
                normalizedName = "stream"
            )
        )

        val blendOnly = repository.getFilteredMedia(null, setOf("peach.blender.org")).first()
        assertEquals(1, blendOnly.size)
        assertEquals("Big Buck Bunny", blendOnly[0].title)

        val muxOnly = repository.getFilteredMedia("Live", setOf("mux.com")).first()
        assertEquals(1, muxOnly.size)
        assertEquals("Mux Live Stream", muxOnly[0].title)
    }
}
