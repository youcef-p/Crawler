package com.example.reelscraper.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CoralPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.VividViolet
import java.util.Locale

@Composable
fun ReelOverlayControls(
    media: ScrapedMedia,
    isPlaying: Boolean,
    isMuted: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    aspectRatioName: String = "FIT",
    playbackSpeedText: String = "1.0x",
    onTogglePlayPause: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCycleAspectRatio: () -> Unit = {},
    onCycleSpeed: () -> Unit = {},
    onRefreshStream: () -> Unit = {},
    onToggleBroken: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    // Heart scale animation
    val heartScale by animateFloatAsState(
        targetValue = if (media.isFavorite) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "heartScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTogglePlayPause
            )
    ) {
        // Subtle top gradient scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )

        // Bottom gradient scrim for readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
        )

        // Center pause icon indicator
        AnimatedVisibility(
            visible = !isPlaying && media.mediaType != MediaType.GIF,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = CircleShape,
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Paused",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }
        }

        // Right-hand action buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 70.dp)
        ) {
            // Favorite Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .scale(heartScale)
                        .testTag("favorite_button")
                ) {
                    Icon(
                        imageVector = if (media.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (media.isFavorite) CoralPink else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = if (media.isFavorite) "Liked" else "Like",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Aspect Ratio Toggle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onCycleAspectRatio,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Toggle Aspect Ratio",
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = aspectRatioName,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Playback Speed Selector
            if (media.mediaType != MediaType.GIF) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onCycleSpeed,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Playback Speed",
                            tint = NeonPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = playbackSpeedText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Mute / Unmute Button
            if (media.mediaType != MediaType.GIF) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .testTag("mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = if (isMuted) CoralPink else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = if (isMuted) "Muted" else "Audio",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Refresh Dynamic Stream Button
            if (media.isDynamic) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onRefreshStream,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Stream",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Refresh",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Share / Copy Link Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        shareMediaUrl(context, media)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .testTag("share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Stream",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Share",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Open Original Webpage
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        openWebPage(context, media.sourcePageUrl)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .testTag("source_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open Source Website",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Source",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Bottom Content (Title, Tags, Timeline Scrubber)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 76.dp, bottom = 20.dp)
        ) {
            // Badges row: Domain, Format, Dynamic
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                // Media Type Badge
                Surface(
                    color = when (media.mediaType) {
                        MediaType.HLS -> VividViolet
                        MediaType.DASH -> CoralPink
                        MediaType.GIF -> NeonCyan
                        MediaType.VIDEO -> MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = media.typeBadge,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                // Dynamic Stream Badge
                if (media.isDynamic) {
                    Surface(
                        color = NeonPurple.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "DYNAMIC",
                            color = NeonPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                // Domain chip
                if (media.sourceDomain.isNotBlank()) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = media.sourceDomain,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                // Broken indicator
                if (media.isBroken) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "BROKEN",
                            color = Color.Red,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Title
            Text(
                text = media.displayTitle,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 19.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Direct URL snippet
            Text(
                text = media.url,
                color = Color.LightGray.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Progress Scrubber (for video streams)
            if (media.mediaType != MediaType.GIF && totalDurationMs > 0L) {
                val progressFraction = (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = NeonCyan,
                        trackColor = Color.White.copy(alpha = 0.25f),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = formatTime(totalDurationMs),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

private fun shareMediaUrl(context: Context, media: ScrapedMedia) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, media.displayTitle)
            putExtra(
                Intent.EXTRA_TEXT,
                "${media.displayTitle}\n\nStream URL: ${media.url}\n\nSource: ${media.sourcePageUrl}"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Video Stream"))
    } catch (_: Exception) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Media URL", media.url))
        Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}

private fun openWebPage(context: Context, url: String) {
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(browserIntent)
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
    }
}
