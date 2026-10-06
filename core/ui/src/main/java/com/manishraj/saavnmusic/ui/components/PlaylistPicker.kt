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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
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
 * Overflow (⋮) button for a song row: the single entry point into
 * song-level actions across Home / Search / Detail / Library lists.
 * Today it carries "Add to playlist"; future song actions belong here
 * too, so every list behaves the same way.
 */
@Composable
fun SongOverflowMenuButton(
    song: Song,
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
        DropdownMenuItem(
            text = { Text("Add to playlist", maxLines = 1, softWrap = false) },
            leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
            onClick = {
                expanded = false
                onAddToPlaylist()
            },
        )
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
                                headlineContent = {
                                    Text(
                                        playlist.name,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        "${playlist.songCount} songs",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                leadingContent = {
                                    Icon(Icons.Filled.PlaylistPlay, contentDescription = null)
                                },
                                modifier = Modifier.clickable { onPick(playlist) },
                            )
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
        Text("Playlist name", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(OmegaSpacing.xs))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Playlist name") },
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
