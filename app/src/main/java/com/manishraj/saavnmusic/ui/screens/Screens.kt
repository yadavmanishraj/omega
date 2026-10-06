package com.manishraj.saavnmusic.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.manishraj.saavnmusic.domain.*
import com.manishraj.saavnmusic.ui.components.*
import com.manishraj.saavnmusic.ui.viewmodel.*

@Composable fun HomeScreen(
    vm: HomeViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
    onAlbum: (String) -> Unit,
    onPlaylist: (String) -> Unit,
    onArtist: (String) -> Unit,
) {
    val trending by vm.trending.collectAsState()
    val albums by vm.albums.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val artists by vm.artists.collectAsState()
    val history by vm.history.collectAsState()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(16.dp)) {
                Text("Good listening", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "No account. Just music.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (history.isNotEmpty()) {
            item { SectionHeader("Recently played") }
            item { LazyRow { items(history) { h -> MediaCard(h.name, h.artist, h.imageUrl) {} } } }
        }
        item { SectionHeader("Trending songs") }
        when (val s = trending) {
            is UiState.Loading -> item { ShimmerList() }
            is UiState.Error -> item { ErrorState(s.message) { vm.load() } }
            is UiState.Success -> items(s.data.take(10)) { song -> SongRow(song, { playerVm.play(s.data, s.data.indexOf(song)) }) }
        }
        item { SectionHeader("Albums") }
        item { LazyRow { items(albums) { a -> MediaCard(a.name, a.artist, a.imageUrl) { onAlbum(a.id) } } } }
        item { SectionHeader("Playlists") }
        item { LazyRow { items(playlists) { p -> MediaCard(p.name, p.description.orEmpty(), p.imageUrl) { onPlaylist(p.id) } } } }
        item { SectionHeader("Artists") }
        item { LazyRow { items(artists) { a -> MediaCard(a.name, "Artist", a.imageUrl) { onArtist(a.id) } } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    vm: SearchViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
    onAlbum: (String) -> Unit,
    onPlaylist: (String) -> Unit,
    onArtist: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val songs by vm.songs.collectAsState()
    val albums by vm.albums.collectAsState()
    val artists by vm.artists.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val recent by vm.recent.collectAsState()
    val tab by vm.tab.collectAsState()
    Column(Modifier.fillMaxSize()) {
        SearchBar(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            inputField = {
                SearchBarInputField(
                    query = text,
                    onQueryChange = { text = it },
                    onSearch = { vm.search(text) },
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("Songs, albums, artists…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                )
            },
            expanded = false,
            onExpandedChange = {},
        ) {}
        if (recent.isNotEmpty() &&
            songs is UiState.Success &&
            (songs as UiState.Success<List<Song>>).data.isEmpty()
        ) {
            Text("Recent searches", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            LazyRow(Modifier.padding(horizontal = 12.dp)) {
                items(recent) { r ->
                    AssistChip(onClick = {
                        text =
                            r.query
                        ; vm.search(r.query)
                    }, label = { Text(r.query) })
                    ; Spacer(Modifier.width(8.dp))
                }
            }
            TextButton(onClick = { vm.clearRecent() }) { Text("Clear recent searches") }
        }
        TabRow(selectedTabIndex = tab) {
            listOf("Songs", "Albums", "Artists", "Playlists").forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = {
                    vm.tab.value =
                        i
                }, text = { Text(t) })
            }
        }
        when (tab) {
            0 ->
                when (val s = songs) {
                    is UiState.Loading -> ShimmerList()
                    is UiState.Error -> ErrorState(s.message) { vm.search(text) }
                    is UiState.Success ->
                        if (s.data.isEmpty()) {
                            EmptyState("Search for music", "Results will appear here")
                        } else {
                            LazyColumn {
                                items(s.data) { song -> SongRow(song, { playerVm.play(s.data, s.data.indexOf(song)) }) }
                            }
                        }
                }
            1 ->
                LazyColumn {
                    items(albums) { a ->
                        ListItem(headlineContent = {
                            Text(a.name)
                        }, supportingContent = {
                            Text(
                                a.artist,
                            )
                        }, leadingContent = { Artwork(a.imageUrl) }, modifier = Modifier.clickable { onAlbum(a.id) })
                    }
                }
            2 ->
                LazyColumn {
                    items(artists) { a ->
                        ListItem(headlineContent = {
                            Text(a.name)
                        }, leadingContent = { Artwork(a.imageUrl) }, modifier = Modifier.clickable { onArtist(a.id) })
                    }
                }
            else ->
                LazyColumn {
                    items(playlists) { p ->
                        ListItem(headlineContent = {
                            Text(p.name)
                        }, supportingContent = {
                            Text("${p.songCount ?: ""} songs")
                        }, leadingContent = { Artwork(p.imageUrl) }, modifier = Modifier.clickable { onPlaylist(p.id) })
                    }
                }
        }
    }
}

@Composable fun AlbumScreen(
    id: String,
    vm: DetailViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    LaunchedEffect(id) { vm.loadAlbum(id) }
    val s by vm.album.collectAsState()
    DetailList(s, {
        it.songs
    }, { a -> SongListHeader(a.name, a.artist, a.imageUrl, a.description) }, { songs, i -> playerVm.play(songs, i) }, { vm.loadAlbum(id) })
}

@Composable fun PlaylistScreen(
    id: String,
    vm: DetailViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    LaunchedEffect(id) { vm.loadPlaylist(id) }
    val s by vm.playlist.collectAsState()
    DetailList(s, {
        it.songs
    }, { p ->
        SongListHeader(
            p.name,
            "Playlist",
            p.imageUrl,
            p.description,
        )
    }, { songs, i -> playerVm.play(songs, i) }, { vm.loadPlaylist(id) })
}

@Composable fun <T> DetailList(
    state: UiState<T>,
    songs: (T) -> List<Song>,
    header: @Composable (T) -> Unit,
    play: (List<Song>, Int) -> Unit,
    retry: () -> Unit,
) {
    when (state) {
        is UiState.Loading -> ShimmerList()
        is UiState.Error -> ErrorState(state.message, retry)
        is UiState.Success -> {
            val list = songs(state.data)
            LazyColumn {
                item { header(state.data) }
                item {
                    Row(Modifier.padding(16.dp)) {
                        Button(onClick = { play(list, 0) }) {
                            Icon(Icons.Default.PlayArrow, null)
                            Text(" Play all")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { play(list.shuffled(), 0) }) { Text("Shuffle") }
                    }
                }
                items(list) { song -> SongRow(song, { play(list, list.indexOf(song)) }) }
            }
        }
    }
}

@Composable fun SongListHeader(
    title: String,
    subtitle: String,
    image: String?,
    desc: String?,
) {
    GradientHeader(image) {
        Column(Modifier.padding(16.dp)) {
            Artwork(image, 180, 16)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            if (!desc.isNullOrBlank()) {
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable fun ArtistScreen(
    id: String,
    vm: DetailViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
    onAlbum: (String) -> Unit,
) {
    LaunchedEffect(id) { vm.loadArtist(id) }
    val s by vm.artist.collectAsState()
    when (val a = s) {
        is UiState.Loading -> ShimmerList()
        is UiState.Error -> ErrorState(a.message) { vm.loadArtist(id) }
        is UiState.Success ->
            LazyColumn {
                item {
                    SongListHeader(
                        a.data.name,
                        listOfNotNull(a.data.followers?.let { "$it followers" }).joinToString(),
                        a.data.imageUrl,
                        a.data.bio,
                    )
                }
                item { SectionHeader("Top songs") }
                items(a.data.topSongs) { song -> SongRow(song, { playerVm.play(a.data.topSongs, a.data.topSongs.indexOf(song)) }) }
                item { SectionHeader("Top albums") }
                item { LazyRow { items(a.data.topAlbums) { al -> MediaCard(al.name, al.artist, al.imageUrl) { onAlbum(al.id) } } } }
            }
    }
}
