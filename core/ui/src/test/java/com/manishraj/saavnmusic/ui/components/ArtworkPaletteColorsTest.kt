package com.manishraj.saavnmusic.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Artwork palette v2 — chrome math (visual-uplift spec §3.2).
 *
 * The pre-uplift mini-player fill capped only HSL lightness, leaving a
 * saturated magenta cover at full saturation: the muddy maroon bar.
 * [ArtworkPaletteColors.chromeTint] must scale saturation by 0.45
 * (capped at 0.38) and clamp lightness into the theme's chrome band,
 * while the hero path (role views over the extracted fields) stays
 * exactly as shipped.
 */
class ArtworkPaletteColorsTest {
    private fun palette(sourceSwatch: Color): ArtworkPaletteColors =
        ArtworkPaletteColors(
            dominant = Color(0xFF101010),
            vibrant = Color(0xFF202020),
            mutedDark = Color(0xFF181818),
            onMutedDark = Color.White,
            sourceSwatch = sourceSwatch,
        )

    @Test
    fun `magenta fixture lands as the spec worked example`() {
        // HSL(334°, 0.65, 0.55) — the audit's Kesariya (Lofi Flip)
        // vibrant swatch, as an 8-bit color.
        val tint = palette(Color(0xFFD74282)).chromeTint(darkTheme = true)
        val hsl = hslOf(tint)
        // s' = min(0.65 × 0.45, 0.38) = 0.29; l' = clamp(0.55) = 0.18.
        assertFloatEquals(0.29f, hsl[1], 0.02f)
        assertFloatEquals(CHROME_MAX_LIGHTNESS_DARK, hsl[2], 0.01f)
        assertFloatEquals(334f, hsl[0], 3f)
    }

    @Test
    fun `saturation cap bites on a fully saturated swatch`() {
        // Pure red: HSL(0°, 1.0, 0.5) → 1.0 × 0.45 = 0.45 > cap 0.38.
        val tint = palette(Color.Red).chromeTint(darkTheme = true)
        val hsl = hslOf(tint)
        assertFloatEquals(CHROME_SATURATION_CAP, hsl[1], 0.01f)
        assertFloatEquals(CHROME_MAX_LIGHTNESS_DARK, hsl[2], 0.01f)
    }

    @Test
    fun `gray swatch stays gray and clamps into both bands`() {
        val gray = palette(Color(0xFF808080))
        val dark = hslOf(gray.chromeTint(darkTheme = true))
        val light = hslOf(gray.chromeTint(darkTheme = false))
        assertFloatEquals(0f, dark[1], 0.01f)
        assertFloatEquals(CHROME_MAX_LIGHTNESS_DARK, dark[2], 0.01f)
        assertFloatEquals(0f, light[1], 0.01f)
        assertFloatEquals(CHROME_MIN_LIGHTNESS_LIGHT, light[2], 0.01f)
    }

    @Test
    fun `near-white swatch clamps to the light band ceiling`() {
        val tint = palette(Color(0xFFF2F2F2)).chromeTint(darkTheme = false)
        assertFloatEquals(CHROME_MAX_LIGHTNESS_LIGHT, hslOf(tint)[2], 0.01f)
    }

    @Test
    fun `fallback passes through the same clamps`() {
        val dark = hslOf(ArtworkPaletteColors.Fallback.chromeTint(darkTheme = true))
        assertTrue(dark[1] <= CHROME_SATURATION_CAP + 0.01f)
        assertTrue(dark[2] >= CHROME_MIN_LIGHTNESS_DARK - 0.01f)
        assertTrue(dark[2] <= CHROME_MAX_LIGHTNESS_DARK + 0.01f)
        // Hue survives: the deep teal stays teal (~169°).
        assertFloatEquals(169f, dark[0], 5f)
        val light = hslOf(ArtworkPaletteColors.Fallback.chromeTint(darkTheme = false))
        assertFloatEquals(CHROME_MIN_LIGHTNESS_LIGHT, light[2], 0.01f)
    }

    @Test
    fun `default source swatch is the muted dark tone`() {
        val implicit =
            ArtworkPaletteColors(
                dominant = Color(0xFF101010),
                vibrant = Color(0xFFD74282),
                mutedDark = Color(0xFF5C1A33),
                onMutedDark = Color.White,
            )
        val explicit = implicit.copy(sourceSwatch = implicit.mutedDark)
        assertEquals(explicit.chromeTint(darkTheme = true), implicit.chromeTint(darkTheme = true))
        assertEquals(explicit.chromeTint(darkTheme = false), implicit.chromeTint(darkTheme = false))
    }

    @Test
    fun `hero path is untouched by the chrome derivation`() {
        val colors = palette(Color(0xFFD74282))
        assertEquals(colors.vibrant, colors.rolePrimary)
        assertEquals(colors.mutedDark, colors.roleContainer)
        assertEquals(colors.onMutedDark, colors.onRoleContainer)
    }

    /** [hue 0..360, saturation 0..1, lightness 0..1] — the HSL definition, restated for assertions. */
    private fun hslOf(color: Color): FloatArray {
        val maxChannel = max(color.red, max(color.green, color.blue))
        val minChannel = min(color.red, min(color.green, color.blue))
        val lightness = (maxChannel + minChannel) / 2f
        if (maxChannel == minChannel) {
            return floatArrayOf(0f, 0f, lightness)
        }
        val delta = maxChannel - minChannel
        val saturation =
            if (lightness > 0.5f) {
                delta / (2f - maxChannel - minChannel)
            } else {
                delta / (maxChannel + minChannel)
            }
        val hue =
            when (maxChannel) {
                color.red -> ((color.green - color.blue) / delta + if (color.green < color.blue) 6f else 0f) * 60f
                color.green -> ((color.blue - color.red) / delta + 2f) * 60f
                else -> ((color.red - color.green) / delta + 4f) * 60f
            }
        return floatArrayOf(hue, saturation, lightness)
    }

    private fun assertFloatEquals(
        expected: Float,
        actual: Float,
        delta: Float,
    ) {
        assertTrue("expected <$expected> but was <$actual>", abs(expected - actual) <= delta)
    }
}
