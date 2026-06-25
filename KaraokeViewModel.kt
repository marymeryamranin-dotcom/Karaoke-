package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.*
import com.example.data.repository.KaraokeRepository
import com.example.ui.localization.AppLanguage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

// Lyric line model parsed from LRC format
data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val words: List<String>
)

sealed interface AuthState {
    object Unauthenticated : AuthState
    data class Authenticated(val user: UserEntity) : AuthState
}

class KaraokeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = KaraokeRepository(db)

    // UI language
    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    // Dark Mode Toggle
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Auth State
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Search and Categories
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Songs & Seeding State
    val allSongs: StateFlow<List<SongEntity>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<SongEntity>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecordings: StateFlow<List<RecordingEntity>> = repository.allRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHistory: StateFlow<List<HistoryEntity>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active screen navigation (simulated or explicit Compose screens)
    private val _currentScreen = MutableStateFlow("login") // login, home, sing, profile, admin
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // ==========================================
    // KARAOKE PLAYER ENGINE & SINGING ROOM STATE
    // ==========================================
    private val _activeSong = MutableStateFlow<SongEntity?>(null)
    val activeSong: StateFlow<SongEntity?> = _activeSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgressMs = MutableStateFlow(0L)
    val playbackProgressMs: StateFlow<Long> = _playbackProgressMs.asStateFlow()

    // Key pitch shift (-4 to +4)
    private val _pitchShift = MutableStateFlow(0)
    val pitchShift: StateFlow<Int> = _pitchShift.asStateFlow()

    // Speed (0.5x to 2.0x)
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Audio effects
    private val _vocalRemovalActive = MutableStateFlow(false)
    val vocalRemovalActive: StateFlow<Boolean> = _vocalRemovalActive.asStateFlow()

    private val _echoDelay = MutableStateFlow(0.3f) // seconds
    val echoDelay: StateFlow<Float> = _echoDelay.asStateFlow()

    private val _reverbIntensity = MutableStateFlow(0.4f)
    val reverbIntensity: StateFlow<Float> = _reverbIntensity.asStateFlow()

    private val _bassBoost = MutableStateFlow(0.5f)
    val bassBoost: StateFlow<Float> = _bassBoost.asStateFlow()

    private val _micGain = MutableStateFlow(1.2f)
    val micGain: StateFlow<Float> = _micGain.asStateFlow()

    // Recording State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Live Decibels Waveform
    private val _liveWaveform = MutableStateFlow<List<Float>>(emptyList())
    val liveWaveform: StateFlow<List<Float>> = _liveWaveform.asStateFlow()

    // Real-time singing scoring system
    private val _currentLiveScore = MutableStateFlow(0)
    val currentLiveScore: StateFlow<Int> = _currentLiveScore.asStateFlow()

    private val _activeScoreRating = MutableStateFlow("Keep it up! 🎤") // Perfect, Great, Good, Keep it up
    val activeScoreRating: StateFlow<String> = _activeScoreRating.asStateFlow()

    // Live pitch target vs actual alignment helper (for visual feedback)
    private val _livePitchGap = MutableStateFlow(0f) // 0 means perfectly aligned
    val livePitchGap: StateFlow<Float> = _livePitchGap.asStateFlow()

    // Cloud syncing state
    private val _syncProgress = MutableStateFlow<Int?>(null) // null = idle, 0-100 = syncing
    val syncProgress: StateFlow<Int?> = _syncProgress.asStateFlow()

    // Parsed lyrics for active song
    private val _parsedLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val parsedLyrics: StateFlow<List<LyricLine>> = _parsedLyrics.asStateFlow()

    private var playbackJob: Job? = null
    private var waveformJob: Job? = null

    init {
        // Seed songs and check if we already have a logged in user
        viewModelScope.launch {
            repository.seedSongsIfEmpty()
            // Auto login a default beautiful profile for preview speed!
            loginUser("guest_singer", "sing123")
        }
    }

    // ==========================================
    // ACTIONS & CONTROLS
    // ==========================================

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setLanguage(lang: AppLanguage) {
        _appLanguage.value = lang
        val user = (authState.value as? AuthState.Authenticated)?.user
        if (user != null) {
            viewModelScope.launch {
                repository.updateUserLanguage(user.username, lang.name)
                _authState.value = AuthState.Authenticated(user.copy(selectedLanguage = lang.name))
            }
        }
    }

    fun toggleDarkMode() {
        val next = !isDarkMode.value
        _isDarkMode.value = next
        val user = (authState.value as? AuthState.Authenticated)?.user
        if (user != null) {
            viewModelScope.launch {
                repository.updateUserTheme(user.username, next)
                _authState.value = AuthState.Authenticated(user.copy(isDarkMode = next))
            }
        }
    }

    // Auth Engine
    fun loginUser(username: String, email: String) {
        viewModelScope.launch {
            val existing = repository.getUser(username)
            if (existing != null) {
                _authState.value = AuthState.Authenticated(existing)
                _appLanguage.value = AppLanguage.valueOf(existing.selectedLanguage)
                _isDarkMode.value = existing.isDarkMode
                _currentScreen.value = "home"
            } else {
                // Register standard profile with awesome XP and rank
                val newUser = UserEntity(
                    username = username,
                    email = email,
                    xp = 340,
                    level = 3,
                    rank = "Silver Vocalist",
                    selectedLanguage = _appLanguage.value.name,
                    isDarkMode = _isDarkMode.value
                )
                repository.registerUser(newUser)
                _authState.value = AuthState.Authenticated(newUser)
                _currentScreen.value = "home"
            }
        }
    }

    fun logout() {
        _authState.value = AuthState.Unauthenticated
        _currentScreen.value = "login"
    }

    // Song Selection and Sing Start
    fun selectSongToSing(song: SongEntity) {
        _activeSong.value = song
        _parsedLyrics.value = parseLrc(song.lyricsLrc)
        // Reset playback stats
        _playbackProgressMs.value = 0L
        _pitchShift.value = 0
        _playbackSpeed.value = 1.0f
        _currentLiveScore.value = 0
        _vocalRemovalActive.value = false
        _isRecording.value = false
        _liveWaveform.value = emptyList()

        // Track in history
        viewModelScope.launch {
            repository.addSongToHistory(song.id, song.title, song.artist)
        }
        navigateTo("sing")
    }

    // LRC Parser
    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = lrcText.split("\n")
        val lyricLines = mutableListOf<LyricLine>()
        val timeRegex = """\[(\d+):(\d+)\.(\d+)\]""".toRegex()

        for (line in lines) {
            val match = timeRegex.find(line)
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val ms = match.groupValues[3].toLong() * 10 // Convert e.g. .50 to 500ms
                val timestamp = (min * 60 * 1000) + (sec * 1000) + ms
                val text = line.replace(timeRegex, "").trim()
                val words = text.split(" ").filter { it.isNotEmpty() }
                lyricLines.add(LyricLine(timestamp, text, words))
            }
        }
        return lyricLines.sortedBy { it.timestampMs }
    }

    // Player controls
    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayer()
        } else {
            startPlayer()
        }
    }

    private fun startPlayer() {
        _isPlaying.value = true
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val tickRateMs = 50L
            val durationMs = (_activeSong.value?.durationSec ?: 120) * 1000L

            while (_isPlaying.value && _playbackProgressMs.value < durationMs) {
                delay(tickRateMs)
                val delta = (tickRateMs * _playbackSpeed.value).toLong()
                _playbackProgressMs.value += delta

                // Update real-time score & pitch indicator if singing/recording
                if (_isRecording.value) {
                    val activeLrcLine = findActiveLyricLine(_playbackProgressMs.value)
                    if (activeLrcLine != null && activeLrcLine.text.isNotEmpty()) {
                        // Simulate vocal pitching alignment:
                        // Random walk towards target pitch to keep the user engaged and looking professional
                        val gap = Random.nextFloat() * 1.8f - 0.9f
                        _livePitchGap.value = gap

                        if (Math.abs(gap) < 0.4f) {
                            _currentLiveScore.value += 1
                            _activeScoreRating.value = "Perfect! 🔥"
                        } else if (Math.abs(gap) < 0.7f) {
                            _currentLiveScore.value += 1
                            _activeScoreRating.value = "Great! ✨"
                        } else {
                            _activeScoreRating.value = "Good! 👍"
                        }
                    }
                }
            }
            if (_playbackProgressMs.value >= durationMs) {
                // Done singing!
                stopSingingAndSave()
            }
        }
    }

    fun pausePlayer() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun seekTo(progressMs: Long) {
        _playbackProgressMs.value = progressMs
    }

    fun setPitch(shift: Int) {
        _pitchShift.value = shift
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun toggleVocalRemoval() {
        _vocalRemovalActive.value = !_vocalRemovalActive.value
    }

    // Effects
    fun setEchoDelay(v: Float) { _echoDelay.value = v }
    fun setReverbIntensity(v: Float) { _reverbIntensity.value = v }
    fun setBassBoost(v: Float) { _bassBoost.value = v }
    fun setMicGain(v: Float) { _micGain.value = v }

    // Recording Controls
    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        _isRecording.value = true
        // If music not playing, start it
        if (!_isPlaying.value) {
            startPlayer()
        }

        // Start decibels/waveform visualizer flow
        waveformJob?.cancel()
        waveformJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(120)
                // Generate a random voice decibel sample between 0.1f and 1.0f
                val sample = Random.nextFloat() * 0.8f + 0.15f
                val currentList = _liveWaveform.value.takeLast(40).toMutableList()
                currentList.add(sample)
                _liveWaveform.value = currentList
            }
        }
    }

    private fun stopRecording() {
        _isRecording.value = false
        waveformJob?.cancel()
    }

    fun stopSingingAndSave() {
        pausePlayer()
        stopRecording()

        val song = _activeSong.value ?: return
        val user = (_authState.value as? AuthState.Authenticated)?.user ?: return

        viewModelScope.launch {
            // Save recording metadata to Room DB
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val scoreVal = if (_currentLiveScore.value > 0) Math.min(_currentLiveScore.value, 100) else Random.nextInt(75, 99)

            val newRecord = RecordingEntity(
                songId = song.id,
                songTitle = song.title,
                artist = song.artist,
                dateString = dateStr,
                score = scoreVal,
                localFilePath = "/storage/emulated/0/Android/data/com.example/files/recordings/vocal_${System.currentTimeMillis()}.wav",
                isCloudSynced = false,
                echoDelay = _echoDelay.value,
                reverbIntensity = _reverbIntensity.value,
                bassBoost = _bassBoost.value,
                micGain = _micGain.value,
                durationSec = song.durationSec
            )
            repository.saveRecording(newRecord)

            // Reward Singer XP & level up!
            val gainedXp = scoreVal * 5
            val currentXp = user.xp + gainedXp
            val newLevel = (currentXp / 500) + 1
            val rankTitle = when {
                newLevel >= 10 -> "Karaoke Legend"
                newLevel >= 7 -> "Platinum Artist"
                newLevel >= 5 -> "Gold Star"
                newLevel >= 3 -> "Silver Vocalist"
                else -> "Bronze Vocalist"
            }
            repository.updateUserProgress(user.username, currentXp, newLevel, rankTitle)

            // Sync model in authState
            _authState.value = AuthState.Authenticated(
                user.copy(xp = currentXp, level = newLevel, rank = rankTitle)
            )

            // Direct to profile to view achievements and mix files!
            navigateTo("profile")
        }
    }

    // Active Lyric Index Selector
    fun findActiveLyricLine(currentTimeMs: Long): LyricLine? {
        val lyrics = _parsedLyrics.value
        if (lyrics.isEmpty()) return null

        var activeLine: LyricLine? = null
        for (i in lyrics.indices) {
            if (lyrics[i].timestampMs <= currentTimeMs) {
                activeLine = lyrics[i]
            } else {
                break
            }
        }
        return activeLine
    }

    fun getActiveWordIndex(currentTimeMs: Long, line: LyricLine): Int {
        val lyrics = _parsedLyrics.value
        val index = lyrics.indexOf(line)
        if (index == -1 || line.words.isEmpty()) return 0

        // Determine duration of this line
        val nextTime = if (index + 1 < lyrics.size) lyrics[index + 1].timestampMs else (line.timestampMs + 8000)
        val lineDuration = nextTime - line.timestampMs
        val elapsed = currentTimeMs - line.timestampMs

        val msPerWord = lineDuration / line.words.size
        val activeWordIdx = (elapsed / msPerWord).toInt()
        return Math.min(Math.max(0, activeWordIdx), line.words.size - 1)
    }

    // Toggle heart icon on track
    fun toggleSongFavorite(song: SongEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, !song.isFavorite)
        }
    }

    fun deleteRecording(id: Int) {
        viewModelScope.launch {
            repository.deleteRecording(id)
        }
    }

    // Create a playlist
    fun addNewPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    // Simulated cloud storage backup
    fun syncRecordingToCloud(recording: RecordingEntity) {
        if (_syncProgress.value != null) return // Already syncing

        viewModelScope.launch {
            _syncProgress.value = 0
            for (p in 10..100 step 15) {
                delay(300)
                _syncProgress.value = p
            }
            delay(200)
            repository.markRecordingAsSynced(recording.id)
            _syncProgress.value = null
        }
    }

    // Admin track management
    fun adminAddSong(title: String, artist: String, category: String, durationSec: Int, lrcText: String, lang: String) {
        viewModelScope.launch {
            val newSong = SongEntity(
                title = title,
                artist = artist,
                category = category,
                durationSec = durationSec,
                lyricsLrc = lrcText,
                languageCode = lang
            )
            repository.addCustomSong(newSong)
        }
    }

    fun adminDeleteSong(songId: Int) {
        viewModelScope.launch {
            repository.deleteSong(songId)
        }
    }
}
