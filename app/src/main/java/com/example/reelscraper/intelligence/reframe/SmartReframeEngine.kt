package com.example.reelscraper.intelligence.reframe

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

enum class ReframeMode {
    OFF,
    CENTER_CROP,
    MOTION_TRACKING,
    SUBJECT_TRACKING
}

data class ReframeOffset(
    val normalizedOffsetX: Float = 0f, // -1.0 to 1.0 (left to right offset from center)
    val normalizedOffsetY: Float = 0f
)

interface SmartReframeEngine {
    suspend fun computeOffset(
        currentFrame: Bitmap?,
        videoWidth: Int,
        videoHeight: Int,
        targetAspectRatio: Float = 9f / 16f
    ): ReframeOffset
}

class CenterCropReframeEngine : SmartReframeEngine {
    override suspend fun computeOffset(
        currentFrame: Bitmap?,
        videoWidth: Int,
        videoHeight: Int,
        targetAspectRatio: Float
    ): ReframeOffset {
        return ReframeOffset(0f, 0f)
    }
}

/**
 * On-device motion and luminance centroid tracker.
 * Calculates the center of visual activity without heavy external models.
 * Smoothly interpolates pan offset to keep active subjects centered.
 */
class SubjectTrackingReframeEngine : SmartReframeEngine {
    private var smoothedOffsetX = 0f
    private var prevFrame: Bitmap? = null

    override suspend fun computeOffset(
        currentFrame: Bitmap?,
        videoWidth: Int,
        videoHeight: Int,
        targetAspectRatio: Float
    ): ReframeOffset = withContext(Dispatchers.Default) {
        if (currentFrame == null || videoWidth <= 0 || videoHeight <= 0) {
            return@withContext ReframeOffset(smoothedOffsetX, 0f)
        }

        // Only need horizontal reframe if video is wider than vertical container
        val currentAspect = videoWidth.toFloat() / videoHeight.toFloat()
        if (currentAspect <= targetAspectRatio) {
            return@withContext ReframeOffset(0f, 0f)
        }

        // Sample small grid (e.g. 16x9 downscaled)
        val w = 16
        val h = 9
        val scaled = Bitmap.createScaledBitmap(currentFrame, w, h, false)

        var totalWeight = 0.0f
        var weightedXSum = 0.0f

        val prev = prevFrame
        for (y in 0 until h) {
            for (x in 0 until w) {
                val pixel = scaled.getPixel(x, y)
                val lum = (0.299f * ((pixel shr 16) and 0xFF) +
                        0.587f * ((pixel shr 8) and 0xFF) +
                        0.114f * (pixel and 0xFF)) / 255f

                var motionWeight = lum
                if (prev != null && prev.width == w && prev.height == h) {
                    val prevPix = prev.getPixel(x, y)
                    val prevLum = (0.299f * ((prevPix shr 16) and 0xFF) +
                            0.587f * ((prevPix shr 8) and 0xFF) +
                            0.114f * (prevPix and 0xFF)) / 255f
                    motionWeight += abs(lum - prevLum) * 3f
                }

                weightedXSum += (x.toFloat() / (w - 1)) * motionWeight
                totalWeight += motionWeight
            }
        }

        prevFrame = scaled

        val targetCenterNormX = if (totalWeight > 0f) {
            (weightedXSum / totalWeight).coerceIn(0f, 1f)
        } else {
            0.5f
        }

        // Convert center (0..1) to offset (-1..1)
        val rawOffset = (targetCenterNormX - 0.5f) * 2f

        // Smooth moving average
        smoothedOffsetX = (0.2f * rawOffset) + (0.8f * smoothedOffsetX)
        smoothedOffsetX = smoothedOffsetX.coerceIn(-0.6f, 0.6f)

        ReframeOffset(smoothedOffsetX, 0f)
    }
}
