package com.fidelityone.player.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.fidelityone.player.domain.model.*
import com.fidelityone.player.domain.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "fidelity_settings")

/** Persistência: tema/layout, crossfade, ReplayGain, gapless, sleep timer, presets custom. */
class DataStoreSettings(private val context: Context) : SettingsRepository {

    private object Keys {
        val LAYOUT = stringPreferencesKey("layout")
        val THEME = stringPreferencesKey("theme")
        val CROSSFADE = intPreferencesKey("crossfade_ms")
        val RG_ON = booleanPreferencesKey("replaygain_enabled")
        val RG_ALBUM = booleanPreferencesKey("replaygain_album")
        val GAPLESS = booleanPreferencesKey("gapless")
        val SLEEP = intPreferencesKey("sleep_min")
        val EQ_STATE = stringPreferencesKey("eq_state")           // serializado simples
        val CUSTOM_PREFIX = "custom_eq_"                          // custom_eq_<nome> = "preamp;g1,g2..."
    }

    override val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            preset = VisualPreset(
                layout = LayoutBase.entries.firstOrNull { it.id == p[Keys.LAYOUT] }
                    ?: LayoutBase.CLASSIC_LIBRARY,
                theme = ColorTheme.entries.firstOrNull { it.id == p[Keys.THEME] }
                    ?: ColorTheme.ONYX_DARK
            ),
            crossfadeMs = p[Keys.CROSSFADE] ?: 0,
            replayGainEnabled = p[Keys.RG_ON] ?: false,
            replayGainSourceAlbum = p[Keys.RG_ALBUM] ?: false,
            gapless = p[Keys.GAPLESS] ?: true,
            sleepTimerMinutes = p[Keys.SLEEP] ?: 0
        )
    }

    override suspend fun save(settings: AppSettings) {
        context.dataStore.edit { p ->
            p[Keys.LAYOUT] = settings.preset.layout.id
            p[Keys.THEME] = settings.preset.theme.id
            p[Keys.CROSSFADE] = settings.crossfadeMs
            p[Keys.RG_ON] = settings.replayGainEnabled
            p[Keys.RG_ALBUM] = settings.replayGainSourceAlbum
            p[Keys.GAPLESS] = settings.gapless
            p[Keys.SLEEP] = settings.sleepTimerMinutes
        }
    }

    override suspend fun saveCustomPreset(name: String, gains: FloatArray, preamp: Float) {
        val safe = name.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifBlank { "Custom" }
        context.dataStore.edit { p ->
            p[stringPreferencesKey(Keys.CUSTOM_PREFIX + safe)] =
                "$preamp;" + gains.joinToString(",")
        }
    }

    override fun customPresets(): Flow<List<Pair<String, FloatArray>>> =
        context.dataStore.data.map { p ->
            p.asMap().entries
                .filter { it.key.name.startsWith(Keys.CUSTOM_PREFIX) }
                .mapNotNull { e ->
                    val nm = e.key.name.removePrefix(Keys.CUSTOM_PREFIX)
                    val raw = e.value as? String ?: return@mapNotNull null
                    val parts = raw.split(";")
                    val gains = parts.getOrNull(1)?.split(",")?.mapNotNull { it.toFloatOrNull() }
                        ?: return@mapNotNull null
                    nm to gains.toFloatArray()
                }
        }

    /** Serializa/desserializa o estado atual do EQ para restaurar entre sessões. */
    suspend fun saveEqState(state: EqState) {
        val s = "${state.enabled};${state.preampDb};" +
                state.bands.joinToString(",") { "${it.frequencyHz}|${it.gainDb}|${it.q}" } +
                ";${state.preset.name}"
        context.dataStore.edit { it[Keys.EQ_STATE] = s }
    }

    fun eqState(): Flow<EqState?> = context.dataStore.data.map { p ->
        val s = p[Keys.EQ_STATE] ?: return@map null
        runCatching {
            val parts = s.split(";")
            EqState(
                enabled = parts[0].toBoolean(),
                preampDb = parts[1].toFloat(),
                bands = parts[2].split(",").map { seg ->
                    val f = seg.split("|")
                    EqBand(f[0].toFloat(), f[1].toFloat(), f.getOrElse(2) { "1.0" }.toFloat())
                },
                preset = EqPreset.valueOf(parts.getOrElse(3) { "FLAT" })
            )
        }.getOrNull()
    }
}
