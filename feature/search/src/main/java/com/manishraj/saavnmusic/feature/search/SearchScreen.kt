package com.manishraj.saavnmusic.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.TopResult
import com.manishraj.saavnmusic.domain.UiState
import com.manishraj.saavnmusic.playback.InsertNextResult
import com.manishraj.saavnmusic.ui.components.Artwork
import com.manishraj.saavnmusic.ui.components.CircularArtwork
import com.manishraj.saavnmusic.ui.components.EmptyState
import com.manishraj.saavnmusic.ui.components.ErrorState
import com.manishraj.saavnmusic.ui.components.LocalOmegaSnackbar
import com.manishraj.saavnmusic.ui.components.MediaCard
import com.manishraj.saavnmusic.ui.components.OmegaLoadingIndicator
import com.manishraj.saavnmusic.ui.components.PlaylistPickerDialog
import com.manishraj.saavnmusic.ui.components.SectionHeader
import com.manishraj.saavnmusic.ui.components.SongOverflowMenuButton
import com.manishraj.saavnmusic.ui.components.SongRow
import com.manishraj.saavnmusic.ui.components.songCountLabel
import com.manishraj.saavnmusic.ui.theme.OmegaRadius
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    vm: SearchViewModel = hiltViewModel(),
    onAlbum: (String) -> Unit,
    onPlaylist: (String) -> Unit,
    onArtist: (String) -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
    onOpenLibrary: () -> Unit,
    onDownloadEnqueued: () -> Unit = {},
) {
    val query by vm.query.collectAsState()
    val songs by vm.songs.collectAsState()
    val albums by vm.albums.collectAsState()
    val artists by vm.artists.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val topResults by vm.topResults.collectAsState()
    val recent by vm.recent.collectAsState()
    val tab by vm.tab.collectAsState()
    val searchedQuery by vm.searchedQuery.collectAsState()
    val localPlaylists by vm.userPlaylists.collectAsState()
    val favorites by vm.favorites.collectAsState()
    val online by vm.online.collectAsState()
    val snackbar = LocalOmegaSnackbar.current
    var playlistTarget by remember { mutableStateOf<Song?>(null) }
    val isIdle = searchedQuery.isBlank()
    val noResults =
        !isIdle &&
            songs is UiState.Success &&
            (songs as UiState.Success<List<Song>>).data.isEmpty() &&
            albums.isEmpty() &&
            artists.isEmpty() &&
            playlists.isEmpty() &&
            topResults.isEmpty()

    Column(Modifier.fillMaxSize()) {
        // Current SearchBar API (inputField overload +
        // SearchBarDefaults.InputField): the deprecated
        // SearchBar(query, active, ...) overload is gone. The bar
        // is a permanent field, never an expandable search session —
        // this screen drives results itself — so expanded is pinned
        // false exactly as active=false was before.
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { vm.onQueryChange(it) },
                    onSearch = { vm.search(query) },
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("Songs, albums, artists…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { vm.onQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                )
            },
            expanded = false,
            onExpandedChange = {},
            // Contained expressive field (spec §5): the bar sits in
            // the brightest container role with the shape language's
            // card radius instead of the stock docked pill.
            shape = RoundedCornerShape(OmegaRadius.xl),
            colors =
                SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(OmegaSpacing.md),
        ) {}

        if (!online) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.xs)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "You're offline. Search needs a connection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = OmegaSpacing.md),
                )
                TextButton(onClick = onOpenLibrary) { Text("Library") }
            }
        }

        if (isIdle) {
            // The idle column SCROLLS (A17 audit F-12): at font 2.0
            // the field + cards grow past the fold, and a fixed
            // column left everything below it unreachable. Every
            // other fragile surface survives 2.0 precisely because
            // it scrolls.
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Idle groups are contained blocks (spec §5): recents and
                // starters read as two grouped surfaces on the container-
                // low role, not loose chips on the flat background.
                if (recent.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(OmegaRadius.xl),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.sm),
                    ) {
                        Column(Modifier.padding(OmegaSpacing.md)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Recent searches",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { vm.clearRecent() }) { Text("Clear all") }
                            }
                            // FlowRow: chips wrap the collection to the
                            // next line before any label is compressed
                            // (chip-reflow rule). Recents are Material
                            // InputChips (spec §3): the trailing ✕ is the
                            // component's own remove slot, so its target
                            // sizing is the component's, not hand-rolled.
                            FlowRow(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
                                verticalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
                            ) {
                                recent.forEach { q ->
                                    InputChip(
                                        selected = false,
                                        onClick = { vm.search(q) },
                                        label = { Text(q, maxLines = 1, softWrap = false) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "Remove '$q'",
                                                modifier =
                                                    Modifier
                                                        .size(InputChipDefaults.IconSize)
                                                        .clickable { vm.removeRecent(q) },
                                            )
                                        },
                                        shape = InputChipDefaults.shape,
                                    )
                                }
                            }
                        }
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(OmegaRadius.xl),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = OmegaSpacing.lg, vertical = OmegaSpacing.sm),
                ) {
                    Column(Modifier.padding(OmegaSpacing.md)) {
                        Text(
                            "Try",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(OmegaSpacing.sm))
                        FlowRow(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
                        ) {
                            listOf("Arijit Singh", "Lo-fi beats", "Punjabi hits", "Old Bollywood").forEach { starter ->
                                SuggestionChip(
                                    onClick = { vm.search(starter) },
                                    label = { Text(starter, maxLines = 1, softWrap = false) },
                                )
                            }
                        }
                    }
                }
                // The illustration block only earns its space when
                // there is nothing else to show (F-12): with recents
                // present it was a third stacked block pushing content
                // off the fold for no information.
                if (recent.isEmpty()) {
                    EmptyState(
                        title = "Search for music",
                        subtitle = "Songs, albums, artists and playlists. No account needed.",
                        icon = Icons.Filled.Search,
                    )
                }
            }
            return@Column
        }

        if (noResults) {
            EmptyState(
                title = "No results for \"$searchedQuery\"",
                subtitle = "Try a different spelling, or search by artist name.",
                icon = Icons.Outlined.SearchOff,
            )
            return@Column
        }

        // Tab labels carry their result counts (F-20), in the same
        // grammar Library's tabs use — one glance gives per-tab scope.
        SearchTabs(
            tab = tab,
            counts =
                listOf(
                    (songs as? UiState.Success)?.data?.size ?: 0,
                    albums.size,
                    artists.size,
                    playlists.size,
                ),
            onSelect = { vm.tab.value = it },
        )

        when (tab) {
            0 ->
                when (val s = songs) {
                    is UiState.Loading -> SearchLoading()
                    is UiState.Error -> ErrorState(s.message, onRetry = { vm.search(searchedQuery) })
                    is UiState.Success ->
                        LazyColumn {
                            if (topResults.isNotEmpty()) {
                                item { SectionHeader("Top results") }
                                // Section-prefixed indexed keys: the
                                // top hit usually ALSO appears in the
                                // songs list below — bare song-id keys
                                // would collide inside one LazyColumn.
                                // The repeat itself is DELIBERATE
                                // (polish R-P3): the top result is a
                                // distinct, entity-typed presentation
                                // answering a "take me to the thing"
                                // intent, while Songs below stays the
                                // complete playable list. Deduping the
                                // song out of Songs would silently make
                                // the full list incomplete, so both
                                // presentations keep it.
                                itemsIndexed(
                                    topResults.take(3),
                                    key = { index, item -> "top-$index-${item.id}" },
                                ) { _, item ->
                                    Box(Modifier.animateItem()) {
                                        // Top results are entity-TYPED
                                        // (F-02): songs play; artists /
                                        // albums / playlists navigate,
                                        // exactly like their tabs. The
                                        // pre-fix code rendered every
                                        // item as a Song, so an artist
                                        // top hit was a dead row whose
                                        // tap resolved the artist id as
                                        // a song and silently failed.
                                        when (item) {
                                            is TopResult.SongResult -> {
                                                val song = item.song
                                                val itemIsFavorite = favorites.any { it.id == song.id }
                                                SongRow(
                                                    song,
                                                    { vm.resolveAndPlay(song, onPlayQueue) },
                                                    trailing = {
                                                        SongOverflowMenuButton(
                                                            song = song,
                                                            isFavorite = itemIsFavorite,
                                                            onPlayNext = {
                                                                snackbar?.showMessage(
                                                                    playNextMessage(vm.playNext(song), song.name),
                                                                )
                                                            },
                                                            onDownload = {
                                                                vm.download(song)
                                                                onDownloadEnqueued()
                                                                snackbar?.showMessage("Download queued")
                                                            },
                                                            onToggleFavorite = {
                                                                vm.toggleFavorite(song, itemIsFavorite)
                                                            },
                                                            onAddToPlaylist = { playlistTarget = song },
                                                        )
                                                    },
                                                )
                                            }
                                            is TopResult.ArtistResult ->
                                                TopEntityRow(
                                                    title = item.artist.name,
                                                    subtitle = "Artist",
                                                    imageUrl = item.artist.imageUrl,
                                                    circular = true,
                                                    onClick = { onArtist(item.artist.id) },
                                                )
                                            is TopResult.AlbumResult ->
                                                TopEntityRow(
                                                    title = item.album.name,
                                                    subtitle = item.album.artist.ifBlank { "Album" },
                                                    imageUrl = item.album.imageUrl,
                                                    circular = false,
                                                    onClick = { onAlbum(item.album.id) },
                                                )
                                            is TopResult.PlaylistResult ->
                                                TopEntityRow(
                                                    title = item.playlist.name,
                                                    subtitle = "Playlist",
                                                    imageUrl = item.playlist.imageUrl,
                                                    circular = false,
                                                    onClick = { onPlaylist(item.playlist.id) },
                                                )
                                        }
                                    }
                                }
                                item { SectionHeader("Songs") }
                            }
                            itemsIndexed(
                                s.data,
                                key = { index, song -> "song-$index-${song.id}" },
                            ) { _, song ->
                                val songIsFavorite = favorites.any { it.id == song.id }
                                Box(Modifier.animateItem()) {
                                    SongRow(
                                        song,
                                        { onPlayQueue(s.data, s.data.indexOf(song)) },
                                        trailing = {
                                            SongOverflowMenuButton(
                                                song = song,
                                                isFavorite = songIsFavorite,
                                                onPlayNext = {
                                                    snackbar?.showMessage(
                                                        playNextMessage(vm.playNext(song), song.name),
                                                    )
                                                },
                                                onDownload = {
                                                    vm.download(song)
                                                    onDownloadEnqueued()
                                                    snackbar?.showMessage("Download queued")
                                                },
                                                onToggleFavorite = { vm.toggleFavorite(song, songIsFavorite) },
                                                onAddToPlaylist = { playlistTarget = song },
                                            )
                                        },
                                    )
                                }
                            }
                        }
                }

            1 ->
                when {
                    // A failed load owns EVERY tab (S1): the shared
                    // error treatment replaces results even if the
                    // user was sitting on this tab when it failed.
                    songs is UiState.Error ->
                        ErrorState(
                            (songs as UiState.Error).message,
                            onRetry = { vm.search(searchedQuery) },
                        )
                    albums.isEmpty() ->
                        if (songs is UiState.Loading) {
                            SearchLoading()
                        } else {
                            EmptyState("No albums found", "Try a different search.", icon = Icons.Outlined.SearchOff)
                        }
                    else ->
                        LazyVerticalGrid(
                            // Adaptive, not Fixed(2) (F-13): phone
                            // portrait still computes 2 columns at a
                            // 160dp minimum, while ≥600dp / landscape
                            // fit more columns instead of inflating
                            // two giant posters.
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier.padding(horizontal = OmegaSpacing.sm),
                        ) {
                            items(albums) { a ->
                                MediaCard(a.name, a.artist, a.imageUrl) { onAlbum(a.id) }
                            }
                        }
                }

            2 ->
                when {
                    songs is UiState.Error ->
                        ErrorState(
                            (songs as UiState.Error).message,
                            onRetry = { vm.search(searchedQuery) },
                        )
                    artists.isEmpty() ->
                        if (songs is UiState.Loading) {
                            SearchLoading()
                        } else {
                            EmptyState("No artists found", "Try a different search.", icon = Icons.Outlined.SearchOff)
                        }
                    else ->
                        LazyColumn {
                            items(artists) { a ->
                                ListItem(
                                    leadingContent = { CircularArtwork(a.imageUrl, contentDescription = a.name) },
                                    trailingContent = { EntityRowChevron() },
                                    modifier = Modifier.clickable { onArtist(a.id) },
                                ) {
                                    Text(a.name, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                }

            else ->
                when {
                    songs is UiState.Error ->
                        ErrorState(
                            (songs as UiState.Error).message,
                            onRetry = { vm.search(searchedQuery) },
                        )
                    playlists.isEmpty() ->
                        if (songs is UiState.Loading) {
                            SearchLoading()
                        } else {
                            EmptyState("No playlists found", "Try a different search.", icon = Icons.Outlined.SearchOff)
                        }
                    else ->
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier.padding(horizontal = OmegaSpacing.sm),
                        ) {
                            items(playlists) { p ->
                                MediaCard(p.name, songCountLabel(p.songCount ?: 0), p.imageUrl) { onPlaylist(p.id) }
                            }
                        }
                }
        }

        // Picker confirmation rides the shell snackbar (spec §7),
        // matching Detail's Wave 1 proof wiring.
        playlistTarget?.let { song ->
            PlaylistPickerDialog(
                playlists = localPlaylists,
                onPick = { playlist ->
                    vm.addToPlaylist(playlist.id, song)
                    playlistTarget = null
                    snackbar?.showMessage("Added to ${playlist.name}")
                },
                onCreatePlaylist = { name ->
                    vm.createPlaylistAndAdd(name, song)
                    playlistTarget = null
                    snackbar?.showMessage("Added to $name")
                },
                onDismiss = { playlistTarget = null },
            )
        }
    }
}

/**
 * Play-next snackbar copy (spec §3/§7): with a live queue the song
 * is inserted after the current track ("Will play next"), but with
 * nothing playing the engine APPENDS it without starting playback —
 * the copy must not promise "next" in that case.
 */
private fun playNextMessage(
    result: InsertNextResult,
    title: String,
): String =
    when (result) {
        InsertNextResult.INSERTED_NEXT -> "Will play next: $title"
        InsertNextResult.APPENDED -> "Added to queue: $title"
    }

/** Initial results load (spec §5): the expressive morph, centered —
 * search loads are short, undifferentiated waits, unlike the
 * content-shaped skeleton loads on Home/Detail. */
@Composable
private fun SearchLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        OmegaLoadingIndicator(contentDescription = "Searching")
    }
}

