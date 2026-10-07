package com.example.data.remote

import android.content.Context
import com.example.data.model.Ayah
import com.example.data.model.Reciter
import com.example.data.model.SurahDatabase
import com.example.data.model.VerseTiming
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

data class QuranApiResponse(
    @Json(name = "code") val code: Int,
    @Json(name = "status") val status: String,
    @Json(name = "data") val data: List<SurahEditionData>?
)

data class SurahEditionData(
    @Json(name = "number") val number: Int,
    @Json(name = "name") val name: String,
    @Json(name = "ayahs") val ayahs: List<AyahRemoteData>?
)

data class AyahRemoteData(
    @Json(name = "number") val number: Int,
    @Json(name = "text") val text: String,
    @Json(name = "numberInSurah") val numberInSurah: Int
)

interface QuranApiService {
    @GET("surah/{number}/editions/quran-uthmani,fa.ansarian")
    suspend fun getSurahVerses(
        @Path("number") surahNumber: Int
    ): QuranApiResponse
}

data class QuranComTimestampsResponse(
    @Json(name = "audio_file") val audioFile: QuranComAudioFile?
)

data class QuranComAudioFile(
    @Json(name = "chapter_id") val chapterId: Int?,
    @Json(name = "timestamps") val timestamps: List<QuranComTimestampItem>?
)

data class QuranComTimestampItem(
    @Json(name = "verse_key") val verseKey: String?,
    @Json(name = "timestamp_from") val timestampFrom: Double?,
    @Json(name = "timestamp_to") val timestampTo: Double?,
    @Json(name = "duration") val duration: Double?
)

interface QuranTimingsApiService {
    @GET("chapter_recitations/{reciterId}/{chapterId}?segments=true")
    suspend fun getChapterRecitationTimings(
        @Path("reciterId") reciterId: Int,
        @Path("chapterId") chapterId: Int
    ): QuranComTimestampsResponse
}

