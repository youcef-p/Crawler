package com.example.reelscraper.ui.components

import android.view.SurfaceView
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reelscraper.data.model.Chapter
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.model.SubtitleTrack
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.intelligence.reframe.CenterCropReframeEngine
import com.example.reelscraper.intelligence.reframe.ReframeMode
import com.example.reelscraper.intelligence.reframe.SmartReframeEngine
import com.example.reelscraper.intelligence.reframe.SubjectTrackingReframeEngine
import com.example.reelscraper.player.AudioNormalizationProcessor
import com.example.reelscraper.player.CustomAbrStrategy
import com.example.reelscraper.player.FrameRateConverter
import com.example.reelscraper.player.HeatmapTracker
import com.example.reelscraper.player.TrickPlayManager
import com.example.reelscraper.player.VideoPlayerManager
import com.example.reelscraper.player.WebVttCue
import com.example.reelscraper.player.WebVttParser
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CoralPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VividViolet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

enum class PlayerAspectRatio {
    FIT,
    FILL,
    STRETCH
}

@OptIn(UnstableApi::class)
@Composable
fun CustomVideoPlayer(
    media: ScrapedMedia,
    isActive: Boolean,
    isPlaying: Boolean,
    isMuted: Boolean,
    settings: AppSettings,
    aspectRatio: PlayerAspectRatio = PlayerAspectRatio.FIT,
    playbackSpeed: Float = 1.0f,
    resumePositionMs: Long = 0L,
    trickPlayManager: TrickPlayManager? = null,
    heatmapTracker: HeatmapTracker? = null,
    chapters: List<Chapter> = emptyList(),
    subtitles: List<SubtitleTrack> = emptyList(),
    onProgressUpdate: (positionMs: Long, durationMs: Long, isBuffering: Boolean) -> Unit,
    onStreamAutoRefreshNeeded: (() -> Unit)? = null,
    onAddChapter: ((String, Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (media.mediaType == MediaType.GIF) {
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
                contentScale = when (aspectRatio) {
                    PlayerAspectRatio.FIT -> ContentScale.Fit
                    PlayerAspectRatio.FILL -> ContentScale.Crop
                    PlayerAspectRatio.STRETCH -> ContentScale.FillBounds
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var retryCount by remember { mutableIntStateOf(0) }
    var resumeApplied by remember(media.id) { mutableStateOf(false) }
    var videoWidth by remember { mutableIntStateOf(0) }
    var videoHeight by remember { mutableIntStateOf(0) }
    var isHdrStream by remember { mutableStateOf(false) }
    var is60FpsStream by remember { mutableStateOf(false) }
    var fpsValue by remember { mutableFloatStateOf(0f) }

    // Heatmap values
    var heatmapValues by remember { mutableStateOf<List<Float>>(emptyList()) }

    // Smart Reframe pan offset
    var reframePanX by remember { mutableFloatStateOf(0f) }

    // Track Selection & Subtitle Cues
    var showTrackMenu by remember { mutableStateOf(false) }
    var selectedSubtitleTrack by remember { mutableStateOf<SubtitleTrack?>(null) }
    var activeCues by remember { mutableStateOf<List<WebVttCue>>(emptyList()) }
    var currentCue by remember { mutableStateOf<WebVttCue?>(null) }

    // Audio Normalization & ABR strategy
    val audioNormProcessor = remember {
        AudioNormalizationProcessor().apply {
            isEnabled = settings.audioNormalization
            strength = settings.normalizationStrength
        }
    }

    val trackSelector = remember { DefaultTrackSelector(context) }
    val abrStrategy = remember(trackSelector) { CustomAbrStrategy(context, trackSelector) }
    val frameRateConverter = remember {
        FrameRateConverter().apply {
            is60FpsEnabled = settings.enable60FpsConverter
        }
    }

    // Load Heatmap data for this video
    LaunchedEffect(media.id, duration) {
        if (heatmapTracker != null && duration > 0 && settings.enableHeatmapCollection) {
            heatmapValues = heatmapTracker.getNormalizedHeatmap(media.id, duration)
        }
    }

    // Load Subtitle Cues if a subtitle track is selected
    LaunchedEffect(selectedSubtitleTrack) {
        val track = selectedSubtitleTrack
        if (track != null && !track.localUri.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val file = File(track.localUri)
                    if (file.exists()) {
                        activeCues = WebVttParser.parse(file.readText())
                    }
                } catch (_: Exception) {
                    activeCues = emptyList()
                }
            }
        } else {
            activeCues = emptyList()
            currentCue = null
        }
    }

    // Update active cue based on current position
    LaunchedEffect(currentPosition, activeCues) {
        if (activeCues.isNotEmpty()) {
            currentCue = activeCues.firstOrNull { currentPosition in it.startTimeMs..it.endTimeMs }
        }
    }

    // Decide whether to use SurfaceView or TextureView:
    // Prompt: "Use SurfaceView instead of TextureView when HDR or high frame rate content is active. Fall back to TextureView for normal content if needed for Compose compatibility."
    val shouldUseSurfaceView = settings.preferSurfaceView || isHdrStream || is60FpsStream

    // Initialize ExoPlayer
    val exoPlayer = remember(media.url, retryCount, isActive) {
        if (!isActive) {
            null
        } else {
            playbackError = null
            isBuffering = true
            abrStrategy.applyQualityPreference(settings.qualityPreference, settings.dataSaverMode)

            VideoPlayerManager.createPlayer(
                context = context,
                isMuted = isMuted,
                loop = true,
                audioNormalizationProcessor = audioNormProcessor,
                trackSelector = trackSelector
            ).apply {
                val dataSourceFactory = VideoPlayerManager.createDataSourceFactory(
                    referer = media.sourcePageUrl
                )
                val mediaSource = VideoPlayerManager.buildMediaSource(media.url, media.mediaType, dataSourceFactory)
                setMediaSource(mediaSource)
                playbackParameters = PlaybackParameters(playbackSpeed)
                prepare()
            }
        }
    }

    // Player state and format listener
    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                    isBuffering = false
                    duration = player.duration.coerceAtLeast(0L)
                    if (!resumeApplied &&
                        resumePositionMs > 5_000L &&
                        resumePositionMs < duration - 2_000L
                    ) {
                        player.seekTo(resumePositionMs)
                        currentPosition = resumePositionMs
                        resumeApplied = true
                    }
                    abrStrategy.updatePlayerState(player)
                } else if (playbackState == Player.STATE_ENDED) {
                    isBuffering = false
                }
            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
                abrStrategy.updatePlayerState(player)
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                val format = player.videoFormat
                val fps = format?.frameRate ?: 0f
                fpsValue = fps
                is60FpsStream = fps >= 55f
                val colorTransfer = format?.colorInfo?.colorTransfer ?: 0
                isHdrStream = colorTransfer == androidx.media3.common.C.COLOR_TRANSFER_ST2084 ||
                        colorTransfer == androidx.media3.common.C.COLOR_TRANSFER_HLG
                frameRateConverter.configurePlayerPacing(player, fps)
                abrStrategy.updatePlayerState(player)
            }

            override fun onPlayerError(error: PlaybackException) {
                abrStrategy.onRebufferEvent()
                val errorMsg = error.message ?: ""
                val causeMsg = error.cause?.message ?: ""
                playbackError = when {
                    errorMsg.contains("403") || causeMsg.contains("403") ->
                        "HTTP 403 Forbidden: Host restricts direct streaming"
                    errorMsg.contains("404") || causeMsg.contains("404") ->
                        "HTTP 404: Media file no longer available on server"
                    else -> errorMsg.ifBlank { "Stream temporarily unavailable" }
                }
                isBuffering = false

                if (media.isDynamic && onStreamAutoRefreshNeeded != null) {
                    onStreamAutoRefreshNeeded()
                }
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            VideoPlayerManager.releasePlayerSafely(player)
        }
    }

    // Sync play/pause
    LaunchedEffect(isActive, isPlaying, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (isActive && isPlaying) {
            player.play()
        } else {
            player.pause()
        }
    }

    // Sync mute
    LaunchedEffect(isMuted, exoPlayer) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    // Sync speed
    LaunchedEffect(playbackSpeed, exoPlayer) {
        exoPlayer?.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    // Progress updates & Heatmap recording loop
    LaunchedEffect(isActive, isPlaying, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        var lastTrackedPos = 0L

        while (isActive && this.coroutineContext.isActive) {
            val currentPos = player.currentPosition.coerceAtLeast(0L)
            val totalDur = player.duration.coerceAtLeast(0L)
            val bufPos = player.bufferedPosition.coerceAtLeast(0L)

            currentPosition = currentPos
            duration = totalDur
            bufferedPosition = bufPos

            onProgressUpdate(currentPos, totalDur, isBuffering)

            // Heatmap recording every 3 seconds
            if (isPlaying && settings.enableHeatmapCollection && heatmapTracker != null) {
                val isReplay = currentPos < lastTrackedPos
                heatmapTracker.recordWatchSegment(media.id, currentPos, isReplay, false)
                lastTrackedPos = currentPos
            }

            abrStrategy.updatePlayerState(player)
            delay(250)
        }
    }

    // Smart Reframe calculation if landscape media in vertical feed
    val reframeEngine: SmartReframeEngine = remember(settings.smartReframeMode) {
        when (settings.smartReframeMode) {
            "OFF" -> CenterCropReframeEngine()
            "MOTION_TRACKING", "SUBJECT_TRACKING" -> SubjectTrackingReframeEngine()
            else -> CenterCropReframeEngine()
        }
    }

    LaunchedEffect(videoWidth, videoHeight, settings.smartReframeMode) {
        if (settings.smartReframeMode != "OFF" && videoWidth > videoHeight && videoWidth > 0) {
            val offset = reframeEngine.computeOffset(null, videoWidth, videoHeight)
            reframePanX = offset.normalizedOffsetX
        } else {
            reframePanX = 0f
        }
    }

    val playbackStats by abrStrategy.stats.collectAsState()

    var seekPillText by remember { mutableStateOf<String?>(null) }
    var isLongPress2xActive by remember { mutableStateOf(false) }

    LaunchedEffect(seekPillText) {
        if (seekPillText != null) {
            kotlinx.coroutines.delay(800)
            seekPillText = null
        }
    }

    val resizeMode = when (aspectRatio) {
        PlayerAspectRatio.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        PlayerAspectRatio.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        PlayerAspectRatio.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(settings.gesturesEnabled, duration) {
                if (!settings.gesturesEnabled) return@pointerInput
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val isRightSide = offset.x >= (size.width / 2f)
                        val seekSec = settings.seekIncrementSeconds
                        val seekMs = seekSec * 1000L
                        val newPos = if (isRightSide) {
                            (currentPosition + seekMs).coerceAtMost(duration)
                        } else {
                            (currentPosition - seekMs).coerceAtLeast(0L)
                        }
                        exoPlayer?.seekTo(newPos)
                        seekPillText = if (isRightSide) "+${seekSec}s" else "-${seekSec}s"
                    },
                    onLongPress = {
                        isLongPress2xActive = true
                        exoPlayer?.playbackParameters = PlaybackParameters(settings.longPressSpeed)
                    },
                    onPress = {
                        tryAwaitRelease()
                        if (isLongPress2xActive) {
                            isLongPress2xActive = false
                            exoPlayer?.playbackParameters = PlaybackParameters(playbackSpeed)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Cinematic Backdrop
        AmbientBackdrop(
            media = media,
            ambientMode = settings.ambientMode,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Poster backdrop fallback
        if (!media.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(media.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = media.displayTitle,
                contentScale = when (aspectRatio) {
                    PlayerAspectRatio.FIT -> ContentScale.Fit
                    PlayerAspectRatio.FILL -> ContentScale.Crop
                    PlayerAspectRatio.STRETCH -> ContentScale.FillBounds
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Active ExoPlayer View (SurfaceView or TextureView)
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        player = exoPlayer
                        setResizeMode(resizeMode)
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                    playerView.setResizeMode(resizeMode)
                },
                onRelease = { playerView ->
                    playerView.player = null
                },
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        // Apply Smart Reframe horizontal pan offset
                        IntOffset((reframePanX * 80.dp.toPx()).roundToInt(), 0)
                    }
            )
        }

        // 4. Subtitle Overlay
        SubtitleOverlay(
            currentCue = currentCue,
            settings = settings,
            modifier = Modifier.fillMaxSize()
        )

        // 5. Badges: SDR / HDR10 / HLG / 60fps
        if (settings.showFormatBadges) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (playbackStats.isHdr) {
                    Text(
                        text = playbackStats.hdrType,
                        color = CoralPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(CinemaBlack.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                }

                if (playbackStats.is60Fps || settings.enable60FpsConverter) {
                    Text(
                        text = if (playbackStats.is60Fps) "60 FPS" else "60Hz Cadence",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(CinemaBlack.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                }

                // Track selection button
                IconButton(
                    onClick = { showTrackMenu = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(CinemaBlack.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                        .testTag("stream_tracks_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Stream Tracks",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 6. Interactive Scrubber with Trick-Play & Heatmap
        if (duration > 0 && exoPlayer != null) {
            ScrubberWithPreview(
                media = media,
                currentPositionMs = currentPosition,
                totalDurationMs = duration,
                bufferedPositionMs = bufferedPosition,
                chapters = chapters,
                heatmapValues = heatmapValues,
                trickPlayManager = trickPlayManager,
                onSeek = { seekPos ->
                    exoPlayer.seekTo(seekPos)
                    if (heatmapTracker != null && settings.enableHeatmapCollection) {
                        heatmapTracker.recordWatchSegment(media.id, seekPos, false, true)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
            )
        }

        // 7. Buffering Spinner
        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = NeonCyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(52.dp)
            )
        }

        // 7b. Long-Press 2x Speed Indicator Pill
        AnimatedVisibility(
            visible = isLongPress2xActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
        ) {
            Surface(
                color = CinemaBlack.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${settings.longPressSpeed}X SPEED",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 7c. Double-Tap Seek Pill (+10s / -10s)
        AnimatedVisibility(
            visible = seekPillText != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = CinemaBlack.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = seekPillText ?: "",
                    color = NeonCyan,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }

        // 8. Player Engine HUD Debug Overlay
        if (settings.showPlayerDebugStats && exoPlayer != null) {
            PlayerDebugOverlay(
                stats = playbackStats,
                poolActiveCount = 1,
                modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp)
            )
        }

        // 9. Error State with Retry
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Playback Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = "Playback Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = playbackError ?: "Stream unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(20.dp))
                    Button(
                        onClick = {
                            if (media.isDynamic && onStreamAutoRefreshNeeded != null) {
                                onStreamAutoRefreshNeeded()
                            }
                            retryCount++
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = CinemaBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("retry_playback_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(if (media.isDynamic) "Refresh Dynamic Stream" else "Retry Stream")
                    }
                }
            }
        }
    }

    // Track selection and chapters modal bottom sheet
    if (showTrackMenu) {
        PlayerTrackMenuSheet(
            chapters = chapters,
            subtitles = subtitles,
            selectedQuality = settings.qualityPreference,
            selectedSubtitleId = selectedSubtitleTrack?.id,
            currentPositionMs = currentPosition,
            onSelectQuality = { quality ->
                abrStrategy.applyQualityPreference(quality, settings.dataSaverMode)
            },
            onSelectSubtitle = { track ->
                selectedSubtitleTrack = track
            },
            onSeekToChapter = { posMs ->
                exoPlayer?.seekTo(posMs)
            },
            onAddChapter = { title, posMs ->
                onAddChapter?.invoke(title, posMs)
            },
            onDismiss = { showTrackMenu = false }
        )
    }
}
