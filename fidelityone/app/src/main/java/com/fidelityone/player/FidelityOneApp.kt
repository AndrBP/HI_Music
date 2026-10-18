package com.fidelityone.player

import android.app.Application
import com.fidelityone.player.data.equalizer.AndroidEqController
import com.fidelityone.player.data.library.LibraryRepositoryImpl
import com.fidelityone.player.data.metadata.Media3MetadataReader
import com.fidelityone.player.data.playback.Media3PlaybackEngine
import com.fidelityone.player.data.settings.DataStoreSettings
import com.fidelityone.player.domain.port.*

/**
 * Service locator enxuto (MVP sem DI framework para manter estável e simples).
 * Singleton por processo — os ViewModels acessam via AppContainer.
 */
class FidelityOneApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    val metadataReader: MetadataReader = Media3MetadataReader()
    val library: LibraryRepository = LibraryRepositoryImpl(app)
    val settings: DataStoreSettings = DataStoreSettings(app)          // tipo concreto p/ EQ state
    val player: AudioPlayerController = Media3PlaybackEngine(app)
    val equalizer: AndroidEqController = AndroidEqController()         // API 28+ DynamicsProcessing
}
