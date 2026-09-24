package com.example.reelscraper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.player.WebVttCue

@Composable
fun SubtitleOverlay(
    currentCue: WebVttCue?,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    if (currentCue == null || currentCue.text.isBlank()) return

    val textColor = try {
        Color(android.graphics.Color.parseColor(settings.subtitleTextColorHex))
    } catch (_: Exception) {
        Color.White
    }

    val bgColor = try {
        Color(android.graphics.Color.parseColor(settings.subtitleBgColorHex))
    } catch (_: Exception) {
        Color.Black.copy(alpha = 0.6f)
    }

    val textAlign = when (currentCue.alignment.lowercase()) {
        "left" -> TextAlign.Start
        "right" -> TextAlign.End
        else -> TextAlign.Center
    }

    val alignment = when (currentCue.alignment.lowercase()) {
        "left" -> Alignment.BottomStart
        "right" -> Alignment.BottomEnd
        else -> Alignment.BottomCenter
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = settings.subtitleVerticalOffsetDp.dp + 48.dp, start = 16.dp, end = 16.dp),
        contentAlignment = alignment
    ) {
        Text(
            text = currentCue.text,
            color = textColor,
            fontSize = settings.subtitleFontSizeSp.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = textAlign,
            modifier = Modifier
                .background(bgColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
