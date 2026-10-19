package com.fidelityone.player.domain.port

import android.content.Context
import android.net.Uri
import com.fidelityone.player.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Leitura de metadados (ID3v1/v2, Vorbis, FLAC, MP4, WAV/AIFF/DSF básicos via Media3). */
interface MetadataReader {
    suspend fun read(context: Context, uri: Uri): Track?
    suspend fun artWork(context: Context, uri: Uri): ByteArray?
}

/** Repositório da biblioteca local. */
interface LibraryRepository {
    val tracks: Flow<List<Track>>
    val favorites: Flow<List<Track>>
    suspend fun scan(context: Context): ScanProgress
    fun observeScanning(): Flow<ScanProgress>
    suspend fun setFavorite(trackId: Long, favorite: Boolean)
    suspend fun isFavorite(trackId: Long): Boolean
}

data class ScanProgress(val done: Boolean = true, val found: Int = 0, val total: Int = 0)

/** Controle de reprodução (implementado pelo engine Media3 via MediaController). */
interface AudioPlayerController {
    val state: StateFlow<PlaybackState>
    fun playTracks(tracks: List<Track>, startIndex: Int = 0)
    fun playPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun setShuffle(enabled: Boolean)
    fun setRepeatMode(mode: RepeatMode)
    fun addToQueue(tracks: List<Track>)
    fun removeFromQueue(index: Int)
    fun moveInQueue(from: Int, to: Int)
    fun jumpToQueueItem(index: Int)
    fun setCrossfadeSeconds(sec: Float)
    fun release()
}

/** Controle do equalizador. */
interface EqController {
    val state: StateFlow<EqState?>
    suspend fun attach(sessionId: Int)
    fun apply(state: EqState)
    fun reset()
    fun bypass(enabled: Boolean)
    fun supported(): Boolean
}

/** Persistência de configurações. */
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun save(settings: AppSettings)
    suspend fun saveCustomPreset(name: String, gains: FloatArray, preamp: Float)
    fun customPresets(): Flow<List<Pair<String, FloatArray>>>
}
