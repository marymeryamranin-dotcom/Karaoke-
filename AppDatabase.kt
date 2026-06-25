package com.example.data.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// ROOM ENTITIES
// ==========================================

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val email: String,
    val xp: Int = 0,
    val level: Int = 1,
    val rank: String = "Bronze Vocalist",
    val selectedLanguage: String = "ENGLISH", // AppLanguage enum string
    val isDarkMode: Boolean = true
)

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val category: String,
    val durationSec: Int,
    val lyricsLrc: String,
    val originalAudioMockUrl: String = "",
    val instrumentalMockUrl: String = "",
    val isFavorite: Boolean = false,
    val languageCode: String = "EN" // EN, FR, AR
)

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: Int,
    val songTitle: String,
    val artist: String,
    val dateString: String,
    val score: Int,
    val localFilePath: String,
    val isCloudSynced: Boolean = false,
    // Effects saved
    val echoDelay: Float = 0f,
    val reverbIntensity: Float = 0f,
    val bassBoost: Float = 0f,
    val micGain: Float = 1f,
    val durationSec: Int = 0
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val songsCount: Int = 0
)

@Entity(tableName = "singing_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val songId: Int,
    val songTitle: String,
    val artist: String,
    val timestamp: Long = System.currentTimeMillis()
)

// ==========================================
// DATA ACCESS OBJECTS (DAOs)
// ==========================================

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET xp = :xp, level = :level, rank = :rank WHERE username = :username")
    suspend fun updateUserProgress(username: String, xp: Int, level: Int, rank: String)

    @Query("UPDATE users SET selectedLanguage = :lang WHERE username = :username")
    suspend fun updateUserLanguage(username: String, lang: String)

    @Query("UPDATE users SET isDarkMode = :isDark WHERE username = :username")
    suspend fun updateUserTheme(username: String, isDark: Boolean)
}

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("UPDATE songs SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFav: Boolean)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSongById(id: Int)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongsCount(): Int
}

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY id DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)

    @Query("UPDATE recordings SET isCloudSynced = 1 WHERE id = :id")
    suspend fun markAsCloudSynced(id: Int)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Int)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET songsCount = songsCount + 1 WHERE id = :playlistId")
    suspend fun incrementSongsCount(playlistId: Int)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM singing_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)
}

// ==========================================
// APP DATABASE HOLDER
// ==========================================

@Database(
    entities = [
        UserEntity::class,
        SongEntity::class,
        RecordingEntity::class,
        PlaylistEntity::class,
        HistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun songDao(): SongDao
    abstract fun recordingDao(): RecordingDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "karaoke_studio_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
