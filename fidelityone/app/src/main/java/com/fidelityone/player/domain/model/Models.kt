package com.fidelityone.player.domain.model

import android.net.Uri
import com.fidelityone.player.core.AudioFormat

/** Faixa de áudio local com metadados reconhecidos (ou fallback). */
data class Track(
    val id: Long,                          // MediaStore id ou hash do caminho
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String?,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val composer: String?,
    val comment: String?,
    val durationMs: Long,
    val format: AudioFormat?,
    val sampleRateHz: Int?,
    val bitDepth: Int?,
    val fileSizeBytes: Long?,
    val folder: String,                    // pasta de origem (fallback/ordenação)
    val embeddedArtAvailable: Boolean,
    val externalArtUri: Uri? = null,       // capa na pasta (cover.jpg/front.png...)
    val replayGainTrackDb: Float? = null,
    val replayGainAlbumDb: Float? = null
) {
    val bitrateEstimateBps: Long? get() =
        if (fileSizeBytes != null && durationMs > 0) fileSizeBytes * 1000 / durationMs else null
}

data class Album(
    val key: String,                       // album|albumArtist (normalizado)
    val title: String,
    val artist: String,
    val year: Int?,
    val genre: String?,
    val coverUri: Uri?,
    val tracks: List<Track>
) {
    val durationMs: Long get() = tracks.sumOf { it.durationMs }
    val dominantFormat: AudioFormat? get() = tracks.mapNotNull { it.format }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
}

data class Artist(
    val name: String,
    val albums: List<Album>,
    val tracks: List<Track>,
    val coverUri: Uri?
)

/** Estado de reprodução observado pela UI. */
data class PlaybackState(
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val bufferedMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = -1,
    // Infos técnicas em tempo real vindas do decoder (Media3 Format)
    val liveSampleRateHz: Int? = null,
    val liveBitDepth: Int? = null,
    val liveCodec: String? = null,
    val dsdFallbackToPcm: Boolean = false
)

enum class RepeatMode { NONE, ONE, ALL }

/* ── Equalizador ─────────────────────────────────────────────── */

data class EqBand(val frequencyHz: Float, var gainDb: Float, val q: Float = 1.0f)

enum class EqPreset(val label: String) {
    FLAT("Flat"), BASS_BOOST("Bass Boost"), VOCAL("Vocal"), JAZZ("Jazz"),
    CLASSICAL("Classical"), ROCK("Rock"), ELECTRONIC("Electronic"),
    ACOUSTIC("Acoustic"), WARM("Warm"), BRIGHT("Bright"), CUSTOM("Custom");

    companion object {
        /** Curvas de ganho (dB) por banda — 10 bandas gráficas padrão. */
        val FREQUENCIES = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

        fun gains(preset: EqPreset): FloatArray = when (preset) {
            FLAT -> floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            BASS_BOOST -> floatArrayOf(6f, 5f, 4f, 2f, 0f, 0f, -1f, -1f, 0f, 0f)
            VOCAL -> floatArrayOf(-2f, -1f, 0f, 2f, 4f, 4f, 3f, 1f, 0f, -1f)
            JAZZ -> floatArrayOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f)
            CLASSICAL -> floatArrayOf(3f, 2f, 1f, 0f, 0f, 0f, 1f, 2f, 3f, 4f)
            ROCK -> floatArrayOf(4f, 3f, 1f, -1f, -1f, 1f, 2f, 3f, 4f, 3f)
            ELECTRONIC -> floatArrayOf(5f, 4f, 1f, 0f, -1f, 1f, 2f, 4f, 5f, 4f)
            ACOUSTIC -> floatArrayOf(2f, 3f, 2f, 1f, 0f, 1f, 2f, 2f, 1f, 0f)
            WARM -> floatArrayOf(3f, 2f, 1f, 1f, 0f, -1f, -2f, -2f, -1f, 0f)
            BRIGHT -> floatArrayOf(-1f, -1f, 0f, 0f, 1f, 2f, 3f, 4f, 5f, 4f)
            CUSTOM -> floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        }
    }
}

data class EqState(
    val enabled: Boolean = true,           // bypass quando false
    val preampDb: Float = 0f,
    val bands: List<EqBand>,
    val preset: EqPreset = EqPreset.FLAT,
    val customName: String? = null
)

/* ── Temas / Layouts ─────────────────────────────────────────── */

enum class ColorTheme(val id: String, val label: String) {
    ONYX_DARK("onyx_dark", "Onyx Dark"),
    IVORY_LIGHT("ivory_light", "Ivory Light"),
    MIDNIGHT_BLUE("midnight_blue", "Midnight Blue"),
    GRAPHITE_GOLD("graphite_gold", "Graphite Gold")
}

enum class LayoutBase(val id: String, val label: String) {
    CLASSIC_LIBRARY("classic_library", "Classic Library"),
    MINIMAL_PLAYER("minimal_player", "Minimal Player"),
    AUDIOPHILE_DASHBOARD("audiophile_dashboard", "Audiophile Dashboard"),
    EDITORIAL_PREMIUM("editorial_premium", "Editorial Premium"),
    COMPACT_HIFI("compact_hifi", "Compact Hi-Fi")
}

/** Preset visual = layout × cor (5 × 4 = 20). */
data class VisualPreset(val layout: LayoutBase, val theme: ColorTheme) {
    val id: String get() = "${layout.id}__${theme.id}"
    val label: String get() = "${layout.label} · ${theme.label}"
}

/* ── Configurações persistidas ───────────────────────────────── */

data class AppSettings(
    val preset: VisualPreset = VisualPreset(LayoutBase.CLASSIC_LIBRARY, ColorTheme.ONYX_DARK),
    val crossfadeMs: Int = 0,              // 0 = desligado; até 12000
    val replayGainEnabled: Boolean = false,
    val replayGainSourceAlbum: Boolean = false,
    val gapless: Boolean = true,
    val sleepTimerMinutes: Int = 0         // 0 = desligado
)
