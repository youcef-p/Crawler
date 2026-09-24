package com.example.reelscraper.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reelscraper.data.extractor.ExtractedMediaCandidate
import com.example.reelscraper.data.model.ExtractionRule
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.repository.MediaRepository
import com.example.reelscraper.data.repository.SiteProfileRepository
import com.example.reelscraper.data.util.MediaNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Collections

data class SnifferCapturedItem(
    val url: String,
    val mediaType: MediaType,
    val title: String,
    val mimeType: String = "",
    val detectionSource: String = "NETWORK",
    val sourcePageUrl: String = "",
    val posterUrl: String? = null,
    val isSaved: Boolean = false
)

class SnifferViewModel(
    private val mediaRepository: MediaRepository,
    private val siteProfileRepository: SiteProfileRepository
) : ViewModel() {

    private val _currentUrl = MutableStateFlow("https://durian.blender.org/")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _isDesktopMode = MutableStateFlow(false)
    val isDesktopMode: StateFlow<Boolean> = _isDesktopMode.asStateFlow()

    private val _capturedItems = MutableStateFlow<List<SnifferCapturedItem>>(emptyList())
    val capturedItems: StateFlow<List<SnifferCapturedItem>> = _capturedItems.asStateFlow()

    private val _snackMessage = MutableStateFlow<String?>(null)
    val snackMessage: StateFlow<String?> = _snackMessage.asStateFlow()

    private val seenUrls = Collections.synchronizedSet(mutableSetOf<String>())

    fun setUrl(url: String) {
        _currentUrl.value = url
    }

    fun toggleDesktopMode() {
        _isDesktopMode.value = !_isDesktopMode.value
    }

    fun clearCaptured() {
        seenUrls.clear()
        _capturedItems.value = emptyList()
    }

    fun clearSnackMessage() {
        _snackMessage.value = null
    }

    fun onMediaIntercepted(
        url: String,
        source: String,
        mimeType: String,
        pageUrl: String
    ) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank() || seenUrls.contains(cleanUrl)) return
        seenUrls.add(cleanUrl)

        val type = determineType(cleanUrl, mimeType)
        val name = MediaNormalizer.normalizeMediaName(cleanUrl, pageUrl = pageUrl)
        val title = name.replace('-', ' ').replace('_', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

        val item = SnifferCapturedItem(
            url = cleanUrl,
            mediaType = type,
            title = title,
            mimeType = mimeType,
            detectionSource = source,
            sourcePageUrl = pageUrl
        )

        _capturedItems.value = _capturedItems.value + item
    }

    fun saveItem(item: SnifferCapturedItem) {
        viewModelScope.launch {
            val domain = MediaNormalizer.normalizeDomain(item.sourcePageUrl).ifBlank {
                MediaNormalizer.normalizeDomain(item.url)
            }
            val normalizedName = MediaNormalizer.normalizeMediaName(item.url, pageUrl = item.sourcePageUrl)

            val scraped = ScrapedMedia(
                url = item.url,
                title = item.title,
                mediaType = item.mediaType,
                thumbnailUrl = item.posterUrl,
                sourcePageUrl = item.sourcePageUrl,
                sourceDomain = domain,
                normalizedName = normalizedName,
                fileExtension = MediaNormalizer.extractExtension(item.url),
                extractorType = "SNIFFER_${item.detectionSource}",
                isDynamic = item.mediaType == MediaType.HLS || item.mediaType == MediaType.DASH
            )

            val id = mediaRepository.insertMedia(scraped)
            if (id > 0) {
                _snackMessage.value = "Saved '${item.title}' to Library!"
                _capturedItems.value = _capturedItems.value.map {
                    if (it.url == item.url) it.copy(isSaved = true) else it
                }
            } else {
                _snackMessage.value = "'${item.title}' already exists in Library (duplicate excluded)."
            }
        }
    }

    fun createRuleFromItem(item: SnifferCapturedItem) {
        viewModelScope.launch {
            val domain = MediaNormalizer.normalizeDomain(item.sourcePageUrl)
            if (domain.isBlank()) return@launch

            val ext = MediaNormalizer.extractExtension(item.url)
            val regex = if (ext.isNotBlank()) ".*\\.$ext.*" else ".*"
            val rule = ExtractionRule(
                domain = domain,
                ruleType = "REGEX",
                regex = regex,
                targetType = "MEDIA_URL",
                priority = 10,
                enabled = true
            )
            siteProfileRepository.saveRule(rule)
            _snackMessage.value = "Created extraction rule for domain '$domain'"
        }
    }

    private fun determineType(url: String, mime: String): MediaType {
        val lowerUrl = url.lowercase()
        val lowerMime = mime.lowercase()
        return when {
            lowerUrl.contains(".m3u8") || lowerMime.contains("mpegurl") -> MediaType.HLS
            lowerUrl.contains(".mpd") || lowerMime.contains("dash+xml") -> MediaType.DASH
            lowerUrl.contains(".gif") || lowerMime.contains("image/gif") -> MediaType.GIF
            else -> MediaType.VIDEO
        }
    }
}
