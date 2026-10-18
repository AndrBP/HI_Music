# Fidelity One — Local Hi-Fi Music Player (Android MVP)

Player de música **local-first** em Kotlin + Jetpack Compose + Material 3, inspirado (apenas visualmente)
em apps premium de alta fidelidade. Reproduz arquivos do dispositivo:
**FLAC, WAV, AIFF/AIF, MP3, AAC/M4A, DSF/DFF (DSD)** — com fallback DSD→PCM via Media3/ExoPlayer.

> ⚠️ Sem streaming, sem scraping, sem conteúdo protegido. Não usa nem menciona marcas de serviços externos.
> Não promete "bit-perfect universal": o pipeline padrão é resample-avoidance quando possível, estável para teste.

---

## 1. Arquitetura (MVVM + camadas)

```
┌───────────────────────────────────────────────────────────────┐
│ UI (Jetpack Compose / Material 3)                             │
│  LibraryScreen · AlbumsScreen · ArtistsScreen · FoldersScreen │
│  FavoritesScreen · QueueScreen · PlayerScreen · Equalizer     │
│  ThemesScreen · SettingsScreen                                │
└───────────────▲───────────────────────────────────────────────┘
                │ StateFlow (UiState imutável)
┌───────────────┴───────────────────────────────────────────────┐
│ Presentation (ViewModels — MVVM)                              │
│  LibraryViewModel · PlayerViewModel · EqualizerViewModel      │
│  SettingsViewModel                                            │
└───────────────▲───────────────────────────────────────────────┘
                │ Domínio
┌───────────────┴───────────────────────────────────────────────┐
│ Domain                                                        │
│  Models: Track, Album, Artist, Format, TechnicalInfo,         │
│          PlaybackState, EqBand, Preset, LayoutBase, ColorTheme│
│  Ports: MetadataReader, AudioPlayerController, EqController,  │
│         LibraryRepository, SettingsRepository                 │
└───────────────▲───────────────────────────────────────────────┘
                │ Data
┌───────────────┴───────────────────────────────────────────────┐
│ Data                                                          │
│  Media3MetadataReader (MediaMetadataRetriever + ExoPlayer)    │
│  MediaStoreScanner (scoped storage, READ_MEDIA_AUDIO)         │
│  RoomLibraryRepository (cache da biblioteca + favoritos)      │
│  Media3PlaybackEngine (ExoPlayer + gapless + crossfade opc.)  │
│  AndroidEqController (AudioEffect/DynamicsProcessing 10-band) │
│  DataStoreSettings (tema, layout, EQ, ReplayGain, sleep timer)│
└───────────────────────────────────────────────────────────────┘
                │
┌───────────────┴───────────────────────────────────────────────┐
│ Service: PlaybackService (MediaSessionService, Media3)        │
│  → notificação, tela de bloqueio, controle por mídia externa  │
└───────────────────────────────────────────────────────────────┘
```

**Decisão sobre DSD:** Media3 (ExoPlayer ≥ 1.x) possui decoder DSD nativo que decodifica
DSF/DFF para PCM internamente quando o hardware não suportar DSD-over-HDMI/USB. Portanto
**não é necessário embutir FFmpeg no MVP** — isso mantém o APK pequeno e estável. O código
detecta `mimeType` e reporta "DSD → PCM (fallback)" nas infos técnicas quando aplicável.

**Equalizador:** 10 bandas gráficas via `DynamicsProcessing` (API 28+) com fallback para
`BassBoost`+`Presets` simples em API < 28. A arquitetura (`EqController` port) já permite
trocar por DSP próprio (por ex. pipeline ExoPlayer com `TonalControl`/processor custom) no futuro.

## 2. Estrutura de pastas

