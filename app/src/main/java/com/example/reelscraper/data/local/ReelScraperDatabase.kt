package com.example.reelscraper.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.reelscraper.data.model.CrawlJob
import com.example.reelscraper.data.model.PlaybackState
import com.example.reelscraper.data.model.Chapter
import com.example.reelscraper.data.model.SubtitleTrack
import com.example.reelscraper.data.model.TrickPlayAsset
import com.example.reelscraper.data.model.HeatmapBucket
import com.example.reelscraper.data.model.ExtractionRule
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.model.SiteProfile
import com.example.reelscraper.data.model.StreamSession

@Database(
    entities = [
        ScrapedMedia::class,
        StreamSession::class,
        SiteProfile::class,
        ExtractionRule::class,
        CrawlJob::class,
        PlaybackState::class,
        Chapter::class,
        SubtitleTrack::class,
        TrickPlayAsset::class,
        HeatmapBucket::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ReelScraperDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun streamSessionDao(): StreamSessionDao
    abstract fun siteProfileDao(): SiteProfileDao
    abstract fun crawlJobDao(): CrawlJobDao
    abstract fun playbackStateDao(): PlaybackStateDao
    abstract fun chapterDao(): ChapterDao
    abstract fun subtitleTrackDao(): SubtitleTrackDao
    abstract fun trickPlayDao(): TrickPlayDao
    abstract fun heatmapDao(): HeatmapDao

    companion object {
        @Volatile
        private var INSTANCE: ReelScraperDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN normalizedName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN durationMillis INTEGER")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN width INTEGER")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN height INTEGER")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN extractorType TEXT NOT NULL DEFAULT 'HTML'")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_scraped_media_normalizedName_sourceDomain ON scraped_media(normalizedName, sourceDomain)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scraped_media_sourceDomain ON scraped_media(sourceDomain)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN isDynamic INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN streamSessionId INTEGER")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN playbackHeadersJson TEXT")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN isBroken INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN playCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN lastPositionMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN lastPlayedTimestamp INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scraped_media_isDynamic ON scraped_media(isDynamic)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scraped_media_isBroken ON scraped_media(isBroken)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS stream_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mediaId INTEGER NOT NULL,
                        sourcePageUrl TEXT NOT NULL,
                        streamUrl TEXT NOT NULL,
                        streamType TEXT NOT NULL,
                        httpMethod TEXT NOT NULL,
                        requestHeadersJson TEXT NOT NULL,
                        referer TEXT,
                        userAgent TEXT,
                        cookieHeader TEXT,
                        capturedAt INTEGER NOT NULL,
                        expiresAt INTEGER,
                        refreshPolicy TEXT NOT NULL,
                        lastPlaybackFailure TEXT,
                        lastRefreshResult TEXT,
                        isActive INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stream_sessions_mediaId ON stream_sessions(mediaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stream_sessions_streamUrl ON stream_sessions(streamUrl)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS site_profiles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        domain TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        defaultDepth INTEGER NOT NULL,
                        maxLinksPerPage INTEGER NOT NULL,
                        maxPagesPerCrawl INTEGER NOT NULL,
                        requestDelayMillis INTEGER NOT NULL,
                        timeoutMillis INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL,
                        userAgentMode TEXT NOT NULL,
                        customUserAgent TEXT,
                        customHeadersJson TEXT,
                        allowedUrlPatterns TEXT,
                        blockedUrlPatterns TEXT,
                        allowedMediaTypes TEXT,
                        disabledMediaTypes TEXT,
                        enableHtmlExtraction INTEGER NOT NULL,
                        enableMetaExtraction INTEGER NOT NULL,
                        enableJsonLdExtraction INTEGER NOT NULL,
                        enableScriptExtraction INTEGER NOT NULL,
                        enableRegexExtraction INTEGER NOT NULL,
                        enableAttributeExtraction INTEGER NOT NULL,
                        enableIframeExtraction INTEGER NOT NULL,
                        enableFeedExtraction INTEGER NOT NULL,
                        enableSitemapExtraction INTEGER NOT NULL,
                        enableJsonApiExtraction INTEGER NOT NULL,
                        enableDynamicStreaming INTEGER NOT NULL,
                        dynamicMode TEXT NOT NULL,
                        enableWebViewFallback INTEGER NOT NULL,
                        enableLocalProxy INTEGER NOT NULL,
                        playbackHeadersJson TEXT,
                        paginationMode TEXT NOT NULL,
                        nextpageSelector TEXT,
                        paginationUrlTemplate TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_site_profiles_domain ON site_profiles(domain)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS extraction_rules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        domain TEXT NOT NULL,
                        ruleType TEXT NOT NULL,
                        selector TEXT,
                        attribute TEXT,
                        regex TEXT,
                        jsonPath TEXT,
                        scriptPattern TEXT,
                        targetType TEXT NOT NULL,
                        titleSource TEXT,
                        posterSource TEXT,
                        priority INTEGER NOT NULL,
                        enabled INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_extraction_rules_domain ON extraction_rules(domain)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // The previous normalizedName+domain uniqueness rule incorrectly collapsed
                // legitimate quality/variant URLs from the same page.
                db.execSQL("DROP INDEX IF EXISTS index_scraped_media_normalizedName_sourceDomain")

                // Remove any pre-existing duplicate URLs before adding the canonical URL index.
                db.execSQL("""
                    DELETE FROM scraped_media
                    WHERE id NOT IN (
                        SELECT MIN(id)
                        FROM scraped_media
                        GROUP BY url
                    )
                """.trimIndent())

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_scraped_media_url ON scraped_media(url)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scraped_media_normalizedName_sourceDomain ON scraped_media(normalizedName, sourceDomain)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS playback_states (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mediaId INTEGER NOT NULL,
                        positionMillis INTEGER NOT NULL,
                        durationMillis INTEGER NOT NULL,
                        watchedPercent REAL NOT NULL,
                        playCount INTEGER NOT NULL,
                        lastPlayedAt INTEGER NOT NULL,
                        completed INTEGER NOT NULL,
                        preferredAudioTrack TEXT,
                        preferredSubtitleTrack TEXT,
                        preferredQuality TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_playback_states_mediaId ON playback_states(mediaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_states_lastPlayedAt ON playback_states(lastPlayedAt)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chapters (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mediaId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        positionMillis INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapters_mediaId ON chapters(mediaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_chapters_mediaId_positionMillis ON chapters(mediaId, positionMillis)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS subtitle_tracks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mediaId INTEGER NOT NULL,
                        label TEXT NOT NULL,
                        language TEXT NOT NULL,
                        sourceUrl TEXT,
                        localUri TEXT,
                        type TEXT NOT NULL,
                        format TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_subtitle_tracks_mediaId ON subtitle_tracks(mediaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_subtitle_tracks_language ON subtitle_tracks(language)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS trick_play_assets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mediaId INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        imageUrl TEXT,
                        vttUrl TEXT,
                        tileWidth INTEGER NOT NULL,
                        tileHeight INTEGER NOT NULL,
                        columns INTEGER NOT NULL,
                        rows INTEGER NOT NULL,
                        intervalMillis INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trick_play_assets_mediaId ON trick_play_assets(mediaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trick_play_assets_type ON trick_play_assets(type)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS heatmap_buckets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mediaId INTEGER NOT NULL,
                        bucketStartMillis INTEGER NOT NULL,
                        bucketEndMillis INTEGER NOT NULL,
                        replayCount INTEGER NOT NULL,
                        seekCount INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_heatmap_buckets_mediaId ON heatmap_buckets(mediaId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_heatmap_buckets_mediaId_bucketStartMillis ON heatmap_buckets(mediaId, bucketStartMillis)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS crawl_jobs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        seedUrl TEXT NOT NULL,
                        maxDepth INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        pagesVisited INTEGER NOT NULL,
                        linksDiscovered INTEGER NOT NULL,
                        mediaDiscovered INTEGER NOT NULL,
                        mediaInserted INTEGER NOT NULL,
                        duplicatesSkipped INTEGER NOT NULL,
                        startedAt INTEGER NOT NULL,
                        completedAt INTEGER,
                        errorMessage TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_crawl_jobs_status ON crawl_jobs(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_crawl_jobs_startedAt ON crawl_jobs(startedAt)")
            }
        }

        private val DB_CALLBACK = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                try {
                    val now = System.currentTimeMillis()
                    // Cleanup orphan RUNNING jobs on startup without touching media rows
                    db.execSQL("UPDATE crawl_jobs SET status = 'CANCELLED', completedAt = $now, errorMessage = 'Process terminated mid-scan' WHERE status = 'RUNNING'")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context): ReelScraperDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReelScraperDatabase::class.java,
                    "reel_scraper_database"
                )
                     .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(DB_CALLBACK)
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
