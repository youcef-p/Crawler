package com.example.reelscraper.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackStats(
    val resolution: String = "--",
    val bitrate: Long = 0L,
    val bufferedDurationMs: Long = 0L,
    val estimatedBandwidthKbps: Long = 0L,
    val droppedFrames: Int = 0,
    val rebufferCount: Int = 0,
    val isHdr: Boolean = false,
    val hdrType: String = "SDR",
    val frameRate: Float = 0f,
    val is60Fps: Boolean = false,
    val activeAudioLanguage: String = "--",
    val activeSubtitleLanguage: String = "None"
)

@OptIn(UnstableApi::class)
class CustomAbrStrategy(
    private val context: Context,
    private val trackSelector: DefaultTrackSelector
) {
    private val _stats = MutableStateFlow(PlaybackStats())
    val stats: StateFlow<PlaybackStats> = _stats.asStateFlow()

    private var totalRebuffers = 0
    private var totalDroppedFrames = 0

    fun isCellular(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /**
     * Applies user quality policy and bandwidth optimization.
     * Prioritizes zero rebuffering over maximum resolution.
     */
    fun applyQualityPreference(qualityPref: String, isDataSaver: Boolean) {
        val builder = trackSelector.buildUponParameters()

        // Zero-rebuffering tuning
        builder.setAllowVideoMixedMimeTypeAdaptiveness(true)
        builder.setAllowVideoNonSeamlessAdaptiveness(true)

        if (isDataSaver || qualityPref.equals("DATA_SAVER", ignoreCase = true)) {
            // Restrict to max 480p and 800 kbps
            builder.setMaxVideoSize(854, 480)
            builder.setMaxVideoBitrate(800_000)
            builder.clearVideoSizeConstraints()
            builder.setMaxVideoSize(854, 480)
        } else if (qualityPref.equals("LOWEST", ignoreCase = true)) {
            builder.setMaxVideoSize(640, 360)
            builder.setMaxVideoBitrate(500_000)
        } else if (qualityPref.equals("HIGHEST", ignoreCase = true)) {
            builder.clearVideoSizeConstraints()
            builder.setMaxVideoBitrate(Int.MAX_VALUE)
        } else if (qualityPref.contains("1080", ignoreCase = true)) {
            builder.setMinVideoSize(1920, 1080)
            builder.setMaxVideoSize(1920, 1080)
        } else if (qualityPref.contains("720", ignoreCase = true)) {
            builder.setMinVideoSize(1280, 720)
            builder.setMaxVideoSize(1280, 720)
        } else if (qualityPref.contains("480", ignoreCase = true)) {
            builder.setMinVideoSize(854, 480)
            builder.setMaxVideoSize(854, 480)
        } else {
            // AUTO mode:
            builder.clearVideoSizeConstraints()
            if (isCellular()) {
                // Prefer conservative lower startup on cellular
                builder.setMaxVideoSize(1280, 720)
                builder.setMaxVideoBitrate(2_500_000)
            } else {
                builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                builder.setMaxVideoBitrate(Int.MAX_VALUE)
            }
        }

        trackSelector.setParameters(builder)
    }

    fun onRebufferEvent() {
        totalRebuffers++
        updateStats(rebufferCount = totalRebuffers)
    }

    fun onFrameDropped(count: Int) {
        totalDroppedFrames += count
        updateStats(droppedFrames = totalDroppedFrames)
    }

    fun updateBandwidthEstimate(bandwidthBps: Long) {
        val kbps = bandwidthBps / 1000L
        updateStats(estimatedBandwidthKbps = kbps)
    }

    fun updatePlayerState(player: ExoPlayer) {
        val format = player.videoFormat
        val buffered = player.bufferedPosition - player.currentPosition
        val tracks = player.currentTracks

        var audioLang = "--"
        var subtitleLang = "None"

        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO && group.isSelected) {
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) {
                        audioLang = group.getTrackFormat(i).language ?: "Unknown"
                    }
                }
            } else if (group.type == C.TRACK_TYPE_TEXT && group.isSelected) {
                for (i in 0 until group.length) {
                    if (group.isTrackSelected(i)) {
                        subtitleLang = group.getTrackFormat(i).language ?: "Default"
                    }
                }
            }
        }

        val resolutionStr = if (format != null && format.width > 0 && format.height > 0) {
            "${format.width}x${format.height}"
        } else {
            "--"
        }

        val fps = format?.frameRate ?: 0f
        val is60 = fps >= 55f

        val hdrTypeStr = when {
            format?.colorInfo?.colorTransfer == C.COLOR_TRANSFER_ST2084 -> "HDR10"
            format?.colorInfo?.colorTransfer == C.COLOR_TRANSFER_HLG -> "HLG"
            else -> "SDR"
        }

        _stats.value = _stats.value.copy(
            resolution = resolutionStr,
            bitrate = format?.bitrate?.toLong() ?: 0L,
            bufferedDurationMs = if (buffered > 0) buffered else 0L,
            droppedFrames = totalDroppedFrames,
            rebufferCount = totalRebuffers,
            isHdr = hdrTypeStr != "SDR",
            hdrType = hdrTypeStr,
            frameRate = fps,
            is60Fps = is60,
            activeAudioLanguage = audioLang,
            activeSubtitleLanguage = subtitleLang
        )
    }

    private fun updateStats(
        rebufferCount: Int = _stats.value.rebufferCount,
        droppedFrames: Int = _stats.value.droppedFrames,
        estimatedBandwidthKbps: Long = _stats.value.estimatedBandwidthKbps
    ) {
        _stats.value = _stats.value.copy(
            rebufferCount = rebufferCount,
            droppedFrames = droppedFrames,
            estimatedBandwidthKbps = estimatedBandwidthKbps
        )
    }
}
