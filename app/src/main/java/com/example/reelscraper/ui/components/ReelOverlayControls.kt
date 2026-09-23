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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
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
import com.example.ui.theme.CoralPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VividViolet
import java.util.Locale

@Composable
fun ReelOverlayControls(
    media: ScrapedMedia,
    isPlaying: Boolean,
    isMuted: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onTogglePlayPause: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleFavorite: () -> Unit,
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
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 80.dp)
        ) {
            // Favorite Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .scale(heartScale)
                        .testTag("favorite_button")
                ) {
                    Icon(
                        imageVector = if (media.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (media.isFavorite) CoralPink else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = if (media.isFavorite) "Liked" else "Like",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Mute / Unmute Button
            if (media.mediaType != MediaType.GIF) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .testTag("mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = if (isMuted) CoralPink else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = if (isMuted) "Muted" else "Audio",
                        color = Color.White,
                        fontSize = 11.sp,
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
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .testTag("share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Stream",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Share",
                    color = Color.White,
                    fontSize = 11.sp,
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
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .testTag("source_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open Source Website",
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Source",
                    color = Color.White,
                    fontSize = 11.sp,
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
                .padding(start = 16.dp, end = 80.dp, bottom = 24.dp)
        ) {
            // Badges row: Domain & Format
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Title
            Text(
                text = media.displayTitle,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Direct URL snippet
            Text(
                text = media.url,
                color = Color.LightGray.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 12.dp)
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

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (media.isStream && totalDurationMs <= 0L) "LIVE" else formatTime(totalDurationMs),
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, remainingSeconds)
}

private fun shareMediaUrl(context: Context, media: ScrapedMedia) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Extracted with ReelScraper:\n${media.displayTitle}\n${media.url}")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Media Stream")
        context.startActivity(shareIntent)
    } catch (_: Exception) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Media URL", media.url)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}

private fun openWebPage(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Cannot open source page", Toast.LENGTH_SHORT).show()
    }
}
