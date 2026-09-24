package com.example.reelscraper.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.reelscraper.data.model.MediaType

@OptIn(UnstableApi::class)
object VideoPlayerManager {
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36 ReelScraper/2.0"

    fun createDataSourceFactory(
        referer: String? = null,
        userAgent: String? = null,
        cookie: String? = null,
        extraHeaders: Map<String, String>? = null
    ): DefaultHttpDataSource.Factory {
        val factory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent ?: USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)

        val headers = mutableMapOf(
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.9"
        )
        if (!referer.isNullOrBlank()) {
            headers["Referer"] = referer
        }
        if (!cookie.isNullOrBlank()) {
            headers["Cookie"] = cookie
        }
        if (extraHeaders != null) {
            headers.putAll(extraHeaders)
        }
        factory.setDefaultRequestProperties(headers)
        return factory
    }

    fun buildMediaSource(
        url: String,
        mediaType: MediaType,
        dataSourceFactory: DefaultHttpDataSource.Factory
    ): MediaSource {
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .build()

        return when {
            mediaType == MediaType.HLS || url.contains(".m3u8", ignoreCase = true) -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            }
            mediaType == MediaType.DASH || url.contains(".mpd", ignoreCase = true) -> {
                DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            else -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }

    fun createPlayer(
        context: Context,
        isMuted: Boolean = false,
        loop: Boolean = true,
        audioNormalizationProcessor: AudioNormalizationProcessor? = null,
        trackSelector: DefaultTrackSelector = DefaultTrackSelector(context)
    ): ExoPlayer {
        val renderersFactory = object : DefaultRenderersFactory(context.applicationContext) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                val processors = if (audioNormalizationProcessor != null) {
                    arrayOf<AudioProcessor>(audioNormalizationProcessor)
                } else {
                    emptyArray()
                }
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(processors)
                    .build()
            }
        }.apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        }

        val player = ExoPlayer.Builder(context.applicationContext, renderersFactory)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()

        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        player.volume = if (isMuted) 0f else 1f

        return player
    }

    fun releasePlayerSafely(player: ExoPlayer?) {
        if (player == null) return
        try {
            player.stop()
            player.clearMediaItems()
            player.clearVideoSurface()
            player.release()
        } catch (_: Exception) {
            // Prevent crashes during cleanup
        }
    }
}
