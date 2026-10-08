package com.manishraj.saavnmusic.ui.components

import androidx.compose.foundation.layout.Arrangement
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

/** Ranked chart rows (uplift W2-B, spec §5.2): zero-padded rank
 * numerals in their 24dp column, hairline dividers between rows,
 * the shared overflow menu riding the trailing slot unchanged. */
@Preview(showBackground = true)
@Composable
private fun RankedSongRowPreview() {
    SaavnTheme(dark = true) {
        Surface {
            Column {
                RankedSongRow(
                    song = previewSong,
                    rank = 1,
                    onClick = {},
                    trailing = {
                        SongOverflowMenuButton(
                            song = previewSong,
                            onAddToPlaylist = {},
                        )
                    },
                )
                EditorialRowDivider()
                RankedSongRow(
                    song =
                        previewSong.copy(
                            id = "preview-2",
                            name = "Kesariya",
                            artist = "Pritam, Arijit Singh",
                            durationSec = 268,
                        ),
                    rank = 2,
                    onClick = {},
                )
                EditorialRowDivider()
                RankedSongRow(
                    song =
                        previewSong.copy(
                            id = "preview-3",
                            name = "Chaiyya Chaiyya",
                            artist = "Sukhwinder Singh, Sapna Awasthi",
                            durationSec = 355,
                        ),
                    rank = 3,
                    onClick = {},
                )
            }
        }
    }
}

/** Section taxonomy label (uplift W2-B, spec §5.4): uppercase
 * label + hairline rule, with and without the trailing action. */
@Preview(showBackground = true)
@Composable
private fun OmegaSectionLabelPreview() {
    SaavnTheme(dark = true) {
        Surface {
            Column(Modifier.padding(OmegaSpacing.lg)) {
                OmegaSectionLabel(text = "Trending songs")
                OmegaSectionLabel(
                    text = "Jump back in",
                    actionLabel = "See all",
                    onAction = {},
                    modifier = Modifier.padding(top = OmegaSpacing.lg),
                )
            }
        }
    }
}

/** Media cards at the W2-A geometry (spec §5.3): 124dp artwork
 * @ xl (16), plus the circular artist variant on the same rail. */
@Preview(showBackground = true)
@Composable
private fun MediaCardPreview() {
    SaavnTheme(dark = true) {
        Surface {
            Row(
                Modifier.padding(OmegaSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(OmegaSpacing.md),
            ) {
                MediaCard(
                    title = "Brahmastra (Original Motion Picture Soundtrack)",
                    subtitle = "Pritam, Arijit Singh",
                    imageUrl = null,
                    onClick = {},
                )
                MediaCard(
                    title = "Arijit Singh",
                    subtitle = "Artist",
                    imageUrl = null,
                    circular = true,
                    onClick = {},
                )
            }
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
