package com.fidelityone.player.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.fidelityone.player.AppContainer
import com.fidelityone.player.FidelityOneApp
import com.fidelityone.player.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EqualizerUiState(
    val supported: Boolean = true,
    val state: EqState = EqState(
        enabled = true, preampDb = 0f,
        bands = EqPreset.FREQUENCIES.map { EqBand(it, 0f, q = 1.41f) }
    ),
    val customPresets: List<Pair<String, FloatArray>> = emptyList()
)

class EqualizerViewModel(app: Application) : AndroidViewModel(app) {

    private val container: AppContainer = (app as FidelityOneApp).container
    private val eq get() = container.equalizer

    private val _state = MutableStateFlow(
        EqualizerUiState(supported = eq.supported())
    )
    val ui: StateFlow<EqualizerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // restaura último estado salvo do EQ
            container.settings.eqState().filterNotNull().collect { saved ->
                _state.value = _state.value.copy(state = saved)
                eq.apply(saved)
            }
        }
        viewModelScope.launch {
            container.settings.customPresets().collect { list ->
                _state.value = _state.value.copy(customPresets = list)
            }
        }
    }

    /** Anexa o efeito à sessão de áudio do player quando disponível. */
    fun attachToSession(sessionId: Int) = viewModelScope.launch {
        eq.attach(sessionId)
    }

    fun selectPreset(preset: EqPreset) {
        val gains = EqPreset.gains(preset)
        update(_state.value.state.copy(
            bands = EqPreset.FREQUENCIES.mapIndexed { i, f -> EqBand(f, gains.getOrElse(i) { 0f }, q = 1.41f) },
            preset = preset, customName = null
        ))
    }

    fun setBandGain(index: Int, gainDb: Float) {
        val bands = _state.value.state.bands.toMutableList()
        bands[index] = bands[index].copy(gainDb = gainDb.coerceIn(-15f, 15f))
        update(_state.value.state.copy(bands = bands, preset = EqPreset.CUSTOM, customName = null))
    }

    fun setBandQ(index: Int, q: Float) {
        val bands = _state.value.state.bands.toMutableList()
        bands[index] = bands[index].copy(q = q.coerceIn(0.3f, 12f))
        update(_state.value.state.copy(bands = bands))
    }

    fun setPreamp(db: Float) =
        update(_state.value.state.copy(preampDb = db.coerceIn(-15f, 15f)))

    fun toggleBypass() = update(_state.value.state.copy(enabled = !_state.value.state.enabled))

    fun reset() {
        update(EqState(
            enabled = _state.value.state.enabled, preampDb = 0f,
            bands = EqPreset.FREQUENCIES.map { EqBand(it, 0f, q = 1.41f) },
            preset = EqPreset.FLAT
        ))
    }

    fun saveCustom(name: String) = viewModelScope.launch {
        val s = _state.value.state
        container.settings.saveCustomPreset(name, s.bands.map { it.gainDb }.toFloatArray(), s.preampDb)
    }

    fun applyCustom(name: String, gains: FloatArray) {
        update(_state.value.state.copy(
            bands = EqPreset.FREQUENCIES.mapIndexed { i, f -> EqBand(f, gains.getOrElse(i) { 0f }, 1.41f) },
            preset = EqPreset.CUSTOM, customName = name
        ))
    }

    private fun update(state: EqState) {
        _state.value = _state.value.copy(state = state)
        eq.apply(state)
        viewModelScope.launch { container.settings.saveEqState(state) }
    }
}