/** Result-type tabs: always scrollable (labels never compress) and
 * with explicit content colors — unselected tabs de-emphasize to
 * onSurfaceVariant; the primary color + indicator carry selection
 * (UI/UX audit, 2026-10-07). */
@Composable
private fun SearchTabs(
    tab: Int,
    counts: List<Int>,
    onSelect: (Int) -> Unit,
) {
    val labels = listOf("Songs", "Albums", "Artists", "Playlists")
    PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = OmegaSpacing.lg) {
        labels.forEachIndexed { i, label ->
            val count = counts.getOrElse(i) { 0 }
            Tab(
                selected = tab == i,
                onClick = { onSelect(i) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    Text(
                        if (count > 0) "$label · $count" else label,
                        maxLines = 1,
                        softWrap = false,
                    )
                },
            )
        }
    }
}

/**
 * Trailing affordance for entity rows: the row itself is the door,
 * so the chevron only SIGNALS navigation. Decorative (null content
 * description), never a separate action.
 */
@Composable
private fun EntityRowChevron() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
    )
}

/**
 * Row for a non-song top result (F-02): the same ListItem grammar as
 * the Artists tab — artwork, name, type line, the whole row
 * navigates, trailing chevron as the navigation affordance.
 * Deliberately NO overflow button: entity rows have no
 * song menu, and the inert ⋮ was part of what made the old
 * force-mapped rows read as broken songs.
 */
@Composable
private fun TopEntityRow(
    title: String,
    subtitle: String,
    imageUrl: String?,
    circular: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        },
        leadingContent = {
            if (circular) {
                CircularArtwork(imageUrl, contentDescription = title)
            } else {
                Artwork(imageUrl, contentDescription = title)
            }
        },
        trailingContent = { EntityRowChevron() },
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
