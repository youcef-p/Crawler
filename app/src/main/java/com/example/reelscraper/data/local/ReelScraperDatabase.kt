package com.example.reelscraper.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.reelscraper.data.model.ScrapedMedia

@Database(entities = [ScrapedMedia::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ReelScraperDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: ReelScraperDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop and recreate table if needed or add new columns safely
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN normalizedName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN durationMillis INTEGER")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN width INTEGER")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN height INTEGER")
                db.execSQL("ALTER TABLE scraped_media ADD COLUMN extractorType TEXT NOT NULL DEFAULT 'HTML'")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_scraped_media_normalizedName_sourceDomain ON scraped_media(normalizedName, sourceDomain)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_scraped_media_sourceDomain ON scraped_media(sourceDomain)")
            }
        }

        fun getDatabase(context: Context): ReelScraperDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReelScraperDatabase::class.java,
                    "reel_scraper_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
