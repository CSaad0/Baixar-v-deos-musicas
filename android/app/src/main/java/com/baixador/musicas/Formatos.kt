package com.baixador.musicas

import java.io.File

data class Formato(val nome: String, val video: Boolean, val semPerda: Boolean = false) {
    val qualidades: List<String>
        get() = when {
            video -> listOf("Melhor", "2160p", "1440p", "1080p", "720p", "480p", "360p")
            semPerda -> listOf("Sem perda")
            else -> listOf("Melhor", "320 kbps", "256 kbps", "192 kbps", "128 kbps", "96 kbps")
        }
}

object Formatos {
    val TODOS = listOf(
        Formato("MP3", false),
        Formato("M4A", false),
        Formato("OPUS", false),
        Formato("OGG", false),
        Formato("FLAC", false, semPerda = true),
        Formato("WAV", false, semPerda = true),
        Formato("MP4", true),
        Formato("WEBM", true),
        Formato("MKV", true),
    )

    val MIME = mapOf(
        "mp3" to "audio/mpeg", "m4a" to "audio/mp4", "opus" to "audio/ogg", "ogg" to "audio/ogg",
        "flac" to "audio/flac", "wav" to "audio/wav",
        "mp4" to "video/mp4", "webm" to "video/webm", "mkv" to "video/x-matroska",
    )

    /** Argumentos do yt-dlp para o formato/qualidade escolhidos. */
    fun opcoes(f: Formato, qualidade: String, pasta: File): List<List<String>> {
        val ops = mutableListOf(
            listOf("-o", pasta.absolutePath + "/%(title).150B.%(ext)s"),
            listOf("--no-playlist"),
            listOf("--no-mtime"),
            listOf("--embed-metadata"),
        )
        if (!f.video) {
            val codec = if (f.nome == "OGG") "vorbis" else f.nome.lowercase()
            ops += listOf("-f", "bestaudio/best")
            ops += listOf("-x")
            ops += listOf("--audio-format", codec)
            if (!f.semPerda) {
                ops += listOf("--audio-quality", if (qualidade == "Melhor") "0" else qualidade.substringBefore(" ") + "K")
            }
            if (f.nome == "MP3") ops += listOf("--embed-thumbnail")
        } else {
            val h = if (qualidade == "Melhor") "" else "[height<=${qualidade.removeSuffix("p")}]"
            val sel = if (f.nome == "MP4")
                "bv*[ext=mp4]$h+ba[ext=m4a]/b[ext=mp4]$h/bv*$h+ba/b$h/b"
            else
                "bv*$h+ba/b$h/b"
            ops += listOf("-f", sel)
            ops += listOf("--merge-output-format", f.nome.lowercase())
        }
        return ops
    }
}
