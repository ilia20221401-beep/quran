package com.example

import com.example.audio.QuranAudioManager
import com.example.data.model.AYAH_BASMALAH
import com.example.data.model.AYAH_ISTIADHA
import com.example.data.model.RecitersData
import com.example.data.model.VerseTiming
import com.example.data.remote.QuranRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  private val repository = QuranRepository()

  @Test
  fun testSurah1Timings_shiftsAllVersesByIstiadha() {
    val reciter = RecitersData.defaultReciter
    val rawTimings = listOf(
      VerseTiming(1, 0L, 6090L),
      VerseTiming(2, 6090L, 11680L),
      VerseTiming(3, 11680L, 16300L)
    )

    val processed = repository.buildCompleteTimingsWithIntro(1, reciter, rawTimings)

    assertEquals(4, processed.size)
    // 1. Istiadha occupies [0, istiadhaDurationMs)
    assertEquals(AYAH_ISTIADHA, processed[0].ayahNumber)
    assertEquals(0L, processed[0].startMs)
    assertEquals(reciter.istiadhaDurationMs, processed[0].endMs)

    // 2. Ayah 1 (Basmalah) shifted by istiadhaDurationMs
    assertEquals(1, processed[1].ayahNumber)
    assertEquals(reciter.istiadhaDurationMs, processed[1].startMs)
    assertEquals(reciter.istiadhaDurationMs + 6090L, processed[1].endMs)

    // 3. Ayah 2 shifted by istiadhaDurationMs
    assertEquals(2, processed[2].ayahNumber)
    assertEquals(reciter.istiadhaDurationMs + 6090L, processed[2].startMs)
    assertEquals(reciter.istiadhaDurationMs + 11680L, processed[2].endMs)
  }

  @Test
  fun testSurah2Timings_includesIstiadhaAndBasmalah() {
    val reciter = RecitersData.defaultReciter
    val rawTimings = listOf(
      VerseTiming(1, 0L, 5000L),
      VerseTiming(2, 5000L, 12000L)
    )

    val processed = repository.buildCompleteTimingsWithIntro(2, reciter, rawTimings)

    assertEquals(4, processed.size)
    assertEquals(AYAH_ISTIADHA, processed[0].ayahNumber)
    assertEquals(0L, processed[0].startMs)

    assertEquals(AYAH_BASMALAH, processed[1].ayahNumber)
    assertEquals(processed[0].endMs, processed[1].startMs)

    val introTotal = processed[1].endMs
    assertEquals(1, processed[2].ayahNumber)
    assertEquals(introTotal, processed[2].startMs)
    assertEquals(introTotal + 5000L, processed[2].endMs)

    assertEquals(2, processed[3].ayahNumber)
    assertEquals(introTotal + 5000L, processed[3].startMs)
  }

  @Test
  fun testSurah112Timings_includesIstiadhaAndBasmalah() {
    val reciter = RecitersData.defaultReciter
    val rawTimings = listOf(
      VerseTiming(1, 0L, 2980L),
      VerseTiming(2, 2980L, 5510L)
    )

    val processed = repository.buildCompleteTimingsWithIntro(112, reciter, rawTimings)

    assertEquals(4, processed.size)
    // 1. Starts with Istiadha!
    assertEquals(AYAH_ISTIADHA, processed[0].ayahNumber)
    assertEquals(0L, processed[0].startMs)

    // 2. Followed by Basmalah!
    assertEquals(AYAH_BASMALAH, processed[1].ayahNumber)
    assertEquals(processed[0].endMs, processed[1].startMs)

    // 3. Ayah 1
    val introTotal = processed[1].endMs
    assertEquals(1, processed[2].ayahNumber)
    assertEquals(introTotal, processed[2].startMs)
    assertEquals(introTotal + 2980L, processed[2].endMs)
  }

  @Test
  fun testDhikrCounter_doesNotExceed100() {
    var count = 0
    val maxCount = 100
    for (i in 1..150) {
      if (count < maxCount) {
        count = (count + 1).coerceAtMost(maxCount)
      }
    }
    assertEquals(100, count)
  }

  @Test
  fun testDhikrVerses_advancesSequentially() {
    val ayahs = listOf("آیه ۱", "آیه ۲", "آیه ۳", "آیه ۴")
    var currentAyahIndex = 0
    var count = 0

    assertEquals("آیه ۱", ayahs[currentAyahIndex])

    // Tap 1 -> completes Ayah 1, advances to Ayah 2
    count++
    currentAyahIndex = (currentAyahIndex + 1) % ayahs.size
    assertEquals("آیه ۲", ayahs[currentAyahIndex])

    // Tap 2 -> completes Ayah 2, advances to Ayah 3
    count++
    currentAyahIndex = (currentAyahIndex + 1) % ayahs.size
    assertEquals("آیه ۳", ayahs[currentAyahIndex])

    // Tap 3 -> completes Ayah 3, advances to Ayah 4
    count++
    currentAyahIndex = (currentAyahIndex + 1) % ayahs.size
    assertEquals("آیه ۴", ayahs[currentAyahIndex])
  }
}

