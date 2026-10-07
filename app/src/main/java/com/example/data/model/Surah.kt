package com.example.data.model

data class Surah(
    val number: Int,
    val nameArabic: String,
    val namePersian: String,
    val englishName: String,
    val meaningPersian: String,
    val revelationType: String, // "مکی" / "مدنی"
    val numberOfAyahs: Int,
    val juz: Int,
    val pageNumber: Int,
    val description: String = ""
)
