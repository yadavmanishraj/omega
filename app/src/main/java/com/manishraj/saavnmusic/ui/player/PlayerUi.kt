package com.manishraj.saavnmusic.ui.player
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.WorkManager
import com.manishraj.saavnmusic.domain.formatDuration
import com.manishraj.saavnmusic.download.DownloadWorker
import com.manishraj.saavnmusic.ui.components.Artwork
import com.manishraj.saavnmusic.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    vm: PlayerViewModel = hiltViewModel(),
    onOpen: () -> Unit,
) {
    val st by vm.state.collectAsState()
    val cur = st.current ?: return
    Surface(onClick = onOpen, tonalElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(cur.imageUrl, 48, 8)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(cur.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                Text(cur.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }
            if (st.isBuffering) CircularProgressIndicator(Modifier.size(24.dp))
            IconButton(onClick = { vm.player.prev() }) { Icon(Icons.Default.SkipPrevious, null) }
            IconButton(
                onClick = { vm.player.playPause() },
            ) { Icon(if (st.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null) }
            IconButton(onClick = { vm.player.next() }) { Icon(Icons.Default.SkipNext, null) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayer(
    vm: PlayerViewModel = hiltViewModel(),
    detailVm: DetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val st by vm.state.collectAsState()
    val cur = st.current
    val ctx = LocalContext.current
    if (cur == null) {
        Column(Modifier.padding(32.dp)) { Text("Nothing playing") }
        return
    }
    val fav by vm.isFavorite(cur.id).collectAsState(false)
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var lyrics by remember { mutableStateOf<String?>(null) }
    var sleep by remember { mutableIntStateOf(0) }
    LaunchedEffect(showLyrics, cur.id) { if (showLyrics) detailVm.lyrics(cur.id) { lyrics = it } }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, null) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                showQueue =
                    true
            }) { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) }
        }
        Artwork(cur.imageUrl, 300, 20)
        Spacer(Modifier.height(20.dp))
        Text(cur.name, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(cur.artist, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        var scrub by remember { mutableStateOf<Float?>(null) }
        Slider(
            value =
                scrub ?: if (st.durationMs >
                    0
                ) {
                    st.positionMs.toFloat() / st.durationMs
                } else {
                    0f
                },
            onValueChange = { scrub = it },
            onValueChangeFinished = {
                scrub?.let {
                    vm.player.seekTo(
                        (
                            it *
                                st.durationMs
                        ).toLong(),
                    )
                }
                ; scrub = null
            },
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatDuration(st.positionMs / 1000))
            Spacer(Modifier.weight(1f))
            Text(
                formatDuration(
                    if (st.durationMs >
                        0
                    ) {
                        st.durationMs / 1000
                    } else {
                        cur.durationSec
                    },
                ),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                vm.player.toggleShuffle()
            }) {
                Icon(
                    Icons.Default.Shuffle,
                    null,
                    tint = if (st.shuffle) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
            IconButton(onClick = { vm.player.prev() }) { Icon(Icons.Default.SkipPrevious, null, Modifier.size(36.dp)) }
            FilledIconButton(
                onClick = {
                    vm.player.playPause()
                },
                Modifier.size(
                    64.dp,
                ),
            ) { Icon(if (st.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(32.dp)) }
            IconButton(onClick = { vm.player.next() }) { Icon(Icons.Default.SkipNext, null, Modifier.size(36.dp)) }
            IconButton(onClick = { vm.player.cycleRepeat() }) {
                Icon(
                    if (st.repeatMode ==
                        2
                    ) {
                        Icons.Default.RepeatOne
                    } else {
                        Icons.Default.Repeat
                    },
                    null,
                    tint =
                        if (st.repeatMode !=
                            0
                        ) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor.current
                        },
                )
            }
        }
        Row {
            IconButton(onClick = {
                vm.toggleFavorite(cur, fav)
            }) {
                Icon(
                    if (fav) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    null,
                    tint = if (fav) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
            IconButton(onClick = {
                DownloadWorker.enqueue(WorkManager.getInstance(ctx), cur.id, vm.appSettings.value.downloadQuality)
            }) { Icon(Icons.Default.Download, null) }
            IconButton(onClick = {
                showLyrics =
                    !showLyrics
            }) { Icon(Icons.Default.Lyrics, null) }
            IconButton(onClick = {
                val next =
                    when (sleep) {
                        0 -> 15
                        15 -> 30
                        30 -> 60
                        else -> 0
                    }
                sleep = next
                vm.player.setSleepTimer(next)
            }) { Icon(Icons.Default.Bedtime, null) }
            Text("${if (sleep == 0) "" else sleep.toString()}", Modifier.align(Alignment.CenterVertically))
        }
        Text("Speed", style = MaterialTheme.typography.bodySmall)
        // FlowRow so the chips wrap instead of overflowing on narrow
        // screens / large font sizes (same bug as the settings chips).
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(0.75f, 1f, 1.25f, 1.5f).forEach { v ->
                FilterChip(
                    selected = st.speed == v,
                    onClick = { vm.player.setSpeed(v) },
                    label = { Text("${v}x", maxLines = 1, softWrap = false) },
                )
            }
        }
        if (showLyrics) {
            Spacer(Modifier.height(12.dp))
            Text(
                lyrics ?: if (cur.hasLyrics) "Loading lyrics…" else "No lyrics available for this song",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    if (showQueue) {
        ModalBottomSheet(onDismissRequest = { showQueue = false }) {
            Text("Queue", Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
            LazyColumn {
                items(st.queue) { s ->
                    ListItem(headlineContent = {
                        Text(s.name)
                    }, supportingContent = {
                        Text(s.artist)
                    }, leadingContent = {
                        Artwork(
                            s.imageUrl,
                            44,
                            6,
                        )
                    }, modifier = Modifier.clickable { vm.player.playIndex(st.queue.indexOf(s)) })
                }
            }
        }
    }
}
