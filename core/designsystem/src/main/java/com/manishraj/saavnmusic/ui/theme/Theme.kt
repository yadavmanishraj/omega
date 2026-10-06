package com.manishraj.saavnmusic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Omega design tokens (REDESIGN_SPEC §2): Dark Mode (OLED) + Vibrant &
 * Block-based. Semantic Material 3 roles only in screens; raw hex lives
 * here. Play-green is the constant brand accent; the midnight/indigo
 * base lets artwork carry the color.
 *
 * Font families per spec §2.2: Righteous (display) + Poppins (body) via
 * the Google Fonts downloadable provider.
 *
 * DEVIATION (documented in GAP_ANALYSIS): the downloadable provider
 * needs the GMS fonts certs resource array, which cannot be validated
 * without a device build, so the families currently resolve to the
 * platform default. The full type SCALE (sizes/weights/leading) below
 * is implemented exactly as specified, and wiring the provider later is
 * a two-line change confined to this file.
 */
val DisplayFontFamily: FontFamily = FontFamily.Default
val BodyFontFamily: FontFamily = FontFamily.Default

/** 4dp-base spacing scale (spec §2.3). */
object OmegaSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** Corner radii (spec §2.4): row artwork 8, cards 12, hero 16, sheets/dialogs 24. */
object OmegaRadius {
    val sm = 4.dp
    val md = 8.dp
    val lg = 12.dp
    val xl = 16.dp
    val xxl = 24.dp
}

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF3BE477),
        onPrimary = Color(0xFF0F172A),
        primaryContainer = Color(0xFF14532D),
        onPrimaryContainer = Color(0xFFBBF7D0),
        secondary = Color(0xFFA5B4FC),
        onSecondary = Color(0xFF1E1B4B),
        secondaryContainer = Color(0xFF1E1B4B),
        onSecondaryContainer = Color(0xFFE0E7FF),
        background = Color(0xFF0F0F23),
        onBackground = Color(0xFFF8FAFC),
        surface = Color(0xFF12122B),
        onSurface = Color(0xFFF8FAFC),
        surfaceVariant = Color(0xFF1B1B30),
        onSurfaceVariant = Color(0xFFC3CAD9),
        surfaceContainerHighest = Color(0xFF27273B),
        outline = Color(0xFF312E81),
        outlineVariant = Color(0xFF27273B),
        error = Color(0xFFEF4444),
        onError = Color(0xFF000000),
        scrim = Color(0x99000000),
    )

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF15803D),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDCFCE7),
        onPrimaryContainer = Color(0xFF14532D),
        secondary = Color(0xFF4338CA),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE0E7FF),
        onSecondaryContainer = Color(0xFF1E1B4B),
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF0F172A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFE8EAF3),
        onSurfaceVariant = Color(0xFF475569),
        surfaceContainerHighest = Color(0xFFE8EAF3),
        outline = Color(0xFF94A3B8),
        outlineVariant = Color(0xFFCBD5E1),
        error = Color(0xFFDC2626),
        onError = Color(0xFFFFFFFF),
        scrim = Color(0x99000000),
    )

/** Type scale per spec §2.2 (Righteous display / Poppins body roles). */
private val OmegaTypography =
    Typography(
        displaySmall =
            TextStyle(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 36.sp,
                lineHeight = 44.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 36.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
    )

/** Player time labels: tabular figures so timers never jitter (spec §2.2). */
val TabularTimeStyle: TextStyle
    @Composable
    get() = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum")

@Composable
fun SaavnTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Dynamic/artwork color is opt-in (Settings) and off by default; it may
    // only re-tint surfaces - play-green primary stays constant (spec §2.1).
    val colors =
        when {
            dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val dynamicScheme =
                    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                val base = if (dark) DarkColors else LightColors
                // Dynamic color may re-tint surfaces only: the play-green
                // primary family stays constant (spec §2.1).
                dynamicScheme.copy(
                    primary = base.primary,
                    onPrimary = base.onPrimary,
                    primaryContainer = base.primaryContainer,
                    onPrimaryContainer = base.onPrimaryContainer,
                )
            }
            dark -> DarkColors
            else -> LightColors
        }
    MaterialTheme(colorScheme = colors, typography = OmegaTypography, content = content)
}
