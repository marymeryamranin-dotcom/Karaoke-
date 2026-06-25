package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.*
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.Localizer
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.KaraokeViewModel
import com.example.ui.viewmodel.LyricLine
import kotlinx.coroutines.launch

@Composable
fun KaraokeAppContent(viewModel: KaraokeViewModel) {
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    val layoutDirection = Localizer.getLayoutDirection(language)

    MyApplicationTheme(darkTheme = isDarkMode) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "screen_nav"
                ) { screen ->
                    when (screen) {
                        "login" -> LoginScreen(viewModel)
                        "home" -> AppShell(viewModel) { HomeScreen(viewModel) }
                        "sing" -> SingScreen(viewModel)
                        "profile" -> AppShell(viewModel) { ProfileScreen(viewModel) }
                        "admin" -> AppShell(viewModel) { AdminDashboardScreen(viewModel) }
                    }
                }
            }
        }
    }
}

// Shell wrapper to provide bottom navigation and top bars to primary app views
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    viewModel: KaraokeViewModel,
    content: @Composable () -> Unit
) {
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    val tAppName = Localizer.translate("app_name", language)
    val tHome = Localizer.translate("sing", language)
    val tRecordings = Localizer.translate("profile", language)
    val tAdmin = Localizer.translate("admin", language)

    val currentUser = (authState as? AuthState.Authenticated)?.user

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = SpotifyGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tAppName,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Quick Language Switcher
                    IconButton(onClick = {
                        val nextLang = when (language) {
                            AppLanguage.ENGLISH -> AppLanguage.FRENCH
                            AppLanguage.FRENCH -> AppLanguage.ARABIC
                            AppLanguage.ARABIC -> AppLanguage.ENGLISH
                        }
                        viewModel.setLanguage(nextLang)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Theme Toggle
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (currentUser != null) {
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Log Out",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentScreen == "home",
                    onClick = { viewModel.navigateTo("home") },
                    icon = { Icon(Icons.Default.Mic, contentDescription = null) },
                    label = { Text(tHome) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpotifyGreen,
                        selectedTextColor = SpotifyGreen,
                        indicatorColor = SpotifyGreen.copy(alpha = 0.15f)
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == "profile",
                    onClick = { viewModel.navigateTo("profile") },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text(tRecordings) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpotifyGreen,
                        selectedTextColor = SpotifyGreen,
                        indicatorColor = SpotifyGreen.copy(alpha = 0.15f)
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == "admin",
                    onClick = { viewModel.navigateTo("admin") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(tAdmin) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpotifyGreen,
                        selectedTextColor = SpotifyGreen,
                        indicatorColor = SpotifyGreen.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}

// ==========================================
// 1. LOGIN SCREEN
// ==========================================
@Composable
fun LoginScreen(viewModel: KaraokeViewModel) {
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }

    val tLogin = Localizer.translate("login", language)
    val tRegister = Localizer.translate("register", language)
    val tUsername = Localizer.translate("username", language)
    val tEmail = Localizer.translate("email", language)
    val tPassword = Localizer.translate("password", language)
    val tNoAccount = Localizer.translate("no_account", language)
    val tHaveAccount = Localizer.translate("have_account", language)
    val tWelcome = Localizer.translate("welcome", language)
    val tEnterDetails = Localizer.translate("enter_details", language)
    val tCreateAccount = Localizer.translate("create_account", language)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .drawBehind {
                // Background visual effects
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(SpotifyGreen.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.15f),
                        radius = size.minDimension * 0.6f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonFuchsia.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.75f),
                        radius = size.minDimension * 0.6f
                    )
                )
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        // Glowing App Mascot
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(SpotifyGreen.copy(alpha = 0.1f))
                .border(2.dp, SpotifyGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isRegistering) tCreateAccount else tWelcome,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tEnterDetails,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(tUsername) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (isRegistering) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(tEmail) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(tPassword) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (username.isNotBlank()) {
                    viewModel.loginUser(username.trim(), if (isRegistering) email.trim() else "${username.trim()}@karaoke.com")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
        ) {
            Text(
                text = if (isRegistering) tRegister else tLogin,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = { isRegistering = !isRegistering }) {
            Text(
                text = if (isRegistering) tHaveAccount else tNoAccount,
                color = SpotifyGreen,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==========================================
// 2. HOME SCREEN (EXPLORE & SING SELECTION)
// ==========================================
@Composable
fun HomeScreen(viewModel: KaraokeViewModel) {
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val tSearch = Localizer.translate("search_hint", language)
    val tTapToSing = Localizer.translate("tap_to_sing", language)
    val tHistory = Localizer.translate("history", language)
    val tFavorites = Localizer.translate("favorites", language)
    val tEmptyFavorites = Localizer.translate("empty_favorites", language)

    val categories = listOf("All", "Pop / Synthwave", "Classic / Chanson", "Rai / Pop", "Jazz / Traditional", "Rai / Fusion")

    // Filter songs dynamically
    val filteredSongs = remember(allSongs, searchQuery, selectedCategory) {
        allSongs.filter { song ->
            val matchQuery = song.title.contains(searchQuery, ignoreCase = true) ||
                    song.artist.contains(searchQuery, ignoreCase = true) ||
                    song.category.contains(searchQuery, ignoreCase = true)

            val matchCat = selectedCategory == null || selectedCategory == "All" || song.category == selectedCategory
            matchQuery && matchCat
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcoming Hero banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(SpotifyGreen.copy(alpha = 0.4f), NeonFuchsia.copy(alpha = 0.15f))
                            )
                        )
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = "Vocal Challenge of the Day!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sing 'Blinding Lights' with a 90+ pitch accuracy and win 500 bonus XP!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text(tSearch) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SpotifyGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
            )
        )

        // Categories selector
        LazyRow(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat || (selectedCategory == null && cat == "All")
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setSelectedCategory(if (cat == "All") null else cat) },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpotifyGreen,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        // Main song list heading
        Text(
            text = tTapToSing,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Song List
        if (filteredSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No songs found matches query.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (song in filteredSongs) {
                    SongCardItem(song, viewModel)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent history segment
        if (recentHistory.isNotEmpty()) {
            Text(
                text = tHistory,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(recentHistory) { hist ->
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .clickable {
                                val s = allSongs.find { it.id == hist.songId }
                                if (s != null) viewModel.selectSongToSing(s)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Icon(Icons.Default.History, contentDescription = null, tint = SpotifyGreen)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(hist.songTitle, maxLines = 1, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis)
                            Text(hist.artist, maxLines = 1, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongCardItem(song: SongEntity, viewModel: KaraokeViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.selectSongToSing(song) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simulated album thumbnail with neon letters
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NeonCyan, SpotifyGreen)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = song.title.take(1),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(SpotifyGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(song.category, fontSize = 9.sp, color = SpotifyGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${song.durationSec}s", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                }
            }

            IconButton(onClick = { viewModel.toggleSongFavorite(song) }) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) NeonFuchsia else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ==========================================
// 3. SING SCREEN (CORE TIKTOK/SPOTIFY PLAYBACK)
// ==========================================
@Composable
fun SingScreen(viewModel: KaraokeViewModel) {
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()
    val songNullable by viewModel.activeSong.collectAsStateWithLifecycle()
    val song = songNullable
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.playbackProgressMs.collectAsStateWithLifecycle()
    val speed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val pitch by viewModel.pitchShift.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val liveScore by viewModel.currentLiveScore.collectAsStateWithLifecycle()
    val activeRating by viewModel.activeScoreRating.collectAsStateWithLifecycle()
    val parsedLyrics by viewModel.parsedLyrics.collectAsStateWithLifecycle()
    val pitchGap by viewModel.livePitchGap.collectAsStateWithLifecycle()
    val waveform by viewModel.liveWaveform.collectAsStateWithLifecycle()

    // Effects state
    val vocalRemoval by viewModel.vocalRemovalActive.collectAsStateWithLifecycle()
    val echo by viewModel.echoDelay.collectAsStateWithLifecycle()
    val reverb by viewModel.reverbIntensity.collectAsStateWithLifecycle()
    val bass by viewModel.bassBoost.collectAsStateWithLifecycle()
    val micGain by viewModel.micGain.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showEffectsDrawer by remember { mutableStateOf(false) }

    val activeLine = viewModel.findActiveLyricLine(progress)

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Automatically scroll active lyric to middle of viewport
    LaunchedEffect(activeLine) {
        if (activeLine != null) {
            val idx = parsedLyrics.indexOf(activeLine)
            if (idx != -1) {
                listState.animateScrollToItem(Math.max(0, idx - 2))
            }
        }
    }

    if (song == null) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDarkSlate)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // High fidelity canvas background animation
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw concentric glowing sound waves in background based on recording
            val pulse = if (isPlaying) (System.currentTimeMillis() % 2000) / 2000f else 0.5f
            val baseRadius = size.minDimension * 0.25f
            val maxExtraRadius = size.minDimension * 0.3f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SpotifyGreen.copy(alpha = 0.08f * (1f - pulse)), Color.Transparent),
                    center = center,
                    radius = baseRadius + maxExtraRadius * pulse
                )
            )

            // Draw a subtle pitch tracker guide line behind
            val steps = 30
            val pathPoints = mutableListOf<Offset>()
            for (i in 0..steps) {
                val x = (size.width / steps) * i
                // Pitch target vs user alignment offset
                val waveAmplitude = 12.dp.toPx()
                val freq = 2.5 * Math.PI * i / steps
                val y = size.height * 0.45f + Math.sin(freq).toFloat() * waveAmplitude + (pitchGap * 25.dp.toPx())
                pathPoints.add(Offset(x, y))
            }
            for (i in 0 until pathPoints.size - 1) {
                drawLine(
                    color = if (isRecording) SpotifyGreen.copy(alpha = 0.35f) else NeonCyan.copy(alpha = 0.2f),
                    start = pathPoints[i],
                    end = pathPoints[i + 1],
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Full Screen Karaoke Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Controls & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.pausePlayer()
                    viewModel.navigateTo("home")
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                // Dynamic Header showing title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = song.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = song.artist,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = { viewModel.toggleSongFavorite(song) }) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) NeonFuchsia else Color.White
                    )
                }
            }

            // Real-Time Score Tracker HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular scoring widget
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(Localizer.translate("score", language), fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                        Text(
                            text = if (isRecording) "$liveScore" else "Ready",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SpotifyGreen
                        )
                        Text(activeRating, fontSize = 11.sp, color = NeonFuchsia, fontWeight = FontWeight.Bold)
                    }
                }

                // Waveform mini bar when recording
                if (isRecording) {
                    Row(
                        modifier = Modifier.height(40.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        waveform.takeLast(15).forEach { value ->
                            val scaleHeight = (value * 30.dp.value).dp
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(scaleHeight)
                                    .clip(CircleShape)
                                    .background(SpotifyGreen)
                            )
                        }
                    }
                }
            }

            // Synchronized Scrolling Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 120.dp)
                ) {
                    items(parsedLyrics) { line ->
                        val isActive = line == activeLine
                        val activeWordIdx = if (isActive) viewModel.getActiveWordIndex(progress, line) else -1

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (line.words.isNotEmpty()) {
                                // Word-by-word highlighted text line
                                FlowRow(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    line.words.forEachIndexed { wordIdx, word ->
                                        val isWordHighlighted = isActive && wordIdx <= activeWordIdx
                                        val wordColor = if (isWordHighlighted) SpotifyGreen else if (isActive) Color.White else Color.White.copy(alpha = 0.35f)
                                        val scaleFont = if (isActive) 22.sp else 16.sp
                                        val weightFont = if (isActive) FontWeight.ExtraBold else FontWeight.Medium

                                        Text(
                                            text = "$word ",
                                            color = wordColor,
                                            fontSize = scaleFont,
                                            fontWeight = weightFont,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = line.text,
                                    color = if (isActive) SpotifyGreen else Color.White.copy(alpha = 0.35f),
                                    fontSize = if (isActive) 22.sp else 16.sp,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Vocal Removal AI Control Overlay
            Button(
                onClick = { viewModel.toggleVocalRemoval() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (vocalRemoval) SpotifyGreen else Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = if (vocalRemoval) Icons.Default.MusicVideo else Icons.Default.VolumeMute,
                    contentDescription = null,
                    tint = if (vocalRemoval) Color.Black else Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (vocalRemoval) "Vocal Cut Active (Instrumental Only)" else "Normal Vocals (Dual Track)",
                    color = if (vocalRemoval) Color.Black else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Audio Player Dashboard (Speed, Pitch, Play, Rec)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                // Pitch and Speed controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pitch controls (-4 to +4)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("KEY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.setPitch(Math.max(-4, pitch - 1)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = if (pitch >= 0) "+$pitch" else "$pitch",
                            color = SpotifyGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(
                            onClick = { viewModel.setPitch(Math.min(4, pitch + 1)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Speed controls (0.5x, 1x, 1.5x, 2x)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SPEED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = {
                            val nextSpeed = when (speed) {
                                0.5f -> 1.0f
                                1.0f -> 1.5f
                                1.5f -> 2.0f
                                else -> 0.5f
                            }
                            viewModel.setSpeed(nextSpeed)
                        }) {
                            Text("${speed}x", color = SpotifyGreen, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // Effects panel slider trigger
                    IconButton(onClick = { showEffectsDrawer = !showEffectsDrawer }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Audio Effects",
                            tint = if (showEffectsDrawer) SpotifyGreen else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Playback progress bar
                val durationMs = (song.durationSec) * 1000L
                val progRatio = if (durationMs > 0) progress.toFloat() / durationMs else 0f
                Slider(
                    value = progRatio,
                    onValueChange = { viewModel.seekTo((it * durationMs).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = SpotifyGreen,
                        activeTrackColor = SpotifyGreen,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currSecs = progress / 1000
                    val durSecs = song.durationSec
                    Text(
                        text = String.format("%02d:%02d", currSecs / 60, currSecs % 60),
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = String.format("%02d:%02d", durSecs / 60, durSecs % 60),
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Record & Play Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Record mic button
                    IconButton(
                        onClick = { viewModel.toggleRecording() },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) NeonFuchsia else Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.RadioButtonChecked else Icons.Default.Mic,
                            contentDescription = "Record Voice",
                            tint = if (isRecording) Color.White else SpotifyGreen
                        )
                    }

                    // Play Pause button
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(SpotifyGreen)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Done & Save Button
                    IconButton(
                        onClick = { viewModel.stopSingingAndSave() },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = "Save Vocal Mix",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Live Audio effects slide-over drawer
        if (showEffectsDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(DarkGreyCard, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(20.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Localizer.translate("effects", language),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { showEffectsDrawer = false }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Echo Delay
                    Text("${Localizer.translate("echo", language)}: ${String.format("%.2fs", echo)}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    Slider(
                        value = echo,
                        onValueChange = { viewModel.setEchoDelay(it) },
                        valueRange = 0f..1.5f,
                        colors = SliderDefaults.colors(activeTrackColor = SpotifyGreen, thumbColor = SpotifyGreen)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reverb Intensity
                    Text("${Localizer.translate("reverb", language)}: ${String.format("%d%%", (reverb * 100).toInt())}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    Slider(
                        value = reverb,
                        onValueChange = { viewModel.setReverbIntensity(it) },
                        colors = SliderDefaults.colors(activeTrackColor = SpotifyGreen, thumbColor = SpotifyGreen)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bass Boost
                    Text("${Localizer.translate("bass_boost", language)}: ${String.format("%d%%", (bass * 100).toInt())}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    Slider(
                        value = bass,
                        onValueChange = { viewModel.setBassBoost(it) },
                        colors = SliderDefaults.colors(activeTrackColor = SpotifyGreen, thumbColor = SpotifyGreen)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mic Gain Volume
                    Text("${Localizer.translate("mic_gain", language)}: ${String.format("%.1fx", micGain)}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    Slider(
                        value = micGain,
                        onValueChange = { viewModel.setMicGain(it) },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(activeTrackColor = SpotifyGreen, thumbColor = SpotifyGreen)
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. PROFILE SCREEN (XP, BADGES, HISTORY & SYNC)
// ==========================================
@Composable
fun ProfileScreen(viewModel: KaraokeViewModel) {
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val recordings by viewModel.allRecordings.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()

    val tXp = Localizer.translate("xp", language)
    val tLevel = Localizer.translate("level", language)
    val tAchievements = Localizer.translate("achievements", language)
    val tBadges = Localizer.translate("badges", language)
    val tShare = Localizer.translate("share", language)
    val tRecordingsHeading = Localizer.translate("recordings", language)
    val tEmptyRec = Localizer.translate("empty_recordings", language)

    val currentUser = (authState as? AuthState.Authenticated)?.user ?: return
    val context = LocalContext.current

    // Calculated progress to next Level (XP base 500 per level)
    val nextLevelXp = 500
    val currentLevelXp = currentUser.xp % nextLevelXp
    val progressRatio = currentLevelXp.toFloat() / nextLevelXp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // High fidelity user profile badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors = listOf(SpotifyGreen, NeonFuchsia))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentUser.username.take(2).uppercase(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "@${currentUser.username}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = currentUser.rank,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpotifyGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${currentUser.email}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Level & XP Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("$tLevel ${currentUser.level}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("$currentLevelXp / $nextLevelXp XP", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progressRatio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = SpotifyGreen,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Overall XP earned: ${currentUser.xp}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Achievements / Badges Grid
        Text(
            text = tAchievements,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                BadgeCard(name = "First Verse", desc = "Sang 1 song", icon = Icons.Default.EmojiEvents, unlocked = true)
            }
            item {
                BadgeCard(name = "Pitch Perfect", desc = "Scored 90+ on a song", icon = Icons.Default.Star, unlocked = currentUser.level >= 2)
            }
            item {
                BadgeCard(name = "Night Owl", desc = "Sing past 11 PM", icon = Icons.Default.NightlightRound, unlocked = true)
            }
            item {
                BadgeCard(name = "Vocal Maestro", desc = "Earn 2,000 overall XP", icon = Icons.Default.MusicNote, unlocked = currentUser.xp >= 2000)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Vocal recordings with Playback & Sync buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tRecordingsHeading,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            if (syncProgress != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Syncing $syncProgress%", fontSize = 11.sp, color = SpotifyGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(tEmptyRec, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), textAlign = TextAlign.Center)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recordings.forEach { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.songTitle, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(record.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Score: ${record.score}", fontSize = 11.sp, color = SpotifyGreen, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(record.dateString, fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Direct cloud sync
                                IconButton(onClick = { viewModel.syncRecordingToCloud(record) }) {
                                    Icon(
                                        imageVector = if (record.isCloudSynced) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                        contentDescription = "Cloud backup",
                                        tint = if (record.isCloudSynced) SpotifyGreen else MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Playback simulated
                                IconButton(onClick = {
                                    Toast.makeText(context, "Playing recorded vocals locally. Effects applied - Reverb: ${record.reverbIntensity}, Echo: ${record.echoDelay}s", Toast.LENGTH_LONG).show()
                                }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Listen", tint = SpotifyGreen)
                                }

                                // Social share button
                                IconButton(onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Listen to me sing '${record.songTitle}' on Karaoke Studio! I scored a ${record.score} points! 🎤")
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, tShare)
                                    context.startActivity(shareIntent)
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                                }

                                // Delete
                                IconButton(onClick = { viewModel.deleteRecording(record.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeCard(name: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, unlocked: Boolean) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(130.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (unlocked) SpotifyGreen.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (unlocked) SpotifyGreen else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(name, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
            Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

// ==========================================
// 5. ADMIN SONG MANAGEMENT DASHBOARD
// ==========================================
@Composable
fun AdminDashboardScreen(viewModel: KaraokeViewModel) {
    val language by viewModel.appLanguage.collectAsStateWithLifecycle()
    val allSongs by viewModel.allSongs.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var durationSec by remember { mutableStateOf("180") }
    var lyricsLrc by remember { mutableStateOf("") }
    var selectedLangCode by remember { mutableStateOf("EN") }

    val tAdmin = Localizer.translate("admin", language)
    val tIntro = Localizer.translate("admin_intro", language)
    val tAddSong = Localizer.translate("add_song", language)
    val tTitle = Localizer.translate("song_title", language)
    val tArtist = Localizer.translate("artist", language)
    val tCat = Localizer.translate("category", language)
    val tDuration = Localizer.translate("duration", language)
    val tLyrics = Localizer.translate("lyrics_lrc", language)
    val tSave = Localizer.translate("save", language)

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(tAdmin, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(tIntro, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(tAddSong, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SpotifyGreen)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(tTitle) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(tArtist) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(tCat) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = durationSec,
                        onValueChange = { durationSec = it },
                        label = { Text("$tDuration (sec)") },
                        modifier = Modifier.weight(1f)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Language Code", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        Row {
                            listOf("EN", "FR", "AR").forEach { code ->
                                val selected = selectedLangCode == code
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedLangCode = code },
                                    label = { Text(code) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = lyricsLrc,
                    onValueChange = { lyricsLrc = it },
                    label = { Text(tLyrics) },
                    placeholder = { Text(Localizer.translate("lyrics_placeholder", language)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank() && artist.isNotBlank()) {
                            val dur = durationSec.toIntOrNull() ?: 180
                            val finalLrc = if (lyricsLrc.isBlank()) {
                                "[00:01.00]Default synchronized lyrics...\n[00:10.00]Song of $title"
                            } else lyricsLrc

                            viewModel.adminAddSong(title, artist, category, dur, finalLrc, selectedLangCode)

                            // Reset
                            title = ""
                            artist = ""
                            category = ""
                            lyricsLrc = ""
                            Toast.makeText(context, "Track added successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please complete song details", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                ) {
                    Text(tSave, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(Localizer.translate("custom_songs", language), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            allSongs.forEach { song ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(song.title, fontWeight = FontWeight.Bold)
                            Text("${song.artist} • ${song.category}", fontSize = 12.sp)
                        }
                        IconButton(onClick = { viewModel.adminDeleteSong(song.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// Custom flow row layout for displaying lyrics word-by-word responsive wrap
@Composable
fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val layoutWidth = constraints.maxWidth
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0

        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width > layoutWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val rowHeights = rows.map { row -> row.maxOfOrNull { it.height } ?: 0 }
        val layoutHeight = rowHeights.sum()

        layout(layoutWidth, layoutHeight) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                val rowHeight = rowHeights[rowIndex]
                val totalRowWidth = row.sumOf { it.width }
                var x = when (horizontalArrangement) {
                    Arrangement.Center -> (layoutWidth - totalRowWidth) / 2
                    Arrangement.End -> layoutWidth - totalRowWidth
                    else -> 0
                }
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width
                }
                y += rowHeight
            }
        }
    }
}
