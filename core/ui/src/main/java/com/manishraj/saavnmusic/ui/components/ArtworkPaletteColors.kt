package com.manishraj.saavnmusic.ui.components

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Artwork-derived colors (UIUX_DESIGN §3.1.3): extracted from a song's
 * artwork with AndroidX Palette, cached in memory by artwork URL.
 *
 * - [mutedDark] is the vibrant (else dominant) artwork color darkened
 *   to HSL lightness <= 0.18, so it is always a safe backdrop: the
 *   player background is a vertical gradient mutedDark -> background.
 * - [onMutedDark] is picked by an in-code contrast test (white when it
 *   passes 4.5:1 against [mutedDark], black otherwise) — the spec's
 *   guaranteed-contrast invariant. Never put `onSurfaceVariant` text
 *   directly on [mutedDark].
 * - Loading and every failure path resolve to [Fallback]: the spec's
 *   deep teal. Never grey, never a random hue, never a crash.
 *
 * Extraction runs off the main thread (Coil fetch + Palette on
 * [Dispatchers.Default]) and goes through the app's Coil singleton, so
 * it shares the memory/disk cache with `AsyncImage`.
 */
data class ArtworkPaletteColors(
    val dominant: Color,
    val vibrant: Color,
    val mutedDark: Color,
    val onMutedDark: Color,
) {
    companion object {
        /** Spec fallback (no artwork / extraction fails): deep teal. */
        val Fallback =
            ArtworkPaletteColors(
                dominant = Color(0xFF0E3B33),
                vibrant = Color(0xFF1B7A64),
                mutedDark = Color(0xFF0E3B33),
                onMutedDark = Color.White,
            )
    }
}

/** HSL lightness cap for [ArtworkPaletteColors.mutedDark] (spec: L* <= 0.18). */
private const val MAX_GRADIENT_LIGHTNESS = 0.18f

/** Minimum contrast for text over the artwork color (spec invariant: 4.5:1). */
private const val MIN_TEXT_CONTRAST = 4.5f

/** Side of the bitmap Palette samples; artwork color needs no more. */
private const val SAMPLE_SIZE_PX = 128

/** URL-keyed in-memory cache (spec §3.1.3); cleared wholesale when full. */
private object ArtworkPaletteCache {
    private const val MAX_ENTRIES = 64

    private val entries = ConcurrentHashMap<String, ArtworkPaletteColors>()

    operator fun get(url: String): ArtworkPaletteColors? = entries[url]

    fun put(
        url: String,
        colors: ArtworkPaletteColors,
    ) {
        if (entries.size >= MAX_ENTRIES) {
            entries.clear()
        }
        entries[url] = colors
    }
}

/**
 * Loads (or returns the cached) palette for [imageUrl]. A null/blank
 * URL and ANY failure resolve to [ArtworkPaletteColors.Fallback];
 * this function never throws (cancellation aside).
 */
