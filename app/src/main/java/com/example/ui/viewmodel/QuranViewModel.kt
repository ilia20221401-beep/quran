package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerState
import com.example.audio.QuranAudioManager
import com.example.audio.RepeatMode
import com.example.data.model.Ayah
import com.example.data.model.BookmarkedAyah
import com.example.data.model.DailyAyah
import com.example.data.model.DailyAyahManager
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
    PRAYER_TIMES,
    DUAS,
    TASBIH,
    ASMA_UL_HUSNA,
    ABOUT
}

enum class ReadingTheme(val titleFa: String) {
    DEFAULT("پیش‌فرض"),
    NIGHT("حالت شب"),
    SEPIA("کاغذ کاهی")
}

enum class SurahFilter(val titleFa: String) {
    ALL("همه سوره‌ها"),
    JUZ_30("جزء ۳۰"),
    MECCAN("مکی"),
    MEDINAN("مدنی"),
    POPULAR("پرخوانش"),
    FAVORITES("نشان‌شده‌ها")
}

enum class DhikrMode {
    QURAN_VERSES,
    GENERAL_DHIKR
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

    // Reading Theme (Default, Night, Sepia)
    private val _readingTheme = MutableStateFlow(loadReadingTheme())
    val readingTheme: StateFlow<ReadingTheme> = _readingTheme.asStateFlow()

    // Target Ayah to auto-scroll to when opening reader
    private val _targetAyahToScroll = MutableStateFlow<Int?>(null)
    val targetAyahToScroll: StateFlow<Int?> = _targetAyahToScroll.asStateFlow()

    // Bookmarked Ayahs & Notes
    private val _bookmarkedAyahs = MutableStateFlow<Map<String, BookmarkedAyah>>(loadBookmarkedAyahs())
    val bookmarkedAyahs: StateFlow<Map<String, BookmarkedAyah>> = _bookmarkedAyahs.asStateFlow()

    // Full Quran Search Dialog
    private val _showFullSearchDialog = MutableStateFlow(false)
    val showFullSearchDialog: StateFlow<Boolean> = _showFullSearchDialog.asStateFlow()

    // Daily Ayah
    val todayDailyAyah: DailyAyah = DailyAyahManager.getTodayAyah()

    // Dialog & Sheet states
    private val _showReciterSheet = MutableStateFlow(false)
    val showReciterSheet: StateFlow<Boolean> = _showReciterSheet.asStateFlow()

    private val _showFullPlayerDialog = MutableStateFlow(false)
    val showFullPlayerDialog: StateFlow<Boolean> = _showFullPlayerDialog.asStateFlow()

    // Digital Tasbih state
    private val _dhikrMode = MutableStateFlow(DhikrMode.QURAN_VERSES)
    val dhikrMode: StateFlow<DhikrMode> = _dhikrMode.asStateFlow()

    private val _dhikrSurah = MutableStateFlow(SurahDatabase.getSurah(1) ?: SurahDatabase.surahs[0])
    val dhikrSurah: StateFlow<Surah> = _dhikrSurah.asStateFlow()

    private val _dhikrAyahs = MutableStateFlow<List<Ayah>>(SurahDatabase.offlineVersesMap[1] ?: emptyList())
    val dhikrAyahs: StateFlow<List<Ayah>> = _dhikrAyahs.asStateFlow()

    private val _dhikrAyahIndex = MutableStateFlow(0)
    val dhikrAyahIndex: StateFlow<Int> = _dhikrAyahIndex.asStateFlow()

    private val _verseRepeatTarget = MutableStateFlow(1)
    val verseRepeatTarget: StateFlow<Int> = _verseRepeatTarget.asStateFlow()

    private val _currentAyahRepeatCount = MutableStateFlow(0)
    val currentAyahRepeatCount: StateFlow<Int> = _currentAyahRepeatCount.asStateFlow()

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

    private val _dhikrTarget = MutableStateFlow(dhikrPresets[0].defaultTarget)
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
        audioManager.onDurationAvailable = { duration ->
            val surah = playerState.value.currentSurah ?: _readingSurah.value
            if (surah != null) {
                viewModelScope.launch {
                    val ayahs = if (_readingSurah.value?.number == surah.number && _readingAyahs.value.isNotEmpty()) {
                        _readingAyahs.value
                    } else {
                        repository.getSurahVerses(surah.number)
                    }
                    val timings = repository.getVerseTimings(
                        surahNumber = surah.number,
                        reciter = playerState.value.currentReciter,
                        ayahs = ayahs,
                        totalDurationMs = duration
                    )
                    audioManager.updateVerseTimings(timings)
                }
            }
        }
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

