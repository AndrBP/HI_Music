package com.fidelityone.player.core

import android.net.Uri

/** Formatos de áudio suportados pelo Fidelity One. */
enum class AudioFormat(val ext: String, val label: String, val lossless: Boolean, val dsd: Boolean) {
    FLAC("flac", "FLAC", true, false),
    WAV("wav", "WAV", true, false),
    AIFF("aiff", "AIFF", true, false),
    AIF("aif", "AIF", true, false),
    MP3("mp3", "MP3", false, false),
    AAC("m4a", "AAC/M4A", false, false),
    M4A("m4a", "AAC/M4A", false, false),
    DSF("dsf", "DSD (DSF)", true, true),
    DFF("dff", "DSD (DFF)", true, true);

    companion object {
        val ALL_EXTENSIONS: Set<String> = entries.map { it.ext }.toSet()

        fun fromExtension(pathOrName: String?): AudioFormat? {
            if (pathOrName.isNullOrBlank()) return null
            val ext = pathOrName.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { it.ext == ext }
        }

        fun fromMime(mime: String?): AudioFormat? = when {
            mime == null -> null
            mime.contains("flac") -> FLAC
            mime.contains("wav") || mime.contains("wave") -> WAV
            mime.contains("aiff") || mime.contains("aif") -> AIFF
            mime.contains("mpeg") || mime.contains("mp3") -> MP3
            mime.contains("mp4") || mime.contains("aac") || mime.contains("m4a") -> AAC
            mime.contains("dsd") || mime.contains("dsf") || mime.contains("dff") -> DSF
            else -> null
        }
    }
}

/** Utilitários de formatação para as infos técnicas. */
object TechFormatter {
    fun duration(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSec = ms / 1000
        val h = totalSec / 3600; val m = (totalSec % 3600) / 60; val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
    }

    fun sampleRate(hz: Int?): String = when {
        hz == null || hz <= 0 -> "—"
        hz >= 1_000_000 -> String.format("%.1f MHz", hz / 1_000_000.0)   // DSD64 = 2.8 MHz
        hz >= 1000 -> String.format("%.1f kHz", hz / 1000.0)
        else -> "$hz Hz"
    }

    fun bitDepth(bits: Int?, format: AudioFormat?): String = when {
        format?.dsd == true -> "1-bit (DSD)"
        bits == null || bits <= 0 -> "—"
        else -> "$bits-bit"
    }

    fun bitrateEstimate(bytesPerSecond: Long?, format: AudioFormat?): String = when {
        bytesPerSecond == null || bytesPerSecond <= 0 -> "—"
        else -> "${(bytesPerSecond * 8) / 1000} kbps (${format?.label ?: "?"})"
    }
}

/** Nome "bonito" a partir do arquivo quando não há tags (fallback). */
fun displayTitleFromFileName(name: String?): String {
    if (name.isNullOrBlank()) return "Desconhecido"
    return name.substringBeforeLast('.').replace('_', ' ').replace(Regex("\\s+"), " ").trim()
}

fun folderOf(uri: Uri?): String {
    val p = uri?.lastPathSegment ?: return "/"
    return p.substringBeforeLast('/', "/")
}
