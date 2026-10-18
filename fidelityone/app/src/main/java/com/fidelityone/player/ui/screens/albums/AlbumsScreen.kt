package com.fidelityone.player.ui.screens.albums

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fidelityone.player.core.TechFormatter
import com.fidelityone.player.domain.model.Album
import com.fidelityone.player.domain.model.Track
import com.fidelityone.player.ui.screens.*
import com.fidelityone.player.viewmodel.LibraryUiState

/** Grade de álbuns + detalhe do álbum (reproduzir álbum / adicionar à fila). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    state: LibraryUiState,
    currentTrackId: Long?,
    onPlayAlbum: (List<Track>, Int) -> Unit,
    onQueueAlbum: (List<Track>) -> Unit,
    onFavorite: (Track) -> Unit
) {
    var selected by remember { mutableStateOf<Album?>(null) }

    if (selected == null) {
        Scaffold(topBar = { TopAppBar(title = { Text("Álbuns") }) }) { pad ->
            if (state.albums.isEmpty()) {
                Box(Modifier.padding(pad)) {
                    EmptyState(Icons.Filled.Album, "Sem álbuns", "Sincronize a biblioteca primeiro.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(
                        top = (pad.calculateTopPadding() + 8.dp),
                        start = 12.dp, end = 12.dp, bottom = 96.dp
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.albums, key = { it.key }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { selected = album },
                            onPlay = { onPlayAlbum(album.tracks, 0) },
                            onQueue = { onQueueAlbum(album.tracks) }
                        )
                    }
                }
            }
        }
    } else {
        val album = selected!!
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(album.title) },
                    navigationIcon = {
                        IconButton(onClick = { selected = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                        }
                    }
                )
            }
        ) { pad ->
            Column(Modifier.padding(pad)) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CoverArt(album.coverUri, modifier = Modifier.size(120.dp))
                    Column {
                        Text(album.title, style = MaterialTheme.typography.headlineMedium)
                        Text(album.artist, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            buildString {
                                album.year?.let { append("$it · ") }
                                append("${album.tracks.size} faixas · ")
                                append(TechFormatter.duration(album.durationMs))
                                album.dominantFormat?.let { append(" · ${it.label}") }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onPlayAlbum(album.tracks, 0) }) { Text("Reproduzir álbum") }
                            OutlinedButton(onClick = { onQueueAlbum(album.tracks) }) { Text("Fila") }
                        }
                    }
                }
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(album.tracks, key = { it.id }) { t ->
                        TrackRow(
                            track = t,
                            isCurrent = t.id == currentTrackId,
                            isFavorite = t.id in state.favoriteIds,
                            onClick = { onPlayAlbum(album.tracks, album.tracks.indexOf(t)) },
                            onFavorite = { onFavorite(t) },
                            onQueue = { onQueueAlbum(listOf(t)) }
                        )
                    }
                }
            }
        }
    }
}
