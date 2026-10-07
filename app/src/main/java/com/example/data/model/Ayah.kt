package com.example.data.model

data class Ayah(
    val numberInSurah: Int,
    val textArabic: String,
    val textPersian: String,
    val sajda: Boolean = false
)
