package com.manishraj.saavnmusic

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.feature.detail.AlbumScreen
import com.manishraj.saavnmusic.feature.detail.ArtistScreen
import com.manishraj.saavnmusic.feature.detail.PlaylistScreen
import com.manishraj.saavnmusic.feature.home.HomeScreen
import com.manishraj.saavnmusic.feature.library.LIBRARY_TAB_DOWNLOADS
import com.manishraj.saavnmusic.feature.library.LibraryScreen
import com.manishraj.saavnmusic.feature.player.FullPlayer
import com.manishraj.saavnmusic.feature.player.MiniPlayer
import com.manishraj.saavnmusic.feature.player.PlayerViewModel
import com.manishraj.saavnmusic.feature.search.SearchScreen
import com.manishraj.saavnmusic.feature.settings.SettingsScreen
import com.manishraj.saavnmusic.feature.settings.SettingsViewModel
import com.manishraj.saavnmusic.playback.PlayerController
import com.manishraj.saavnmusic.ui.components.LocalOmegaSnackbar
import com.manishraj.saavnmusic.ui.components.OmegaSnackbarHost
import com.manishraj.saavnmusic.ui.components.rememberOmegaSnackbarController
import com.manishraj.saavnmusic.ui.theme.OmegaMotion
import com.manishraj.saavnmusic.ui.theme.SaavnTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/** Single-activity, NO login / NO onboarding account flow: app opens straight into Home (music). Edge-to-edge per system/edge-to-edge skill. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var player: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        player.connect()
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val s by settingsVm.state.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val dark =
                when (s.themeMode) {
                    "LIGHT" -> false
                    "DARK" -> true
                    else -> systemDark
                }
            SaavnTheme(dark = dark, dynamic = s.dynamicColor) { AppRoot() }
        }
    }
}

/**
 * Root shell. The whole Scaffold lives inside a [SharedTransitionLayout]
 * so the mini-player's artwork and the full player's artwork form one
 * shared element — the Material 3 container transform for
 * mini-player → player: opening the player morphs the 48dp thumbnail
 * into the 300dp artwork (and back, on collapse) while the surrounding
 * content cross-fades. Motion: the shell's own chrome still follows
 * REDESIGN_SPEC §2.7 (emphasized tweens, system animator duration
 * scale honored — at scale 0 / reduced motion only crossfades remain),
 * while the PLAYER surfaces follow the M3 Expressive spec §4: the
 * artwork flight is the theme's slowSpatial spring (interruptible,
 * velocity-preserving), the open/close fades run on defaultEffects,
 * and a predictive-back gesture scrubs the collapse before it commits.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val playerVm: PlayerViewModel = hiltViewModel()
    var showPlayer by remember { mutableStateOf(false) }
    val back by nav.currentBackStackEntryAsState()
    // Destination routes are patterns (e.g. "library?tab={tab}"); the
    // bottom bar compares against the plain destination name.
    val route = back?.destination?.route?.substringBefore('?')
    // Features never depend on :feature:player; :app injects playback as a
    // lambda, the same pattern used for cross-feature navigation.
    val playQueue: (List<Song>, Int) -> Unit = { songs, index -> playerVm.play(songs, index) }
    // Motion setup (REDESIGN_SPEC §2.7): read the system animator
    // duration scale once and scale every duration by it instead of
    // fighting it. Scale 0 = reduced motion: crossfades only.
    val context = LocalContext.current
    val animatorScale =
        remember {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }
    val reducedMotion = animatorScale == 0f
    val enterMs = (OmegaMotion.SLOW_MS * animatorScale).roundToInt()
    val exitMs = (OmegaMotion.EXIT_MS * animatorScale).roundToInt()
    // Artwork flight for the container transform (M3 Expressive spec
    // §4.2): the theme's slowSpatial spring — interruptible and
    // velocity-preserving, so a collapse mid-flight (or a predictive
    // back that cancels) retargets smoothly instead of restarting a
    // fixed tween.
    val motionScheme = MaterialTheme.motionScheme
    val artworkBoundsTransform =
        BoundsTransform { _, _ ->
            motionScheme.slowSpatialSpec()
        }
    val miniEnter =
        if (reducedMotion) {
            fadeIn(tween(OmegaMotion.FAST_MS))
        } else {
            fadeIn(tween(durationMillis = enterMs, easing = OmegaMotion.emphasizedDecelerate)) +
                expandVertically(tween(durationMillis = enterMs, easing = OmegaMotion.emphasizedDecelerate))
        }
    val miniExit =
        if (reducedMotion) {
            fadeOut(tween(OmegaMotion.FAST_MS))
        } else {
            fadeOut(tween(durationMillis = exitMs, easing = OmegaMotion.emphasizedAccelerate)) +
                shrinkVertically(tween(durationMillis = exitMs, easing = OmegaMotion.emphasizedAccelerate))
        }
    // While the player is open the NavHost is out of composition, so the
    // nav controller's own back handling is gone: system back collapses
    // the player here (running the reverse transition) instead of
    // leaving the app while playback keeps going.
    //
    // Predictive back (M3 Expressive spec §4.6): the gesture scrubs the
    // collapse — the player fades/shrinks with the gesture progress —
    // and only commits (showPlayer = false, the standard reverse
    // transition) when the gesture completes. A cancelled gesture
    // springs the progress back to 0 on slowSpatial. Reduced motion
    // keeps the plain instant BackHandler: no scrub, no spring.
    val backProgress = remember { Animatable(0f) }
    val backSettleSpec = remember(motionScheme) { motionScheme.slowSpatialSpec<Float>() }
    val compositionScope = rememberCoroutineScope()
    if (reducedMotion) {
        BackHandler(enabled = showPlayer) { showPlayer = false }
    } else {
        PredictiveBackHandler(enabled = showPlayer) { progressFlow ->
            try {
                progressFlow.collect { event -> backProgress.snapTo(event.progress) }
                backProgress.snapTo(0f)
                showPlayer = false
            } catch (e: CancellationException) {
                // The handler's own coroutine is cancelled here, so
                // the settle animation runs on the composition scope.
                compositionScope.launch {
                    backProgress.animateTo(0f, backSettleSpec)
                }
                throw e
            }
        }
    }
    // App-wide snackbar feedback (spec §3/§7): one host for the whole
    // shell. The Scaffold's snackbar slot renders ABOVE the bottom
    // bar — i.e. above the mini-player — and screens fire messages
    // through LocalOmegaSnackbar instead of growing private hosts.
    val snackbarController = rememberOmegaSnackbarController()
    CompositionLocalProvider(LocalOmegaSnackbar provides snackbarController) {
        SharedTransitionLayout {
            val sharedScope = this
            Scaffold(
                snackbarHost = { OmegaSnackbarHost(snackbarController) },
                bottomBar = {
                    Column {
                        AnimatedVisibility(
                            visible = !showPlayer,
                            enter = miniEnter,
                            exit = miniExit,
                        ) {
                            MiniPlayer(
                                onOpen = { showPlayer = true },
                                sharedTransitionScope = if (reducedMotion) null else sharedScope,
                                animatedVisibilityScope = this,
                                artworkBoundsTransform = artworkBoundsTransform,
                            )
                        }
                        NavigationBar {
                            listOf(
                                TopLevelDestination("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
                                TopLevelDestination("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
                                TopLevelDestination("library", "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
                                TopLevelDestination("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
                            ).forEach { dest ->
                                NavigationBarItem(
                                    selected = route == dest.route,
                                    onClick = {
                                        nav.navigate(dest.route) {
                                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            if (route == dest.route) dest.selectedIcon else dest.icon,
                                            contentDescription = dest.label,
                                        )
                                    },
                                    label = { Text(dest.label, maxLines = 1, softWrap = false) },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    AnimatedContent(
                        targetState = showPlayer,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            // Material 3 container transform: the shared
                            // artwork morphs; everything else cross-fades.
                            // Fades are EFFECTS — the theme's
                            // defaultEffectsSpec (critically damped,
                            // never overshoots; M3 Expressive spec
                            // §4.3). Reduced motion: crossfade only, at
                            // the FAST token.
                            if (reducedMotion) {
                                fadeIn(tween(OmegaMotion.FAST_MS)) togetherWith
                                    fadeOut(tween(OmegaMotion.FAST_MS))
                            } else {
                                fadeIn(motionScheme.defaultEffectsSpec()) togetherWith
                                    fadeOut(motionScheme.defaultEffectsSpec())
                            }
                        },
                        label = "playerExpand",
                    ) { playerOpen ->
                        val contentScope = this
                        if (playerOpen) {
                            // Predictive-back scrub layer: while a back
                            // gesture is in progress the player recedes
                            // (fade + slight shrink) with the gesture;
                            // at rest the progress is 0 and this layer
                            // is identity.
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        val p = backProgress.value
                                        alpha = 1f - 0.3f * p
                                        scaleX = 1f - 0.08f * p
                                        scaleY = 1f - 0.08f * p
                                    },
                            ) {
                                FullPlayer(
                                    onBack = { showPlayer = false },
                                    sharedTransitionScope = if (reducedMotion) null else sharedScope,
                                    animatedVisibilityScope = contentScope,
                                    artworkBoundsTransform = artworkBoundsTransform,
                                )
                            }
                        } else {
                            NavHost(nav, startDestination = "home") {
                                composable("home") {
                                    HomeScreen(
                                        onAlbum = { nav.navigate("album/$it") },
                                        onPlaylist = { nav.navigate("playlist/$it") },
                                        onArtist = { nav.navigate("artist/$it") },
                                        onPlayQueue = playQueue,
                                        onOpenDownloads = {
                                            nav.navigate("library?tab=$LIBRARY_TAB_DOWNLOADS") {
                                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    )
                                }
                                composable("search") {
                                    SearchScreen(
                                        onAlbum = { nav.navigate("album/$it") },
                                        onPlaylist = { nav.navigate("playlist/$it") },
                                        onArtist = { nav.navigate("artist/$it") },
                                        onPlayQueue = playQueue,
                                        onOpenLibrary = {
                                            nav.navigate("library") {
                                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    )
                                }
                                composable(
                                    "library?tab={tab}",
                                    arguments =
                                        listOf(
                                            navArgument("tab") {
                                                type = NavType.IntType
                                                defaultValue = -1
                                            },
                                        ),
                                ) { entry ->
                                    LibraryScreen(
                                        onPlayQueue = playQueue,
                                        onOpenSearch = {
                                            nav.navigate("search") {
                                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        initialTab = entry.arguments?.getInt("tab")?.takeIf { it >= 0 } ?: 0,
                                    )
                                }
                                composable("settings") { SettingsScreen() }
                                composable(
                                    "album/{id}",
                                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                                ) {
                                    AlbumScreen(
                                        it.arguments?.getString("id") ?: "",
                                        onPlayQueue = playQueue,
                                        onBack = { nav.popBackStack() },
                                    )
                                }
                                composable(
                                    "playlist/{id}",
                                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                                ) {
                                    PlaylistScreen(
                                        it.arguments?.getString("id") ?: "",
                                        onPlayQueue = playQueue,
                                        onBack = { nav.popBackStack() },
                                    )
                                }
                                composable(
                                    "artist/{id}",
                                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                                ) {
                                    ArtistScreen(
                                        it.arguments?.getString("id") ?: "",
                                        onAlbum = { a -> nav.navigate("album/$a") },
                                        onPlayQueue = playQueue,
                                        onBack = { nav.popBackStack() },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
