package com.fidelityone.player.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.fidelityone.player.MainActivity

/**
 * Serviço de reprodução (MediaSessionService). O ExoPlayer vive aqui:
 * - notificação + tela de bloqueio + controles de mídia externos
 * - gapless nativo (mesmo renderer entre itens consecutivos)
 * - buffer grande (8 MB) para estabilidade em lossless de alto bitrate
 * - DSD (DSF/DFF): decoder embutido do Media3 converte para PCM quando o
 *   hardware não suporta DSD nativo — a UI reporta "DSD → PCM (fallback)".
 * - AnalyticsListener expõe sample-rate/canal info do Format real em uso.
 */
class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    /** Callback opcional instalado pelo engine da UI para receber infos técnicas ao vivo. */
    var liveFormatListener: ((sampleRateHz: Int?, bitDepthBits: Int?, codec: String?, dsdFallback: Boolean) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()

        // Buffer generoso para arquivos lossless grandes
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 60_000, 3_000, 5_000)
            .setTargetBufferBytes(8 * 1024 * 1024)
            .build()

        val exo = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)   // pausa se fone desconecta
            .setSkipSilenceEnabled(false)         // mantém transições naturais (gapless-friendly)
            .setLoadControl(loadControl)
            .build()

        exo.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus = */ true
        )

        exo.addAnalyticsListener(object : AnalyticsListener {
            override fun onVideoEnabled(context: AnalyticsListener.EventSource, format: androidx.media3.common.Format) {}
            override fun onAudioInputFormatChanged(
                eventSource: AnalyticsListener.EventSource,
                format: androidx.media3.common.Format,
                counters: DecoderCounters
            ) {
                val isDsdSource = format.containerMimeType?.contains("dsf", true) == true ||
                        format.containerMimeType?.contains("dff", true) == true ||
                        format.sampleMimeType?.contains("dsd", true) == true
                liveFormatListener?.invoke(
                    format.sampleRate?.takeIf { it > 0 },
                    format.pcmEncoding.takeIf { it != C.ENCODING_INVALID }?.let { pcmEncodingBits(it) },
                    format.codecs ?: format.sampleMimeType,
                    isDsdSource // reproduzido via conversão PCM interna do decoder
                )
            }
        })

        player = exo

        val sessionIntent = PendingIntent.getActivity(
            this, 0, android.content.Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exo)
            .setSessionActivity(sessionIntent)
            .build()
    }

    private fun pcmEncodingBits(encoding: Int): Int? = when (encoding) {
        C.ENCODING_PCM_16BIT, C.ENCODING_PCM_16BIT_BIG -> 16
        C.ENCODING_PCM_24BIT_PACKED -> 24
        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
        else -> null
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
            player = null
        }
        super.onDestroy()
    }
}
