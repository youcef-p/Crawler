package com.example.reelscraper.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.reelscraper.data.model.CrawlJob
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
        CrawlJob::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ReelScraperDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun streamSessionDao(): StreamSessionDao
    abstract fun siteProfileDao(): SiteProfileDao
    abstract fun crawlJobDao(): CrawlJobDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(DB_CALLBACK)
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
