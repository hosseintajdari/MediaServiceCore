package com.liskovsoft.youtubeapi.innertube.impl

import com.google.gson.Gson
import com.liskovsoft.mediaserviceinterfaces.data.MediaFormat
import com.liskovsoft.sharedutils.TestHelpers
import com.liskovsoft.youtubeapi.common.models.gen.TextItem
import com.liskovsoft.youtubeapi.innertube.models.CaptionTrack
import com.liskovsoft.youtubeapi.innertube.models.PlayerResult
import com.liskovsoft.youtubeapi.innertube.models.TranslationLanguage
import com.liskovsoft.youtubeapi.innertube.utils.getMergeCaptionTracks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaItemFormatInfoImplTest {
    @Test
    fun sabrResponsePreservesFormatMetadataAndPrefersSabr() {
        val formatInfo = MediaItemFormatInfoImpl(
            parsePlayerResponse("player/web/2025.11.10_player_sabr_only.json")
        )
        val audioFormat = formatInfo.getAdaptiveFormats()
            ?.firstOrNull { it.getAudioTrackId() != null }

        assertTrue(formatInfo.containsSabrFormats())
        assertFalse(formatInfo.containsDashFormats())
        assertNotNull(audioFormat)
        assertNotNull(audioFormat?.getXtags())
        assertNotNull(audioFormat?.getAudioTrackId())
    }

    @Test
    fun responseWithoutSabrEndpointRetainsDashFallback() {
        val response = parsePlayerResponse("player/web/2025.11.10_player_regular.json")
        val dashFormat = response.streamingData?.adaptiveFormats
            ?.first { it.mimeType?.startsWith("video/") == true }
            ?.copy(
                url = "https://example.com/videoplayback",
                cipher = null,
                signatureCipher = null
            )
        val withoutSabrEndpoint = response.copy(
            streamingData = response.streamingData?.copy(
                adaptiveFormats = listOfNotNull(dashFormat),
                serverAbrStreamingUrl = null
            )
        )
        val formatInfo = MediaItemFormatInfoImpl(withoutSabrEndpoint)

        assertFalse(formatInfo.containsSabrFormats())
        assertTrue(formatInfo.containsDashFormats())
        assertEquals(MediaFormat.FORMAT_TYPE_DASH, formatInfo.getAdaptiveFormats()?.first()?.getFormatType())
    }

    @Test
    fun responseWithoutUstreamerConfigRetainsDashFallback() {
        val response = parsePlayerResponse("player/web/2025.11.10_player_regular.json")
        val dashFormat = response.streamingData?.adaptiveFormats
            ?.first { it.mimeType?.startsWith("video/") == true }
            ?.copy(
                url = "https://example.com/videoplayback",
                cipher = null,
                signatureCipher = null
            )
        val withoutUstreamerConfig = response.copy(
            streamingData = response.streamingData?.copy(
                adaptiveFormats = listOfNotNull(dashFormat),
                serverAbrStreamingUrl = "https://example.com/sabr"
            ),
            playerConfig = response.playerConfig?.copy(
                mediaCommonConfig = response.playerConfig.mediaCommonConfig?.copy(
                    mediaUstreamerRequestConfig = null
                )
            )
        )
        val formatInfo = MediaItemFormatInfoImpl(withoutUstreamerConfig)

        assertFalse(formatInfo.containsSabrFormats())
        assertTrue(formatInfo.containsDashFormats())
        assertEquals(MediaFormat.FORMAT_TYPE_DASH, formatInfo.getAdaptiveFormats()?.first()?.getFormatType())
    }

    @Test
    fun translatableCaptionsOfferPersianWhenTranslationLanguagesAreMissing() {
        val result = captionResponse(true, "en", null).getMergeCaptionTracks().orEmpty()

        assertEquals(listOf("en", "fa"), result.map(CaptionTrack::languageCode))
        assertEquals("https://example.com/captions?lang=en&tlang=fa", result.last().baseUrl)
        assertEquals("asr", result.last().kind)
    }

    @Test
    fun translatableCaptionsPreserveOtherTranslationsAndAddPersian() {
        val result = captionResponse(true, "en", listOf(translation("fr"))).getMergeCaptionTracks().orEmpty()

        assertEquals(listOf("en", "fr", "fa"), result.map(CaptionTrack::languageCode))
    }

    @Test
    fun persianFallbackIsNotDuplicatedWhenAlreadyAvailable() {
        val native = captionResponse(true, "fa-IR", null).getMergeCaptionTracks().orEmpty()
        val advertised = captionResponse(true, "en", listOf(translation("fa"))).getMergeCaptionTracks().orEmpty()

        assertEquals(listOf("fa-IR"), native.map(CaptionTrack::languageCode))
        assertEquals(listOf("en", "fa"), advertised.map(CaptionTrack::languageCode))
    }

    @Test
    fun nonTranslatableCaptionsDoNotInventPersian() {
        val result = captionResponse(false, "en", null).getMergeCaptionTracks().orEmpty()

        assertEquals(listOf("en"), result.map(CaptionTrack::languageCode))
    }

    private fun captionResponse(
        translatable: Boolean,
        language: String,
        translations: List<TranslationLanguage>?
    ): PlayerResult {
        val track = CaptionTrack("https://example.com/captions?lang=$language", translatable, language, null, null, null)
        return PlayerResult(
            playabilityStatus = null,
            streamingData = null,
            playerConfig = null,
            captions = PlayerResult.Captions(
                PlayerResult.Captions.PlayerCaptionsTracklistRenderer(listOf(track), translations)
            ),
            videoDetails = null,
            storyboards = null,
            microformat = null,
            paidContentOverlay = null,
            playbackTracking = null
        )
    }

    private fun translation(code: String) = TranslationLanguage(code, TextItem(null, code, null, null))

    private fun parsePlayerResponse(path: String) =
        Gson().fromJson(TestHelpers.readResource(path), PlayerResult::class.java)
}
