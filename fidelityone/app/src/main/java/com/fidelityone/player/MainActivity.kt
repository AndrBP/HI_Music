package com.fidelityone.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fidelityone.player.data.playback.Media3PlaybackEngine
import com.fidelityone.player.domain.model.Track
import com.fidelityone.player.ui.navigation.MiniPlayerBar
import com.fidelityone.player.ui.screens.albums.AlbumsScreen
import com.fidelityone.player.ui.screens.artists.ArtistsScreen
import com.fidelityone.player.ui.screens.equalizer.EqualizerScreen
import com.fidelityone.player.ui.screens.favorites.FavoritesScreen
import com.fidelityone.player.ui.screens.folders.FoldersScreen
import com.fidelityone.player.ui.screens.library.LibraryScreen
import com.fidelityone.player.ui.screens.player.PlayerScreen
import com.fidelityone.player.ui.screens.queue.QueueScreen
import com.fidelityone.player.ui.screens.settings.SettingsScreen
import com.fidelityone.player.ui.screens.themes.ThemesScreen
import com.fidelityone.player.ui.theme.FidelityOneTheme
import com.fidelityone.player.ui.theme.backgroundColors
import com.fidelityone.player.ui.screens.GradientBackground
import com.fidelityone.player.viewmodel.*

/**
 * Entry point do Fidelity One.
 * - Pede apenas READ_MEDIA_AUDIO (scoped storage).
 * - NavHost: abas (Biblioteca / Álbuns / Artistas / Pastas / Favoritos) + rotas
 *   Player, Fila, Equalizador, Temas e Configurações.
 * - MiniPlayerBar persistente sobre o conteúdo quando há faixa ativa.
 */
class MainActivity : ComponentActivity() {

    private val vmFactory by lazy { LibraryViewModel.factory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val readPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                             else Manifest.permission.READ_EXTERNAL_STORAGE
        val granted = checkSelfPermission(readPermission) == PackageManager.PERMISSION_GRANTED

        val requestPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* o scan é disparado pelo ViewModel quando a permissão existir */ }

        if (!granted) requestPermission.launch(readPermission)

