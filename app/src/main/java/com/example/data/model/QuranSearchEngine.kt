package com.example.data.model

data class SearchResultAyah(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahNamePersian: String,
    val surahNameArabic: String,
    val textArabic: String,
    val textPersian: String
)

object QuranSearchEngine {
    // Collect all pre-bundled and known Quranic verses
    private val corpus: List<SearchResultAyah> by lazy {
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

        return corpus.filter { item ->
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
