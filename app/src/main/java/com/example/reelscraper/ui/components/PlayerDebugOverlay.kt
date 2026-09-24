package com.example.reelscraper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reelscraper.player.PlaybackStats
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.NeonCyan

@Composable
fun PlayerDebugOverlay(
    stats: PlaybackStats,
    poolActiveCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .background(CinemaBlack.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .testTag("player_debug_overlay")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ENGINE HUD",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${stats.hdrType} • ${if (stats.is60Fps) "60fps" else "${stats.frameRate.toInt()}fps"}",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            DebugItem("Res", stats.resolution)
            DebugItem("Bitrate", if (stats.bitrate > 0) "${stats.bitrate / 1000} kbps" else "--")
            DebugItem("Buffer", "${stats.bufferedDurationMs / 1000}s")
            DebugItem("Bandwidth", if (stats.estimatedBandwidthKbps > 0) "${stats.estimatedBandwidthKbps} kbps" else "Measuring")
            DebugItem("Dropped", "${stats.droppedFrames}")
            DebugItem("Rebuffers", "${stats.rebufferCount}")
            DebugItem("Audio Trk", stats.activeAudioLanguage)
            DebugItem("Subtitles", stats.activeSubtitleLanguage)
            DebugItem("Pool Active", "$poolActiveCount")
        }
    }
}

@Composable
private fun DebugItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