```
fidelityone/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/fidelityone/player/
        │   ├── MainActivity.kt
        │   ├── FidelityOneApp.kt
        │   ├── core/AudioFormats.kt
        │   ├── domain/model/Models.kt
        │   ├── domain/port/Ports.kt
        │   ├── data/metadata/Media3MetadataReader.kt
        │   ├── data/library/MediaStoreScanner.kt
        │   ├── data/library/LibraryRepositoryImpl.kt
        │   ├── data/playback/Media3PlaybackEngine.kt
        │   ├── data/equalizer/AndroidEqController.kt
        │   ├── data/settings/DataStoreSettings.kt
        │   ├── service/PlaybackService.kt
        │   ├── ui/theme/Tokens.kt
        │   ├── ui/theme/Themes.kt          # 4 temas de cor
        │   ├── ui/theme/Layouts.kt         # 5 layouts-base → 20 presets
        │   ├── ui/navigation/AppNavHost.kt
        │   ├── viewmodel/LibraryViewModel.kt
        │   ├── viewmodel/PlayerViewModel.kt
        │   ├── viewmodel/EqualizerViewModel.kt
        │   ├── viewmodel/SettingsViewModel.kt
        │   └── ui/screens/
        │       ├── library/LibraryScreen.kt
        │       ├── albums/AlbumsScreen.kt
        │       ├── artists/ArtistsScreen.kt
        │       ├── folders/FoldersScreen.kt
        │       ├── favorites/FavoritesScreen.kt
        │       ├── queue/QueueScreen.kt
        │       ├── player/PlayerScreen.kt
        │       ├── equalizer/EqualizerScreen.kt
        │       ├── themes/ThemesScreen.kt
        │       └── settings/SettingsScreen.kt
        └── res/values/{strings,themes}.xml
```

## 3. Como rodar no Android Studio

1. **Requisitos:** Android Studio Ladybug+ (Hedgehog serve), JDK 17, Android SDK 34+.
2. Abra a pasta `fidelityone/` no Android Studio (*File → Open*). Aguarde o Gradle Sync
   (o wrapper será sugerido; rode `gradle wrapper --gradle-version 8.7` se pedir).
3. Em `app/build.gradle.kts`, confirme `compileSdk = 34` e `minSdk = 26`.
4. Rode **Run ▶** num smartphone físico (recomendado, para testar áudio) ou emulador API 26+.
5. Ao abrir, conceda a permissão **"Permitir acesso a músicas"** (READ_MEDIA_AUDIO).
6. Vá em **Biblioteca → sincronizar** (ícone refresh): o app escaneia o MediaStore e monta a
   biblioteca (cache Room). Formatos fora do MediaStore (ex.: DSF/DFF em algumas ROMs) podem
   aparecer apenas na aba **Pastas** — use "Reproduzir" direto do arquivo.
7. Toque numa faixa → MiniPlayer → tela cheia. Equalizador e Temas em ☰ / Configurações.

## 4. Status do MVP (prioridades)

| # | Recurso | Estado |
|---|---------|--------|
| 1 | Player (play/pause/next/prev/shuffle/repeat/queue/seek) | ✅ completo |
| 2 | Biblioteca (músicas/álbuns/artistas/pastas/favoritos, busca, ordenação, filtro formato) | ✅ completo |
| 3 | Metadados (ID3v2/Vorbis/MP4/FLC via Media3 + fallback nome/pasta) | ✅ completo |
| 4 | Temas (5 layouts × 4 cores = 20 presets, tokens) | ✅ completo |
| 5 | Equalizador (10 bandas, preamp, presets, bypass, salvar custom) | ✅ funcional |
| 6 | Gapless (nativo ExoPlayer) · Crossfade opcional (config) · ReplayGain (tag-based, opc.) · Sleep timer | ✅ básico |
| – | Edição manual de tags · DSP paramétrico próprio | 🔜 pós-MVP (porta já definida) |

## 5. Notas de qualidade de áudio

- ExoPlayer com `handleAudioLoss=true`, `PriorityAudioAttributes`, buffer grande (8 MB).
- Gapless habilitado por padrão (mesmo renderer, sem recriar AudioTrack entre faixas do álbum).
- Sample-rate/bit-depth exibidos vêm do Media3 `Format` durante a reprodução (tempo real).
- DSD: reproduzido via decoder embutido do Media3 (conversão PCM interna); a UI informa isso.
- Nada de "promessa bit-perfect": saída passa pelo mixer Android (audio policy do SO).
