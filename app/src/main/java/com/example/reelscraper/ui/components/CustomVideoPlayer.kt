package com.example.reelscraper.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.player.VideoPlayerManager
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun CustomVideoPlayer(
    media: ScrapedMedia,
    isActive: Boolean,
    isPlaying: Boolean,
    isMuted: Boolean,
    onProgressUpdate: (positionMs: Long, durationMs: Long, isBuffering: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (media.mediaType == MediaType.GIF) {
        // GIF playback via Coil
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(media.url)
                    .crossfade(true)
                    .build(),
                contentDescription = media.displayTitle,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var duration by remember { mutableLongStateOf(0L) }

    // Initialize ExoPlayer
    val exoPlayer = remember(media.url) {
        VideoPlayerManager.createPlayer(context, isMuted).apply {
            val dataSourceFactory = VideoPlayerManager.createDataSourceFactory()
            val mediaSource = VideoPlayerManager.buildMediaSource(media.url, media.mediaType, dataSourceFactory)
            setMediaSource(mediaSource)
            prepare()
        }
    }

    // Player state listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message ?: "Failed to play stream"
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Sync play/pause with active page state and isPlaying parameter
    LaunchedEffect(isActive, isPlaying) {
        if (isActive && isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Sync mute state
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Periodic progress updates while active
    LaunchedEffect(isActive, isPlaying) {
        while (isActive && this.coroutineContext.isActive) {
            val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val totalDur = exoPlayer.duration.coerceAtLeast(0L)
            onProgressUpdate(currentPos, totalDur, isBuffering)
            delay(250)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Player Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = NeonCyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(52.dp)
            )
        }

        // Error State
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Playback Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = "Cannot stream media",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = playbackError ?: "Stream unavailable or protected",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
