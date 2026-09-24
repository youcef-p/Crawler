package com.example.reelscraper.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
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

/**
 * SharedPreviewPlayer manages a single muted, lightweight ExoPlayer instance
 * for hover/long-press preview playback in library lists and grids.
 */
@OptIn(UnstableApi::class)
class SharedPreviewPlayer(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    var player: ExoPlayer? = null
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingPreviewRunnable: Runnable? = null
    var currentMediaId: Long? = null
        private set

    private fun ensurePlayer(): ExoPlayer {
        if (player == null) {
            val renderersFactory = DefaultRenderersFactory(context.applicationContext)
                .setEnableDecoderFallback(true)

            player = ExoPlayer.Builder(context.applicationContext, renderersFactory)
                .build().apply {
                    volume = 0f
                    repeatMode = Player.REPEAT_MODE_ONE
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                }
        }
        return player!!
    }

    fun startPreview(media: ScrapedMedia, delayMs: Long = 300L) {
        stopPreview()
        currentMediaId = media.id

        val runnable = Runnable {
            val p = ensurePlayer()
            val factory = DefaultHttpDataSource.Factory()
                .setUserAgent(VideoPlayerManager.USER_AGENT)
                .setAllowCrossProtocolRedirects(true)

            if (!media.sourcePageUrl.isNullOrBlank()) {
                factory.setDefaultRequestProperties(mapOf("Referer" to media.sourcePageUrl))
            }

            val mediaItem = MediaItem.fromUri(media.url)
            val mediaSource: MediaSource = when {
                media.mediaType == MediaType.HLS || media.url.contains(".m3u8", ignoreCase = true) -> {
                    HlsMediaSource.Factory(factory).createMediaSource(mediaItem)
                }
                media.mediaType == MediaType.DASH || media.url.contains(".mpd", ignoreCase = true) -> {
                    DashMediaSource.Factory(factory).createMediaSource(mediaItem)
                }
                else -> {
                    ProgressiveMediaSource.Factory(factory).createMediaSource(mediaItem)
                }
            }

            p.setMediaSource(mediaSource)
            p.prepare()
            p.play()
        }

        pendingPreviewRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    fun stopPreview() {
        pendingPreviewRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingPreviewRunnable = null
        currentMediaId = null

        player?.let {
            it.stop()
            it.clearMediaItems()
        }
    }

    fun release() {
        stopPreview()
        player?.release()
        player = null
    }
}