    fun openSurahReader(surah: Surah, targetAyah: Int? = null) {
        _readingSurah.value = surah
        saveLastRead(surah.number)
        _lastReadSurah.value = surah
        _targetAyahToScroll.value = targetAyah
        loadSurahVerses(surah.number)
        _currentTab.value = QuranTab.READER
    }

    fun openAyahInReader(surahNumber: Int, ayahNumber: Int) {
        val surah = SurahDatabase.getSurah(surahNumber) ?: return
        openSurahReader(surah, targetAyah = ayahNumber)
    }

    fun clearTargetAyahToScroll() {
        _targetAyahToScroll.value = null
    }

    fun setReadingTheme(theme: ReadingTheme) {
        _readingTheme.value = theme
        prefs.edit().putString("reading_theme", theme.name).apply()
    }

    fun setShowFullSearchDialog(show: Boolean) {
        _showFullSearchDialog.value = show
    }

    // Ayah Bookmarks & Notes
    fun isAyahBookmarked(surahNumber: Int, ayahNumber: Int): Boolean {
        val key = "$surahNumber:$ayahNumber"
        return _bookmarkedAyahs.value.containsKey(key)
    }

    fun getAyahNote(surahNumber: Int, ayahNumber: Int): String {
        val key = "$surahNumber:$ayahNumber"
        return _bookmarkedAyahs.value[key]?.note ?: ""
    }

    fun toggleAyahBookmark(
        surahNumber: Int,
        ayahNumber: Int,
        surahName: String,
        arabic: String,
        persian: String
    ) {
        val key = "$surahNumber:$ayahNumber"
        val current = _bookmarkedAyahs.value.toMutableMap()
        if (current.containsKey(key)) {
            val existing = current[key]
            if (existing != null && existing.note.isNotEmpty()) {
                // If it has a note, keep it but toggle or keep
                current.remove(key)
            } else {
                current.remove(key)
            }
        } else {
            current[key] = BookmarkedAyah(
                surahNumber = surahNumber,
                ayahNumber = ayahNumber,
                surahNamePersian = surahName,
                textArabic = arabic,
                textPersian = persian,
                note = ""
            )
        }
        _bookmarkedAyahs.value = current
        saveBookmarkedAyahs(current)
    }

    fun saveAyahNote(
        surahNumber: Int,
        ayahNumber: Int,
        surahName: String,
        arabic: String,
        persian: String,
        note: String
    ) {
        val key = "$surahNumber:$ayahNumber"
        val current = _bookmarkedAyahs.value.toMutableMap()
        val existing = current[key]
        if (existing != null) {
            current[key] = existing.copy(note = note)
        } else {
            current[key] = BookmarkedAyah(
                surahNumber = surahNumber,
                ayahNumber = ayahNumber,
                surahNamePersian = surahName,
                textArabic = arabic,
                textPersian = persian,
                note = note
            )
        }
        _bookmarkedAyahs.value = current
        saveBookmarkedAyahs(current)
    }

    fun deleteAyahNote(surahNumber: Int, ayahNumber: Int) {
        val key = "$surahNumber:$ayahNumber"
        val current = _bookmarkedAyahs.value.toMutableMap()
        val existing = current[key]
        if (existing != null) {
            current[key] = existing.copy(note = "")
        }
        _bookmarkedAyahs.value = current
        saveBookmarkedAyahs(current)
    }

