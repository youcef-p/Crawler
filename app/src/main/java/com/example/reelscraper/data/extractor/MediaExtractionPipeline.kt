package com.example.reelscraper.data.extractor

import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.util.MediaNormalizer

class MediaExtractionPipeline(
    private val contentTypeSniffer: ContentTypeSniffer,
    private val webViewFallbackExtractor: WebViewFallbackExtractor = WebViewFallbackExtractor()
) {
    private val htmlTagExtractor = HtmlTagMediaExtractor()
    private val metaTagExtractor = MetaTagMediaExtractor()
    private val jsonLdExtractor = JsonLdMediaExtractor()
    private val inlineScriptExtractor = InlineScriptMediaExtractor()
    private val regexUrlExtractor = RegexUrlMediaExtractor()
    private val attributeScanExtractor = AttributeScanMediaExtractor()
    private val iframeEmbedExtractor = IframeEmbedMediaExtractor()
    private val feedSitemapExtractor = FeedSitemapMediaExtractor()
    private val jsonApiExtractor = JsonApiMediaExtractor()

    suspend fun execute(context: ExtractionContext): List<ScrapedMedia> {
        val settings = context.settings
        val candidates = mutableListOf<ExtractedMediaCandidate>()

        // 1. Fast HTML tag extraction
        if (settings.enableHtmlTag) {
            candidates.addAll(htmlTagExtractor.extract(context))
        }

        // 2. Meta/Open Graph/Twitter extraction
        if (settings.enableMetaTags) {
            candidates.addAll(metaTagExtractor.extract(context))
        }

        // 3. JSON-LD extraction
        if (settings.enableJsonLd) {
            candidates.addAll(jsonLdExtractor.extract(context))
        }

        // 4. Inline Script scanning
        if (settings.enableInlineScript) {
            candidates.addAll(inlineScriptExtractor.extract(context))
        }

        // 5. Data attribute scanning
        if (settings.enableAttributeScan) {
            candidates.addAll(attributeScanExtractor.extract(context))
        }

        // 6. Regex URL scanning
        if (settings.enableRegexScan) {
            candidates.addAll(regexUrlExtractor.extract(context))
        }

        // 7. iframe/embed scanning
        if (settings.enableIframeScan) {
            candidates.addAll(iframeEmbedExtractor.extract(context))
        }

        // 8. Feed & Sitemap scanning
        if (settings.enableFeedSitemapScan) {
            candidates.addAll(feedSitemapExtractor.extract(context))
        }

        // 9. JSON API scanning
        if (settings.enableJsonApiScan) {
            candidates.addAll(jsonApiExtractor.extract(context))
        }

        // 10. WebView fallback if static extraction finds zero playable media
        if (candidates.isEmpty() && settings.enableWebViewFallback && context.appContext != null) {
            val webViewCandidates = webViewFallbackExtractor.extractFromPage(
                context = context.appContext,
                pageUrl = context.pageUrl,
                settings = settings
            )
            candidates.addAll(webViewCandidates)
        }

        // Global page fallback title & poster
        val pageDoc = context.document
        val pageTitle = pageDoc?.title()?.trim()
        val ogTitle = pageDoc?.selectFirst("meta[property=og:title]")?.attr("content")?.trim()
        val ogImage = pageDoc?.selectFirst("meta[property=og:image]")?.attr("content")?.trim()
        val firstImg = pageDoc?.selectFirst("img[src]")?.attr("abs:src")?.trim()

        val globalDomain = MediaNormalizer.normalizeDomain(context.pageUrl)

        val mediaMap = mutableMapOf<Pair<String, String>, ScrapedMedia>()

        for (candidate in candidates) {
            val candidateDomain = MediaNormalizer.normalizeDomain(candidate.url).ifBlank { globalDomain }
            val normalizedName = MediaNormalizer.normalizeMediaName(
                mediaUrl = candidate.url,
                pageTitle = candidate.title ?: ogTitle ?: pageTitle,
                pageUrl = context.pageUrl
            )

            val key = Pair(normalizedName, candidateDomain)

            val resolvedTitle = candidate.title?.ifBlank { null }
                ?: ogTitle?.ifBlank { null }
                ?: pageTitle?.ifBlank { null }
                ?: normalizedName.replace('-', ' ').replace('_', ' ')
                    .split(' ')
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

            val resolvedPoster = candidate.posterUrl?.ifBlank { null }
                ?: ogImage?.ifBlank { null }
                ?: firstImg?.ifBlank { null }

            val scraped = ScrapedMedia(
                url = candidate.url,
                title = resolvedTitle,
                mediaType = candidate.mediaType,
                thumbnailUrl = resolvedPoster,
                sourcePageUrl = candidate.sourcePageUrl,
                sourceDomain = candidateDomain,
                normalizedName = normalizedName,
                fileExtension = candidate.fileExtension.ifBlank { MediaNormalizer.extractExtension(candidate.url) },
                durationMillis = candidate.durationMillis,
                width = candidate.width,
                height = candidate.height,
                extractorType = candidate.extractorType,
                discoveredTimestamp = System.currentTimeMillis(),
                hdrType = candidate.hdrType,
                frameRate = candidate.frameRate
            )

            val existing = mediaMap[key]
            if (existing == null) {
                mediaMap[key] = scraped
            }
        }

        return mediaMap.values.toList()
    }
}
