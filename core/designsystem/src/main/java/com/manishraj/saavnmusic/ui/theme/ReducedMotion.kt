package com.manishraj.saavnmusic.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Reduced motion (M3 Expressive spec §2.5): a BINARY model driven by the
 * system animator duration scale. Scale 0 means the user has turned
 * animations off: spatial/spring motion collapses to snaps and
 * crossfades. Any other scale leaves the theme's MotionScheme specs
 * untouched (no duration scaling — the springs are the system now).
 *
 * The scale is RE-READ on every ON_RESUME, so toggling the system
 * animation setting mid-session takes effect when the user returns to
 * the app. (The legacy read-once in the app root missed mid-session
 * changes; it is swapped to consume this provider in Wave 3.) Provided
 * by [SaavnTheme]; read anywhere via [LocalReducedMotion].
 *
 * Outside a theme (unit tests, stray previews) the local defaults to
 * `false` — motion on — matching the platform default.
 */
val LocalReducedMotion = compositionLocalOf { false }

private fun readAnimatorDurationScale(context: Context): Float =
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    )

/** The hosting activity's lifecycle, unwrapped from the context
 * chain; null in previews / headless compositions (the provider then
 * keeps its first-composition read). Reading a CompositionLocal
 * lifecycle owner defensively isn't an option — the Compose compiler
 * forbids try/catch around composable invocations. */
private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? =
    when {
        this is LifecycleOwner -> this
        this is ContextWrapper -> baseContext.findLifecycleOwner()
        else -> null
    }

@Composable
fun ProvideReducedMotion(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var animatorScale by remember { mutableStateOf(readAnimatorDurationScale(context)) }
    val lifecycleOwner = remember(context) { context.findLifecycleOwner() }
    DisposableEffect(lifecycleOwner, context) {
        if (lifecycleOwner == null) {
            onDispose { }
        } else {
            val observer =
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        animatorScale = readAnimatorDurationScale(context)
                    }
                }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }
    CompositionLocalProvider(
        LocalReducedMotion provides (animatorScale == 0f),
        content = content,
    )
}
