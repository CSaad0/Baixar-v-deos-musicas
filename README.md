# Baixar Vídeos e Músicas

Programa com interface gráfica para baixar músicas (e vídeos) a partir de um link
(YouTube, SoundCloud e [vários outros sites](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md)).

- **Campo para colar o link** (com botão "Colar")
- **Tipo de arquivo:** MP3, M4A, OPUS, OGG, FLAC, WAV (áudio) ou MP4, WEBM, MKV (vídeo)
- **Qualidade:** 320/256/192/128/96 kbps para áudio, ou 2160p a 360p para vídeo
- Escolha da pasta de destino, barra de progresso, capa e metadados embutidos no áudio

## Download (sem precisar de Python)

Pegue a versão mais recente na página de [Releases](https://github.com/CSaad0/Baixar-v-deos-musicas/releases/latest):

- **Windows:** `BaixadorDeMusicas.exe` — dois cliques e pronto (FFmpeg já incluso)
- **Android:** `BaixadorDeMusicas-arm64.apk` (celulares atuais) ou `-universal.apk` (qualquer aparelho).
  Arquivos ficam em `Download/Baixador`. Dica: no YouTube, use *Compartilhar → Baixador de Músicas*.

Os arquivos são gerados automaticamente pelo GitHub Actions (`.github/workflows/build.yml`) a cada push.
O código do app Android está em `android/`.

## Rodando pelo código-fonte (Python)

### Requisitos

1. [Python 3.9+](https://www.python.org/downloads/) (no Windows, marque "Add Python to PATH"; o Tkinter já vem junto)
2. [FFmpeg](https://ffmpeg.org/download.html) instalado e no PATH
   - Windows: `winget install ffmpeg`
   - macOS: `brew install ffmpeg`
   - Linux: `sudo apt install ffmpeg python3-tk`

### Instalação e uso

```bash
pip install -r requirements.txt
python app.py
```

1. Cole o link da música
2. Escolha o tipo de arquivo e a qualidade
3. Clique em **Baixar** — o arquivo vai para a pasta `Downloads` (ou a que você escolher)

Se algum site parar de funcionar, atualize o yt-dlp: `pip install -U yt-dlp`.

> Baixe apenas conteúdo que você tem direito de baixar.
