package com.manishraj.saavnmusic.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.data.repository.toSong
import com.manishraj.saavnmusic.domain.DownloadInfo
import com.manishraj.saavnmusic.domain.LocalPlaylist
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.playback.InsertNextResult
import com.manishraj.saavnmusic.ui.components.Artwork
import com.manishraj.saavnmusic.ui.components.EmptyState
import com.manishraj.saavnmusic.ui.components.LocalOmegaSnackbar
import com.manishraj.saavnmusic.ui.components.OmegaSegmentedListItem
import com.manishraj.saavnmusic.ui.components.PlaylistPickerDialog
import com.manishraj.saavnmusic.ui.components.SongOverflowMenuButton
import com.manishraj.saavnmusic.ui.components.SongRow
import com.manishraj.saavnmusic.ui.components.songCountLabel
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing

/** Library tab indices, in tab-bar order. Navigation deep-links use these. */
const val LIBRARY_TAB_FAVORITES = 0
const val LIBRARY_TAB_DOWNLOADS = 1
const val LIBRARY_TAB_HISTORY = 2
const val LIBRARY_TAB_PLAYLISTS = 3

private fun formatBytes(bytes: Long): String =
    when {
        bytes <= 0 -> "0 MB"
        bytes >= 1024L * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        else -> String.format("%.0f MB", bytes / (1024.0 * 1024))
    }

/**
 * Play-next snackbar copy (spec §3/§7): with a live queue the song
 * is inserted after the current track ("Will play next"), but with
 * nothing playing the engine APPENDS it without starting playback —
 * the copy must not promise "next" in that case.
 */
internal fun playNextMessage(
    result: InsertNextResult,
    title: String,
): String =
    when (result) {
        InsertNextResult.INSERTED_NEXT -> "Will play next: $title"
        InsertNextResult.APPENDED -> "Added to queue: $title"
    }

/** User-facing download status (spec §7: COMPLETED reads "Downloaded"; the stored enum is unchanged). */
private fun downloadStatusLabel(status: String): String =
    when (status) {
        "COMPLETED" -> "Downloaded"
        "DOWNLOADING" -> "Downloading"
        "FAILED" -> "Failed"
        else -> status
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
 * One filled segment of a Library list (M3 Expressive spec §3/§5):
 * the same surfaceContainerHigh + large-shape treatment as
 * [OmegaSegmentedListItem], wrapping rows whose content is richer
 * than the item's headline/supporting strings — [SongRow] (its
 * protected duration slot is a Phase B guarantee) and the Downloads
 * row (progress / error affordances). Lists separate segments by
 * the kit's 2dp gap; grouping is carried by containment, not
 * dividers.
 */
@Composable
internal fun SegmentedSegment(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
        content = content,
    )
}

