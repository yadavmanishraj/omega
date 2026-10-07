package com.manishraj.saavnmusic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
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
 * Font families per spec §2.2: Righteous (display) + Poppins (body),
 * BUNDLED as static TTFs in this module's res/font - no downloadable
 * provider, no GMS cert arrays, no runtime font download, so text is
 * correct on first frame on every device. Both fonts are SIL Open
 * Font License 1.1; the license texts ship alongside the TTFs
 * (res/font/ofl_poppins.txt, res/font/ofl_righteous.txt). The type
 * SCALE (sizes/weights/leading) below is spec §2.2 exactly.
 */
val DisplayFontFamily: FontFamily =
    FontFamily(
        Font(R.font.righteous_regular, FontWeight.Normal),
    )

val BodyFontFamily: FontFamily =
    FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal),
        Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold),
        Font(R.font.poppins_bold, FontWeight.Bold),
    )

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

/**
 * Material shape scale for Omega (M3 Expressive spec §2.3). The classic five
 * slots continue the [OmegaRadius] scale (4/8/12/16, hero 28) so stock
 * components keep their current corners; the expressive `…Increased` slots
 * and `extraExtraLarge` extend the same scale for hero surfaces (player
 * artwork, sheets) in later waves. Artwork: medium in rows, large on cards,
 * extraLarge in the player hero.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val OmegaShapes =
    Shapes(
        extraSmall = RoundedCornerShape(OmegaRadius.sm),
        small = RoundedCornerShape(OmegaRadius.md),
        medium = RoundedCornerShape(OmegaRadius.lg),
        large = RoundedCornerShape(OmegaRadius.xl),
        largeIncreased = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp),
        extraLargeIncreased = RoundedCornerShape(32.dp),
        extraExtraLarge = RoundedCornerShape(48.dp),
    )

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

/**
 * Type scale per spec §2.2 (Righteous display / Poppins body roles), plus the
 * Material 3 Expressive `…Emphasized` twins for all 15 styles (M3 Expressive
 * spec §2.2): same family and size as the baseline style, one weight step up
 * (400→500, 500→700, 600→700). Defined for the redesign waves — screens do
 * not consume the emphasized styles yet. Righteous ships a single weight, so
 * its emphasized twins resolve to the same glyphs with the stepped weight
 * recorded in the style.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        displayLargeEmphasized =
            TextStyle(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 57.sp,
                lineHeight = 64.sp,
            ),
        displayMediumEmphasized =
            TextStyle(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 45.sp,
                lineHeight = 52.sp,
            ),
        displaySmallEmphasized =
            TextStyle(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 36.sp,
                lineHeight = 44.sp,
            ),
        headlineLargeEmphasized =
            TextStyle(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 32.sp,
                lineHeight = 40.sp,
            ),
        headlineMediumEmphasized =
            TextStyle(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 28.sp,
                lineHeight = 36.sp,
            ),
        headlineSmallEmphasized =
            TextStyle(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
            ),
        titleLargeEmphasized =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
        titleMediumEmphasized =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        titleSmallEmphasized =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodyLargeEmphasized =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMediumEmphasized =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        bodySmallEmphasized =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        labelLargeEmphasized =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelMediumEmphasized =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        labelSmallEmphasized =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            ),
    )

/** Player time labels: tabular figures so timers never jitter (spec §2.2). */
val TabularTimeStyle: TextStyle
    @Composable
    get() = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum")

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    // Material 3 Expressive foundation (M3_EXPRESSIVE_SPEC §0/§2.1): the
    // expressive theme with the expressive motion scheme; the color
    // resolution above is unchanged. Accessors below this theme stay
    // MaterialTheme.colorScheme / .typography / .shapes / .motionScheme.
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        shapes = OmegaShapes,
        typography = OmegaTypography,
        // Reduced-motion state (spec §2.5) rides the theme so every
        // screen and shared component sees the same live value.
        content = { ProvideReducedMotion(content) },
    )
}
