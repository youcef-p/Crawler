package com.example.reelscraper.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.reelscraper.data.model.MediaType

@OptIn(UnstableApi::class)
object VideoPlayerManager {
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36 ReelScraper/1.0"

    fun createDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
    }

    fun buildMediaSource(url: String, mediaType: MediaType, dataSourceFactory: DefaultHttpDataSource.Factory): MediaSource {
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
        isMuted: Boolean = false
    ): ExoPlayer {
        val player = ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()

        player.repeatMode = Player.REPEAT_MODE_ONE
        player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        player.volume = if (isMuted) 0f else 1f

        return player
    }
}
