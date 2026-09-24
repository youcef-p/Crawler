package com.example.reelscraper.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.reelscraper.data.local.CrawlJobDao
import com.example.reelscraper.data.local.PlaybackStateDao
import com.example.reelscraper.data.local.ChapterDao
import com.example.reelscraper.data.local.SubtitleTrackDao
import com.example.reelscraper.data.local.TrickPlayDao
import com.example.reelscraper.data.local.HeatmapDao
import com.example.reelscraper.data.local.MediaDao
import com.example.reelscraper.data.local.ReelScraperDatabase
import com.example.reelscraper.data.local.SiteProfileDao
import com.example.reelscraper.data.local.StreamSessionDao
import com.example.reelscraper.data.repository.MediaRepository
import com.example.reelscraper.data.repository.MediaRepositoryImpl
import com.example.reelscraper.data.repository.SiteProfileRepository
import com.example.reelscraper.data.repository.SiteProfileRepositoryImpl
import com.example.reelscraper.data.scraper.MediaPersister
import com.example.reelscraper.data.scraper.WebScraperEngine
import com.example.reelscraper.data.settings.SettingsRepository
import com.example.reelscraper.player.DynamicStreamResolver
import com.example.reelscraper.player.LocalStreamingProxy
import com.example.reelscraper.player.TrickPlayManager
import com.example.reelscraper.intelligence.chapter.LocalChapterGenerator
import com.example.reelscraper.ui.viewmodel.FeedViewModel
import com.example.reelscraper.ui.viewmodel.ScraperViewModel
import com.example.reelscraper.ui.viewmodel.SearchViewModel
import com.example.reelscraper.ui.viewmodel.SettingsViewModel
import com.example.reelscraper.ui.viewmodel.SiteProfilesViewModel
import com.example.reelscraper.ui.viewmodel.SnifferViewModel
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

interface AppContainer {
    val database: ReelScraperDatabase
    val mediaDao: MediaDao
    val streamSessionDao: StreamSessionDao
    val siteProfileDao: SiteProfileDao
    val crawlJobDao: CrawlJobDao
    val playbackStateDao: PlaybackStateDao
    val chapterDao: ChapterDao
    val subtitleTrackDao: SubtitleTrackDao
    val trickPlayDao: TrickPlayDao
    val heatmapDao: HeatmapDao
    val trickPlayManager: TrickPlayManager
    val chapterGenerator: LocalChapterGenerator
    val okHttpClient: OkHttpClient
    val localStreamingProxy: LocalStreamingProxy
    val dynamicStreamResolver: DynamicStreamResolver
    val mediaPersister: MediaPersister
    val scraperEngine: WebScraperEngine
    val mediaRepository: MediaRepository
    val siteProfileRepository: SiteProfileRepository
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

    override val streamSessionDao: StreamSessionDao by lazy {
        database.streamSessionDao()
    }

    override val siteProfileDao: SiteProfileDao by lazy {
        database.siteProfileDao()
    }

    override val crawlJobDao: CrawlJobDao by lazy {
        database.crawlJobDao()
    }

    override val playbackStateDao: PlaybackStateDao by lazy {
        database.playbackStateDao()
    }

    override val chapterDao: ChapterDao by lazy {
        database.chapterDao()
    }

    override val subtitleTrackDao: SubtitleTrackDao by lazy {
        database.subtitleTrackDao()
    }

    override val trickPlayDao: TrickPlayDao by lazy {
        database.trickPlayDao()
    }

    override val heatmapDao: HeatmapDao by lazy {
        database.heatmapDao()
    }

    override val trickPlayManager: TrickPlayManager by lazy {
        TrickPlayManager(
            context = context,
            trickPlayDao = trickPlayDao,
            okHttpClient = okHttpClient
        )
    }

    override val chapterGenerator: LocalChapterGenerator by lazy {
        LocalChapterGenerator(chapterDao)
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

    override val localStreamingProxy: LocalStreamingProxy by lazy {
        LocalStreamingProxy(okHttpClient)
    }

    override val dynamicStreamResolver: DynamicStreamResolver by lazy {
        DynamicStreamResolver(
            appContext = context,
            mediaDao = mediaDao,
            sessionDao = streamSessionDao,
            localProxy = localStreamingProxy
        )
    }

    override val mediaPersister: MediaPersister by lazy {
        MediaPersister(
            mediaDao = mediaDao,
            crawlJobDao = crawlJobDao
        )
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
            crawlJobDao = crawlJobDao,
            scraperEngine = scraperEngine,
            mediaPersister = mediaPersister
        )
    }

    override val siteProfileRepository: SiteProfileRepository by lazy {
        SiteProfileRepositoryImpl(siteProfileDao)
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
                        FeedViewModel(
                            repository = mediaRepository,
                            streamResolver = dynamicStreamResolver,
                            settingsRepository = settingsRepository,
                            trickPlayManager = trickPlayManager,
                            chapterGenerator = chapterGenerator,
                            playbackStateDao = playbackStateDao,
                            chapterDao = chapterDao,
                            subtitleDao = subtitleTrackDao,
                            runtimeOkHttpClient = okHttpClient,
                            runtimeHeatmapDao = heatmapDao
                        ) as T
                    }
                    modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                        SearchViewModel(mediaRepository) as T
                    }
                    modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                        SettingsViewModel(settingsRepository, mediaRepository) as T
                    }
                    modelClass.isAssignableFrom(SnifferViewModel::class.java) -> {
                        SnifferViewModel(mediaRepository, siteProfileRepository) as T
                    }
                    modelClass.isAssignableFrom(SiteProfilesViewModel::class.java) -> {
                        SiteProfilesViewModel(siteProfileRepository) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
