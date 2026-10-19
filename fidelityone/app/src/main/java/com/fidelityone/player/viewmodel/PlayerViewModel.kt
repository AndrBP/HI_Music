package com.fidelityone.player.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.fidelityone.player.AppContainer
import com.fidelityone.player.FidelityOneApp
import com.fidelityone.player.domain.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlayerUiState(
    val playback: PlaybackState = PlaybackState(),
    val isFavorite: Boolean = false,
    val sleepRemainingMs: Long = 0L
)

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val container: AppContainer = (app as FidelityOneApp).container
    private val player get() = container.player
    private val library get() = container.library

    private val _favorite = MutableStateFlow(false)
    private val _sleepRemaining = MutableStateFlow(0L)
    private var sleepJob: Job? = null

    val ui: StateFlow<PlayerUiState> = combine(
        player.state, _favorite, _sleepRemaining
    ) { pb, fav, sleep ->
        PlayerUiState(pb, fav || (pb.current?.let { it.id in favIdsCache } ?: false), sleep)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUiState())

    private var favIdsCache: Set<Long> = emptySet()

    init {
        // cache de favoritos para o botão da tela do player
        viewModelScope.launch {
            library.favorites.collect { favs -> favIdsCache = favs.map { it.id }.toSet() }
        }
        // restaura/configura crossfade e sleep timer das configurações
        viewModelScope.launch {
            container.settings.settings.collect { s ->
                player.setCrossfadeSeconds(s.crossfadeMs / 1000f)
                if (s.sleepTimerMinutes > 0 && sleepJob == null) startSleepTimer(s.sleepTimerMinutes)
            }
        }
    }

    /* ── Ações da UI ─────────────────────────────────────────── */

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) = player.playTracks(tracks, startIndex)
    fun playPause() = player.playPause()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(ms: Long) = player.seekTo(ms)
    fun toggleShuffle() = player.setShuffle(!player.state.value.shuffle)

    fun cycleRepeat() {
        val next = when (player.state.value.repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        player.setRepeatMode(next)
    }

    fun addToQueue(tracks: List<Track>) = player.addToQueue(tracks)
    fun removeFromQueue(index: Int) = player.removeFromQueue(index)
    fun jumpToQueueItem(index: Int) = player.jumpToQueueItem(index)

    fun toggleFavoriteCurrent() {
        val t = player.state.value.current ?: return
        viewModelScope.launch {
            val nowFav = t.id in favIdsCache
            library.setFavorite(t.id, !nowFav)
            _favorite.value = !nowFav
        }
    }

    /** Sleep timer básico: pausa após N minutos (contagem regressiva na UI). */
    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) { _sleepRemaining.value = 0; return }
        viewModelScope.launch {
            container.settings.save(currentSettings().copy(sleepTimerMinutes = minutes))
        }
        val totalMs = minutes * 60_000L
        _sleepRemaining.value = totalMs
        sleepJob = viewModelScope.launch {
            var left = totalMs
            while (left > 0) { delay(1_000); left -= 1_000; _sleepRemaining.value = left }
            if (player.state.value.isPlaying) player.playPause()   // pausa ao fim do timer
            container.settings.save(currentSettings().copy(sleepTimerMinutes = 0))
            _sleepRemaining.value = 0
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel(); sleepJob = null; _sleepRemaining.value = 0
        viewModelScope.launch { container.settings.save(currentSettings().copy(sleepTimerMinutes = 0)) }
    }

    private suspend fun currentSettings() = container.settings.settings.first()
}
