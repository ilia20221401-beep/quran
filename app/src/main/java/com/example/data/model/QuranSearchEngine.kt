package com.example.data.model

import android.content.Context
import org.json.JSONArray

data class SearchResultAyah(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahNamePersian: String,
    val surahNameArabic: String,
    val textArabic: String,
    val textPersian: String
)

object QuranSearchEngine {
    private val fullCorpus = mutableListOf<SearchResultAyah>()
    private var isFullLoaded = false

    // Fallback mini corpus
    private val fallbackCorpus: List<SearchResultAyah> by lazy {
        val list = mutableListOf<SearchResultAyah>()

        // 1. All offline verses from SurahDatabase
        SurahDatabase.offlineVersesMap.forEach { (surahNum, ayahs) ->
            val surah = SurahDatabase.getSurah(surahNum)
            ayahs.forEach { ayah ->
                list.add(
                    SearchResultAyah(
                        surahNumber = surahNum,
                        ayahNumber = ayah.numberInSurah,
                        surahNamePersian = surah?.namePersian ?: "سوره $surahNum",
                        surahNameArabic = surah?.nameArabic ?: "",
                        textArabic = ayah.textArabic,
                        textPersian = ayah.textPersian
                    )
                )
            }
        }

        // 2. Add Rabbana Duas from QuranicDuasData
        QuranicDuasData.rabbanaDuas.forEach { dua ->
            val surah = SurahDatabase.getSurah(dua.surahNumber)
            list.add(
                SearchResultAyah(
                    surahNumber = dua.surahNumber,
                    ayahNumber = dua.ayahNumber,
                    surahNamePersian = surah?.namePersian ?: dua.surahName,
                    surahNameArabic = surah?.nameArabic ?: "",
                    textArabic = dua.arabic,
                    textPersian = dua.persian
                )
            )
        }

        list.distinctBy { "${it.surahNumber}:${it.ayahNumber}" }
    }

    fun init(context: Context) {
        if (isFullLoaded) return
        try {
            val list = mutableListOf<SearchResultAyah>()
            for (surah in SurahDatabase.surahs) {
                try {
                    val stream = context.assets.open("quran/${surah.number}.json")
                    val jsonStr = stream.bufferedReader().use { it.readText() }
                    val array = JSONArray(jsonStr)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val numInSurah = obj.getInt("numberInSurah")
                        val ar = obj.getString("textArabic")
                        val fa = obj.getString("textPersian")
                        list.add(
                            SearchResultAyah(
                                surahNumber = surah.number,
                                ayahNumber = numInSurah,
                                surahNamePersian = surah.namePersian,
                                surahNameArabic = surah.nameArabic,
                                textArabic = ar,
                                textPersian = fa
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Ignored
                }
            }
            if (list.isNotEmpty()) {
                synchronized(fullCorpus) {
                    fullCorpus.clear()
                    fullCorpus.addAll(list)
                    isFullLoaded = true
                }
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun search(query: String): List<SearchResultAyah> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return emptyList()

        val normalized = trimmed.lowercase()
            .replace("ي", "ی")
            .replace("ك", "ک")
            .replace("ة", "ه")
            .replace("إ", "ا")
            .replace("أ", "ا")
            .replace("آ", "ا")
            .replace("[\u064B-\u065F]".toRegex(), "") // Remove Arabic diacritics for flexible search

        val source = if (isFullLoaded && fullCorpus.isNotEmpty()) fullCorpus else fallbackCorpus

        return source.filter { item ->
            val cleanArabic = item.textArabic
                .replace("[\u064B-\u065F]".toRegex(), "")
                .replace("ي", "ی")
                .replace("ك", "ک")
                .replace("ة", "ه")
                .replace("إ", "ا")
                .replace("أ", "ا")
                .replace("آ", "ا")
            val cleanPersian = item.textPersian.lowercase()
            val cleanSurah = item.surahNamePersian.lowercase()

            cleanArabic.contains(normalized, ignoreCase = true) ||
            cleanPersian.contains(normalized, ignoreCase = true) ||
            cleanSurah.contains(normalized, ignoreCase = true)
        }
    }
}
