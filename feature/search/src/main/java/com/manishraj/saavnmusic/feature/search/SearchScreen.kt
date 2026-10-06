package com.manishraj.saavnmusic.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.Song
import com.manishraj.saavnmusic.domain.UiState
import com.manishraj.saavnmusic.ui.components.CircularArtwork
import com.manishraj.saavnmusic.ui.components.EmptyState
import com.manishraj.saavnmusic.ui.components.ErrorState
import com.manishraj.saavnmusic.ui.components.MediaCard
import com.manishraj.saavnmusic.ui.components.SectionHeader
import com.manishraj.saavnmusic.ui.components.ShimmerGrid
import com.manishraj.saavnmusic.ui.components.ShimmerList
import com.manishraj.saavnmusic.ui.components.SongRow
import com.manishraj.saavnmusic.ui.theme.OmegaSpacing

// NOTE: SearchBarInputField (the non-deprecated SearchBar API) does not
// resolve against material3 1.3.1 (Compose BOM 2024.12.01) on the user's
// toolchain, so this screen deliberately uses the deprecated
// SearchBar(query, active, ...) overload until the BOM is bumped.
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    vm: SearchViewModel = hiltViewModel(),
    onAlbum: (String) -> Unit,
    onPlaylist: (String) -> Unit,
    onArtist: (String) -> Unit,
    onPlayQueue: (List<Song>, Int) -> Unit,
    onOpenLibrary: () -> Unit,
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
    val online by vm.online.collectAsState()
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
        SearchBar(
            query = query,
            onQueryChange = { vm.onQueryChange(it) },
            onSearch = { vm.search(query) },
            active = false,
            onActiveChange = {},
            placeholder = { Text("Songs, albums, artists…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { vm.onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
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
                    "You're offline — search needs a connection",
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
            if (recent.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OmegaSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Recent searches",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { vm.clearRecent() }) { Text("Clear all") }
                }
                // FlowRow: chips wrap the collection to the next line
                // before any label is compressed (chip-reflow rule).
                FlowRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OmegaSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
                ) {
                    recent.forEach { q ->
                        RecentChip(
                            query = q,
                            onClick = { vm.search(q) },
                            onRemove = { vm.removeRecent(q) },
                        )
                    }
                }
                Spacer(Modifier.height(OmegaSpacing.xl))
            }
            Text(
                "Try",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = OmegaSpacing.lg),
            )
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(OmegaSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(OmegaSpacing.sm),
            ) {
                listOf("Arijit Singh", "Lo-fi beats", "Punjabi hits", "Old Bollywood").forEach { starter ->
                    AssistChip(
                        onClick = { vm.search(starter) },
                        label = { Text(starter, maxLines = 1, softWrap = false) },
                    )
                }
            }
            EmptyState(
                title = "Search for music",
                subtitle = "Songs, albums, artists and playlists — no account needed.",
                icon = Icons.Filled.Search,
            )
            return@Column
        }

        if (noResults) {
            EmptyState(
                title = "No results for '$searchedQuery'",
                subtitle = "Check the spelling, or try another name.",
                icon = Icons.Outlined.SearchOff,
            )
            return@Column
        }

        SearchTabs(tab = tab, onSelect = { vm.tab.value = it })

        when (tab) {
            0 ->
                when (val s = songs) {
                    is UiState.Loading -> ShimmerList()
                    is UiState.Error -> ErrorState(s.message) { vm.search(searchedQuery) }
                    is UiState.Success ->
                        LazyColumn {
                            if (topResults.isNotEmpty()) {
                                item { SectionHeader("Top results") }
                                items(topResults.take(3)) { item ->
                                    SongRow(item, { vm.resolveAndPlay(item, onPlayQueue) })
                                }
                                item { SectionHeader("Songs") }
                            }
                            items(s.data) { song ->
                                SongRow(song, { onPlayQueue(s.data, s.data.indexOf(song)) })
                            }
                        }
                }

            1 ->
                if (albums.isEmpty()) {
                    if (songs is UiState.Loading) {
                        ShimmerGrid()
                    } else {
                        EmptyState("No albums found", "Try a different search.", icon = Icons.Outlined.SearchOff)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.padding(horizontal = OmegaSpacing.sm),
                    ) {
                        items(albums) { a ->
                            MediaCard(a.name, a.artist, a.imageUrl) { onAlbum(a.id) }
                        }
                    }
                }

            2 ->
                if (artists.isEmpty()) {
                    if (songs is UiState.Loading) {
                        ShimmerList()
                    } else {
                        EmptyState("No artists found", "Try a different search.", icon = Icons.Outlined.SearchOff)
                    }
                } else {
                    LazyColumn {
                        items(artists) { a ->
                            ListItem(
                                headlineContent = { Text(a.name, style = MaterialTheme.typography.titleMedium) },
                                leadingContent = { CircularArtwork(a.imageUrl, contentDescription = a.name) },
                                modifier = Modifier.clickable { onArtist(a.id) },
                            )
                        }
                    }
                }

            else ->
                if (playlists.isEmpty()) {
                    if (songs is UiState.Loading) {
                        ShimmerGrid()
                    } else {
                        EmptyState("No playlists found", "Try a different search.", icon = Icons.Outlined.SearchOff)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.padding(horizontal = OmegaSpacing.sm),
                    ) {
                        items(playlists) { p ->
                            MediaCard(p.name, "${p.songCount ?: 0} songs", p.imageUrl) { onPlaylist(p.id) }
                        }
                    }
                }
        }
    }
}

/** Tab row that switches to scrollable before labels can compress (fontScale ≥ 1.6). */
@Composable
private fun SearchTabs(
    tab: Int,
    onSelect: (Int) -> Unit,
) {
    val labels = listOf("Songs", "Albums", "Artists", "Playlists")
    val fontScale = LocalDensity.current.fontScale
    if (fontScale >= 1.6f) {
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = OmegaSpacing.lg) {
            labels.forEachIndexed { i, label ->
                Tab(
                    selected = tab == i,
                    onClick = { onSelect(i) },
                    text = { Text(label, maxLines = 1, softWrap = false) },
                )
            }
        }
    } else {
        TabRow(selectedTabIndex = tab) {
            labels.forEachIndexed { i, label ->
                Tab(
                    selected = tab == i,
                    onClick = { onSelect(i) },
                    text = { Text(label, maxLines = 1, softWrap = false) },
                )
            }
        }
    }
}

/** Recent-search chip: tap searches; the ✕ removes just this entry. */
@Composable
private fun RecentChip(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                query,
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelLarge,
                modifier =
                    Modifier
                        .clickable { onClick() }
                        .padding(start = OmegaSpacing.md, top = OmegaSpacing.sm, bottom = OmegaSpacing.sm),
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove '$query'",
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
