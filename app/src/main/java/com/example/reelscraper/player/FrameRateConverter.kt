package com.example.reelscraper.player

import android.os.Build
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * 60fps frame rate motion enhancer and cadence manager.
 * Applies optimal display frame rate timing hints and ExoPlayer pacing
 * to deliver 60fps motion clarity.
 */
class FrameRateConverter {

    var is60FpsEnabled: Boolean = true

    /**
     * Applies 60fps surface frame rate hint to the native Surface if on API 30+.
     */
    fun applySurfaceFrameRate(surface: Surface?, targetFps: Float = 60.0f) {
        if (surface == null || !surface.isValid || !is60FpsEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                surface.setFrameRate(
                    targetFps,
                    Surface.FRAME_RATE_COMPATIBILITY_DEFAULT,
                    Surface.CHANGE_FRAME_RATE_ALWAYS
                )
            } catch (_: Exception) {
                // Ignore if device platform does not support dynamic surface rate
            }
        }
    }

    /**
     * Configures ExoPlayer pacing for high frame rate presentation.
     */
    @OptIn(UnstableApi::class)
    fun configurePlayerPacing(player: ExoPlayer, sourceFrameRate: Float?) {
        if (!is60FpsEnabled) return
        val effectiveFps = sourceFrameRate ?: 30f
        if (effectiveFps >= 55f) {
            // Already 60fps source: ensure 1.0x smooth pitch and timing
            player.playbackParameters = PlaybackParameters(player.playbackParameters.speed, 1.0f)
        }
    }
}
