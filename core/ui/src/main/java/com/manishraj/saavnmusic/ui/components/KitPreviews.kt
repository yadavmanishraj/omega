package com.manishraj.saavnmusic.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing
import com.manishraj.saavnmusic.ui.theme.SaavnTheme

/**
 * Preview coverage for the Wave 1 component kit (spec §8: new
 * components land with previews, including the 1.33 / 2.0 font scales
 * where layout could crush). These double as compile coverage for the
 * wrappers — if an alpha rename breaks a signature, this file fails
 * the build alongside the component.
 */

private val previewSong =
    Song(
        id = "preview-1",
        name = "Tum Hi Ho",
        artist = "Arijit Singh, Mithoon",
        album = "Aashiqui 2",
        imageUrl = null,
        durationSec = 262,
        streamUrl = null,
    )

@Preview(showBackground = true)
@Composable
private fun OmegaLoadingIndicatorPreview() {
    SaavnTheme(dark = true) {
        Surface {
            Row(Modifier.padding(OmegaSpacing.lg)) {
                OmegaLoadingIndicator()
                OmegaLoadingIndicator(contained = true)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OmegaChoiceGroupPreview() {
    SaavnTheme(dark = true) {
        Surface {
            OmegaChoiceGroup(
                options = listOf("System", "Dark", "Light"),
                selected = "Dark",
                onSelect = {},
                label = { it },
                modifier = Modifier.padding(OmegaSpacing.lg),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OmegaActionGroupPreview() {
    SaavnTheme(dark = true) {
        Surface {
            OmegaActionGroup(
                primaryLabel = "Play all",
                onPrimary = {},
                secondaryLabel = "Shuffle",
                onSecondary = {},
                primaryIcon = Icons.Filled.PlayArrow,
                secondaryIcon = Icons.Filled.Shuffle,
                modifier = Modifier.padding(OmegaSpacing.lg),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OmegaSegmentedListPreview() {
    SaavnTheme(dark = true) {
        Surface {
            OmegaSegmentedList(Modifier.padding(OmegaSpacing.lg)) {
                OmegaSegmentedListItem(
                    headline = "Theme",
                    supporting = "Dark",
                    leading = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
                )
                OmegaSegmentedListItem(
                    headline = "Now playing row",
                    supporting = "Highlighted selection treatment",
                    selected = true,
                )
                OmegaSegmentedListItem(
                    headline = "Dynamic color",
                    supporting = "Off",
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransportMorphsPreview() {
    SaavnTheme(dark = true) {
        Surface {
            Row(Modifier.padding(OmegaSpacing.lg)) {
                OmegaPlayPauseIcon(isPlaying = true)
                OmegaPlayPauseIcon(isPlaying = false)
                OmegaFavoriteIcon(isFavorite = true)
                OmegaFavoriteIcon(isFavorite = false)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShimmerPreview() {
    SaavnTheme(dark = true) {
        Surface { ShimmerList() }
    }
}

/** SongRow with the unified menu at the user's everyday scale —
 * the Phase B guarantee (duration slot protected) must hold. */
@Preview(showBackground = true, fontScale = 1.33f)
@Composable
private fun SongRowFontScale133Preview() {
    SaavnTheme(dark = true) {
        Surface {
            SongRow(
                song = previewSong,
                onClick = {},
                trailing = {
                    SongOverflowMenuButton(
                        song = previewSong,
                        isFavorite = true,
                        onDownload = {},
                        onToggleFavorite = {},
                        onAddToPlaylist = {},
                    )
                },
            )
        }
    }
}

/** Same row at the maximum supported scale: nothing may crush or
 * push the menu off the row. */
@Preview(showBackground = true, fontScale = 2.0f)
@Composable
private fun SongRowFontScale200Preview() {
    SaavnTheme(dark = true) {
        Surface {
            Column {
                SongRow(
                    song = previewSong,
                    onClick = {},
                    trailing = {
                        SongOverflowMenuButton(
                            song = previewSong,
                            isFavorite = false,
                            onDownload = {},
                            onToggleFavorite = {},
                            onAddToPlaylist = {},
                        )
                    },
                )
            }
        }
    }
}
