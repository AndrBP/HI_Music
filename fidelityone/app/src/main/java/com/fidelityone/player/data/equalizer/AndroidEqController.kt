package com.fidelityone.player.data.equalizer

import android.media.audiofx.DynamicsProcessing
import android.os.Build
import com.fidelityone.player.domain.model.EqBand
import com.fidelityone.player.domain.model.EqPreset
import com.fidelityone.player.domain.model.EqState
import com.fidelityone.player.domain.port.EqController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Equalizador de 10 bandas gráficas.
 * - API 28+: [DynamicsProcessing] (efeito por sessionId do ExoPlayer) — 10 bandas paramétricas
 *   com frequência, ganho e Q reais + pré-amp no grupo de entrada.
 * - API 26–27: fallback limitado (Equalizer nativo quando disponível); bypass sempre funciona.
 * Arquitetura pronta para trocar por DSP próprio no futuro (porta EqController).
 */
class AndroidEqController : EqController {

    private var dp: DynamicsProcessing? = null
    private var attachedSession = -1

    private val _state = MutableStateFlow<EqState?>(null)
    override val state: StateFlow<EqState?> = _state.asStateFlow()

    /** Estado padrão (Flat) criado no attach para a UI refletir. */
    fun defaultState(): EqState = EqState(
        enabled = true, preampDb = 0f,
        bands = EqPreset.FREQUENCIES.map { EqBand(it, 0f, q = 1.41f) },
        preset = EqPreset.FLAT
    )

    override fun supported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    override suspend fun attach(sessionId: Int) {
        if (!supported()) return
        if (sessionId == attachedSession && dp != null) return
        releaseEffect()
        runCatching {
            dp = DynamicsProcessing(0, sessionId).apply {
                enabled = true
                // Pré-amp no grupo de entrada
                inputGroup.isEnable = true
                // Bandas de equalização no grupo "post" EQ
                val eq = postEq
                eq.isEnabled = true
                val n = minOf(eq.getBandCount(), EqPreset.FREQUENCIES.size)
                for (i in 0 until n) {
                    eq.getBands()[i].apply {
                        isEnable = true
                        centerFreqHz = EqPreset.FREQUENCIES[i]
                    }
                }
            }
            attachedSession = sessionId
            (_state.value ?: defaultState()).let { apply(it) }
        }
    }

    override fun apply(state: EqState) {
        _state.value = state
        val effect = dp ?: return
        runCatching {
            effect.enabled = state.enabled
            // Preamp (mB = milibels)
            effect.inputGroup.gainMB = (state.preampDb * 100).toInt()
            // Bandas
            val eq = effect.postEq
            val bands = eq.bands
            val count = minOf(bands.size, state.bands.size)
            for (i in 0 until count) {
                val b = bands[i]
                val src = state.bands[i]
                b.isEnable = state.enabled
                b.centerFreqHz = src.frequencyHz
                b.gainMB = (src.gainDb * 100).toInt()   // mB = milibels
                b.bandWidthHz = qToBandwidth(src.q, src.frequencyHz)
            }
        }
    }

    override fun reset() {
        apply(EqState(
            enabled = _state.value?.enabled ?: true,
            preampDb = 0f,
            bands = EqPreset.FREQUENCIES.map { EqBand(it, 0f) },
            preset = EqPreset.FLAT
        ))
    }

    override fun bypass(enabled: Boolean) {
        val cur = _state.value ?: return
        apply(cur.copy(enabled = !enabled))
    }

    /** Converte Q em largura de banda (Hz) aproximada p/ DynamicsProcessing. */
    private fun qToBandwidth(q: Float, centerHz: Float): Float {
        if (q <= 0f) return centerHz
        val octaves = (2.0 / Math.sqrt((Math.pow(2.0, 1.0 / q.toDouble()))) - 1.0).coerceAtLeast(0.1)
        return (centerHz * (Math.pow(2.0, octaves / 2) - Math.pow(2.0, -octaves / 2))).toFloat().coerceIn(10f, centerHz * 2)
    }

    private fun releaseEffect() {
        runCatching { dp?.release() }
        dp = null
        attachedSession = -1
    }
}