        setContent {
            val settingsVm: SettingsViewModel = viewModel(factory = vmFactory)
            val settings by settingsVm.settings.collectAsState()

            FidelityOneTheme(preset = settings.preset) {
                val container = (application as FidelityOneApp).container
                val libraryVm: LibraryViewModel = viewModel(factory = vmFactory)
                val playerVm: PlayerViewModel = viewModel(factory = vmFactory)
                val eqVm: EqualizerViewModel = viewModel(factory = vmFactory)

                // Conecta o equalizador à sessão de áudio do Media3 (API 28+)
                LaunchedEffect(Unit) {
                    (container.player as? Media3PlaybackEngine)?.audioSessionId?.collect { sid ->
                        if (sid != 0) eqVm.attachToSession(sid)
                    }
                }

                val libState by libraryVm.ui.collectAsState()
                val playState by playerVm.ui.collectAsState()
                val nav: NavHostController = rememberNavController()
                val backStackEntry by nav.currentBackStackEntryAsState()
                val route = backStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        Column {
                            if (route in TAB_ROUTES) NavigationBar {
                                TabEntries.forEach { tab ->
                                    NavigationBarItem(
                                        selected = route == tab.route,
                                        onClick = {
                                            if (route != tab.route) {
                                                nav.navigate(tab.route) {
                                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        icon = { Icon(tab.icon, tab.label) },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                            MiniPlayerBar(
                                title = playState.playback.current?.title,
                                artist = playState.playback.current?.artist,
                                isPlaying = playState.playback.isPlaying,
                                onPlayPause = playerVm::playPause,
                                onNext = playerVm::next,
                                onOpen = { nav.navigate(Routes.PLAYER) }
                            )
                        }
                    }
                ) { padding ->
                    GradientBackground(colors = backgroundColors(), modifier = Modifier.padding(padding)) {
                        NavHost(
                            navController = nav,
                            startDestination = Routes.LIBRARY,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(Routes.LIBRARY) {
                                LibraryScreen(
                                    state = libState,
                                    currentTrackId = playState.playback.current?.id,
                                    onQuery = libraryVm::setQuery,
                                    onSort = libraryVm::setSort,
                                    onFormatFilter = libraryVm::setFormatFilter,
                                    onRescan = libraryVm::scan,
                                    onPlay = { list, i -> playerVm.playTracks(list, i); nav.navigate(Routes.PLAYER) },
                                    onQueue = playerVm::addToQueue,
                                    onFavorite = libraryVm::toggleFavorite
                                )
                            }
                            composable(Routes.ALBUMS) {
                                AlbumsScreen(
                                    state = libState,
                                    currentTrackId = playState.playback.current?.id,
                                    onPlayAlbum = { list, i -> playerVm.playTracks(list, i); nav.navigate(Routes.PLAYER) },
                                    onQueueAlbum = playerVm::addToQueue,
                                    onFavorite = libraryVm::toggleFavorite
                                )
                            }
                            composable(Routes.ARTISTS) {
                                ArtistsScreen(
                                    state = libState,
                                    currentTrackId = playState.playback.current?.id,
                                    onPlay = { list, i -> playerVm.playTracks(list, i); nav.navigate(Routes.PLAYER) },
                                    onQueue = playerVm::addToQueue,
                                    onFavorite = libraryVm::toggleFavorite
                                )
                            }
                            composable(Routes.FOLDERS) {
                                FoldersScreen(
                                    state = libState,
                                    currentTrackId = playState.playback.current?.id,
                                    onPlay = { list, i -> playerVm.playTracks(list, i); nav.navigate(Routes.PLAYER) },
                                    onQueue = playerVm::addToQueue,
                                    onFavorite = libraryVm::toggleFavorite
                                )
                            }
                            composable(Routes.FAVORITES) {
                                FavoritesScreen(
                                    state = libState,
                                    currentTrackId = playState.playback.current?.id,
                                    onPlay = { list, i -> playerVm.playTracks(list, i); nav.navigate(Routes.PLAYER) },
                                    onQueue = playerVm::addToQueue,
                                    onFavorite = libraryVm::toggleFavorite
                                )
                            }
                            composable(Routes.PLAYER) {
                                PlayerScreen(
                                    ui = playState,
                                    onClose = { nav.popBackStack() },
                                    onPlayPause = playerVm::playPause,
                                    onNext = playerVm::next,
                                    onPrev = playerVm::previous,
                                    onSeek = playerVm::seekTo,
                                    onShuffle = playerVm::toggleShuffle,
                                    onRepeat = playerVm::cycleRepeat,
                                    onFavorite = playerVm::toggleFavoriteCurrent,
                                    onOpenQueue = { nav.navigate(Routes.QUEUE) },
                                    onSleepTimer = playerVm::startSleepTimer
                                )
                            }
                            composable(Routes.QUEUE) {
                                QueueScreen(
                                    queue = playState.playback.queue,
                                    currentIndex = playState.playback.queueIndex,
                                    onJump = playerVm::jumpToQueueItem,
                                    onRemove = playerVm::removeFromQueue,
                                    onClose = { nav.popBackStack() }
                                )
                            }
                            composable(Routes.EQUALIZER) {
                                val eqState by eqVm.ui.collectAsState()
                                EqualizerScreen(
                                    ui = eqState,
                                    onSelectPreset = eqVm::selectPreset,
                                    onBandGain = eqVm::setBandGain,
                                    onPreamp = eqVm::setPreamp,
                                    onBypass = eqVm::toggleBypass,
                                    onReset = eqVm::reset,
                                    onSaveCustom = eqVm::saveCustom,
                                    onApplyCustom = eqVm::applyCustom,
                                    onClose = { nav.popBackStack() }
                                )
                            }
                            composable(Routes.THEMES) {
                                ThemesScreen(
                                    current = settings.preset,
                                    onPreset = settingsVm::setPreset,
                                    onClose = { nav.popBackStack() }
                                )
                            }
                            composable(Routes.SETTINGS) {
                                SettingsScreen(
                                    settings = settings,
                                    onCrossfade = settingsVm::setCrossfade,
                                    onReplayGain = settingsVm::setReplayGain,
                                    onReplayGainAlbum = settingsVm::setReplayGainAlbum,
                                    onGapless = settingsVm::setGapless,
                                    onOpenThemes = { nav.navigate(Routes.THEMES) },
                                    onOpenEqualizer = { nav.navigate(Routes.EQUALIZER) },
                                    onRescan = libraryVm::scan
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        object Routes {
            const val LIBRARY = "library"
            const val ALBUMS = "albums"
            const val ARTISTS = "artists"
            const val FOLDERS = "folders"
            const val FAVORITES = "favorites"
            const val PLAYER = "player"
            const val QUEUE = "queue"
            const val EQUALIZER = "equalizer"
            const val THEMES = "themes"
            const val SETTINGS = "settings"
        }

        private data class Tab(val route: String, val label: String, val icon: ImageVector)
        private val TabEntries = listOf(
            Tab(Routes.LIBRARY, "Músicas", Icons.Filled.MusicNote),
            Tab(Routes.ALBUMS, "Álbuns", Icons.Filled.QueueMusic),
            Tab(Routes.ARTISTS, "Artistas", Icons.Filled.Person),
            Tab(Routes.FOLDERS, "Pastas", Icons.Filled.Folder),
            Tab(Routes.FAVORITES, "Favoritos", Icons.Filled.Favorite)
        )
        private val TAB_ROUTES = TabEntries.map { it.route }.toSet()
    }
}
