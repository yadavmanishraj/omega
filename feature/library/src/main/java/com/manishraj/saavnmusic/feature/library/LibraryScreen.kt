package com.manishraj.saavnmusic.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.DownloadInfo
import com.manishraj.saavnmusic.domain.LocalPlaylist
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.ui.components.Artwork
import com.manishraj.saavnmusic.ui.components.EmptyState
import com.manishraj.saavnmusic.ui.components.SongRow
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing
import kotlinx.coroutines.launch

private fun formatBytes(bytes: Long): String =
    when {
        bytes <= 0 -> "0 MB"
        bytes >= 1024L * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        else -> String.format("%.0f MB", bytes / (1024.0 * 1024))
    }

private fun <T> sorted(
    list: List<T>,
    mode: SortMode,
    name: (T) -> String,
): List<T> =
    when (mode) {
        SortMode.NEWEST -> list
        SortMode.OLDEST -> list.reversed()
        SortMode.A_Z -> list.sortedBy { name(it).lowercase() }
    }

/**
 * Library (REDESIGN_SPEC §6): all-local content. Single-line tab labels
 * with counts, a labeled sort chip + menu (no "Toggle"), downloads with
 * progress/retry/summary and confirm-then-delete with Undo, favorites
 * with Undo, history cleared from the title-row overflow with
 * confirmation, and playlists behind a FAB + validated create dialog.
 */
