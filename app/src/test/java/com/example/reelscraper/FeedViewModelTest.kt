package com.example.reelscraper

import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.ui.viewmodel.FeedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeMediaRepository
    private lateinit var viewModel: FeedViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMediaRepository()
        viewModel = FeedViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testTogglePlayPause() = runTest {
        assertTrue(viewModel.feedState.value.isPlaying)
        viewModel.togglePlayPause()
        assertFalse(viewModel.feedState.value.isPlaying)
        viewModel.togglePlayPause()
        assertTrue(viewModel.feedState.value.isPlaying)
    }

    @Test
    fun testToggleMute() = runTest {
        assertFalse(viewModel.feedState.value.isMuted)
        viewModel.toggleMute()
        assertTrue(viewModel.feedState.value.isMuted)
        viewModel.toggleMute()
        assertFalse(viewModel.feedState.value.isMuted)
    }

    @Test
    fun testPageChanged() = runTest {
        viewModel.onPageChanged(2)
        assertEquals(2, viewModel.feedState.value.currentItemIndex)
        assertTrue(viewModel.feedState.value.isPlaying)
        assertEquals(0L, viewModel.feedState.value.currentPositionMs)
    }

    @Test
    fun testUpdateProgress() = runTest {
        viewModel.updateProgress(1500L, 5000L, false)
        assertEquals(1500L, viewModel.feedState.value.currentPositionMs)
        assertEquals(5000L, viewModel.feedState.value.totalDurationMs)
        assertFalse(viewModel.feedState.value.isBuffering)
    }
}
