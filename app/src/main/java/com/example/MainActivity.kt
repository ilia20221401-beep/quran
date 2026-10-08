package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AudioPlayerDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.ReciterSelectorSheet
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.DhikrCounterScreen
import com.example.ui.screens.DuaScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.SurahDetailScreen
import com.example.ui.screens.SurahListScreen
import com.example.ui.theme.QuranAppTheme
import com.example.ui.viewmodel.QuranTab
import com.example.ui.viewmodel.QuranViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: QuranViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuranAppTheme {
                // Apply Right-To-Left layout direction for natural Arabic/Persian reading experience
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    QuranAppMain(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun QuranAppMain(viewModel: QuranViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val filteredSurahs by viewModel.filteredSurahs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val lastReadSurah by viewModel.lastReadSurah.collectAsStateWithLifecycle()

    val readingSurah by viewModel.readingSurah.collectAsStateWithLifecycle()
    val readingAyahs by viewModel.readingAyahs.collectAsStateWithLifecycle()
    val isLoadingVerses by viewModel.isLoadingVerses.collectAsStateWithLifecycle()
    val showTranslation by viewModel.showTranslation.collectAsStateWithLifecycle()
    val arabicFontSize by viewModel.arabicFontSize.collectAsStateWithLifecycle()

    val showReciterSheet by viewModel.showReciterSheet.collectAsStateWithLifecycle()
    val showFullPlayerDialog by viewModel.showFullPlayerDialog.collectAsStateWithLifecycle()

    val cachedSurahNumbers by viewModel.cachedSurahNumbers.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

    val dhikrCount by viewModel.dhikrCount.collectAsStateWithLifecycle()
    val dhikrTarget by viewModel.dhikrTarget.collectAsStateWithLifecycle()
    val selectedDhikrPreset by viewModel.selectedDhikrPreset.collectAsStateWithLifecycle()
    val dhikrMode by viewModel.dhikrMode.collectAsStateWithLifecycle()
    val dhikrSurah by viewModel.dhikrSurah.collectAsStateWithLifecycle()
    val dhikrAyahs by viewModel.dhikrAyahs.collectAsStateWithLifecycle()
    val dhikrAyahIndex by viewModel.dhikrAyahIndex.collectAsStateWithLifecycle()
    val verseRepeatTarget by viewModel.verseRepeatTarget.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Persistent mini audio player when a Surah is ready or playing
                if (playerState.currentSurah != null) {
                    MiniPlayerBar(
                        playerState = playerState,
                        onTogglePlay = { viewModel.togglePlayPause() },
                        onNext = { viewModel.audioManager.playNext() },
                        onCloseClick = { viewModel.stopAudio() },
                        onExpandClick = { viewModel.setShowFullPlayerDialog(true) },
                        onReciterClick = { viewModel.setShowReciterSheet(true) }
                    )
                }

                // Bottom Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == QuranTab.SURAHS,
                        onClick = { viewModel.navigateTo(QuranTab.SURAHS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "فهرست سوره‌ها"
                            )
                        },
                        label = { Text("سوره‌ها") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_surahs")
                    )

                    NavigationBarItem(
                        selected = currentTab == QuranTab.READER,
                        onClick = { viewModel.navigateTo(QuranTab.READER) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Subject,
                                contentDescription = "متن و ترجمه"
                            )
                        },
                        label = { Text("قرائت") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_reader")
                    )

                    NavigationBarItem(
                        selected = currentTab == QuranTab.DUAS,
                        onClick = { viewModel.navigateTo(QuranTab.DUAS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = "ادعیه قرآنی"
                            )
                        },
                        label = { Text("ادعیه") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_duas")
                    )

                    NavigationBarItem(
                        selected = currentTab == QuranTab.FAVORITES,
                        onClick = { viewModel.navigateTo(QuranTab.FAVORITES) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "نشانه‌ها"
                            )
                        },
                        label = { Text("نشانه‌ها") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_favorites")
                    )

                    NavigationBarItem(
                        selected = currentTab == QuranTab.TASBIH,
                        onClick = { viewModel.navigateTo(QuranTab.TASBIH) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "ذکرشمار"
                            )
                        },
                        label = { Text("ذکرشمار") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_tasbih")
                    )

                    NavigationBarItem(
                        selected = currentTab == QuranTab.ABOUT,
                        onClick = { viewModel.navigateTo(QuranTab.ABOUT) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "درباره"
                            )
                        },
                        label = { Text("درباره") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_about")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                QuranTab.SURAHS -> {
                    SurahListScreen(
                        surahs = filteredSurahs,
                        lastReadSurah = lastReadSurah,
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        playerState = playerState,
                        favoriteIds = favoriteIds,
                        cachedSurahNumbers = cachedSurahNumbers,
                        downloadProgress = downloadProgress,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onFilterSelect = { viewModel.setFilter(it) },
                        onSurahClick = { viewModel.openSurahReader(it) },
                        onPlaySurah = { viewModel.onSurahAction(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDownloadSurah = { viewModel.downloadSurah(it) },
                        onSelectReciter = { viewModel.setShowReciterSheet(true) }
                    )
                }

                QuranTab.READER -> {
                    val surahToRead = readingSurah ?: filteredSurahs.firstOrNull()
                    if (surahToRead != null) {
                        SurahDetailScreen(
                            surah = surahToRead,
                            ayahs = readingAyahs,
                            isLoading = isLoadingVerses,
                            playerState = playerState,
                            isFavorite = favoriteIds.contains(surahToRead.number),
                            showTranslation = showTranslation,
                            arabicFontSize = arabicFontSize,
                            onBack = { viewModel.navigateTo(QuranTab.SURAHS) },
                            onTogglePlay = { viewModel.onSurahAction(surahToRead) },
                            onToggleFavorite = { viewModel.toggleFavorite(surahToRead.number) },
                            onToggleTranslation = { viewModel.toggleShowTranslation() },
                            onChangeFontSize = { viewModel.setArabicFontSize(it) },
                            onSeekToAyah = { viewModel.seekToAyah(it) }
                        )
                    }
                }

                QuranTab.DUAS -> {
                    DuaScreen(
                        onNavigateToSurah = { viewModel.openSurahReader(it) },
                        onBack = { viewModel.navigateTo(QuranTab.SURAHS) }
                    )
                }

                QuranTab.FAVORITES -> {
                    FavoritesScreen(
                        favoriteIds = favoriteIds,
                        playerState = playerState,
                        cachedSurahNumbers = cachedSurahNumbers,
                        downloadProgress = downloadProgress,
                        onSurahClick = { viewModel.openSurahReader(it) },
                        onPlaySurah = { viewModel.onSurahAction(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDownloadSurah = { viewModel.downloadSurah(it) },
                        onBack = { viewModel.navigateTo(QuranTab.SURAHS) }
                    )
                }

                QuranTab.TASBIH -> {
                    DhikrCounterScreen(
                        dhikrMode = dhikrMode,
                        currentSurah = dhikrSurah,
                        ayahs = dhikrAyahs,
                        currentAyahIndex = dhikrAyahIndex,
                        verseRepeatTarget = verseRepeatTarget,
                        presets = viewModel.dhikrPresets,
                        selectedPreset = selectedDhikrPreset,
                        count = dhikrCount,
                        target = dhikrTarget,
                        onIncrement = { viewModel.incrementDhikr() },
                        onReset = { viewModel.resetDhikr() },
                        onNextAyah = { viewModel.nextDhikrAyah() },
                        onPrevAyah = { viewModel.prevDhikrAyah() },
                        onSelectSurah = { viewModel.selectDhikrSurah(it) },
                        onSetRepeatTarget = { viewModel.setVerseRepeatTarget(it) },
                        onSetDhikrMode = { viewModel.setDhikrMode(it) },
                        onSelectPreset = { viewModel.selectDhikrPreset(it) },
                        onBack = { viewModel.navigateTo(QuranTab.SURAHS) }
                    )
                }

                QuranTab.ABOUT -> {
                    AboutScreen(
                        onBack = { viewModel.navigateTo(QuranTab.SURAHS) }
                    )
                }
            }
        }
    }

    // Modal Reciter Selector BottomSheet
    if (showReciterSheet) {
        ReciterSelectorSheet(
            selectedReciter = playerState.currentReciter,
            onReciterSelected = { viewModel.selectReciter(it) },
            onDismiss = { viewModel.setShowReciterSheet(false) }
        )
    }

    // Full Audio Player Dialog
    if (showFullPlayerDialog && playerState.currentSurah != null) {
        AudioPlayerDialog(
            playerState = playerState,
            onTogglePlay = { viewModel.togglePlayPause() },
            onStop = { viewModel.stopAudio() },
            onSeek = { viewModel.audioManager.seekTo(it) },
            onRewind10 = { viewModel.audioManager.seekRewind10s() },
            onForward10 = { viewModel.audioManager.seekForward10s() },
            onNext = { viewModel.audioManager.playNext() },
            onPrevious = { viewModel.audioManager.playPrevious() },
            onSelectReciter = {
                viewModel.setShowReciterSheet(true)
            },
            onSetSpeed = { viewModel.setPlaybackSpeed(it) },
            onSetRepeatMode = { viewModel.setRepeatMode(it) },
            onSetSleepTimer = { viewModel.setSleepTimer(it) },
            onDismiss = { viewModel.setShowFullPlayerDialog(false) }
        )
    }
}
