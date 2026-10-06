package com.manishraj.saavnmusic
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.manishraj.saavnmusic.playback.PlayerController
import com.manishraj.saavnmusic.ui.player.*
import com.manishraj.saavnmusic.ui.screens.*
import com.manishraj.saavnmusic.ui.theme.SaavnTheme
import com.manishraj.saavnmusic.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Single-activity, NO login / NO onboarding account flow: app opens straight into Home (music). Edge-to-edge per system/edge-to-edge skill. */
@AndroidEntryPoint class MainActivity : ComponentActivity() {
    @Inject lateinit var player: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        player.connect()
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val s by settingsVm.state.collectAsState()
            SaavnTheme(dark = s.darkTheme, dynamic = s.dynamicColor) { AppRoot() }
        }
    }
}

@Composable fun AppRoot() {
    val nav = rememberNavController()
    var showPlayer by remember { mutableStateOf(false) }
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route
    Scaffold(bottomBar = {
        Column {
            if (!showPlayer) MiniPlayer(onOpen = { showPlayer = true })
            NavigationBar {
                listOf(
                    Triple("home", "Home", Icons.Default.Home),
                    Triple("search", "Search", Icons.Default.Search),
                    Triple("library", "Library", Icons.Default.LibraryMusic),
                ).forEach { (r, label, icon) ->
                    NavigationBarItem(selected = route == r, onClick = {
                        nav.navigate(r) {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop =
                                true
                            restoreState = true
                        }
                    }, icon = { Icon(icon, null) }, label = { Text(label) })
                }
                NavigationBarItem(selected = route == "settings", onClick = {
                    nav.navigate("settings") { launchSingleTop = true }
                }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        }
    }) { padding ->
        Box(Modifier.padding(padding)) {
            if (showPlayer) {
                FullPlayer(onBack = { showPlayer = false })
            } else {
                NavHost(nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(onAlbum = {
                            nav.navigate("album/$it")
                        }, onPlaylist = { nav.navigate("playlist/$it") }, onArtist = { nav.navigate("artist/$it") })
                    }
                    composable("search") {
                        SearchScreen(onAlbum = {
                            nav.navigate("album/$it")
                        }, onPlaylist = { nav.navigate("playlist/$it") }, onArtist = { nav.navigate("artist/$it") })
                    }
                    composable("library") { LibraryScreen() }
                    composable("settings") { SettingsScreen() }
                    composable("album/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                        AlbumScreen(
                            it.arguments?.getString("id") ?: "",
                        )
                    }
                    composable("playlist/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                        PlaylistScreen(
                            it.arguments?.getString("id") ?: "",
                        )
                    }
                    composable("artist/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                        ArtistScreen(
                            it.arguments?.getString("id") ?: "",
                            onAlbum = { a -> nav.navigate("album/$a") },
                        )
                    }
                }
            }
        }
    }
}
