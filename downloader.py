"""Lógica de download (independente da interface) usando yt-dlp + FFmpeg."""

from __future__ import annotations

import os
from typing import Callable

import yt_dlp

# Formatos disponíveis -> tipo ("audio" ou "video")
FORMATOS = {
    "MP3": "audio",
    "M4A": "audio",
    "OPUS": "audio",
    "OGG": "audio",
    "FLAC": "audio",
    "WAV": "audio",
    "MP4": "video",
    "WEBM": "video",
    "MKV": "video",
}

# Formatos sem perda: a qualidade (kbps) não se aplica
SEM_PERDA = {"FLAC", "WAV"}

QUALIDADES_AUDIO = ["Melhor", "320 kbps", "256 kbps", "192 kbps", "128 kbps", "96 kbps"]
QUALIDADES_VIDEO = ["Melhor", "2160p", "1440p", "1080p", "720p", "480p", "360p"]


def qualidades_para(formato: str) -> list[str]:
    formato = formato.upper()
    if FORMATOS.get(formato) == "video":
        return QUALIDADES_VIDEO
    if formato in SEM_PERDA:
        return ["Sem perda"]
    return QUALIDADES_AUDIO


def montar_opcoes(formato: str, qualidade: str, pasta: str) -> dict:
    """Monta o dicionário de opções do yt-dlp para o formato/qualidade escolhidos."""
    formato = formato.upper()
    if formato not in FORMATOS:
        raise ValueError(f"Formato não suportado: {formato}")

    opts: dict = {
        "outtmpl": os.path.join(pasta, "%(title)s.%(ext)s"),
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
        "noprogress": True,
    }

    if FORMATOS[formato] == "audio":
        pp: dict = {"key": "FFmpegExtractAudio", "preferredcodec": formato.lower()}
        if formato == "OGG":
            pp["preferredcodec"] = "vorbis"
        if formato not in SEM_PERDA:
            # "0" = melhor qualidade VBR; senão bitrate em kbps
            pp["preferredquality"] = "0" if qualidade == "Melhor" else qualidade.split()[0]
        opts["format"] = "bestaudio/best"
        opts["postprocessors"] = [pp, {"key": "FFmpegMetadata"}]
        if formato in {"MP3", "M4A", "FLAC", "OPUS", "OGG"}:
            opts["writethumbnail"] = True
            opts["postprocessors"].append({"key": "EmbedThumbnail"})
    else:
        limite = "" if qualidade == "Melhor" else f"[height<={qualidade.rstrip('p')}]"
        opts["format"] = f"bestvideo{limite}+bestaudio/best{limite}/best"
        opts["merge_output_format"] = formato.lower()

    return opts


def baixar(
    url: str,
    formato: str,
    qualidade: str,
    pasta: str,
    progresso: Callable[[float, str], None] | None = None,
) -> str:
    """Baixa a mídia e retorna o título. `progresso(percentual, mensagem)` é opcional."""
    url = url.strip()
    if not url:
        raise ValueError("Cole um link válido.")
    os.makedirs(pasta, exist_ok=True)

    opts = montar_opcoes(formato, qualidade, pasta)

    def hook(d: dict) -> None:
        if not progresso:
            return
        if d["status"] == "downloading":
            total = d.get("total_bytes") or d.get("total_bytes_estimate") or 0
            feito = d.get("downloaded_bytes") or 0
            pct = feito * 100 / total if total else 0.0
            vel = d.get("_speed_str", "").strip()
            progresso(pct, f"Baixando... {pct:.1f}% {vel}".strip())
        elif d["status"] == "finished":
            progresso(100.0, "Convertendo arquivo...")

    opts["progress_hooks"] = [hook]

    with yt_dlp.YoutubeDL(opts) as ydl:
        info = ydl.extract_info(url, download=True)
    return (info or {}).get("title", "arquivo")