    fun removeBookmarkedAyah(surahNumber: Int, ayahNumber: Int) {
        val key = "$surahNumber:$ayahNumber"
        val current = _bookmarkedAyahs.value.toMutableMap()
        current.remove(key)
        _bookmarkedAyahs.value = current
        saveBookmarkedAyahs(current)
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
            playSurah(surah)
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
    fun setDhikrMode(mode: DhikrMode) {
        _dhikrMode.value = mode
        resetDhikr()
    }

    fun selectDhikrSurah(surah: Surah) {
        _dhikrSurah.value = surah
        viewModelScope.launch {
            val ayahs = repository.getSurahVerses(surah.number)
            _dhikrAyahs.value = ayahs
            _dhikrAyahIndex.value = 0
            _currentAyahRepeatCount.value = 0
            resetDhikr()
        }
    }

    fun nextDhikrAyah() {
        val ayahs = _dhikrAyahs.value
        if (ayahs.isNotEmpty()) {
            _dhikrAyahIndex.value = (_dhikrAyahIndex.value + 1) % ayahs.size
            _currentAyahRepeatCount.value = 0
        }
    }

    fun prevDhikrAyah() {
        val ayahs = _dhikrAyahs.value
        if (ayahs.isNotEmpty()) {
            _dhikrAyahIndex.value = if (_dhikrAyahIndex.value > 0) _dhikrAyahIndex.value - 1 else ayahs.size - 1
            _currentAyahRepeatCount.value = 0
        }
    }

    fun setVerseRepeatTarget(target: Int) {
        _verseRepeatTarget.value = target.coerceAtLeast(1)
        _currentAyahRepeatCount.value = 0
    }

    fun setDhikrTarget(target: Int) {
        _dhikrTarget.value = target.coerceAtLeast(1)
    }

    fun incrementDhikr() {
        val currentMax = _dhikrTarget.value
        if (_dhikrCount.value >= currentMax) return
        val next = (_dhikrCount.value + 1).coerceAtMost(currentMax)
        _dhikrCount.value = next

        if (_dhikrMode.value == DhikrMode.QURAN_VERSES) {
            val ayahs = _dhikrAyahs.value
            if (ayahs.isNotEmpty()) {
                val nextRepeat = _currentAyahRepeatCount.value + 1
                if (nextRepeat >= _verseRepeatTarget.value) {
                    _currentAyahRepeatCount.value = 0
                    // Advance to next verse sequentially (Ayah 1 -> Ayah 2 -> Ayah 3...)
                    _dhikrAyahIndex.value = (_dhikrAyahIndex.value + 1) % ayahs.size
                } else {
                    _currentAyahRepeatCount.value = nextRepeat
                }
            }
        }
    }

    fun resetDhikr() {
        _dhikrCount.value = 0
        _dhikrAyahIndex.value = 0
        _currentAyahRepeatCount.value = 0
    }

    fun selectDhikrPreset(preset: DhikrPreset) {
        _selectedDhikrPreset.value = preset
        _dhikrTarget.value = preset.defaultTarget
        resetDhikr()
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

    private fun loadReadingTheme(): ReadingTheme {
        val saved = prefs.getString("reading_theme", ReadingTheme.DEFAULT.name) ?: ReadingTheme.DEFAULT.name
        return try {
            ReadingTheme.valueOf(saved)
        } catch (e: Exception) {
            ReadingTheme.DEFAULT
        }
    }

    private fun loadBookmarkedAyahs(): Map<String, BookmarkedAyah> {
        val bPrefs = getApplication<Application>().getSharedPreferences("quran_ayah_bookmarks_v1", Context.MODE_PRIVATE)
        val all = bPrefs.all
        val result = mutableMapOf<String, BookmarkedAyah>()
        for ((key, value) in all) {
            val str = value as? String ?: continue
            val parts = str.split("::|::")
            if (parts.size >= 7) {
                val sNum = parts[0].toIntOrNull() ?: continue
                val aNum = parts[1].toIntOrNull() ?: continue
                val sName = parts[2]
                val ar = parts[3]
                val fa = parts[4]
                val note = parts[5]
                val time = parts[6].toLongOrNull() ?: System.currentTimeMillis()
                result[key] = BookmarkedAyah(sNum, aNum, sName, ar, fa, note, time)
            }
        }
        return result
    }

    private fun saveBookmarkedAyahs(map: Map<String, BookmarkedAyah>) {
        val bPrefs = getApplication<Application>().getSharedPreferences("quran_ayah_bookmarks_v1", Context.MODE_PRIVATE)
        val editor = bPrefs.edit().clear()
        for ((key, item) in map) {
            val serialized = "${item.surahNumber}::|::${item.ayahNumber}::|::${item.surahNamePersian}::|::${item.textArabic}::|::${item.textPersian}::|::${item.note}::|::${item.timestamp}"
            editor.putString(key, serialized)
        }
        editor.apply()
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
