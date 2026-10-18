package com.fidelityone.player.data.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.fidelityone.player.domain.model.*
import com.fidelityone.player.domain.port.AudioPlayerController
import com.fidelityone.player.service.PlaybackService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Engine de reprodução do lado da UI: conecta como MediaController ao PlaybackService
 * (ExoPlayer vive no serviço → notificação/tela de bloqueio e continuidade em background).
 * Gapless: nativo do ExoPlayer (mesmo renderer entre itens consecutivos).
 * Crossfade opcional: implementado como fade-out/fade-in no transition (config crossfadeMs).
 */
class Media3PlaybackEngine(private val context: Context) : AudioPlayerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /** Mapa mediaId -> Track para infos técnicas e fila. */
    internal val trackIndex = mutableMapOf<String, Track>()

    private var pollJob: Job? = null
    private var crossfadeSec = 0f

    /** Sessão de áudio do player — usada para anexar o equalizador (DynamicsProcessing). */
    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val lf = MediaController.Builder(context, token).buildAsync()
        lf.addListener { _, l ->
            if (l.isDone && !l.isCancelled) {
                controller = l.get().apply {
                    addListener(playerListener)
                    _audioSessionId.value = audioSessionId
                    _state.value = _state.value.copy(
                        shuffle = shuffleModeEnabled,
                        repeatMode = mapRepeat(repeatMode)
                    )
                    startPolling()
                }
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refreshFromPlayer()
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                val c = controller
                if (c != null && c.isPlaying) {
                    _state.value = _state.value.copy(positionMs = c.currentPosition.coerceAtLeast(0))
                }
                delay(250)
            }
        }
    }

    private fun refreshFromPlayer() {
        val c = controller ?: return
        val item = c.currentMediaItem
        val id = item?.mediaId
        val track = id?.let { trackIndex[it] }
        _state.value = _state.value.copy(
            current = track,
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0),
            bufferedMs = c.bufferedPosition.coerceAtLeast(0),
            durationMs = if (track != null) track.durationMs else item?.durationMs ?: 0,
            queue = (0 until c.mediaItemCount).mapNotNull { i ->
                trackIndex[c.getMediaItemAt(i).mediaId]
            },
            queueIndex = c.currentMediaItemIndex,
            shuffle = c.shuffleModeEnabled,
            repeatMode = mapRepeat(c.repeatMode)
        )
    }

    /** Recebe infos técnicas em tempo real vindas do DecoderFactory do serviço. */
    internal fun onLiveFormat(sampleRateHz: Int?, bitDepth: Int?, codec: String?, dsdFallback: Boolean) {
        _state.value = _state.value.copy(
            liveSampleRateHz = sampleRateHz,
            liveBitDepth = bitDepth,
            liveCodec = codec,
            dsdFallbackToPcm = dsdFallback
        )
    }

    private fun mapRepeat(r: Int): RepeatMode = when (r) {
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        else -> RepeatMode.NONE
    }

    /* ── API ─────────────────────────────────────────────────── */

    override fun playTracks(tracks: List<Track>, startIndex: Int) {
        val c = controller ?: return
        tracks.forEach { trackIndex[trackKey(it)] = it }
        val items = tracks.map {
            MediaItem.Builder()
                .setUri(it.uri)
                .setMediaId(trackKey(it))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(it.title).setArtist(it.artist).setAlbumTitle(it.album)
                        .setArtworkUri(it.externalArtUri)
                        .build()
                )
                .build()
        }
        c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0)
        if (crossfadeSec > 0f) applyCrossfadeListeners(c)
        c.prepare()
        c.play()
    }

    override fun playPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    override fun next() { controller?.seekToNextMediaItem() }
    override fun previous() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPreviousMediaItem()
    }
    override fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    override fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
        _state.value = _state.value.copy(shuffle = enabled)
    }

    override fun setRepeatMode(mode: RepeatMode) {
        controller?.repeatMode = when (mode) {
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            NONE -> Player.REPEAT_MODE_OFF
        }
        _state.value = _state.value.copy(repeatMode = mode)
    }

    override fun addToQueue(tracks: List<Track>) {
        val c = controller ?: return
        tracks.forEach { trackIndex[trackKey(it)] = it }
        c.addMediaItems(tracks.map {
            MediaItem.Builder().setUri(it.uri).setMediaId(trackKey(it)).build()
        })
    }

    override fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    override fun moveInQueue(from: Int, to: Int) {
        val c = controller ?: return
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount)
            c.moveMediaItemRange(from, 1, to)
    }

    override fun jumpToQueueItem(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) { c.seekTo(index, 0); c.play() }
    }

    override fun setCrossfadeSeconds(sec: Float) {
        crossfadeSec = sec.coerceIn(0f, 12f)
        controller?.let { applyCrossfadeListeners(it) }
    }

    /** Crossfade simples: fade-out no fim + fade-in no início da próxima faixa. */
    private fun applyCrossfadeListeners(c: MediaController) {
        if (crossfadeSec <= 0f) { c.volume = 1f; return }
        // ExoPlayer não tem crossfade nativo entre itens; simulamos com volume ramp.
        scope.launch {
            while (isActive) {
                val dur = c.currentMediaItem?.durationMs ?: 0
                val pos = c.currentPosition
                if (dur > 0 && pos in (dur - crossfadeSec * 1000).toLong()..dur) {
                    val t = ((dur - pos) / (crossfadeSec * 1000)).coerceIn(0f, 1f)
                    c.volume = t
                } else if (pos < crossfadeSec * 1000) {
                    c.volume = (pos / (crossfadeSec * 1000)).coerceIn(0f, 1f)
                } else c.volume = 1f
                delay(60)
            }
        }
    }

    override fun release() {
        pollJob?.cancel(); scope.cancel()
        controller?.release(); controller = null
    }

    companion object {
        fun trackKey(t: Track) = "fid://${t.id}"
    }
}
