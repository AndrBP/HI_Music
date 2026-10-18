package com.fidelityone.player.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fidelityone.player.core.AudioFormat
import com.fidelityone.player.core.TechFormatter
import com.fidelityone.player.domain.model.Album
import com.fidelityone.player.domain.model.Track
import com.fidelityone.player.ui.theme.LocalFidelityTokens

/* ── Componentes compartilhados do Fidelity One ─────────────── */

@Composable
fun GradientBackground(
    colors: List<androidx.compose.ui.graphics.Color>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(colors))) {
        content()
    }
}

@Composable
fun SectionHeader(text: String, action: (@Composable () -> Unit)? = null) {
    val t = LocalFidelityTokens.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = t.spacing.gutter, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.headlineMedium)
        action?.invoke()
    }
}

@Composable
fun CoverArt(uri: Uri?, fallbackIcon: ImageVector = Icons.Filled.MusicNote, modifier: Modifier = Modifier) {
    val t = LocalFidelityTokens.current
    if (uri != null) {
        AsyncImage(
            model = uri,
            contentDescription = "Capa",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(t.shapes.card)
        )
    } else {
        Box(
            modifier = modifier
                .clip(t.shapes.card)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(fallbackIcon, null, tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun FormatChip(format: AudioFormat?) {
    if (format == null) return
    val label = when {
        format.dsd -> "DSD"
        format.lossless -> format.label
        else -> format.label
    }
    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = if (format.lossless) {
            { Icon(Icons.Filled.HighQuality, null, modifier = Modifier.size(14.dp)) }
        } else null,
        modifier = Modifier.height(24.dp)
    )
}

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onQueue: () -> Unit
) {
    val t = LocalFidelityTokens.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = t.spacing.gutter, vertical = 3.dp)
            .height(t.spacing.itemHeight)
            .clickable(onClick = onClick),
        shape = t.shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            CoverArt(track.externalArtUri, modifier = Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    "${track.artist} · ${track.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            track.format?.let {
                Text(it.label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(6.dp))
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    "Favoritar",
                    tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onQueue) {
                Icon(Icons.Filled.QueueMusic, "Adicionar à fila", tint = MaterialTheme.colorScheme.outline)
            }
            Text(TechFormatter.duration(track.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit, onPlay: () -> Unit, onQueue: () -> Unit) {
    val t = LocalFidelityTokens.current
    Column(
        modifier = Modifier
            .padding(4.dp)
            .width(if (t.spacing.gridColumns >= 3) 110.dp else 150.dp)
            .clickable(onClick = onClick)
    ) {
        CoverArt(album.coverUri, modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f))
        Spacer(Modifier.height(6.dp))
        Text(album.title, style = MaterialTheme.typography.titleMedium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            album.artist + (album.year?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            TextButton(onClick = onPlay, contentPadding = PaddingValues(4.dp)) {
                Icon(Icons.Filled.PlayArrow, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp)); Text("Tocar")
            }
            TextButton(onClick = onQueue, contentPadding = PaddingValues(4.dp)) {
                Icon(Icons.Filled.QueueMusic, null, Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction, shape = LocalFidelityTokens.current.shapes.button) { Text(actionLabel) }
        }
    }
}
