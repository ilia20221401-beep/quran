package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerState
import com.example.audio.QuranAudioManager
import com.example.audio.RepeatMode
import com.example.data.model.Ayah
import com.example.data.model.Reciter
import com.example.data.model.RecitersData
import com.example.data.model.Surah
import com.example.data.model.SurahDatabase
import com.example.data.remote.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class QuranTab {
    SURAHS,
    READER,
    FAVORITES,
    DUAS,
    TASBIH
}

enum class SurahFilter(val titleFa: String) {
    ALL("همه سوره‌ها"),
    JUZ_30("جزء ۳۰"),
    MECCAN("مکی"),
    MEDINAN("مدنی"),
    POPULAR("پرخوانش"),
    FAVORITES("نشان‌شده‌ها")
}

data class DhikrPreset(val title: String, val meaning: String, val defaultTarget: Int)

class QuranViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)
    private val repository = QuranRepository(application)
    val audioManager = QuranAudioManager(application)

    val playerState: StateFlow<AudioPlayerState> = audioManager.playerState
    val cachedSurahNumbers: StateFlow<Set<Int>> = audioManager.cacheManager.cachedSurahNumbers
    val downloadProgress: StateFlow<Map<Int, Float>> = audioManager.cacheManager.downloadProgress

    // Navigation
    private val _currentTab = MutableStateFlow(QuranTab.SURAHS)
    val currentTab: StateFlow<QuranTab> = _currentTab.asStateFlow()

    // Surah list filtering & search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SurahFilter.ALL)
    val selectedFilter: StateFlow<SurahFilter> = _selectedFilter.asStateFlow()

    // Favorites
    private val _favoriteIds = MutableStateFlow<Set<Int>>(loadFavorites())
    val favoriteIds: StateFlow<Set<Int>> = _favoriteIds.asStateFlow()

    // Last read
    private val _lastReadSurah = MutableStateFlow(loadLastReadSurah())
    val lastReadSurah: StateFlow<Surah?> = _lastReadSurah.asStateFlow()

    // Surah Reader state
    private val _readingSurah = MutableStateFlow<Surah?>(SurahDatabase.getSurah(1))
    val readingSurah: StateFlow<Surah?> = _readingSurah.asStateFlow()

    private val _readingAyahs = MutableStateFlow<List<Ayah>>(emptyList())
    val readingAyahs: StateFlow<List<Ayah>> = _readingAyahs.asStateFlow()

    private val _isLoadingVerses = MutableStateFlow(false)
    val isLoadingVerses: StateFlow<Boolean> = _isLoadingVerses.asStateFlow()

    private val _showTranslation = MutableStateFlow(true)
    val showTranslation: StateFlow<Boolean> = _showTranslation.asStateFlow()

    private val _arabicFontSize = MutableStateFlow(22f)
    val arabicFontSize: StateFlow<Float> = _arabicFontSize.asStateFlow()

    // Dialog & Sheet states
    private val _showReciterSheet = MutableStateFlow(false)
    val showReciterSheet: StateFlow<Boolean> = _showReciterSheet.asStateFlow()

    private val _showFullPlayerDialog = MutableStateFlow(false)
    val showFullPlayerDialog: StateFlow<Boolean> = _showFullPlayerDialog.asStateFlow()

    // Digital Tasbih state
    val dhikrPresets = listOf(
        DhikrPreset("صَلَوَات", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَ آلِ مُحَمَّد", 100),
        DhikrPreset("تسبیحات حضرت زهرا (س)", "الله اکبر (۳۴) - الحمد لله (۳۳) - سبحان الله (۳۳)", 100),
        DhikrPreset("سُبْحَانَ اللَّهِ", "پاک و منزه است خداوند", 33),
        DhikrPreset("الْحَمْدُ لِلَّهِ", "ستایش و سپاس مخصوص خداست", 33),
        DhikrPreset("لَا إِلٰهَ إِلَّا اللَّهُ", "هیچ معبودی جز خدای یگانه نیست", 100),
        DhikrPreset("أَسْتَغْفِرُ اللَّهَ", "از درگاه الهی طلب آمرزش می‌نمایم", 70),
        DhikrPreset("لَا حَوْلَ وَ لَا قُوَّةَ إِلَّا بِاللَّهِ", "هیچ نیرو و قدرتی جز به خواست خدا نیست", 100)
    )

    private val _selectedDhikrPreset = MutableStateFlow(dhikrPresets[0])
    val selectedDhikrPreset: StateFlow<DhikrPreset> = _selectedDhikrPreset.asStateFlow()

    private val _dhikrCount = MutableStateFlow(0)
    val dhikrCount: StateFlow<Int> = _dhikrCount.asStateFlow()

    private val _dhikrTarget = MutableStateFlow(100)
    val dhikrTarget: StateFlow<Int> = _dhikrTarget.asStateFlow()

    // Filtered surahs flow
    val filteredSurahs: StateFlow<List<Surah>> = combine(
        _searchQuery,
        _selectedFilter,
        _favoriteIds
    ) { query, filter, favorites ->
        val popularNumbers = setOf(1, 2, 18, 36, 55, 56, 67, 78, 112, 113, 114)
        SurahDatabase.surahs.filter { surah ->
            val matchesFilter = when (filter) {
                SurahFilter.ALL -> true
                SurahFilter.JUZ_30 -> surah.juz == 30
                SurahFilter.MECCAN -> surah.revelationType == "مکی"
                SurahFilter.MEDINAN -> surah.revelationType == "مدنی"
                SurahFilter.POPULAR -> surah.number in popularNumbers
                SurahFilter.FAVORITES -> surah.number in favorites
            }

            val q = query.trim().lowercase()
            val matchesQuery = if (q.isEmpty()) true else {
                surah.namePersian.lowercase().contains(q) ||
                surah.nameArabic.contains(q) ||
                surah.meaningPersian.contains(q) ||
                surah.englishName.lowercase().contains(q) ||
                surah.number.toString() == q
            }

            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SurahDatabase.surahs)

    init {
        // Load initial surah verses
        loadSurahVerses(1)
    }

    fun navigateTo(tab: QuranTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: SurahFilter) {
        _selectedFilter.value = filter
    }

    fun toggleFavorite(surahNumber: Int) {
        val current = _favoriteIds.value.toMutableSet()
        if (current.contains(surahNumber)) {
            current.remove(surahNumber)
        } else {
            current.add(surahNumber)
        }
        _favoriteIds.value = current
        saveFavorites(current)
    }

    fun isFavorite(surahNumber: Int): Boolean {
        return _favoriteIds.value.contains(surahNumber)
    }

    fun openSurahReader(surah: Surah) {
        _readingSurah.value = surah
        saveLastRead(surah.number)
        _lastReadSurah.value = surah
        loadSurahVerses(surah.number)
        _currentTab.value = QuranTab.READER
    }

    fun loadSurahVerses(surahNumber: Int) {
        viewModelScope.launch {
            _isLoadingVerses.value = true
            val ayahs = repository.getSurahVerses(surahNumber)
            _readingAyahs.value = ayahs
            _isLoadingVerses.value = false
            loadVerseTimings(surahNumber, ayahs)
        }
    }

    private fun loadVerseTimings(surahNumber: Int, ayahs: List<Ayah>) {
        viewModelScope.launch {
            val timings = repository.getVerseTimings(
                surahNumber = surahNumber,
                reciter = playerState.value.currentReciter,
                ayahs = ayahs,
                totalDurationMs = playerState.value.totalDurationMs
            )
            audioManager.updateVerseTimings(timings)
        }
    }

    fun playSurah(surah: Surah) {
        audioManager.playSurah(surah)
        viewModelScope.launch {
            val ayahs = if (_readingSurah.value?.number == surah.number && _readingAyahs.value.isNotEmpty()) {
                _readingAyahs.value
            } else {
                repository.getSurahVerses(surah.number)
            }
            loadVerseTimings(surah.number, ayahs)
        }
    }

    fun seekToAyah(ayahNumber: Int) {
        val current = playerState.value.currentSurah
        val reading = _readingSurah.value
        if (current != null && reading != null && current.number == reading.number) {
            audioManager.seekToAyah(ayahNumber)
        } else if (reading != null) {
            playSurah(reading)
            viewModelScope.launch {
                kotlinx.coroutines.delay(400)
                audioManager.seekToAyah(ayahNumber)
            }
        }
    }

    fun onSurahAction(surah: Surah) {
        val current = playerState.value.currentSurah
        if (current != null && current.number == surah.number) {
            audioManager.togglePlayPause()
        } else {
            audioManager.playSurah(surah)
        }
    }

    fun stopAudio() {
        audioManager.stop()
    }

    fun downloadSurah(surah: Surah) {
        viewModelScope.launch {
            audioManager.cacheManager.downloadAndCache(surah, playerState.value.currentReciter)
        }
    }

    fun deleteCachedSurah(surah: Surah) {
        audioManager.cacheManager.deleteCache(playerState.value.currentReciter, surah.number)
    }

    fun getTotalCacheSize(): String {
        return audioManager.cacheManager.getTotalCacheSizeFormatted()
    }

    fun playSurahWithReader(surah: Surah) {
        openSurahReader(surah)
        audioManager.playSurah(surah)
    }

    fun togglePlayPause() {
        audioManager.togglePlayPause()
    }

    fun selectReciter(reciter: Reciter) {
        audioManager.setReciter(reciter)
        _showReciterSheet.value = false
        val current = playerState.value.currentSurah ?: _readingSurah.value
        if (current != null) {
            viewModelScope.launch {
                val ayahs = _readingAyahs.value.ifEmpty { repository.getSurahVerses(current.number) }
                loadVerseTimings(current.number, ayahs)
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        audioManager.setPlaybackSpeed(speed)
    }

    fun setRepeatMode(mode: RepeatMode) {
        audioManager.setRepeatMode(mode)
    }

    fun setSleepTimer(minutes: Int) {
        audioManager.setSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        audioManager.cancelSleepTimer()
    }

    fun setShowReciterSheet(show: Boolean) {
        _showReciterSheet.value = show
    }

    fun setShowFullPlayerDialog(show: Boolean) {
        _showFullPlayerDialog.value = show
    }

    fun toggleShowTranslation() {
        _showTranslation.value = !_showTranslation.value
    }

    fun setArabicFontSize(size: Float) {
        _arabicFontSize.value = size.coerceIn(16f, 36f)
    }

    // Tasbih methods
    fun incrementDhikr() {
        val next = _dhikrCount.value + 1
        _dhikrCount.value = next
    }

    fun resetDhikr() {
        _dhikrCount.value = 0
    }

    fun selectDhikrPreset(preset: DhikrPreset) {
        _selectedDhikrPreset.value = preset
        _dhikrTarget.value = preset.defaultTarget
        _dhikrCount.value = 0
    }

    // Persistence
    private fun loadFavorites(): Set<Int> {
        val raw = prefs.getString("favorites", "") ?: ""
        if (raw.isEmpty()) return setOf(1, 36, 55, 67, 112) // Default favorites: Fatiha, Yasin, Rahman, Mulk, Ikhlas
        return raw.split(",").mapNotNull { it.toIntOrNull() }.toSet()
    }

    private fun saveFavorites(ids: Set<Int>) {
        prefs.edit().putString("favorites", ids.joinToString(",")).apply()
    }

    private fun loadLastReadSurah(): Surah? {
        val number = prefs.getInt("last_read_surah", 1)
        return SurahDatabase.getSurah(number)
    }

    private fun saveLastRead(surahNumber: Int) {
        prefs.edit().putInt("last_read_surah", surahNumber).apply()
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
