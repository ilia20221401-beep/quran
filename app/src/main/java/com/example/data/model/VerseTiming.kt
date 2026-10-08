package com.example.data.model

const val AYAH_ISTIADHA = -1
const val AYAH_BASMALAH = 0

data class VerseTiming(
    val ayahNumber: Int,
    val startMs: Long,
    val endMs: Long
)
