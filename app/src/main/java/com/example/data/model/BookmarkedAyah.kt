package com.example.data.model

data class BookmarkedAyah(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahNamePersian: String,
    val textArabic: String,
    val textPersian: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
