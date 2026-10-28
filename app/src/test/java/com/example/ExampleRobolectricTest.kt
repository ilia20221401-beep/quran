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

  @Test
  fun `verify default repeat mode is REPEAT_NONE`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val audioManager = com.example.audio.QuranAudioManager(context)
    assertEquals(com.example.audio.RepeatMode.REPEAT_NONE, audioManager.playerState.value.repeatMode)
    assertEquals("توقف در پایان سوره", audioManager.playerState.value.repeatMode.titleFa)
  }

  @Test
  fun `verify verse timing calculation and active ayah tracking`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val audioManager = com.example.audio.QuranAudioManager(context)
    val repo = com.example.data.remote.QuranRepository(context)
    val reciter = com.example.data.model.RecitersData.defaultReciter

    val testAyahs = listOf(
        com.example.data.model.Ayah(1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "به نام خداوند بخشنده مهربان"),
        com.example.data.model.Ayah(2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "ستایش مخصوص خداوندی است که پروردگار جهانیان است"),
        com.example.data.model.Ayah(3, "الرَّحْمَٰنِ الرَّحِيمِ", "بخشنده و مهربان است")
    )
    val totalDuration = 30_000L
    val timings = repo.calculateEstimatedTimings(1, reciter, testAyahs, totalDuration)
    // 3 ayahs + 1 istiadha intro = 4 timings
    assertEquals(4, timings.size)
    // First item is Istiadha (-1), from 0ms
    assertEquals(com.example.data.model.AYAH_ISTIADHA, timings[0].ayahNumber)
    assertEquals(0L, timings[0].startMs)
    assertEquals(reciter.istiadhaDurationMs, timings[0].endMs)
    // Second item is Ayah 1 (Bismillah in Surah 1), starting after Istiadha
    assertEquals(1, timings[1].ayahNumber)
    assertEquals(reciter.istiadhaDurationMs, timings[1].startMs)
    assertEquals(totalDuration, timings.last().endMs)

    audioManager.updateVerseTimings(timings)
    // At 0ms (start of audio), Istiadha is active, NOT Bismillah!
    assertEquals(com.example.data.model.AYAH_ISTIADHA, audioManager.calculateActiveAyah(0L))
    assertEquals(com.example.data.model.AYAH_ISTIADHA, audioManager.calculateActiveAyah(500L))
    // When Istiadha finishes, Ayah 1 (Bismillah) is active!
    assertEquals(1, audioManager.calculateActiveAyah(timings[1].startMs + 100L))
    assertEquals(2, audioManager.calculateActiveAyah(timings[2].startMs + 100L))
    assertEquals(3, audioManager.calculateActiveAyah(timings[3].startMs + 100L))
  }

  @Test
  fun `verify offline Surah loading and Bismillah stripping`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.remote.QuranRepository(context)

    // Test stripBismillahPrefix
    val sampleWithPrefix = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ قُلْ هُوَ ٱللَّهُ أَحَدٌ"
    val stripped = com.example.data.remote.QuranRepository.stripBismillahPrefix(sampleWithPrefix)
    assertEquals("قُلْ هُوَ ٱللَّهُ أَحَدٌ", stripped)

    // Test offline assets loading for Surah 112 (Ikhlas)
    val ikhlasVerses = repo.loadSurahFromAssets(112)
    assertNotNull(ikhlasVerses)
    assertEquals(4, ikhlasVerses?.size)
    // Ayah 1 of Surah 112 should NOT contain Bismillah
    val ikhlasAyah1 = ikhlasVerses?.firstOrNull()?.textArabic
    assertEquals("قُلْ هُوَ ٱللَّهُ أَحَدٌ", ikhlasAyah1)

    // Test offline assets loading for Surah 1 (Fatiha)
    val fatihaVerses = repo.loadSurahFromAssets(1)
    assertNotNull(fatihaVerses)
    assertEquals(7, fatihaVerses?.size)
    // Ayah 1 of Fatiha IS Bismillah
    val fatihaAyah1 = fatihaVerses?.firstOrNull()?.textArabic
    assertEquals("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", fatihaAyah1)
  }
}