/**
 * Library (REDESIGN_SPEC §6 + M3 Expressive spec §5): all-local
 * content. Single-line tab labels with counts, a labeled sort chip +
 * menu (no "Toggle"), Favorites/Downloads/History as segmented lists
 * with animateItem, downloads with wavy-then-determinate progress /
 * retry / one-line summary and confirm-then-delete with Undo,
 * favorites with Undo, history cleared from the title-row overflow
 * with confirmation, playlist delete behind a confirm dialog, and
 * playlists created from a single medium FAB. All snackbar feedback
 * goes through the shell's [LocalOmegaSnackbar] host (above the
 * mini-player).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    vm: LibraryViewModel = hiltViewModel(),
    onPlayQueue: (List<Song>, Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onDownloadEnqueued: () -> Unit = {},
    initialTab: Int = LIBRARY_TAB_FAVORITES,
) {
    // The user's tab survives leaving and returning; a navigation
    // request carrying a NEW tab argument (e.g. Home's offline
    // "Downloads" path) is applied once, when the argument changes.
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var appliedInitialTab by rememberSaveable { mutableIntStateOf(initialTab) }
    LaunchedEffect(initialTab) {
        if (initialTab != appliedInitialTab) {
            appliedInitialTab = initialTab
            tab = initialTab.coerceIn(0, 3)
        }
    }
    val favs by vm.favorites.collectAsState()
    val dls by vm.downloads.collectAsState()
    val hist by vm.history.collectAsState()
    val pls by vm.playlists.collectAsState()
    val sortMode by vm.sortMode.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DownloadInfo?>(null) }
    var pendingDeletePlaylist by remember { mutableStateOf<LocalPlaylist?>(null) }
    var playlistTarget by remember { mutableStateOf<Song?>(null) }
    val snackbar = LocalOmegaSnackbar.current
    val confirmAdded: (String) -> Unit = { playlistName ->
        snackbar?.showMessage("Added to $playlistName")
    }
    // One unfavorite path for the Favorites tab: the heart button
    // AND the row menu's "Remove from favorites" both go through
    // here, so both get the Undo snackbar.
    val unfavoriteWithUndo: (Song) -> Unit = { song ->
        vm.unfavorite(song)
        snackbar?.showMessage(
            "Removed from Favorites",
            actionLabel = "Undo",
            onAction = { vm.restoreFavorite(song) },
        )
    }

    // Local playlist detail is a NavHost destination now
    // (`library/playlist/{id}`, A17 F-01) — no conditional render
    // here, so system Back and the tab's saved state behave.
    Scaffold(
        floatingActionButton = {
            if (tab == LIBRARY_TAB_PLAYLISTS) {
                // The single playlist-creation affordance (spec §5):
                // a medium extended FAB, icon + text, docked by the
                // Scaffold above the shell's mini-player.
                ExtendedFloatingActionButton(
                    onClick = { showCreate = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New playlist") },
                )
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
                if (tab == LIBRARY_TAB_HISTORY && hist.isNotEmpty()) {
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

            // F-25: no sort chrome over an empty tab — sorting
            // nothing says nothing; the empty state owns the screen.
            val currentTabEmpty =
                when (tab) {
                    LIBRARY_TAB_FAVORITES -> favs.isEmpty()
                    LIBRARY_TAB_DOWNLOADS -> dls.isEmpty()
                    LIBRARY_TAB_HISTORY -> hist.isEmpty()
                    else -> false
                }
            if (tab != LIBRARY_TAB_PLAYLISTS && !currentTabEmpty) {
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
                    // Sort stays a menu, not a connected group (spec
                    // §5): at font 1.33 the tab header is crowded and
                    // sort is a tertiary action. The active order is
                    // check-marked so state reads beyond the chip.
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        SortMenuItem(
                            label = "Newest first",
                            selected = sortMode == SortMode.NEWEST,
                            onClick = {
                                vm.sortMode.value = SortMode.NEWEST
                                showSortMenu = false
                            },
                        )
                        SortMenuItem(
                            label = "Oldest first",
                            selected = sortMode == SortMode.OLDEST,
                            onClick = {
                                vm.sortMode.value = SortMode.OLDEST
                                showSortMenu = false
                            },
                        )
                        SortMenuItem(
                            label = "A–Z",
                            selected = sortMode == SortMode.A_Z,
                            onClick = {
                                vm.sortMode.value = SortMode.A_Z
                                showSortMenu = false
                            },
                        )
                    }
                }
            }

            when (tab) {
                LIBRARY_TAB_FAVORITES ->
                    if (favs.isEmpty()) {
                        EmptyState(
                            "No favorites yet",
                            "Tap the heart on any song to keep it here.",
                        )
                    } else {
                        val ordered = sorted(favs, sortMode) { it.name }
                        LazyColumn(
                            contentPadding =
                                PaddingValues(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(ordered, key = { it.id }) { song ->
                                SegmentedSegment(Modifier.animateItem()) {
                                    SongRow(
                                        song,
                                        { onPlayQueue(ordered, ordered.indexOf(song)) },
                                        trailing = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                SongOverflowMenuButton(
                                                    song = song,
                                                    isFavorite = true,
                                                    onPlayNext = {
                                                        snackbar?.showMessage(
                                                            playNextMessage(vm.playNext(song), song.name),
                                                        )
                                                    },
                                                    onDownload = {
                                                        vm.download(song)
                                                        // F-04: Library downloads announce
                                                        // exactly like Home's.
                                                        snackbar?.showMessage("Download queued")
                                                        onDownloadEnqueued()
                                                    },
                                                    onToggleFavorite = { unfavoriteWithUndo(song) },
                                                    onAddToPlaylist = { playlistTarget = song },
                                                )
                                                IconButton(onClick = { unfavoriteWithUndo(song) }) {
                                                    Icon(
                                                        Icons.Filled.Favorite,
                                                        contentDescription = "Remove ${song.name} from favorites",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                LIBRARY_TAB_DOWNLOADS ->
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
                                "${songCountLabel(ordered.size)} · ${formatBytes(ordered.sumOf { it.sizeBytes })} on this device",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.xs),
                            )
                            LazyColumn(
                                contentPadding =
                                    PaddingValues(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.sm),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                items(ordered, key = { it.songId }) { d ->
                                    SegmentedSegment(Modifier.animateItem()) {
                                        ListItem(
                                            modifier =
                                                if (d.status == "COMPLETED") {
                                                    Modifier.clickable {
                                                        // Play the downloaded files as a queue
                                                        // starting at the tapped track — the
                                                        // local files play fully offline.
                                                        val playable = ordered.filter { it.status == "COMPLETED" }
                                                        onPlayQueue(
                                                            playable.map { it.toSong() },
                                                            playable.indexOf(d),
                                                        )
                                                    }
                                                } else {
                                                    Modifier
                                                },
                                            supportingContent = {
                                                Column {
                                                    // Meta line with a PROTECTED status
                                                    // slot (F-18), mirroring SongRow's
                                                    // duration slot: artist • quality •
                                                    // size flexes and ellipsizes FIRST;
                                                    // the status — the row's primary
                                                    // signal — never truncates.
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            listOf(
                                                                d.artist,
                                                                d.quality,
                                                                formatBytes(d.sizeBytes),
                                                            ).filter { it.isNotBlank() }
                                                                .joinToString(" • "),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            modifier = Modifier.weight(1f, fill = false),
                                                        )
                                                        Text(
                                                            " • " +
                                                                if (d.status == "DOWNLOADING" && d.progress > 0) {
                                                                    "Downloading ${d.progress}%"
                                                                } else {
                                                                    downloadStatusLabel(d.status)
                                                                },
                                                            maxLines = 1,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color =
                                                                when (d.status) {
                                                                    "FAILED" -> MaterialTheme.colorScheme.error
                                                                    "DOWNLOADING" -> MaterialTheme.colorScheme.primary
                                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                                },
                                                        )
                                                    }
                                                    if (d.status == "FAILED") {
                                                        val errorMessage = d.errorMessage
                                                        if (!errorMessage.isNullOrBlank()) {
                                                            Text(
                                                                errorMessage,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.error,
                                                            )
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                Icons.Outlined.ErrorOutline,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.error,
                                                            )
                                                            TextButton(onClick = {
                                                                vm.retryDownload(d)
                                                                snackbar?.showMessage("Download queued")
                                                                onDownloadEnqueued()
                                                            }) {
                                                                Text("Retry")
                                                            }
                                                        }
                                                    } else if (d.status != "COMPLETED") {
                                                        Spacer(Modifier.height(OmegaSpacing.xs))
                                                        if (d.progress > 0) {
                                                            // Bytes are flowing: determinate
                                                            // (the % lives in the protected
                                                            // status slot above).
                                                            LinearProgressIndicator(
                                                                progress = { d.progress / 100f },
                                                                modifier = Modifier.fillMaxWidth(),
                                                            )
                                                        } else {
                                                            // Indeterminate phase (queued /
                                                            // connecting): the wavy indicator.
                                                            LinearWavyProgressIndicator(
                                                                modifier = Modifier.fillMaxWidth(),
                                                            )
                                                        }
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
                                            colors =
                                                ListItemDefaults.colors(containerColor = Color.Transparent),
                                        ) {
                                            Text(d.name, maxLines = 1, style = MaterialTheme.typography.titleMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                LIBRARY_TAB_HISTORY ->
                    if (hist.isEmpty()) {
                        EmptyState(
                            "Nothing played yet",
                            "What you play shows up here — only on this device.",
                        )
                    } else {
                        val ordered = sorted(hist, sortMode) { it.name }
                        LazyColumn(
                            contentPadding =
                                PaddingValues(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(ordered, key = { it.id }) { song ->
                                SegmentedSegment(Modifier.animateItem()) {
                                    SongRow(
                                        song,
                                        { onPlayQueue(ordered, ordered.indexOf(song)) },
                                        trailing = {
                                            SongOverflowMenuButton(
                                                song = song,
                                                isFavorite = favs.any { it.id == song.id },
                                                onPlayNext = {
                                                    snackbar?.showMessage(
                                                        playNextMessage(vm.playNext(song), song.name),
                                                    )
                                                },
                                                onDownload = {
                                                    vm.download(song)
                                                    // F-04: Library downloads announce
                                                    // exactly like Home's.
                                                    snackbar?.showMessage("Download queued")
                                                    onDownloadEnqueued()
                                                },
                                                onToggleFavorite = {
                                                    vm.toggleFavorite(
                                                        song,
                                                        favs.any { it.id == song.id },
                                                    )
                                                },
                                                onAddToPlaylist = { playlistTarget = song },
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }

                else ->
                    if (pls.isEmpty()) {
                        EmptyState(
                            "No playlists yet",
                            "Make one for a mood, a trip, anything.",
                        )
                    } else {
                        LazyColumn(
                            // Bottom padding clears the FAB so it never
                            // covers the last playlist row.
                            contentPadding =
                                PaddingValues(
                                    start = OmegaSpacing.lg,
                                    end = OmegaSpacing.lg,
                                    top = OmegaSpacing.sm,
                                    bottom = 96.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(pls, key = { it.id }) { p ->
                                OmegaSegmentedListItem(
                                    headline = p.name,
                                    supporting = songCountLabel(p.songCount),
                                    trailing = {
                                        IconButton(onClick = { pendingDeletePlaylist = p }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete playlist ${p.name}",
                                            )
                                        }
                                    },
                                    onClick = { onOpenPlaylist(p.id) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
            }
        }
    }

    playlistTarget?.let { song ->
        PlaylistPickerDialog(
            playlists = pls,
            onPick = { playlist ->
                vm.addToPlaylist(playlist.id, song)
                playlistTarget = null
                confirmAdded(playlist.name)
            },
            onCreatePlaylist = { name ->
                vm.createPlaylistAndAdd(name, song)
                playlistTarget = null
                confirmAdded(name)
            },
            onDismiss = { playlistTarget = null },
        )
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
                    snackbar?.showMessage(
                        "Download deleted",
                        actionLabel = "Undo",
                        onAction = {
                            vm.retryDownload(d)
                            onDownloadEnqueued()
                        },
                    )
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    // Playlist delete is destructive and NOT undoable (the VM has no
    // restore path for a playlist + its membership), so per spec §7 it
    // is confirm-first with an error-colored action, then a
    // message-only snackbar — no fake Undo.
    pendingDeletePlaylist?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDeletePlaylist = null },
            title = { Text("Delete playlist?") },
            text = {
                Text("“${p.name}” will be removed from this device. The songs stay in your library. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeletePlaylist = null
                    vm.deletePlaylist(p.id)
                    snackbar?.showMessage("Playlist deleted")
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePlaylist = null }) { Text("Cancel") }
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
                // Label names the field; the placeholder is an EXAMPLE
                // (spec §7) — the same copy the picker dialog uses.
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        interacted = true
                    },
                    label = { Text("Playlist name") },
                    placeholder = { Text("e.g. Monsoon drive") },
                    singleLine = true,
                    isError = interacted && blank,
                    supportingText = {
                        if (interacted && blank) {
                            Text("Give your playlist a name first.", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.createPlaylist(name.trim())
                        showCreate = false
                        // F-24: creation confirms in the same grammar
                        // as every other Library action.
                        snackbar?.showMessage("Playlist created")
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

/** One sort option; the active order carries a check so state reads beyond color. */
@Composable
private fun SortMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label, maxLines = 1, softWrap = false) },
        leadingIcon =
            if (selected) {
                { Icon(Icons.Filled.Check, contentDescription = null) }
            } else {
                null
            },
        onClick = onClick,
    )
}

/** Library tabs with count badges; scrollable before labels can compress. */
@Composable
private fun LibraryTabs(
    tab: Int,
    counts: List<Int>,
    onSelect: (Int) -> Unit,
) {
    val labels = listOf("Favorites", "Downloads", "History", "Playlists")
    // Always scrollable: the fixed TabRow squeezes the labels (with
    // counts) into truncation well below fontScale 1.6 — at the
    // common LARGE setting (~1.3) "Downloads" already became
    // "Downlo…". Scrollable tabs size to content and never compress.
    PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = OmegaSpacing.lg) {
        labels.forEachIndexed { i, label ->
            Tab(
                selected = tab == i,
                onClick = { onSelect(i) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = { Text("$label · ${counts[i]}", maxLines = 1, softWrap = false) },
            )
        }
    }
}
