package com.manishraj.saavnmusic.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.manishraj.saavnmusic.domain.*

@Composable fun Artwork(
    url: String?,
    size: Int = 56,
    corner: Int = 10,
) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(corner.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url !=
            null
        ) {
            AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.MusicNote, null)
        }
    }
}

@Composable fun SongRow(
    song: Song,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    ListItem(headlineContent = {
        Text(song.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }, supportingContent = {
        Text(
            listOf(song.artist, formatDuration(song.durationSec))
                .filter {
                    it.isNotBlank()
                }.joinToString(" • "),
            maxLines = 1,
        )
    }, leadingContent = { Artwork(song.imageUrl) }, trailingContent = trailing, modifier = Modifier.clickable { onClick() })
}

@Composable fun MediaCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    width: Int = 150,
    onClick: () -> Unit,
) {
    Column(Modifier.width(width.dp).clickable { onClick() }.padding(6.dp)) {
        Artwork(imageUrl, width, 14)
        Spacer(Modifier.height(6.dp))
        Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable fun ShimmerList() {
    Column {
        repeat(6) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                Spacer(Modifier.width(12.dp))
                Column {
                    Box(Modifier.fillMaxWidth(0.7f).height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.45f).height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                }
            }
        }
    }
}

@Composable fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable fun EmptyState(
    title: String,
    subtitle: String,
) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.MusicNote, null, Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable fun GradientHeader(
    imageUrl: String?,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), MaterialTheme.colorScheme.background)),
        ),
    ) {
        Column(Modifier.padding(top = 16.dp)) { content() }
    }
}
