package com.fidelityone.player.ui.screens.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fidelityone.player.domain.model.Track
import com.fidelityone.player.ui.screens.*
import com.fidelityone.player.viewmodel.LibraryUiState

/** Navegação por pastas do dispositivo (árvore a partir das pastas dos arquivos). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    state: LibraryUiState,
    currentTrackId: Long?,
    onPlay: (List<Track>, Int) -> Unit,
    onQueue: (List<Track>) -> Unit,
    onFavorite: (Track) -> Unit
) {
    var path by remember { mutableStateOf<String?>(null) }   // null = raiz

    val tracksAtRoot = state.tracks
    val folders = state.folders

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(path ?: "Pastas") },
            navigationIcon = {
                if (path != null) IconButton(onClick = {
                    path = path!!.substringBeforeLast('/', "").ifEmpty { null }
                }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
            }
        )
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(bottom = 96.dp)) {
            if (path == null) {
                // árvore de primeiro nível
                val firstLevel = folders.map { it.split("/").filter { s -> s.isNotBlank() }.firstOrNull() ?: "/" }
                    .distinct().sorted()
                items(firstLevel) { name ->
                    ListItem(
                        headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Icon(Icons.Filled.Folder, null) },
                        supportingContent = {
                            Text("${tracksAtRoot.count { it.folder.contains("/$name") }} faixas")
                        },
                        modifier = Modifier.clickable { path = "/$name" }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            } else {
                val p = path!!
                val subFolders = folders.filter { it.startsWith(p) && it != p }
                    .mapNotNull { f ->
                        val rest = f.removePrefix(p).removePrefix("/")
                        rest.split("/").firstOrNull { it.isNotBlank() }?.let { "$p/$it" }
                    }.distinct().sorted()

                items(subFolders) { f ->
                    ListItem(
                        headlineContent = { Text(f.substringAfterLast('/'), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { Icon(Icons.Filled.Folder, null) },
                        supportingContent = { Text("${state.tracks.count { it.folder.startsWith(f) }} faixas") },
                        modifier = Modifier.clickable { path = f }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }

                val directTracks = state.tracks.filter { it.folder == p }
                items(directTracks, key = { it.id }) { t ->
                    TrackRow(
                        track = t,
                        isCurrent = t.id == currentTrackId,
                        isFavorite = t.id in state.favoriteIds,
                        onClick = { onPlay(directTracks, directTracks.indexOf(t)) },
                        onFavorite = { onFavorite(t) },
                        onQueue = { onQueue(listOf(t)) }
                    )
                }
                if (subFolders.isEmpty() && directTracks.isEmpty()) {
                    item { EmptyState(Icons.Filled.Folder, "Pasta vazia", "Nenhum áudio suportado aqui.") }
                }
            }
        }
    }
}