suspend fun loadArtworkPalette(
    context: Context,
    imageUrl: String?,
): ArtworkPaletteColors {
    if (imageUrl.isNullOrBlank()) {
        return ArtworkPaletteColors.Fallback
    }
    ArtworkPaletteCache[imageUrl]?.let { return it }
    val extracted =
        try {
            extractPalette(context, imageUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    val colors = extracted ?: ArtworkPaletteColors.Fallback
    ArtworkPaletteCache.put(imageUrl, colors)
    return colors
}

/**
 * Compose state holder: returns the cached palette immediately when
 * present, [fallback] while extraction runs (or for a null URL), and
 * recomposes with the extracted colors when they land. Callers animate
 * the change (spec: 300 ms crossfade, no hard cuts).
 */
@Composable
fun rememberArtworkPalette(
    imageUrl: String?,
    fallback: ArtworkPaletteColors = ArtworkPaletteColors.Fallback,
): ArtworkPaletteColors {
    val context = LocalContext.current
    var colors by remember(imageUrl) {
        mutableStateOf(
            if (imageUrl != null) {
                ArtworkPaletteCache[imageUrl] ?: fallback
            } else {
                fallback
            },
        )
    }
    LaunchedEffect(imageUrl) {
        if (imageUrl != null && ArtworkPaletteCache[imageUrl] == null) {
            colors = loadArtworkPalette(context.applicationContext, imageUrl)
        }
    }
    return colors
}

private suspend fun extractPalette(
    context: Context,
    imageUrl: String,
): ArtworkPaletteColors? =
    withContext(Dispatchers.Default) {
        val request =
            ImageRequest
                .Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .size(SAMPLE_SIZE_PX, SAMPLE_SIZE_PX)
                .build()
        val result = Coil.imageLoader(context).execute(request)
        val bitmap =
            ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
                ?: return@withContext null
        val palette = Palette.from(bitmap).generate()
        val vibrantSwatch = palette.vibrantSwatch
        val dominantSwatch = palette.dominantSwatch
        val baseSwatch =
            vibrantSwatch ?: dominantSwatch ?: palette.mutedSwatch
                ?: return@withContext null
        val vibrant =
            vibrantSwatch?.let { Color(it.rgb) } ?: Color(baseSwatch.rgb)
        val dominant =
            dominantSwatch?.let { Color(it.rgb) } ?: Color(baseSwatch.rgb)
        val mutedDark =
            Color(baseSwatch.rgb).darkenToMaxLightness(MAX_GRADIENT_LIGHTNESS)
        ArtworkPaletteColors(
            dominant = dominant,
            vibrant = vibrant,
            mutedDark = mutedDark,
            onMutedDark = contentColorOn(mutedDark),
        )
    }

/** Picks the text color that passes the contrast invariant on [background]. */
private fun contentColorOn(background: Color): Color =
    if (contrastRatio(Color.White, background) >= MIN_TEXT_CONTRAST) {
        Color.White
    } else {
        Color.Black
    }

private fun contrastRatio(
    first: Color,
    second: Color,
): Float {
    val firstLuminance = first.relativeLuminance()
    val secondLuminance = second.relativeLuminance()
    val lighter = max(firstLuminance, secondLuminance)
    val darker = min(firstLuminance, secondLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun Color.relativeLuminance(): Float {
    fun linear(channel: Float): Float =
        if (channel <= 0.04045f) {
            channel / 12.92f
        } else {
            ((channel + 0.055f) / 1.055f).pow(2.4f)
        }
    return 0.2126f * linear(red) +
        0.7152f * linear(green) +
        0.0722f * linear(blue)
}

private fun Color.darkenToMaxLightness(maxLightness: Float): Color {
    val hsl = toHsl()
    val lightness = hsl[2]
    if (lightness <= maxLightness) {
        return this
    }
    return hslToColor(
        hue = hsl[0],
        saturation = hsl[1],
        lightness = maxLightness,
        alpha = alpha,
    )
}

/** Returns [hue 0..360, saturation 0..1, lightness 0..1] for this color. */
private fun Color.toHsl(): FloatArray {
    val maxChannel = max(red, max(green, blue))
    val minChannel = min(red, min(green, blue))
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
            red -> ((green - blue) / delta + if (green < blue) 6f else 0f) * 60f
            green -> ((blue - red) / delta + 2f) * 60f
            else -> ((red - green) / delta + 4f) * 60f
        }
    return floatArrayOf(hue, saturation, lightness)
}

private fun hslToColor(
    hue: Float,
    saturation: Float,
    lightness: Float,
    alpha: Float,
): Color {
    val chroma = (1f - abs(2f * lightness - 1f)) * saturation
    val secondary = chroma * (1f - abs((hue / 60f) % 2f - 1f))
    val match = lightness - chroma / 2f
    val channels =
        when {
            hue < 60f -> Triple(chroma, secondary, 0f)
            hue < 120f -> Triple(secondary, chroma, 0f)
            hue < 180f -> Triple(0f, chroma, secondary)
            hue < 240f -> Triple(0f, secondary, chroma)
            hue < 300f -> Triple(secondary, 0f, chroma)
            else -> Triple(chroma, 0f, secondary)
        }
    return Color(
        red = channels.first + match,
        green = channels.second + match,
        blue = channels.third + match,
        alpha = alpha,
    )
}
