package com.fidelityone.player.data.metadata

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.fidelityone.player.core.AudioFormat
import com.fidelityone.player.core.displayTitleFromFileName
import com.fidelityone.player.domain.model.Track
import com.fidelityone.player.domain.port.MetadataReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Leitor de metadados baseado em MediaMetadataRetriever (framework Android).
 * Cobre ID3v1/ID3v2, Vorbis Comments, FLAC metadata, MP4 tags (AAC/M4A) e
 * metadados básicos de WAV/AIFF/DSF que o framework expõe.
 * Fallback: nome do arquivo + estrutura de pastas.
 */
class Media3MetadataReader : MetadataReader {

    override suspend fun read(context: Context, uri: Uri): Track? = withContext(Dispatchers.IO) {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)

            val meta = MediaMetadataRetriever.METADATA_KEY_TITLE.let { r.extractMetadata(it) }
            val mime = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val displayName = uri.lastPathSegment?.substringAfterLast('/') 
                ?: r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISPLAY_NAME)

            val format = AudioFormat.fromMime(mime) ?: AudioFormat.fromExtension(displayName)

            val durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val sampleRate = extractSampleRate(r)
            val bitDepth = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.let { br ->
                // bitrate não é bit depth; estimamos bit depth apenas para PCM óbvio via chave específica quando existir
                if (br > 0 && (format == AudioFormat.WAV || format == AudioFormat.AIFF)) null else null
            }
            val bitsPerSample = extractBitsPerSample(r)

            val title = meta ?: displayTitleFromFileName(displayName)
            val artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                ?: "Artista desconhecido"
            val album = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?: r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.let { "Faixas de ${displayName?.substringBeforeLast('/')?.substringAfterLast('/', "")}" }
                ?: "Álbum desconhecido"
            val albumArtist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST) ?: artist
            val genre = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            val year = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.take(4)?.toIntOrNull()
            val trackNo = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.trim()?.toIntOrNull()
            val discNo = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
                ?.substringBefore('/')?.trim()?.toIntOrNull()
            val composer = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
            val comment = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMMENT)
            val hasArt = r.getEmbeddedPicture() != null

            // ReplayGain (Vorbis/ID3 TXXX/RG) — o framework nem sempre expõe; melhor sorte via scanner
            val rgTrack = extractReplayGain(r, "replaygain_track_gain")
            val rgAlbum = extractReplayGain(r, "replaygain_album_gain")

            val folder = displayName?.substringBeforeLast('/', "")?.ifEmpty { "/" } ?: "/"

            Track(
                id = uri.toString().stableId(),
                uri = uri,
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                genre = genre,
                year = year,
                trackNumber = trackNo,
                discNumber = discNo,
                composer = composer,
                comment = comment,
                durationMs = durationMs,
                format = format,
                sampleRateHz = sampleRate,
                bitDepth = bitsPerSample ?: bitDepth,
                fileSizeBytes = null, // preenchido pelo scanner (MediaStore)
                folder = folder,
                embeddedArtAvailable = hasArt,
                replayGainTrackDb = rgTrack,
                replayGainAlbumDb = rgAlbum
            )
        } catch (_: Exception) {
            null // arquivo ilegível/não suportado nesta ROM
        } finally {
            try { r.release() } catch (_: Exception) {}
        }
    }

    override suspend fun artWork(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri); r.getEmbeddedPicture()
        } catch (_: Exception) { null } finally { try { r.release() } catch (_: Exception) {} }
    }

    private fun extractSampleRate(r: MediaMetadataRetriever): Int? {
        // METADATA_KEY_SAMPLE_RATE existe desde API 30; abaixo, tenta por reflection-safe
        return try {
            if (android.os.Build.VERSION.SDK_INT >= 30)
                r.extractMetadata(10005 /* METADATA_KEY_SAMPLE_RATE in some SDKs */) ?.toIntOrNull()
                    ?: r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MAX_BITRATE)?.toIntOrNull()?.let { null }
            else null
        } catch (_: Exception) { null }
    }

    private fun extractBitsPerSample(r: MediaMetadataRetriever): Int? = try {
        if (android.os.Build.VERSION.SDK_INT >= 30)
            r.extractMetadata(10006)?.toIntOrNull() else null
    } catch (_: Exception) { null }

    private fun extractReplayGain(r: MediaMetadataRetriever, key: String): Float? = try {
        r.extractMetadata(key)?.trimEnd('d', 'B', ' ')?.toFloatOrNull()
    } catch (_: Exception) { null }
}

/** Id determinístico a partir da URI (para ids fora do MediaStore). */
fun String.stableId(): Long {
    var h = 1125899906842597L
    for (c in this) h = 31 * h + c.code
    return h
}
