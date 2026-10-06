package com.manishraj.saavnmusic.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme; import androidx.compose.material3.*; import androidx.compose.runtime.Composable; import androidx.compose.ui.graphics.Color
// Dark-first music aesthetic; dynamic color opt-in from Settings (jetpack-compose/theming skill patterns).
private val DarkColors = darkColorScheme(primary=Color(0xFFA8E10C), onPrimary=Color(0xFF1A2600), secondary=Color(0xFF8FE3CF), background=Color(0xFF0B0F0C), surface=Color(0xFF151A16), surfaceVariant=Color(0xFF1E2620))
private val LightColors = lightColorScheme(primary=Color(0xFF4C7A00), secondary=Color(0xFF006B5D))
@Composable fun SaavnTheme(dark:Boolean=isSystemInDarkTheme(), dynamic:Boolean=false, content:@Composable ()->Unit){
    val ctx=androidx.compose.ui.platform.LocalContext.current
    val scheme = when { dynamic && android.os.Build.VERSION.SDK_INT>=31 -> if(dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx); dark -> DarkColors; else -> LightColors }
    MaterialTheme(colorScheme=scheme, typography=Typography(), content=content)
}
