package com.example.reelscraper.player

import android.content.Context
import android.util.Log
import com.example.reelscraper.data.extractor.DynamicStreamWebViewExtractor
import com.example.reelscraper.data.local.MediaDao
import com.example.reelscraper.data.local.StreamSessionDao
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.model.StreamSession
import com.example.reelscraper.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed class StreamResolutionResult {
    data class Success(
        val playableUrl: String,
        val headers: Map<String, String>,
        val session: StreamSession?
    ) : StreamResolutionResult()

    data class Error(
        val message: String,
        val isUnsupported: Boolean = false
    ) : StreamResolutionResult()
}

class DynamicStreamResolver(
    private val appContext: Context,
    private val mediaDao: MediaDao,
    private val sessionDao: StreamSessionDao,
    private val localProxy: LocalStreamingProxy,
    private val webViewExtractor: DynamicStreamWebViewExtractor = DynamicStreamWebViewExtractor()
) {
    private val refreshMutex = Mutex()

    suspend fun resolveStream(
        media: ScrapedMedia,
        settings: AppSettings,
        forceRefresh: Boolean = false
    ): StreamResolutionResult = withContext(Dispatchers.IO) {
        // Direct media without dynamic tag and without force refresh
        if (!media.isDynamic && !forceRefresh) {
            val headers = parseHeaders(media.playbackHeadersJson)
            val playableUrl = if (settings.enableLocalProxy) {
                localProxy.buildProxyUrl(
                    targetUrl = media.url,
                    referer = media.sourcePageUrl,
                    userAgent = DynamicStreamWebViewExtractor.DESKTOP_USER_AGENT
                )
            } else {
                media.url
            }
            return@withContext StreamResolutionResult.Success(playableUrl, headers, null)
        }

        // Check if existing session is still fresh
        val session = if (media.streamSessionId != null) {
            sessionDao.getSessionById(media.streamSessionId)
        } else {
            sessionDao.getSessionByMediaId(media.id) ?: sessionDao.getSessionByStreamUrl(media.url)
        }

        val now = System.currentTimeMillis()
        val isExpired = session?.expiresAt != null && session.expiresAt < now

        if (!forceRefresh && session != null && !isExpired && session.isActive) {
            val headers = parseHeaders(session.requestHeadersJson)
            val playableUrl = if (settings.enableLocalProxy) {
                localProxy.buildProxyUrl(
                    targetUrl = session.streamUrl,
                    referer = session.referer,
                    userAgent = session.userAgent,
                    cookie = session.cookieHeader
                )
            } else {
                session.streamUrl
            }
            return@withContext StreamResolutionResult.Success(playableUrl, headers, session)
        }

        // Trigger dynamic stream refresh
        refreshStream(media, settings)
    }

    suspend fun refreshStream(
        media: ScrapedMedia,
        settings: AppSettings
    ): StreamResolutionResult = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            Log.d("DynamicStreamResolver", "Refreshing dynamic stream for: ${media.title} (${media.sourcePageUrl})")

            val captureResults = webViewExtractor.captureDynamicStreams(
                context = appContext,
                pageUrl = media.sourcePageUrl,
                settings = settings
            )

            if (captureResults.isEmpty()) {
                mediaDao.updateBrokenStatus(media.id, true)
                return@withContext StreamResolutionResult.Error(
                    message = "Could not discover dynamic stream on page. Stream might be expired or protected.",
                    isUnsupported = true
                )
            }

            // Rank captured streams instead of assuming the first network request is best.
            val sourceDomain = media.sourceDomain.lowercase()
            val best = captureResults.maxByOrNull { capture ->
                var score = capture.candidate.priority
                if (capture.candidate.mediaType == media.mediaType) score += 600
                if (capture.candidate.mediaType.name == "HLS" || capture.candidate.mediaType.name == "DASH") score += 150
                val candidateDomain = com.example.reelscraper.data.util.MediaNormalizer.normalizeDomain(capture.candidate.url)
                if (candidateDomain == sourceDomain) score += 80
                val lowerUrl = capture.candidate.url.lowercase()
                if (lowerUrl.contains("master") || lowerUrl.contains("manifest") || lowerUrl.contains("playlist")) score += 40
                if (lowerUrl.contains("ads") || lowerUrl.contains("tracking") || lowerUrl.contains("analytics")) score -= 250
                score
            } ?: return@withContext StreamResolutionResult.Error(
                message = "Dynamic capture returned no usable stream candidates.",
                isUnsupported = true
            )

            sessionDao.deactivateSessionsForMedia(media.id)
            val newSession = best.session.copy(
                mediaId = media.id,
                capturedAt = System.currentTimeMillis(),
                lastRefreshResult = "SUCCESS"
            )

            val sessionId = sessionDao.insertSession(newSession)

            // Update media in DB
            mediaDao.updateRefreshedStreamUrl(
                id = media.id,
                newUrl = best.candidate.url,
                newHeaders = best.session.requestHeadersJson
            )

            val headers = parseHeaders(best.session.requestHeadersJson)
            val playableUrl = if (settings.enableLocalProxy) {
                localProxy.buildProxyUrl(
                    targetUrl = best.candidate.url,
                    referer = best.session.referer,
                    userAgent = best.session.userAgent,
                    cookie = best.session.cookieHeader
                )
            } else {
                best.candidate.url
            }

            Log.d("DynamicStreamResolver", "Stream refreshed successfully: $playableUrl")
            return@withContext StreamResolutionResult.Success(
                playableUrl = playableUrl,
                headers = headers,
                session = newSession.copy(id = sessionId)
            )
        }
    }

    private fun parseHeaders(jsonStr: String?): Map<String, String> {
        if (jsonStr.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(jsonStr)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.optString(key)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
