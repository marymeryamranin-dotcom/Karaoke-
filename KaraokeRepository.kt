package com.example.data.repository

import com.example.data.db.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class KaraokeRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val songDao = database.songDao()
    private val recordingDao = database.recordingDao()
    private val playlistDao = database.playlistDao()
    private val historyDao = database.historyDao()

    // Reactive streams
    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<SongEntity>> = songDao.getFavoriteSongs()
    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    val recentHistory: Flow<List<HistoryEntity>> = historyDao.getRecentHistory()

    suspend fun searchSongs(query: String): Flow<List<SongEntity>> {
        return songDao.searchSongs(query)
    }

    // User Profile Actions
    suspend fun getUser(username: String): UserEntity? {
        return userDao.getUserByUsername(username)
    }

    suspend fun registerUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun updateUserProgress(username: String, xp: Int, level: Int, rank: String) {
        userDao.updateUserProgress(username, xp, level, rank)
    }

    suspend fun updateUserLanguage(username: String, language: String) {
        userDao.updateUserLanguage(username, language)
    }

    suspend fun updateUserTheme(username: String, isDarkMode: Boolean) {
        userDao.updateUserTheme(username, isDarkMode)
    }

    // Song Actions
    suspend fun addCustomSong(song: SongEntity) {
        songDao.insertSong(song)
    }

    suspend fun toggleFavorite(songId: Int, isFav: Boolean) {
        songDao.updateFavoriteStatus(songId, isFav)
    }

    suspend fun deleteSong(songId: Int) {
        songDao.deleteSongById(songId)
    }

    // Recording Actions
    suspend fun saveRecording(recording: RecordingEntity) {
        recordingDao.insertRecording(recording)
    }

    suspend fun markRecordingAsSynced(id: Int) {
        recordingDao.markAsCloudSynced(id)
    }

    suspend fun deleteRecording(id: Int) {
        recordingDao.deleteRecordingById(id)
    }

    // Playlist Actions
    suspend fun createPlaylist(name: String) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun addSongToPlaylist(playlistId: Int) {
        playlistDao.incrementSongsCount(playlistId)
    }

    // History Actions
    suspend fun addSongToHistory(songId: Int, title: String, artist: String) {
        historyDao.insertHistory(HistoryEntity(songId = songId, songTitle = title, artist = artist))
    }

    // Seed Data
    suspend fun seedSongsIfEmpty() {
        if (songDao.getSongsCount() == 0) {
            val seedList = listOf(
                SongEntity(
                    title = "Blinding Lights",
                    artist = "The Weeknd",
                    category = "Pop / Synthwave",
                    durationSec = 200,
                    languageCode = "EN",
                    lyricsLrc = """
                        [00:01.00]Instrumental Intro...
                        [00:04.00]Yeah...
                        [00:06.50]I've been on my own for long enough
                        [00:10.80]Maybe you can show me how to love, maybe
                        [00:15.50]I'm going through withdrawals
                        [00:19.00]You don't even have to do too much
                        [00:22.50]You can turn me on with just a touch, baby
                        [00:27.50]I look around and Sin City's cold and empty
                        [00:32.80]No one's around to judge me
                        [00:36.00]I can't see clearly when you're gone
                        [00:40.20]I said, ooh, I'm blinded by the lights
                        [00:46.80]No, I can't sleep until I feel your touch
                        [00:52.00]I said, ooh, I'm drowning in the night
                        [00:58.50]Oh, when I'm like this, you're the one I trust
                        [01:04.00]Instrumental Outro...
                    """.trimIndent()
                ),
                SongEntity(
                    title = "La Vie En Rose",
                    artist = "Édith Piaf",
                    category = "Classic / Chanson",
                    durationSec = 180,
                    languageCode = "FR",
                    lyricsLrc = """
                        [00:01.00]Introduction instrumentale...
                        [00:04.00]Des yeux qui font baisser les miens
                        [00:08.00]Un rire qui se perd sur sa bouche
                        [00:12.50]Voilà le portrait sans retouche
                        [00:16.80]De l'homme auquel j'appartiens
                        [00:21.00]Quand il me prend dans ses bras
                        [00:24.50]Il me parle tout bas
                        [00:28.00]Je vois la vie en rose
                        [00:32.00]Il me dit des mots d'amour
                        [00:36.00]Des mots de tous les jours
                        [00:40.50]Et ça me fait quelque chose
                        [00:44.80]Il est entré dans mon cœur
                        [00:48.50]Une part de bonheur
                        [00:52.00]Dont je connais la cause
                        [00:56.00]C'est lui pour moi, moi pour lui dans la vie
                        [01:01.00]Il me l'a dit, l'a juré pour la vie...
                    """.trimIndent()
                ),
                SongEntity(
                    title = "C'est La Vie",
                    artist = "Khaled",
                    category = "Rai / Pop",
                    durationSec = 220,
                    languageCode = "AR",
                    lyricsLrc = """
                        [00:01.00]مقدمة الموسيقى الإيقاعية...
                        [00:05.50]On va s'aimer, on va danser
                        [00:09.80]Yes, c'est la vie, lalalala!
                        [00:14.00]ولا كان على بالي، هذا الحب يغير حالي
                        [00:19.50]نعيش معاك يا غالي، ليلة ونهار
                        [00:23.80]C'est la vie, make it beautiful
                        [00:28.00]أنا وعمري نعيشو في الهنا، ليلتنا زينة ومضيئة بسهرنا
                        [00:33.50]الحب يجمعنا والخير ينادينـا
                        [00:38.00]On va s'aimer, on va danser
                        [00:42.50]Yes, c'est la vie, lalalala!
                    """.trimIndent()
                ),
                SongEntity(
                    title = "My Way",
                    artist = "Frank Sinatra",
                    category = "Jazz / Traditional",
                    durationSec = 260,
                    languageCode = "EN",
                    lyricsLrc = """
                        [00:01.00]Piano orchestration playing...
                        [00:06.00]And now, the end is near
                        [00:11.20]And so I face the final curtain
                        [00:17.50]My friend, I'll say it clear
                        [00:22.80]I'll state my case, of which I'm certain
                        [00:29.00]I've lived a life that's full
                        [00:34.50]I traveled each and every highway
                        [00:40.20]And more, much more than this
                        [00:46.00]I did it my way...
                    """.trimIndent()
                ),
                SongEntity(
                    title = "Aicha",
                    artist = "Cheb Mami & Outlandish",
                    category = "Rai / Fusion",
                    durationSec = 240,
                    languageCode = "AR",
                    lyricsLrc = """
                        [00:01.00]Aicha, Aicha, écoute-moi...
                        [00:05.00]Comme si je n'existais pas
                        [00:09.50]Elle est passée à côté de moi
                        [00:14.00]عايشة يا عايشة، نبغيك ونموت عليك
                        [00:18.50]هذه غنيتنا، والقلب ينده ليك
                        [00:23.00]She said: "Keep your gold, I want more than that"
                        [00:27.50]Equal rights, respect, and a soul intact!
                    """.trimIndent()
                )
            )
            songDao.insertSongs(seedList)
        }

        // Check and seed default playlists if empty
        if (playlistDao.getAllPlaylists().first().isEmpty()) {
            playlistDao.insertPlaylist(PlaylistEntity(name = "Party Hits", songsCount = 3))
            playlistDao.insertPlaylist(PlaylistEntity(name = "Acoustic Gems", songsCount = 1))
            playlistDao.insertPlaylist(PlaylistEntity(name = "French Classics", songsCount = 1))
        }
    }
}
