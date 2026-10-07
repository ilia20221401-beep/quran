package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.QuranAudioCacheManager
import com.example.data.model.JuzDatabase
import com.example.data.model.QuranicDuasData
import com.example.data.model.RecitersData
import com.example.data.model.SurahDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("قرآن کریم", appName)
  }

  @Test
  fun `verify all 114 surahs exist in database`() {
    assertEquals(114, SurahDatabase.surahs.size)
    val fatiha = SurahDatabase.getSurah(1)
    assertNotNull(fatiha)
    assertEquals("الفَاتِحَة", fatiha?.nameArabic)

    val nas = SurahDatabase.getSurah(114)
    assertNotNull(nas)
    assertEquals("النَّاس", nas?.nameArabic)
  }

  @Test
  fun `verify reciter audio urls formatting`() {
    val reciter = RecitersData.defaultReciter
    val urlSurah1 = reciter.getAudioUrl(1)
    assertEquals("https://cdn.mp3quran.net/audio/mishary-alafasy/r1/001.mp3", urlSurah1)

    val urlSurah114 = reciter.getAudioUrl(114)
    assertEquals("https://cdn.mp3quran.net/audio/mishary-alafasy/r1/114.mp3", urlSurah114)
  }

  @Test
  fun `verify all 30 juz and duas exist`() {
    assertEquals(30, JuzDatabase.allJuz.size)
    assertEquals("جزء ۳۰", JuzDatabase.allJuz[29].namePersian)

    assertNotNull(QuranicDuasData.ayatulKursi)
    assertEquals(2, QuranicDuasData.ayatulKursi.surahNumber)
    assertEquals(255, QuranicDuasData.ayatulKursi.ayahNumber)
    assertEquals(11, QuranicDuasData.rabbanaDuas.size)
  }

  @Test
  fun `verify audio cache manager functionality`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cacheManager = QuranAudioCacheManager(context)
    val reciter = RecitersData.defaultReciter
    val file = cacheManager.getAudioFile(reciter.id, 1)
    assertEquals("alafasy_1.mp3", file.name)
    assertEquals(false, cacheManager.isCached(reciter, 1))
  }
}
