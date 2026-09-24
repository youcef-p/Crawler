package com.example.reelscraper.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.ui.theme.CinemaBlack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ambient cinematic backdrop effect.
 * Samples dominant colors from video poster or sampled frame and renders
 * a smoothly animated, blurred radiant gradient behind the player.
 */
@Composable
fun AmbientBackdrop(
    media: ScrapedMedia,
    ambientMode: String, // "OFF", "POSTER_ONLY", "DYNAMIC_LOW_FREQ", "DYNAMIC_HIGH_FREQ"
    modifier: Modifier = Modifier
) {
    if (ambientMode == "OFF") {
        Box(modifier = modifier.fillMaxSize().background(CinemaBlack))
        return
    }

    val context = LocalContext.current
    var topColor by remember(media.id) { mutableStateOf(Color(0xFF1E1B2E)) }
    var bottomColor by remember(media.id) { mutableStateOf(Color(0xFF0F172A)) }

    LaunchedEffect(media.thumbnailUrl, ambientMode) {
        if (!media.thumbnailUrl.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(media.thumbnailUrl)
                        .allowHardware(false)
                        .build()

                    val result = (loader.execute(request) as? SuccessResult)?.drawable
                    val bitmap = (result as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        val w = bitmap.width
                        val h = bitmap.height

                        // Sample top and bottom pixels
                        val topPixel = bitmap.getPixel(w / 2, (h * 0.25f).toInt().coerceIn(0, h - 1))
                        val bottomPixel = bitmap.getPixel(w / 2, (h * 0.75f).toInt().coerceIn(0, h - 1))

                        topColor = Color(topPixel).copy(alpha = 0.55f)
                        bottomColor = Color(bottomPixel).copy(alpha = 0.45f)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val animatedTop by animateColorAsState(targetValue = topColor, animationSpec = tween(800), label = "ambientTop")
    val animatedBottom by animateColorAsState(targetValue = bottomColor, animationSpec = tween(800), label = "ambientBottom")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedTop,
                        CinemaBlack.copy(alpha = 0.85f),
                        animatedBottom
                    )
                )
            )
            .blur(radius = 32.dp)
    )
}
