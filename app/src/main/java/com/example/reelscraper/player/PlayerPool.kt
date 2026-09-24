package com.example.reelscraper.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * PlayerPool provides a bounded, thread-safe pool of ExoPlayer instances (default 3)
 * to avoid memory exhaustion, hardware decoder limits, and rapid GC cycles.
 */
@OptIn(UnstableApi::class)
class PlayerPool(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    val maxPoolSize: Int = 3
) {
    private val pool = ConcurrentLinkedQueue<ExoPlayer>()
    private val activePlayers = ConcurrentHashMap<Long, ExoPlayer>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var activeCount: Int = 0
        private set

    init {
        mainHandler.post {
            for (i in 0 until maxPoolSize.coerceAtMost(2)) {
                pool.offer(createNewPlayer())
            }
        }
    }

    private fun createNewPlayer(): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context.applicationContext)
            .setEnableDecoderFallback(true)

        return ExoPlayer.Builder(context.applicationContext, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }
    }

    fun acquirePlayer(mediaId: Long): ExoPlayer {
        val existing = activePlayers[mediaId]
        if (existing != null) return existing

        val player = pool.poll() ?: createNewPlayer()
        activePlayers[mediaId] = player
        activeCount = activePlayers.size
        return player
    }

    fun releasePlayer(mediaId: Long) {
        val player = activePlayers.remove(mediaId) ?: return
        activeCount = activePlayers.size
        try {
            player.stop()
            player.clearMediaItems()
            player.clearVideoSurface()

            if (pool.size < maxPoolSize) {
                pool.offer(player)
            } else {
                player.release()
            }
        } catch (_: Exception) {
            try { player.release() } catch (_: Exception) {}
        }
    }

    fun buildMediaSource(media: ScrapedMedia): MediaSource {
        val factory = DefaultHttpDataSource.Factory()
            .setUserAgent(VideoPlayerManager.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)

        if (!media.sourcePageUrl.isNullOrBlank()) {
            factory.setDefaultRequestProperties(mapOf("Referer" to media.sourcePageUrl))
        }

        val mediaItem = MediaItem.fromUri(media.url)
        return when {
            media.mediaType == MediaType.HLS || media.url.contains(".m3u8", ignoreCase = true) -> {
                HlsMediaSource.Factory(factory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            }
            media.mediaType == MediaType.DASH || media.url.contains(".mpd", ignoreCase = true) -> {
                DashMediaSource.Factory(factory)
                    .createMediaSource(mediaItem)
            }
            else -> {
                ProgressiveMediaSource.Factory(factory)
                    .createMediaSource(mediaItem)
            }
        }
    }

    fun releaseAll() {
        mainHandler.post {
            activePlayers.values.forEach {
                try { it.release() } catch (_: Exception) {}
            }
            activePlayers.clear()
            while (pool.isNotEmpty()) {
                try { pool.poll()?.release() } catch (_: Exception) {}
            }
            activeCount = 0
        }
    }
}
