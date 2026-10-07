package com.manishraj.saavnmusic.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.manishraj.saavnmusic.domain.LocalPlaylist
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing

/**
 * Overflow (⋮) button for a song row: THE single song-action menu for
 * every list in the app (M3 Expressive spec §3 — menu parity). One
 * definition lives here; the items a row shows are the callbacks its
 * screen provides:
 *
 * - Play next ([onPlayNext], only when the screen can queue) —
 *   wired since Wave 2.5: every feature row passes it through its
 *   ViewModel to PlayerController.insertNext.
 * - Add to playlist ([onAddToPlaylist], always).
 * - Download ([onDownload]).
 * - Favorite / Remove from favorites ([onToggleFavorite], label and
 *   icon driven by [isFavorite]).
 * - Remove from playlist ([onRemoveFromPlaylist]) — the ONE
 *   contextual item: only LocalPlaylistDetail rows pass it, since
 *   membership removal is meaningless anywhere else.
 *
 * Menu content parity is the D6 fix: Detail rows used to offer ONLY
 * "Add to playlist" while the player could download and favorite —
 * now every row everywhere offers the same actions. Screens must
 * pass every callback they can honor; a missing callback is a
 * deliberate product decision, never an accident of which screen the
 * row happens to render on.
 */
@Composable
fun SongOverflowMenuButton(
    song: Song,
    isFavorite: Boolean = false,
    onPlayNext: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onAddToPlaylist: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(
            Icons.Filled.MoreVert,
            contentDescription = "More options for ${song.name}",
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        if (onPlayNext != null) {
            DropdownMenuItem(
                text = { Text("Play next", maxLines = 1, softWrap = false) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                onClick = {
                    expanded = false
                    onPlayNext()
                },
            )
        }
        DropdownMenuItem(
            text = { Text("Add to playlist", maxLines = 1, softWrap = false) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
            onClick = {
                expanded = false
                onAddToPlaylist()
            },
        )
        if (onDownload != null) {
            DropdownMenuItem(
                text = { Text("Download", maxLines = 1, softWrap = false) },
                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDownload()
                },
            )
        }
        if (onToggleFavorite != null) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (isFavorite) "Remove from favorites" else "Add to favorites",
                        maxLines = 1,
                        softWrap = false,
                    )
                },
                leadingIcon = {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    onToggleFavorite()
                },
            )
        }
        if (onRemoveFromPlaylist != null) {
            DropdownMenuItem(
                text = { Text("Remove from playlist", maxLines = 1, softWrap = false) },
                leadingIcon = { Icon(Icons.Filled.PlaylistRemove, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRemoveFromPlaylist()
                },
            )
        }
    }
}

/**
 * Playlist picker: choose one of the user's local playlists, or create
 * a new one inline and use it immediately. The host owns the data —
 * [onPick] receives an existing playlist, [onCreatePlaylist] receives
 * the validated new name (the host creates the playlist and adds the
 * song to it). When the user has no playlists yet, the dialog opens
 * straight on the create form; when a list exists, create mode is a
 * reversible detour (title-row back arrow + system Back, F-11), not a
 * one-way door.
 *
 * [targetSong] is the song being added; its name rides under the
 * title in both modes (polish item 2), so the action's object is
 * visible before and at the moment of choice — a mis-tap from a long
 * list must not act on an invisible target.
 */
@Composable
fun PlaylistPickerDialog(
    playlists: List<LocalPlaylist>,
    targetSong: Song,
    onPick: (LocalPlaylist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by remember { mutableStateOf(playlists.isEmpty()) }
    var name by remember { mutableStateOf("") }
    var nameInteracted by remember { mutableStateOf(false) }
    // Create mode is NOT a one-way door (F-11): when a list exists
    // behind the form, system Back returns to it instead of throwing
    // the half-typed name away with the whole dialog (the title-row
    // back arrow below is the same path, visible).
    BackHandler(enabled = creating && playlists.isNotEmpty()) { creating = false }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            if (creating && playlists.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { creating = false }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to playlists",
                        )
                    }
                    Column {
                        Text("New playlist")
                        TargetSongLine(targetSong)
                    }
                }
            } else {
                Column {
                    Text(if (creating) "New playlist" else "Add to playlist")
                    TargetSongLine(targetSong)
                }
            }
        },
        text = {
            if (creating) {
                NewPlaylistForm(
                    name = name,
                    onNameChange = {
                        name = it
                        nameInteracted = true
                    },
                    interacted = nameInteracted,
                    intro =
                        if (playlists.isEmpty()) {
                            "You don't have any playlists yet. Create your first one."
                        } else {
                            null
                        },
                )
            } else {
                Column {
                    // A plain column, not a LazyColumn (F-26): the
                    // lazy list measured tall inside the dialog and
                    // left a band of dead space between the last
                    // playlist and Cancel. The column hugs its
                    // content exactly, capped + scrollable when the
                    // library of playlists outgrows the cap.
                    Column(
                        Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        playlists.forEach { playlist ->
                            ListItem(
                                supportingContent = {
                                    Text(
                                        songCountLabel(playlist.songCount),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                leadingContent = {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null)
                                },
                                modifier = Modifier.clickable { onPick(playlist) },
                            ) {
                                Text(
                                    playlist.name,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                    TextButton(onClick = { creating = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("New playlist")
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                Button(
                    onClick = { onCreatePlaylist(name.trim()) },
                    enabled = name.isNotBlank(),
                ) { Text("Create & add") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * The picker's target line (polish item 2): the name of the song
 * being added, under the dialog title in both list and create modes.
 * Plain text in the title slot — TalkBack reads it as part of the
 * dialog's header, so the announced target matches the visible one.
 */
@Composable
private fun TargetSongLine(song: Song) {
    Spacer(Modifier.height(OmegaSpacing.xs))
    Text(
        song.name,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Name entry for a new playlist, with the Library create dialog's
 * validation rules: the error shows only after the field has been
 * touched, and blank names cannot be submitted (the caller's confirm
 * button stays disabled while the name is blank).
 */
@Composable
private fun NewPlaylistForm(
    name: String,
    onNameChange: (String) -> Unit,
    interacted: Boolean,
    intro: String?,
) {
    Column {
        if (intro != null) {
            Text(
                intro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(OmegaSpacing.md))
        }
        // The field's label names it; the placeholder is an EXAMPLE,
        // never the label repeated (spec §7 microcopy fix).
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Playlist name") },
            placeholder = { Text("e.g. Monsoon drive") },
            singleLine = true,
            isError = interacted && name.isBlank(),
            supportingText = {
                if (interacted && name.isBlank()) {
                    Text(
                        "Give your playlist a name first.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
