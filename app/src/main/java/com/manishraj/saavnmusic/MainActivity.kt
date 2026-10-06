package com.manishraj.saavnmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.manishraj.saavnmusic.ui.theme.SaavnTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
    Scaffold(
        bottomBar = {
            Column {
                if (!showPlayer) MiniPlayer(onOpen = { showPlayer = true })
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
            if (showPlayer) {
                FullPlayer(onBack = { showPlayer = false })
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
                        )
                    }
                    composable(
                        "playlist/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    ) {
                        PlaylistScreen(
                            it.arguments?.getString("id") ?: "",
                            onPlayQueue = playQueue,
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
                        )
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
