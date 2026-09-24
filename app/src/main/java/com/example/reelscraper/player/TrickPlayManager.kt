package com.example.reelscraper.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.reelscraper.data.local.TrickPlayDao
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.model.TrickPlayAsset
import com.example.reelscraper.data.model.TrickPlayType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import kotlin.math.max

class TrickPlayManager(
    private val context: Context,
    private val trickPlayDao: TrickPlayDao,
    private val okHttpClient: OkHttpClient
) {
    private val frameGenerator = FramePreviewGenerator(context)
    private var cachedSpriteSheetUrl: String? = null
    private var cachedSpriteSheetBitmap: Bitmap? = null

    /**
     * Obtains scrubbing preview bitmap for given media and seek position.
     */
    suspend fun getPreviewForPosition(
        media: ScrapedMedia,
        positionMs: Long
    ): Bitmap? = withContext(Dispatchers.IO) {
        val asset = trickPlayDao.getTrickPlayForMediaDirect(media.id)

        if (asset != null && asset.type == TrickPlayType.SPRITE_SHEET && !asset.imageUrl.isNullOrBlank()) {
            val tile = extractSpriteTile(asset, positionMs)
            if (tile != null) return@withContext tile
        }

        // Fallback: Lazy on-device frame generation
        frameGenerator.getPreviewFrame(
            mediaId = media.id,
            videoUrl = media.url,
            positionMs = positionMs
        )
    }

    private suspend fun extractSpriteTile(asset: TrickPlayAsset, positionMs: Long): Bitmap? {
        val spriteUrl = asset.imageUrl ?: return null
        val spriteBitmap = loadSpriteSheet(spriteUrl) ?: return null

        val interval = max(1000L, asset.intervalMillis)
        val frameIndex = (positionMs / interval).toInt()
        val totalTiles = asset.columns * asset.rows
        val tileIdx = frameIndex.coerceIn(0, max(0, totalTiles - 1))

        val col = tileIdx % asset.columns
        val row = tileIdx / asset.columns

        val x = col * asset.tileWidth
        val y = row * asset.tileHeight

        if (x + asset.tileWidth <= spriteBitmap.width && y + asset.tileHeight <= spriteBitmap.height) {
            return try {
                Bitmap.createBitmap(spriteBitmap, x, y, asset.tileWidth, asset.tileHeight)
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    private suspend fun loadSpriteSheet(url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (cachedSpriteSheetUrl == url && cachedSpriteSheetBitmap != null && !cachedSpriteSheetBitmap!!.isRecycled) {
            return@withContext cachedSpriteSheetBitmap
        }

        try {
            val req = Request.Builder().url(url).build()
            val resp = okHttpClient.newCall(req).execute()
            resp.use { response ->
                if (response.isSuccessful) {
                    val stream: InputStream = response.body?.byteStream() ?: return@withContext null
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        cachedSpriteSheetUrl = url
                        cachedSpriteSheetBitmap = bitmap
                        return@withContext bitmap
                    }
                }
            }
        } catch (_: Exception) {}

        null
    }
}