class QuranRepository(private val context: Context? = null) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val api: QuranApiService = Retrofit.Builder()
        .baseUrl("https://api.alquran.cloud/v1/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(QuranApiService::class.java)

    private val timingsApi: QuranTimingsApiService = Retrofit.Builder()
        .baseUrl("https://api.quran.com/api/v4/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(QuranTimingsApiService::class.java)

    // In-memory cache for loaded surahs
    private val versesCache = mutableMapOf<Int, List<Ayah>>()
    private val timingsMemoryCache = mutableMapOf<String, List<VerseTiming>>()

    suspend fun getSurahVerses(surahNumber: Int): List<Ayah> = withContext(Dispatchers.IO) {
        // Check cache first
        versesCache[surahNumber]?.let { return@withContext it }

        // Check pre-bundled local data
        SurahDatabase.offlineVersesMap[surahNumber]?.let {
            versesCache[surahNumber] = it
            return@withContext it
        }

        try {
            val response = api.getSurahVerses(surahNumber)
            val editions = response.data
            if (!editions.isNullOrEmpty() && editions.size >= 2) {
                val arabicEdition = editions[0].ayahs ?: emptyList()
                val persianEdition = editions[1].ayahs ?: emptyList()

                val merged = mutableListOf<Ayah>()
                for (i in arabicEdition.indices) {
                    val ar = arabicEdition[i]
                    val fa = persianEdition.getOrNull(i)?.text ?: ""
                    // Clean up Bismillah in first ayah if repeated (except Fatiha)
                    var arabicText = ar.text
                    if (surahNumber != 1 && ar.numberInSurah == 1 && arabicText.startsWith("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ")) {
                        arabicText = arabicText.removePrefix("بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ").trim()
                        if (arabicText.isEmpty()) {
                            arabicText = ar.text
                        }
                    }

                    merged.add(
                        Ayah(
                            numberInSurah = ar.numberInSurah,
                            textArabic = arabicText,
                            textPersian = fa
                        )
                    )
                }

                if (merged.isNotEmpty()) {
                    versesCache[surahNumber] = merged
                    return@withContext merged
                }
            }
        } catch (e: Exception) {
            // Silently fall back to placeholder if offline and not in bundled set
        }

        // Fallback for surahs without internet and not bundled:
        val surahInfo = SurahDatabase.getSurah(surahNumber)
        val placeholder = listOf(
            Ayah(
                numberInSurah = 1,
                textArabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                textPersian = "به نام خداوند بخشنده مهربان"
            ),
            Ayah(
                numberInSurah = 2,
                textArabic = "${surahInfo?.nameArabic ?: ""} - ${surahInfo?.numberOfAyahs ?: 0} آیه",
                textPersian = "برای مشاهده متن کامل تمامی آیات به صورت آنلاین متصل شوید. تلاوت صوتی این سوره بدون مشکل آماده پخش است."
            )
        )
        return@withContext placeholder
    }

    suspend fun getVerseTimings(
        surahNumber: Int,
        reciter: Reciter,
        ayahs: List<Ayah>,
        totalDurationMs: Long = 0L
    ): List<VerseTiming> = withContext(Dispatchers.IO) {
        val cacheKey = "${reciter.id}_$surahNumber"
        timingsMemoryCache[cacheKey]?.let { return@withContext it }

        // Check disk cache if context available
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences("quran_timing_cache", Context.MODE_PRIVATE)
            val raw = prefs.getString(cacheKey, null)
            if (!raw.isNullOrEmpty()) {
                val parsed = parseTimingsString(raw)
                if (parsed.isNotEmpty()) {
                    timingsMemoryCache[cacheKey] = parsed
                    return@withContext parsed
                }
            }
        }

        // Try fetching online from Quran.com if reciter has mapped quranComId
        if (reciter.quranComId != null) {
            try {
                val response = timingsApi.getChapterRecitationTimings(reciter.quranComId, surahNumber)
                val items = response.audioFile?.timestamps
                if (!items.isNullOrEmpty()) {
                    val list = items.mapNotNull { item ->
                        val ayahNum = item.verseKey?.substringAfter(":")?.toIntOrNull()
                        val from = item.timestampFrom?.toLong() ?: 0L
                        val to = item.timestampTo?.toLong() ?: 0L
                        if (ayahNum != null) {
                            VerseTiming(ayahNumber = ayahNum, startMs = from, endMs = to)
                        } else null
                    }.sortedBy { it.ayahNumber }

                    if (list.isNotEmpty()) {
                        timingsMemoryCache[cacheKey] = list
                        context?.let { ctx ->
                            val serialized = serializeTimingsString(list)
                            ctx.getSharedPreferences("quran_timing_cache", Context.MODE_PRIVATE)
                                .edit().putString(cacheKey, serialized).apply()
                        }
                        return@withContext list
                    }
                }
            } catch (e: Exception) {
                // Silently fall through to estimation
            }
        }

        // Proportional estimation fallback based on Arabic verse text lengths
        val estimated = calculateEstimatedTimings(surahNumber, ayahs, totalDurationMs)
        if (estimated.isNotEmpty() && totalDurationMs > 0) {
            timingsMemoryCache[cacheKey] = estimated
        }
        return@withContext estimated
    }

    fun calculateEstimatedTimings(
        surahNumber: Int,
        ayahs: List<Ayah>,
        totalDurationMs: Long
    ): List<VerseTiming> {
        if (ayahs.isEmpty()) return emptyList()

        val effectiveDuration = if (totalDurationMs > 1000L) {
            totalDurationMs
        } else {
            // Default baseline estimate ~5.5 seconds per verse
            ayahs.size * 5500L
        }

        // Calculate Arabic character weights for each verse
        val weights = ayahs.map { ayah ->
            val cleanText = ayah.textArabic.replace(" ", "")
            maxOf(cleanText.length, 10).toLong()
        }
        val totalWeight = weights.sum().coerceAtLeast(1L)

        var currentMs = 0L
        return ayahs.mapIndexed { index, ayah ->
            val start = currentMs
            val end = if (index == ayahs.lastIndex) {
                effectiveDuration
            } else {
                val frac = weights[index].toDouble() / totalWeight.toDouble()
                val verseDuration = (effectiveDuration * frac).toLong().coerceAtLeast(1500L)
                (start + verseDuration).coerceAtMost(effectiveDuration)
            }
            currentMs = end
            VerseTiming(
                ayahNumber = ayah.numberInSurah,
                startMs = start,
                endMs = maxOf(end, start + 1000L)
            )
        }
    }

    private fun serializeTimingsString(timings: List<VerseTiming>): String {
        return timings.joinToString(";") { "${it.ayahNumber},${it.startMs},${it.endMs}" }
    }

    private fun parseTimingsString(raw: String): List<VerseTiming> {
        return raw.split(";").mapNotNull { part ->
            val tokens = part.split(",")
            if (tokens.size == 3) {
                val num = tokens[0].toIntOrNull()
                val start = tokens[1].toLongOrNull()
                val end = tokens[2].toLongOrNull()
                if (num != null && start != null && end != null) {
                    VerseTiming(num, start, end)
                } else null
            } else null
        }
    }
}
