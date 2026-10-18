package com.fidelityone.player.ui.screens.queue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fidelityone.player.core.TechFormatter
import com.fidelityone.player.domain.model.Track
import com.fidelityone.player.ui.screens.CoverArt
import com.fidelityone.player.ui.screens.EmptyState

/** Fila de reprodução com reordenação simples (tocar/remover). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    queue: List<Track>,
    currentIndex: Int,
    onJump: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fila · ${queue.size} faixas") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Fechar") }
                }
            )
        }
    ) { pad ->
        if (queue.isEmpty()) {
            Box(Modifier.padding(pad)) {
                EmptyState(Icons.Filled.QueueMusic, "Fila vazia", "Adicione músicas pela biblioteca.")
            }
        } else {
            LazyColumn(Modifier.padding(pad)) {
                itemsIndexed(queue) { i, t ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable { onJump(i) }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoverArt(t.externalArtUri, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.titleMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.artist, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (i == currentIndex) {
                            Text("▶", color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text(TechFormatter.duration(t.durationMs),
                                style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { onRemove(i) }) {
                            Icon(Icons.Filled.Close, "Remover da fila",
                                tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}