@Composable
fun LibraryScreen(
    vm: LibraryViewModel = hiltViewModel(),
    onPlayQueue: (List<Song>, Int) -> Unit,
    onOpenSearch: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    val favs by vm.favorites.collectAsState()
    val dls by vm.downloads.collectAsState()
    val hist by vm.history.collectAsState()
    val pls by vm.playlists.collectAsState()
    val sortMode by vm.sortMode.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DownloadInfo?>(null) }
    var openPlaylist by remember { mutableStateOf<LocalPlaylist?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val playlist = openPlaylist
    if (playlist != null) {
        LocalPlaylistDetail(
            vm = vm,
            playlist = playlist,
            onBack = { openPlaylist = null },
            onPlayQueue = onPlayQueue,
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (tab == 3) {
                FloatingActionButton(onClick = { showCreate = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "New playlist")
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = OmegaSpacing.lg, end = OmegaSpacing.sm, top = OmegaSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Library", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "On this device — no account, nothing uploaded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (tab == 2 && hist.isNotEmpty()) {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Clear history",
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                confirmClearHistory = true
                            },
                        )
                    }
                }
            }

            LibraryTabs(
                tab = tab,
                counts = listOf(favs.size, dls.size, hist.size, pls.size),
                onSelect = { tab = it },
            )

            if (tab != 3) {
                Row(
                    Modifier.padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = { showSortMenu = true },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null) },
                        label = {
                            Text(
                                when (sortMode) {
                                    SortMode.NEWEST -> "Newest first"
                                    SortMode.OLDEST -> "Oldest first"
                                    SortMode.A_Z -> "A–Z"
                                },
                                maxLines = 1,
                                softWrap = false,
                            )
                        },
                    )
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Newest first", maxLines = 1, softWrap = false) },
                            onClick = {
                                vm.sortMode.value = SortMode.NEWEST
                                showSortMenu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest first", maxLines = 1, softWrap = false) },
                            onClick = {
                                vm.sortMode.value = SortMode.OLDEST
                                showSortMenu = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("A–Z", maxLines = 1, softWrap = false) },
                            onClick = {
                                vm.sortMode.value = SortMode.A_Z
                                showSortMenu = false
                            },
                        )
                    }
                }
            }

            when (tab) {
                0 ->
                    if (favs.isEmpty()) {
                        EmptyState(
                            "No favorites yet",
                            "Tap the heart on any song to keep it here.",
                        )
                    } else {
                        val ordered = sorted(favs, sortMode) { it.name }
                        LazyColumn {
                            items(ordered) { song ->
                                SongRow(
                                    song,
                                    { onPlayQueue(ordered, ordered.indexOf(song)) },
                                    trailing = {
                                        IconButton(onClick = {
                                            vm.unfavorite(song)
                                            scope.launch {
                                                val result =
                                                    snackbar.showSnackbar(
                                                        "Removed from favorites",
                                                        actionLabel = "Undo",
                                                        duration = SnackbarDuration.Short,
                                                    )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    vm.restoreFavorite(song)
                                                }
                                            }
                                        }) {
                                            Icon(
                                                Icons.Filled.Favorite,
                                                contentDescription = "Remove ${song.name} from favorites",
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }

                1 ->
                    if (dls.isEmpty()) {
                        EmptyState(
                            "No downloads yet",
                            "Downloaded songs play offline, no account needed.",
                            actionLabel = "Find music",
                            onAction = onOpenSearch,
                        )
                    } else {
                        val ordered = sorted(dls, sortMode) { it.name }
                        Column {
                            Text(
                                "${ordered.size} songs · ${formatBytes(ordered.sumOf { it.sizeBytes })} on this device",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.xs),
                            )
                            LazyColumn {
                                items(ordered) { d ->
                                    ListItem(
                                        headlineContent = {
                                            Text(d.name, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                                        },
                                        supportingContent = {
                                            Column {
                                                Text(
                                                    listOf(d.artist, d.quality, formatBytes(d.sizeBytes), d.status)
                                                        .filter { it.isNotBlank() }
                                                        .joinToString(" • "),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                                if (d.status == "FAILED") {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Outlined.ErrorOutline,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.error,
                                                        )
                                                        TextButton(onClick = { vm.retryDownload(d) }) {
                                                            Text("Retry")
                                                        }
                                                    }
                                                } else if (d.status != "COMPLETED") {
                                                    Spacer(Modifier.height(OmegaSpacing.xs))
                                                    LinearProgressIndicator(
                                                        progress = { d.progress / 100f },
                                                        modifier = Modifier.fillMaxWidth(),
                                                    )
                                                    Text(
                                                        "${d.progress}%",
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                }
                                            }
                                        },
                                        leadingContent = { Artwork(d.imageUrl, contentDescription = d.name) },
                                        trailingContent = {
                                            IconButton(onClick = { pendingDelete = d }) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = "Delete download ${d.name}",
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                2 ->
                    if (hist.isEmpty()) {
                        EmptyState(
                            "Nothing played yet",
                            "What you play shows up here — only on this device.",
                        )
                    } else {
                        val ordered = sorted(hist, sortMode) { it.name }
                        LazyColumn {
                            items(ordered) { song ->
                                SongRow(song, { onPlayQueue(ordered, ordered.indexOf(song)) })
                            }
                        }
                    }

                else ->
                    if (pls.isEmpty()) {
                        EmptyState(
                            "No playlists yet",
                            "Make one for a mood, a trip, anything.",
                            actionLabel = "New playlist",
                            onAction = { showCreate = true },
                        )
                    } else {
                        LazyColumn {
                            items(pls) { p ->
                                ListItem(
                                    headlineContent = {
                                        Text(p.name, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                                    },
                                    supportingContent = {
                                        Text("${p.songCount} songs", style = MaterialTheme.typography.bodyMedium)
                                    },
                                    trailingContent = {
                                        IconButton(onClick = { vm.deletePlaylist(p.id) }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete playlist ${p.name}",
                                            )
                                        }
                                    },
                                    modifier = Modifier.clickable { openPlaylist = p },
                                )
                            }
                        }
                    }
            }
        }
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("Clear history?") },
            text = { Text("Your listening history on this device will be removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearHistory()
                    confirmClearHistory = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearHistory = false }) { Text("Cancel") }
            },
        )
    }

    pendingDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete download?") },
            text = { Text("“${d.name}” and its file will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    vm.deleteDownload(d)
                    scope.launch {
                        val result =
                            snackbar.showSnackbar(
                                "Download deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short,
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            vm.retryDownload(d)
                        }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var interacted by remember { mutableStateOf(false) }
        val blank = name.isBlank()
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New playlist") },
            text = {
                Column {
                    Text("Playlist name", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(OmegaSpacing.xs))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            interacted = true
                        },
                        label = { Text("Playlist name") },
                        singleLine = true,
                        isError = interacted && blank,
                        supportingText = {
                            if (interacted && blank) {
                                Text("Give your playlist a name first.", color = MaterialTheme.colorScheme.error)
                            }
                        },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.createPlaylist(name.trim())
                        showCreate = false
                    },
                    enabled = !blank,
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Cancel") }
            },
        )
    }
}

/** Library tabs with count badges; scrollable before labels can compress. */
@Composable
private fun LibraryTabs(
    tab: Int,
    counts: List<Int>,
    onSelect: (Int) -> Unit,
) {
    val labels = listOf("Favorites", "Downloads", "History", "Playlists")
    val fontScale = LocalDensity.current.fontScale
    if (fontScale >= 1.6f) {
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = OmegaSpacing.lg) {
            labels.forEachIndexed { i, label ->
                Tab(
                    selected = tab == i,
                    onClick = { onSelect(i) },
                    text = { Text("$label · ${counts[i]}", maxLines = 1, softWrap = false) },
                )
            }
        }
    } else {
        TabRow(selectedTabIndex = tab) {
            labels.forEachIndexed { i, label ->
                Tab(
                    selected = tab == i,
                    onClick = { onSelect(i) },
                    text = { Text("$label · ${counts[i]}", maxLines = 1, softWrap = false) },
                )
            }
        }
    }
}

/** A local playlist's songs, with playback. */
@Composable
private fun LocalPlaylistDetail(
    vm: LibraryViewModel,
    playlist: LocalPlaylist,
    onBack: () -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
) {
    val songs by produceState<List<Song>>(emptyList(), playlist.id) {
        vm.playlistSongs(playlist.id).collect { value = it }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(OmegaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Library")
            }
            Text(playlist.name, style = MaterialTheme.typography.headlineSmall)
        }
        if (songs.isEmpty()) {
            EmptyState(
                "No songs yet",
                "Open a song's menu in the player to add it to this playlist.",
            )
        } else {
            LazyColumn {
                items(songs) { song ->
                    SongRow(song, { onPlayQueue(songs, songs.indexOf(song)) })
                }
            }
        }
    }
}
