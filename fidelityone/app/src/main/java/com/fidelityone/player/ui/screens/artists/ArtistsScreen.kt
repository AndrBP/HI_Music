package com.fidelityone.player.ui.screens.artists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fidelityone.player.domain.model.Artist
import com.fidelityone.player.domain.model.Track
import com.fidelityone.player.ui.screens.*
import com.fidelityone.player.viewmodel.LibraryUiState

/** Lista de artistas → discografia (álbuns) + faixas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    state: LibraryUiState,
    currentTrackId: Long?,
    onPlay: (List<Track>, Int) -> Unit,
    onQueue: (List<Track>) -> Unit,
    onFavorite: (Track) -> Unit
) {
    var selected by remember { mutableStateOf<Artist?>(null) }

    if (selected == null) {
        Scaffold(topBar = { TopAppBar(title = { Text("Artistas") }) }) { pad ->
            if (state.artists.isEmpty()) {
                Box(Modifier.padding(pad)) {
                    EmptyState(Icons.Filled.Person, "Sem artistas", "Sincronize a biblioteca primeiro.")
                }
            } else {
                LazyColumn(Modifier.padding(pad)) {
                    items(state.artists, key = { it.name }) { a ->
                        ListItem(
                            headlineContent = { Text(a.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text("${a.albums.size} álbuns · ${a.tracks.size} faixas") },
                            leadingContent = { CoverArt(a.coverUri, modifier = Modifier.size(48.dp)) },
                            modifier = Modifier.clickable { selected = a }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    } else {
        val artist = selected!!
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(artist.name) },
                    navigationIcon = {
                        IconButton(onClick = { selected = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                    }
                )
            }
        ) { pad ->
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(bottom = 96.dp)) {
                item { SectionHeader("Discografia") }
                items(artist.albums, key = { it.key }) { album ->
                    ListItem(
                        headlineContent = { Text(album.title) },
                        supportingContent = { Text("${album.tracks.size} faixas" + (album.year?.let { " · $it" } ?: "")) },
                        leadingContent = { CoverArt(album.coverUri, modifier = Modifier.size(56.dp)) },
                        trailingContent = {
                            IconButton(onClick = { onPlay(album.tracks, 0) }) {
                                Icon(Icons.Filled.PlayArrow, "Reproduzir")
                            }
                        },
                        modifier = Modifier.clickable { onPlay(album.tracks, 0) }
                    )
                }
                item { SectionHeader("Faixas") }
                items(artist.tracks, key = { "${it.id}-t" }) { t ->
                    TrackRow(
                        track = t,
                        isCurrent = t.id == currentTrackId,
                        isFavorite = t.id in state.favoriteIds,
                        onClick = { onPlay(artist.tracks, artist.tracks.indexOf(t)) },
                        onFavorite = { onFavorite(t) },
                        onQueue = { onQueue(listOf(t)) }
                    )
                }
            }
        }
    }
}
