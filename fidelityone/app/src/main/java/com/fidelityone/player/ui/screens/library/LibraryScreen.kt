package com.fidelityone.player.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fidelityone.player.core.AudioFormat
import com.fidelityone.player.domain.model.Track
import com.fidelityone.player.ui.screens.*
import com.fidelityone.player.viewmodel.LibraryUiState
import com.fidelityone.player.viewmodel.SortBy

/** Tela principal da biblioteca: busca, ordenação, filtro por formato, lista de faixas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    currentTrackId: Long?,
    onQuery: (String) -> Unit,
    onSort: (SortBy) -> Unit,
    onFormatFilter: (AudioFormat?) -> Unit,
    onRescan: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onQueue: (List<Track>) -> Unit,
    onFavorite: (Track) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fidelity One") },
                actions = {
                    IconButton(onClick = onRescan) { Icon(Icons.Filled.Refresh, "Sincronizar biblioteca") }
                    SortMenu(onSort)
                    FormatMenu(onFormatFilter)
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Buscar título, artista ou álbum…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
            if (state.tracks.isEmpty() && !state.scanning) {
                EmptyState(
                    Icons.Filled.LibraryMusic, "Biblioteca vazia",
                    "Toque em sincronizar para escanear o áudio do dispositivo.",
                    "Sincronizar", onRescan
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(state.tracks, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            isFavorite = track.id in state.favoriteIds,
                            onClick = { onPlay(state.tracks, state.tracks.indexOf(track)) },
                            onFavorite = { onFavorite(track) },
                            onQueue = { onQueue(listOf(track)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortMenu(onSort: (SortBy) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.Sort, "Ordenar") }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(text = { Text("Por nome") }, onClick = { onSort(SortBy.TITLE); open = false })
            DropdownMenuItem(text = { Text("Por artista") }, onClick = { onSort(SortBy.ARTIST); open = false })
            DropdownMenuItem(text = { Text("Por álbum") }, onClick = { onSort(SortBy.ALBUM); open = false })
            DropdownMenuItem(text = { Text("Por ano") }, onClick = { onSort(SortBy.YEAR); open = false })
        }
    }
}

@Composable
private fun FormatMenu(onFormatFilter: (AudioFormat?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.FilterAlt, "Filtrar formato") }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem(text = { Text("Todos os formatos") }, onClick = { onFormatFilter(null); open = false })
            Divider()
            AudioFormat.entries.distinctBy { it.label }.forEach { f ->
                DropdownMenuItem(
                    text = { Text(f.label + if (f.lossless) " · lossless" else "") },
                    onClick = { onFormatFilter(f); open = false }
                )
            }
        }
    }
}
