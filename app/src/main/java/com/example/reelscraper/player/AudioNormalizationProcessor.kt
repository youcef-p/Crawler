package com.example.reelscraper.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Real-time audio normalization processor for ExoPlayer.
 * Estimates RMS energy of 16-bit PCM audio frames and applies smooth gain leveling
 * to prevent sudden volume jumps between scraped sources while preventing clipping.
 */
@OptIn(UnstableApi::class)
class AudioNormalizationProcessor : BaseAudioProcessor() {

    var isEnabled: Boolean = true
    var strength: String = "NORMAL" // "OFF", "LIGHT", "NORMAL", "STRONG"

    // Moving RMS tracking
    private var smoothedRms: Float = 0.15f
    private val smoothingFactor: Float = 0.05f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val output = replaceOutputBuffer(remaining)

        if (!isEnabled || strength == "OFF") {
            output.put(inputBuffer)
            output.flip()
            return
        }

        val targetRms = when (strength) {
            "LIGHT" -> 0.18f
            "STRONG" -> 0.28f
            else -> 0.22f // NORMAL
        }

        val maxGain = when (strength) {
            "LIGHT" -> 1.8f
            "STRONG" -> 3.5f
            else -> 2.5f
        }

        // Calculate RMS of this chunk
        val startPos = inputBuffer.position()
        var sumSquares = 0.0
        var sampleCount = 0

        while (inputBuffer.hasRemaining()) {
            val sample = inputBuffer.short
            val normalized = sample.toFloat() / 32768f
            sumSquares += (normalized * normalized)
            sampleCount++
        }

        val currentRms = if (sampleCount > 0) sqrt(sumSquares / sampleCount).toFloat() else smoothedRms
        smoothedRms = (smoothingFactor * currentRms) + ((1f - smoothingFactor) * smoothedRms)
        smoothedRms = max(0.01f, smoothedRms)

        // Calculate dynamic gain
        var computedGain = targetRms / smoothedRms
        computedGain = min(computedGain, maxGain)
        computedGain = max(0.4f, computedGain)

        // Reset position to process and scale samples into output
        inputBuffer.position(startPos)
        while (inputBuffer.hasRemaining()) {
            val sample = inputBuffer.short
            var scaled = (sample * computedGain).toInt()
            // Soft clipping prevention
            if (scaled > 32767) scaled = 32767
            if (scaled < -32768) scaled = -32768
            output.putShort(scaled.toShort())
        }

        output.flip()
    }
}
