package com.fidelityone.player.data.library

import android.content.Context
import androidx.room.*
import com.fidelityone.player.domain.model.Track
import com.fidelityone.player.domain.port.LibraryRepository
import com.fidelityone.player.domain.port.ScanProgress
import kotlinx.coroutines.flow.*

/* ── Room entities/DAOs ─────────────────────────────────────── */

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val trackId: Long)

@Dao
interface FavoritesDao {
    // Room não converte Cursor diretamente em Flow<Set<...>> — usa List e converte no repositório.
    @Query("SELECT trackId FROM favorites")
    fun favoriteIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :id)")
    suspend fun isFavorite(id: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entity: FavoriteEntity)

    // @Delete com parâmetro escalar exige a classe da entidade; por id usa @Query.
    @Query("DELETE FROM favorites WHERE trackId = :id")
    suspend fun remove(id: Long)
}

@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun favorites(): FavoritesDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(context: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(context, AppDb::class.java, "fidelityone.db")
                .fallbackToDestructiveMigration().build().also { inst = it }
        }
    }
}

/**
 * Implementação do repositório: cache em memória (fonte: MediaStore) + favoritos em Room.
 * Mantém a biblioteca reativa para busca/ordenação/filtro na UI.
 */
class LibraryRepositoryImpl(private val context: Context) : LibraryRepository {

    private val db = AppDb.get(context)
    private val scanner = MediaStoreScanner(context)
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    private val _scan = MutableStateFlow(ScanProgress())

    override val tracks: Flow<List<Track>> = _tracks.asStateFlow()

    override val favorites: Flow<List<Track>> =
        combine(_tracks, db.favorites().favoriteIds().map { it.toSet() }) { all, ids ->
            all.filter { ids.contains(it.id) }
        }.stateIn(coroutineScope(), SharingStarted.WhileSubscribed(5_000), emptyList())

    override suspend fun scan(context: Context): ScanProgress {
        _scan.value = ScanProgress(done = false, found = 0)
        return try {
            val result = scanner.scan()
            _tracks.value = result
            ScanProgress(done = true, found = result.size).also { _scan.value = it }
        } catch (e: Exception) {
            ScanProgress(done = true, found = _tracks.value.size).also { _scan.value = it }
        }
    }

    override fun observeScanning(): Flow<ScanProgress> = _scan.asStateFlow()

    override suspend fun setFavorite(trackId: Long, favorite: Boolean) {
        if (favorite) db.favorites().add(FavoriteEntity(trackId))
        else db.favorites().remove(trackId)
    }

    override suspend fun isFavorite(trackId: Long): Boolean = db.favorites().isFavorite(trackId)

    private fun coroutineScope() = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
    )
}
