package com.example.reelscraper.data.extractor

import android.content.Context
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.settings.AppSettings
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document

data class ExtractedMediaCandidate(
    val url: String,
    val mediaType: MediaType,
    val title: String? = null,
    val posterUrl: String? = null,
    val sourcePageUrl: String,
    val fileExtension: String = "",
    val durationMillis: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val extractorType: String,
    val priority: Int = 0, // Higher priority candidate preferred for title/poster
    val trickPlayUrl: String? = null,
    val subtitlesUrl: String? = null,
    val chaptersJson: String? = null,
    val hdrType: String? = null,
    val frameRate: Float? = null
)

data class ExtractionContext(
    val pageUrl: String,
    val document: Document?,
    val html: String,
    val contentType: String? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val client: OkHttpClient,
    val settings: AppSettings,
    val appContext: Context? = null
)

interface MediaExtractor {
    val name: String
    suspend fun extract(context: ExtractionContext): List<ExtractedMediaCandidate>
}
