package com.fidelityone.player.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.fidelityone.player.domain.model.Track
import com.fidelityone.player.ui.screens.*
import com.fidelityone.player.viewmodel.LibraryUiState

/** Favoritos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    state: LibraryUiState,
    currentTrackId: Long?,
    onPlay: (List<Track>, Int) -> Unit,
    onQueue: (List<Track>) -> Unit,
    onFavorite: (Track) -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Favoritos") }) }) { pad ->
        if (state.favorites.isEmpty()) {
            Box(Modifier.padding(pad)) {
                EmptyState(Icons.Filled.Favorite, "Sem favoritos",
                    "Toque no coração em qualquer faixa para favoritar.")
            }
        } else {
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(state.favorites, key = { it.id }) { t ->
                    TrackRow(
                        track = t,
                        isCurrent = t.id == currentTrackId,
                        isFavorite = true,
                        onClick = { onPlay(state.favorites, state.favorites.indexOf(t)) },
                        onFavorite = { onFavorite(t) },
                        onQueue = { onQueue(listOf(t)) }
                    )
                }
            }
        }
    }
}
