package com.manishraj.saavnmusic.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark-first music aesthetic; dynamic color opt-in from Settings (jetpack-compose/theming skill patterns).
// Core roles aligned to docs/sdlc/UIUX_DESIGN.md §3.1 tokens (2026-10-07): dark background #0B0F0E,
// surface #121715, primary #3BE477; light background #F7FAF8, primary #006B32. Roles not listed
// there (onPrimary, secondary, surfaceVariant) keep their v1 values, which remain contrast-compatible.
private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF3BE477),
        onPrimary = Color(0xFF1A2600),
        secondary = Color(0xFF8FE3CF),
        background = Color(0xFF0B0F0E),
        surface = Color(0xFF121715),
        surfaceVariant = Color(0xFF1E2620),
    )
private val LightColors = lightColorScheme(primary = Color(0xFF006B32), secondary = Color(0xFF006B5D), background = Color(0xFFF7FAF8))

@Composable fun SaavnTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = false,
    content: @Composable () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scheme =
        when {
            dynamic && android.os.Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            dark -> DarkColors
            else -> LightColors
        }
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
