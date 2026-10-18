package com.fidelityone.player.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.fidelityone.player.AppContainer
import com.fidelityone.player.FidelityOneApp
import com.fidelityone.player.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val container: AppContainer = (app as FidelityOneApp).container

    val settings: StateFlow<AppSettings> =
        container.settings.settings.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings()
        )

    fun setPreset(preset: VisualPreset) = save { it.copy(preset = preset) }
    fun setLayout(layout: LayoutBase) = save { it.copy(preset = it.preset.copy(layout = layout)) }
    fun setColorTheme(theme: ColorTheme) = save { it.copy(preset = it.preset.copy(theme = theme)) }
    fun setCrossfade(ms: Int) = save { it.copy(crossfadeMs = ms.coerceIn(0, 12_000)) }
    fun setReplayGain(enabled: Boolean) = save { it.copy(replayGainEnabled = enabled) }
    fun setReplayGainAlbum(album: Boolean) = save { it.copy(replayGainSourceAlbum = album) }
    fun setGapless(enabled: Boolean) = save { it.copy(gapless = enabled) }

    private fun save(transform: (AppSettings) -> AppSettings) = viewModelScope.launch {
        val current = settings.value
        val next = transform(current)
        container.settings.save(next)
        // aplica imediatamente no engine onde possível
        container.player.setCrossfadeSeconds(next.crossfadeMs / 1000f)
    }
}
