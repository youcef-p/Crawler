package com.example.reelscraper.player

import android.util.Log
import com.example.reelscraper.data.model.ScrapedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap

/**
 * Directional predictive preloader for vertical feed.
 * Tracks scroll direction and preloads initial segment/manifest bytes for upcoming 1-3 items.
 */
class PredictivePreloader(
    private val okHttpClient: OkHttpClient,
    private val coroutineScope: CoroutineScope
) {
    private val activePreloads = ConcurrentHashMap<String, Job>()
    private var lastIndex: Int = 0

    fun onPageChanged(currentIndex: Int, mediaList: List<ScrapedMedia>, preloadCount: Int, isDataSaver: Boolean) {
        if (isDataSaver || preloadCount <= 0 || mediaList.isEmpty()) {
            cancelAll()
            lastIndex = currentIndex
            return
        }

        val direction = if (currentIndex >= lastIndex) 1 else -1
        lastIndex = currentIndex

        // Cancel previous preloads going in opposite direction
        cancelAll()

        // Predict next target items
        val targets = mutableListOf<ScrapedMedia>()
        for (step in 1..preloadCount) {
            val targetIdx = currentIndex + (step * direction)
            if (targetIdx in mediaList.indices) {
                targets.add(mediaList[targetIdx])
            }
        }

        // Launch preloads
        for (item in targets) {
            val job = coroutineScope.launch(Dispatchers.IO) {
                preloadInitialBytes(item.url)
            }
            activePreloads[item.url] = job
        }
    }

    private fun preloadInitialBytes(url: String) {
        try {
            // Request first 64KB for MP4/WebM or first manifest for HLS/DASH
            val req = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-65536")
                .build()

            okHttpClient.newCall(req).execute().use { resp ->
                resp.body?.byteStream()?.use { stream ->
                    val buffer = ByteArray(8192)
                    var total = 0
                    var read: Int
                    while (stream.read(buffer).also { read = it } != -1 && total < 65536) {
                        total += read
                    }
                }
            }
            Log.d("PredictivePreloader", "Preloaded initial bytes for: $url")
        } catch (_: Exception) {
            // Ignore cancelled or failed preloads
        }
    }

    fun cancelAll() {
        for ((_, job) in activePreloads) {
            job.cancel()
        }
        activePreloads.clear()
    }
}
