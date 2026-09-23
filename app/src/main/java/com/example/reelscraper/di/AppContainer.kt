package com.example.reelscraper.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.reelscraper.data.local.MediaDao
import com.example.reelscraper.data.local.ReelScraperDatabase
import com.example.reelscraper.data.repository.MediaRepository
import com.example.reelscraper.data.repository.MediaRepositoryImpl
import com.example.reelscraper.data.scraper.WebScraperEngine
import com.example.reelscraper.data.settings.SettingsRepository
import com.example.reelscraper.ui.viewmodel.FeedViewModel
import com.example.reelscraper.ui.viewmodel.ScraperViewModel
import com.example.reelscraper.ui.viewmodel.SearchViewModel
import com.example.reelscraper.ui.viewmodel.SettingsViewModel
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

interface AppContainer {
    val database: ReelScraperDatabase
    val mediaDao: MediaDao
    val okHttpClient: OkHttpClient
    val scraperEngine: WebScraperEngine
    val mediaRepository: MediaRepository
    val settingsRepository: SettingsRepository
    val viewModelFactory: ViewModelProvider.Factory
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: ReelScraperDatabase by lazy {
        ReelScraperDatabase.getDatabase(context)
    }

    override val mediaDao: MediaDao by lazy {
        database.mediaDao()
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    override val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    override val scraperEngine: WebScraperEngine by lazy {
        WebScraperEngine(
            okHttpClient = okHttpClient,
            appContext = context
        )
    }

    override val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(
            mediaDao = mediaDao,
            scraperEngine = scraperEngine
        )
    }

    @Suppress("UNCHECKED_CAST")
    override val viewModelFactory: ViewModelProvider.Factory by lazy {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(ScraperViewModel::class.java) -> {
                        ScraperViewModel(mediaRepository, settingsRepository) as T
                    }
                    modelClass.isAssignableFrom(FeedViewModel::class.java) -> {
                        FeedViewModel(mediaRepository) as T
                    }
                    modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                        SearchViewModel(mediaRepository) as T
                    }
                    modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                        SettingsViewModel(settingsRepository, mediaRepository) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
