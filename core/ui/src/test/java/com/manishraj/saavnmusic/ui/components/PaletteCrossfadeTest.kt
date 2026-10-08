package com.manishraj.saavnmusic.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The chrome wash (visual-uplift spec §3.3).
 *
 * The mini-player keeps artwork color only as this wash: the
 * palette's chrome tint at a capped alpha ([CHROME_WASH_ALPHA_DARK] /
 * [CHROME_WASH_ALPHA_LIGHT]) fading to transparent over the
 * artwork-side [CHROME_WASH_FRACTION] of the bar. These tests pin the
 * stop construction the composable brush is built from, so the §3.3
 * numbers cannot drift without a red gate; the tint derivation itself
 * is pinned by [ArtworkPaletteColorsTest].
 */
class PaletteCrossfadeTest {
    private fun palette(sourceSwatch: Color): ArtworkPaletteColors =
        ArtworkPaletteColors(
            dominant = Color(0xFF101010),
            vibrant = Color(0xFF202020),
            mutedDark = Color(0xFF181818),
            onMutedDark = Color.White,
            sourceSwatch = sourceSwatch,
        )

    @Test
    fun `wash spans the spec's artwork-side forty percent`() {
        // §3.3, pinned literally like the §3.2 constants: the wash
        // covers the artwork-side 40% of the bar's width.
        assertFloatEquals(0.40f, CHROME_WASH_FRACTION, 0f)
        val stops = chromeWashStops(palette(Color(0xFFD74282)).chromeTint(darkTheme = true), darkTheme = true)
        assertEquals(2, stops.size)
        assertFloatEquals(0f, stops[0].first, 0f)
        assertFloatEquals(CHROME_WASH_FRACTION, stops[1].first, 0f)
    }

    @Test
    fun `dark wash lays the dark tint at the dark wash alpha`() {
        // The audit's magenta fixture: HSL(334°, 0.65, 0.55).
        val palette = palette(Color(0xFFD74282))
        val tint = palette.chromeTint(darkTheme = true)
        val stops = chromeWashStops(tint, darkTheme = true)
        // Start stop: the tint's own RGB at the §3.3 alpha — to one
        // sRGB step (Color packs alpha to 8 bits: 0.14 lands as
        // 36/255), hence the 0.004 deltas on every alpha compare.
        assertFloatEquals(0.14f, stops[0].second.alpha, 0.004f)
        assertFloatEquals(CHROME_WASH_ALPHA_DARK, stops[0].second.alpha, 0.004f)
        assertFloatEquals(tint.red, stops[0].second.red, 0.004f)
        assertFloatEquals(tint.green, stops[0].second.green, 0.004f)
        assertFloatEquals(tint.blue, stops[0].second.blue, 0.004f)
        // End stop: fully transparent — the wash never reaches the
        // bar's trailing side as a flat fill.
        assertEquals(Color.Transparent, stops[1].second)
    }

    @Test
    fun `light wash lays the light tint at the light wash alpha`() {
        val palette = palette(Color(0xFFD74282))
        val tint = palette.chromeTint(darkTheme = false)
        val stops = chromeWashStops(tint, darkTheme = false)
        assertFloatEquals(0.10f, stops[0].second.alpha, 0.004f)
        assertFloatEquals(CHROME_WASH_ALPHA_LIGHT, stops[0].second.alpha, 0.004f)
        assertFloatEquals(tint.red, stops[0].second.red, 0.004f)
        assertFloatEquals(tint.green, stops[0].second.green, 0.004f)
        assertFloatEquals(tint.blue, stops[0].second.blue, 0.004f)
        assertEquals(Color.Transparent, stops[1].second)
    }

    @Test
    fun `wash alpha cap holds even for the fallback tint`() {
        val stops = chromeWashStops(ArtworkPaletteColors.Fallback.chromeTint(darkTheme = true), darkTheme = true)
        assertTrue(stops[0].second.alpha <= CHROME_WASH_ALPHA_DARK + 0.004f)
        assertEquals(Color.Transparent, stops[1].second)
    }

    private fun assertFloatEquals(
        expected: Float,
        actual: Float,
        delta: Float,
    ) {
        assertTrue("expected <$expected> but was <$actual>", abs(expected - actual) <= delta)
    }
}
