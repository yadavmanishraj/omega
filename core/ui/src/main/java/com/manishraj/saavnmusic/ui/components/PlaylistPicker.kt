package com.manishraj.saavnmusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Modifier
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
 * straight on the create form.
 */
@Composable
fun PlaylistPickerDialog(
    playlists: List<LocalPlaylist>,
    onPick: (LocalPlaylist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var creating by remember { mutableStateOf(playlists.isEmpty()) }
    var name by remember { mutableStateOf("") }
    var nameInteracted by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
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
                            "You don't have any playlists yet — create your first one."
                        } else {
                            null
                        },
                )
            } else {
                Column {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(playlists) { playlist ->
                            ListItem(
                                supportingContent = {
                                    Text(
                                        "${playlist.songCount} songs",
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
