package com.example.reelscraper

import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.repository.MediaRepository
import com.example.reelscraper.data.scraper.ScrapeProgress
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.data.settings.SettingsRepository
import com.example.reelscraper.ui.viewmodel.ScrapeUiState
import com.example.reelscraper.ui.viewmodel.ScraperViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class FakeMediaRepository : MediaRepository {
    private val mediaListFlow = MutableStateFlow<List<ScrapedMedia>>(emptyList())
    private val countFlow = MutableStateFlow(0)
    private val domainsFlow = MutableStateFlow<List<String>>(emptyList())

    var scrapeResultToReturn: Result<List<ScrapedMedia>> = Result.success(emptyList())

    override val allMedia: Flow<List<ScrapedMedia>> = mediaListFlow.asStateFlow()
    override val mediaCount: Flow<Int> = countFlow.asStateFlow()
    override val distinctSourceDomains: Flow<List<String>> = domainsFlow.asStateFlow()

    override fun getFilteredMedia(keyword: String?, selectedDomains: Set<String>): Flow<List<ScrapedMedia>> = mediaListFlow

    override fun searchMedia(query: String, filterType: MediaType?): Flow<List<ScrapedMedia>> = mediaListFlow

    override fun getFavorites(): Flow<List<ScrapedMedia>> = mediaListFlow

    override fun getMediaById(id: Long): Flow<ScrapedMedia?> = MutableStateFlow(null)

    override suspend fun scrapeAndIndex(
        url: String,
        depth: Int,
        settings: AppSettings,
        onProgress: (ScrapeProgress) -> Unit
    ): Result<List<ScrapedMedia>> {
        onProgress(ScrapeProgress("Scanning...", url, 1, currentLevel = 1, maxLevel = depth))
        return scrapeResultToReturn
    }

    override suspend fun insertMedia(media: ScrapedMedia): Long = 1L
    override suspend fun toggleFavorite(mediaId: Long, isFavorite: Boolean) {}
    override suspend fun deleteMedia(mediaId: Long) {}
    override suspend fun clearAll() {
        mediaListFlow.value = emptyList()
        countFlow.value = 0
    }
    override suspend fun clearDuplicates(): Int = 0
    override suspend fun exportJson(): String = "[]"
    override suspend fun importJson(json: String): Int = 0
    override suspend fun seedStarterSamplesIfEmpty() {}
}

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ScraperViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeMediaRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var viewModel: ScraperViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMediaRepository()
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        settingsRepo = SettingsRepository(context, flowOf(AppSettings()))
        viewModel = ScraperViewModel(fakeRepository, settingsRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUrlChangeAndScanLevels() = runTest {
        viewModel.onUrlChanged("https://test.com")
        assertEquals("https://test.com", viewModel.uiState.value.targetUrl)

        viewModel.onScanLevelsChanged(4)
        assertEquals(4, viewModel.uiState.value.scanLevels)

        viewModel.onScanLevelsChanged(15) // Clamped to 10
        assertEquals(10, viewModel.uiState.value.scanLevels)
    }

    @Test
    fun testScanBlankUrlShowsError() = runTest {
        viewModel.onUrlChanged("")
        viewModel.onRequestScan()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.uiState is ScrapeUiState.Error)
    }

    @Test
    fun testDepthDialogWorkflowAndConfirmScan() = runTest {
        val sampleMedia = listOf(
            ScrapedMedia(
                id = 1,
                url = "https://example.com/clip.mp4",
                title = "Clip",
                mediaType = MediaType.VIDEO,
                sourcePageUrl = "https://example.com",
                sourceDomain = "example.com"
            )
        )
        fakeRepository.scrapeResultToReturn = Result.success(sampleMedia)

        viewModel.onUrlChanged("https://example.com")
        viewModel.onRequestScan()
        assertTrue(viewModel.uiState.value.showDepthDialog)

        viewModel.onConfirmScanWithLevels(3)
        assertEquals(3, viewModel.uiState.value.scanLevels)
        assertEquals(false, viewModel.uiState.value.showDepthDialog)

        advanceUntilIdle()

        val state = viewModel.uiState.value.uiState
        assertTrue(state is ScrapeUiState.Success)
        assertEquals(1, (state as ScrapeUiState.Success).itemsFound)
    }

    @Test
    fun testCancelScan() = runTest {
        viewModel.onUrlChanged("https://example.com")
        viewModel.onConfirmScanWithLevels(2)
        viewModel.cancelScan()

        assertEquals(ScrapeUiState.Idle, viewModel.uiState.value.uiState)
    }
}
