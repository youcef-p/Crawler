package com.example.reelscraper

import com.example.reelscraper.data.util.MediaNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaNormalizerTest {

    @Test
    fun normalizeUrl_resolvesRelativeAndProtocolRelativeUrls() {
        assertEquals(
            "https://example.com/videos/clip.mp4",
            MediaNormalizer.normalizeUrl("/videos/clip.mp4", "https://example.com/page")
        )
        assertEquals(
            "https://cdn.example.com/clip.m3u8",
            MediaNormalizer.normalizeUrl("//cdn.example.com/clip.m3u8", "https://example.com/page")
        )
    }

    @Test
    fun normalizeUrl_rejectsUnsupportedSchemes() {
        assertNull(MediaNormalizer.normalizeUrl("javascript:alert(1)", "https://example.com"))
        assertNull(MediaNormalizer.normalizeUrl("blob:https://example.com/id", "https://example.com"))
        assertNull(MediaNormalizer.normalizeUrl("data:text/plain,hello", "https://example.com"))
    }

    @Test
    fun normalizeDomain_removesWwwAndLowercases() {
        assertEquals("example.com", MediaNormalizer.normalizeDomain("https://WWW.Example.COM/video"))
    }

    @Test
    fun normalizeMediaName_prefersUsefulPathSegment() {
        assertEquals(
            "summer-clip-1080p",
            MediaNormalizer.normalizeMediaName(
                "https://cdn.example.com/media/Summer_Clip-1080p.mp4?token=abc"
            )
        )
    }
}
