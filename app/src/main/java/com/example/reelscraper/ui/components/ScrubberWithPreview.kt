package com.example.reelscraper.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reelscraper.data.model.Chapter
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.player.TrickPlayManager
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CoralPink
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun ScrubberWithPreview(
    media: ScrapedMedia,
    currentPositionMs: Long,
    totalDurationMs: Long,
    bufferedPositionMs: Long,
    chapters: List<Chapter> = emptyList(),
    heatmapValues: List<Float> = emptyList(),
    trickPlayManager: TrickPlayManager? = null,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalDurationMs <= 0) return

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentChapterTitle by remember { mutableStateOf<String?>(null) }

    val activeFraction = if (isScrubbing) scrubFraction else (currentPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
    val bufferedFraction = (bufferedPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)

    // Load preview frame lazily when scrubbing
    LaunchedEffect(isScrubbing, scrubFraction) {
        if (isScrubbing && trickPlayManager != null) {
            val seekMs = (scrubFraction * totalDurationMs).toLong()
            currentChapterTitle = chapters.lastOrNull { it.positionMillis <= seekMs }?.title
            withContext(Dispatchers.IO) {
                previewBitmap = trickPlayManager.getPreviewForPosition(media, seekMs)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .testTag("scrubber_with_preview")
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val thumbOffsetXDp = (activeFraction * maxWidth.value).coerceIn(0f, maxWidth.value)

        // Floating thumbnail & timestamp preview popup
        AnimatedVisibility(
            visible = isScrubbing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    val popupWidth = 140.dp.toPx()
                    val targetX = (activeFraction * totalWidthPx) - (popupWidth / 2)
                    IntOffset(targetX.coerceIn(0f, totalWidthPx - popupWidth).roundToInt(), 0)
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CinemaBlack.copy(alpha = 0.92f))
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Scrub Preview",
                        modifier = Modifier
                            .width(120.dp)
                            .height(68.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                currentChapterTitle?.let { chapterTitle ->
                    Text(
                        text = chapterTitle,
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                val seekPosMs = (scrubFraction * totalDurationMs).toLong()
                Text(
                    text = "${formatDuration(seekPosMs)} / ${formatDuration(totalDurationMs)}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Timeline Bar
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .align(Alignment.BottomCenter)
                .pointerInput(totalDurationMs) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((fraction * totalDurationMs).toLong())
                    }
                }
                .pointerInput(totalDurationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isScrubbing = true
                            scrubFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isScrubbing = false
                            onSeek((scrubFraction * totalDurationMs).toLong())
                        },
                        onDragCancel = {
                            isScrubbing = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val deltaFraction = dragAmount / size.width
                            scrubFraction = (scrubFraction + deltaFraction).coerceIn(0f, 1f)
                        }
                    )
                }
        ) {
            val trackHeight = 4.dp.toPx()
            val yPos = size.height / 2f

            // 1. Heatmap overlay (if available)
            if (heatmapValues.isNotEmpty()) {
                val sliceWidth = size.width / heatmapValues.size
                for (i in heatmapValues.indices) {
                    val heat = heatmapValues[i]
                    if (heat > 0.05f) {
                        val barH = (heat * 12.dp.toPx()).coerceAtLeast(2f)
                        drawRect(
                            color = CoralPink.copy(alpha = heat * 0.45f),
                            topLeft = Offset(i * sliceWidth, yPos - barH),
                            size = Size(sliceWidth, barH)
                        )
                    }
                }
            }

            // 2. Background Track
            drawRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(0f, yPos - (trackHeight / 2)),
                size = Size(size.width, trackHeight)
            )

            // 3. Buffered Bar
            drawRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(0f, yPos - (trackHeight / 2)),
                size = Size(size.width * bufferedFraction, trackHeight)
            )

            // 4. Watched Progress Bar
            drawRect(
                color = NeonCyan,
                topLeft = Offset(0f, yPos - (trackHeight / 2)),
                size = Size(size.width * activeFraction, trackHeight)
            )

            // 5. Chapter Markers (slits/ticks)
            for (chapter in chapters) {
                if (chapter.positionMillis > 0 && chapter.positionMillis < totalDurationMs) {
                    val chapterX = (chapter.positionMillis.toFloat() / totalDurationMs) * size.width
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(chapterX, yPos)
                    )
                }
            }

            // 6. Thumb
            drawCircle(
                color = NeonCyan,
                radius = if (isScrubbing) 7.dp.toPx() else 5.dp.toPx(),
                center = Offset(size.width * activeFraction, yPos)
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
