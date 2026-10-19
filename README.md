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

1. **Requisitos de toolchain:**
   - **JDK 21 LTS (recomendado)** — Temurin 21 ou o JBR embutido do Android Studio (Meerkat+ já traz JBR 21).
     O projeto também é **retrocompatível com JDK 17** (bytecode Java/Kotlin alvo = 17, então ambos funcionam).
   - **Gradle 8.12 via wrapper incluído** (`./gradlew` / `gradlew.bat`) — não é necessário Gradle global.
   - **AGP 8.9.0**, Kotlin 2.1.10 + plugin Compose Compiler oficial, KSP 2.1.10-1.0.30.
   - Android SDK platform 34 (compileSdk 34) e build-tools compatíveis com AGP 8.9.
   - ⚠️ **JDK 25 não é suportado** oficialmente pelo AGP/Gradle estáveis — não usar (risco de regressão alto).
2. Abra a pasta `fidelityone/` no Android Studio (*File → Open*). O Gradle Sync usa o wrapper 8.12 automaticamente.
   Se quiser escolher o JDK: *Settings → Build Tools → Gradle → Gradle JDK* → **jbr-21 (ou 17)**.
3. Em `app/build.gradle.kts`, confirme `compileSdk = 34` e `minSdk = 26` (inalterados nesta atualização).
4. Build por linha de comando (opcional):
   ```bash
   cd fidelityone
   JAVA_HOME=/path/to/jdk-21 ./gradlew assembleDebug   # recomendado
   JAVA_HOME=/path/to/jdk-17 ./gradlew assembleDebug   # retrocompatível
   ```
5. Rode **Run ▶** num smartphone físico (recomendado, para testar áudio) ou emulador API 26+.
6. Ao abrir, conceda a permissão **"Permitir acesso a músicas"** (READ_MEDIA_AUDIO).
7. Vá em **Biblioteca → sincronizar** (ícone refresh): o app escaneia o MediaStore e monta a
   biblioteca (cache Room). Formatos fora do MediaStore (ex.: DSF/DFF em algumas ROMs) podem
   aparecer apenas na aba **Pastas** — use "Reproduzir" direto do arquivo.
8. Toque numa faixa → MiniPlayer → tela cheia. Equalizador e Temas em ☰ / Configurações.

### Notas da atualização de toolchain (JDK 21 LTS)
- AGP 8.5.2 → **8.9.0**; Gradle sugerido 8.7 → **wrapper 8.12 fixo** (jar + scripts versionados).
- Kotlin 1.9.24 → **2.1.10**; `composeOptions{kotlinCompilerExtensionVersion}` removido — agora o
  **plugin `org.jetbrains.kotlin.plugin.compose`** gerencia o compilador Compose (obrigatório no Kotlin 2.x).
- Compose BOM 2024.06.00 → **2024.12.01**; Media3 1.4.1 → **1.5.1**; lifecycle 2.8.4 → 2.8.7;
  core-ktx 1.13.1 → 1.15.0; activity-compose 1.9.1 → 1.9.3; navigation 2.7.7 → 2.8.5; Coil 2.6.0 → 2.7.0;
  coroutines 1.8.1 → 1.9.0. Room 2.6.1 e DataStore 1.1.1 permanecem (já compatíveis, sem breaking changes).
- Versões centralizadas em `gradle/libs.versions.toml` (version catalog).
- Nenhuma mudança em arquitetura MVVM, portas/interfaces ou lógica de reprodução de áudio.

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
