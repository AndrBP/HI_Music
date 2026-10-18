package com.fidelityone.player.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.fidelityone.player.AppContainer
import com.fidelityone.player.FidelityOneApp
import com.fidelityone.player.core.AudioFormat
import com.fidelityone.player.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortBy { TITLE, ARTIST, ALBUM, YEAR }

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<String> = emptyList(),
    val favorites: List<Track> = emptyList(),
    val query: String = "",
    val sort: SortBy = SortBy.TITLE,
    val formatFilter: AudioFormat? = null,
    val scanning: Boolean = false,
    val foundCount: Int = 0,
    val favoriteIds: Set<Long> = emptySet()
)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val container: AppContainer = (app as FidelityOneApp).container

    private val _query = MutableStateFlow("")
    private val _sort = MutableStateFlow(SortBy.TITLE)
    private val _formatFilter = MutableStateFlow<AudioFormat?>(null)

    /** Biblioteca completa reagrupada em álbuns/artistas/pastas + filtros da UI. */
    val ui: StateFlow<LibraryUiState> = combine(
        container.library.tracks,      // Flow<List<Track>>
        container.library.favorites,   // Flow<List<Track>>
        _query, _sort, _formatFilter
    ) { tracks, favs, q, sort, fmt ->
        val filtered = tracks.filter { t ->
            (fmt == null || t.format == fmt) &&
                (q.isBlank() || t.title.contains(q, true) ||
                        t.artist.contains(q, true) || t.album.contains(q, true))
        }.let { list ->
            when (sort) {
                SortBy.TITLE -> list.sortedBy { it.title.lowercase() }
                SortBy.ARTIST -> list.sortedWith(compareBy({ it.artist.lowercase() }, { it.album.lowercase() }, { it.trackNumber ?: 0 }))
                SortBy.ALBUM -> list.sortedWith(compareBy({ it.album.lowercase() }, { it.discNumber ?: 0 }, { it.trackNumber ?: 0 }))
                SortBy.YEAR -> list.sortedByDescending { it.year ?: 0 }
            }
        }

        val albums = tracks.groupBy { "${it.album}|${it.albumArtist}" }.map { (key, ts) ->
            Album(
                key = key,
                title = ts.first().album,
                artist = ts.first().albumArtist,
                year = ts.mapNotNull { it.year }.maxOrNull(),
                genre = ts.firstNotNullOfOrNull { it.genre },
                coverUri = ts.firstNotNullOfOrNull { it.externalArtUri },
                tracks = ts.sortedWith(compareBy({ it.discNumber ?: 0 }, { it.trackNumber ?: Int.MAX_VALUE }))
            )
        }.sortedBy { it.title.lowercase() }

        val artists = tracks.groupBy { it.albumArtist }.map { (name, ts) ->
            Artist(
                name = name,
                albums = albums.filter { it.artist == name },
                tracks = ts,
                coverUri = ts.firstNotNullOfOrNull { it.externalArtUri }
            )
        }.sortedBy { it.name.lowercase() }

        val folders = tracks.map { it.folder }.distinct().sorted()

        LibraryUiState(
            tracks = filtered,
            albums = albums,
            artists = artists,
            folders = folders,
            favorites = favs,
            query = q,
            sort = sort,
            formatFilter = fmt,
            favoriteIds = favs.map { it.id }.toSet()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init { scanIfNeeded() }

    fun setQuery(q: String) { _query.value = q }
    fun setSort(s: SortBy) { _sort.value = s }
    fun setFormatFilter(f: AudioFormat?) { _formatFilter.value = f }

    fun scan() = viewModelScope.launch {
        container.library.scan(getApplication())
    }

    private fun scanIfNeeded() = viewModelScope.launch {
        if (container.library.tracks.first().isEmpty()) container.library.scan(getApplication())
    }

    fun toggleFavorite(track: Track) = viewModelScope.launch {
        val nowFav = ui.value.favoriteIds.contains(track.id)
        container.library.setFavorite(track.id, !nowFav)
    }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
                modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(app) as T
                modelClass.isAssignableFrom(PlayerViewModel::class.java) -> PlayerViewModel(app) as T
                modelClass.isAssignableFrom(EqualizerViewModel::class.java) -> EqualizerViewModel(app) as T
                modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(app) as T
                else -> error("Unknown VM $modelClass")
            }
        }
    }
}
