package com.fidelityone.player.data.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.fidelityone.player.core.AudioFormat
import com.fidelityone.player.core.displayTitleFromFileName
import com.fidelityone.player.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Escaneia o MediaStore (scoped storage — apenas leitura de áudio, sem permissões extras).
 * Usa colunas de metadados do próprio MediaStore (rápido) e preenche fallbacks por nome/pasta.
 * DSF/DFF podem não estar indexados em todas as ROMs; nesses casos use a aba Pastas
 * (SAF/document tree) ou "Abrir com".
 */
class MediaStoreScanner(private val context: Context) {

    data class Raw(val track: Track, val isFavoriteColumn: Boolean = false)

    suspend fun scan(): List<Track> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.IS_MUSIC
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sort = "${MediaStore.Audio.Media.ARTIST} ASC, ${MediaStore.Audio.Media.ALBUM} ASC, " +
                   "${MediaStore.Audio.Media.TRACK} ASC"

        val out = mutableListOf<Track>()
        context.contentResolver.query(collection, projection, selection, null, sort)?.use { c ->
            val idxId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val idxTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val idxArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val idxAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val idxAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val idxGenre = c.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)
            val idxYear = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val idxTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val idxDisc = c.runCatching { c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISC_NUMBER) }.getOrNull()
            val idxComposer = c.runCatching { c.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER) }.getOrNull()
            val idxDuration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val idxSize = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val idxData = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val idxMime = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            while (c.moveToNext()) {
                val id = c.getLong(idxId)
                val path = c.getString(idxData) ?: ""
                val name = path.substringAfterLast('/')
                val fmt = AudioFormat.fromExtension(name) ?: AudioFormat.fromMime(c.getString(idxMime))
                // Filtra somente formatos-alvo do app
                if (fmt == null) continue

                val uri = ContentUris.withAppendedId(collection, id)
                val rawTrack = c.getInt(idxTrack) % 1000
                val year = c.getInt(idxYear).takeIf { it > 0 }

                out += Track(
                    id = id,
                    uri = uri,
                    title = c.getString(idxTitle).takeUnless { it.isNullOrBlank() }
                        ?: displayTitleFromFileName(name),
                    artist = c.getString(idxArtist) ?: "Artista desconhecido",
                    album = c.getString(idxAlbum) ?: "Álbum desconhecido",
                    albumArtist = c.getString(idxArtist) ?: "Artista desconhecido",
                    genre = c.getString(idxGenre),
                    year = year,
                    trackNumber = rawTrack.takeIf { it > 0 },
                    discNumber = idxDisc?.let { runCatching { c.getInt(it) }.getOrNull() }?.takeIf { it > 0 },
                    composer = idxComposer?.let { runCatching { c.getString(it) }.getOrNull() },
                    comment = null,
                    durationMs = c.getLong(idxDuration),
                    format = fmt,
                    sampleRateHz = null,   // obtido em tempo real pelo player / reader sob demanda
                    bitDepth = null,
                    fileSizeBytes = c.getLong(idxSize),
                    folder = path.substringBeforeLast('/', "/"),
                    embeddedArtAvailable = true, // MediaStore resolve capa via ALBUM_ID
                    externalArtUri = albumArtUri(c.getLong(idxAlbumId))
                )
            }
        }
        out
    }

    /** Capa do álbum via MediaStore (covers/albuns). */
    private fun albumArtUri(albumId: Long): Uri? = try {
        Uri.withAppendedPath(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId.toString()
        ).let { contentUri ->
            val addr = arrayOf<String>(MediaStore.Audio.Albums.ALBUM_ART)
            context.contentResolver.query(
                contentUri, addr, null, null, null
            )?.use { cur ->
                if (cur.moveToFirst()) {
                    val p = cur.getString(0)
                    if (!p.isNullOrBlank()) Uri.fromFile(java.io.File(p)) else null
                } else null
            }
        }
    } catch (_: Exception) { null }
}
