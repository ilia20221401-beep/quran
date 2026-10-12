package com.example.data.remote

import android.content.Context
import com.example.data.model.AYAH_BASMALAH
import com.example.data.model.AYAH_ISTIADHA
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
        val expectedFirst = getExpectedInitialAyah(surahNumber)
        timingsMemoryCache[cacheKey]?.let { cached ->
            if (totalDurationMs == 0L && cached.isNotEmpty() && cached.firstOrNull()?.ayahNumber == expectedFirst) {
                return@withContext cached
            }
        }

        // Check disk cache if context available (using v9 cache: Basmalah only for Surahs 2..114, Istiadha for Surah 1)
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences("quran_timing_cache_v9", Context.MODE_PRIVATE)
            val raw = prefs.getString(cacheKey, null)
            if (!raw.isNullOrEmpty()) {
                val parsed = parseTimingsString(raw)
                if (parsed.isNotEmpty() && parsed.firstOrNull()?.ayahNumber == expectedFirst) {
                    timingsMemoryCache[cacheKey] = parsed
                    if (totalDurationMs == 0L) {
                        return@withContext parsed
                    }
                }
            }
        }

        // Try fetching online from Quran.com if reciter has mapped quranComId
        if (reciter.quranComId != null) {
            try {
                val response = timingsApi.getChapterRecitationTimings(reciter.quranComId, surahNumber)
                val items = response.audioFile?.timestamps
                if (!items.isNullOrEmpty()) {
                    val rawList = items.mapNotNull { item ->
                        val ayahNum = item.verseKey?.substringAfter(":")?.toIntOrNull()
                        val from = item.timestampFrom?.toLong() ?: 0L
                        val to = item.timestampTo?.toLong() ?: 0L
                        if (ayahNum != null) {
                            VerseTiming(ayahNumber = ayahNum, startMs = from, endMs = to)
                        } else null
                    }.sortedBy { it.ayahNumber }

                    if (rawList.isNotEmpty()) {
                        val processed = buildCompleteTimingsWithIntro(
                            surahNumber = surahNumber,
                            reciter = reciter,
                            rawTimings = rawList,
                            totalDurationMs = totalDurationMs
                        )
                        timingsMemoryCache[cacheKey] = processed
                        context?.let { ctx ->
                            val serialized = serializeTimingsString(processed)
                            ctx.getSharedPreferences("quran_timing_cache_v9", Context.MODE_PRIVATE)
                                .edit().putString(cacheKey, serialized).apply()
                        }
                        return@withContext processed
                    }
                }
            } catch (e: Exception) {
                // Silently fall through to estimation
            }
        }

        // Proportional estimation fallback based on Arabic verse text lengths
        val estimated = calculateEstimatedTimings(surahNumber, reciter, ayahs, totalDurationMs)
        if (estimated.isNotEmpty()) {
            timingsMemoryCache[cacheKey] = estimated
            context?.let { ctx ->
                val serialized = serializeTimingsString(estimated)
                ctx.getSharedPreferences("quran_timing_cache_v9", Context.MODE_PRIVATE)
                    .edit().putString(cacheKey, serialized).apply()
            }
        }
        return@withContext estimated
    }

    private fun getExpectedInitialAyah(surahNumber: Int): Int {
        return when (surahNumber) {
            1 -> AYAH_ISTIADHA
            9 -> 1
            else -> AYAH_BASMALAH
        }
    }

    fun buildCompleteTimingsWithIntro(
        surahNumber: Int,
        reciter: Reciter,
        rawTimings: List<VerseTiming>,
        totalDurationMs: Long = 0L
    ): List<VerseTiming> {
        if (rawTimings.isEmpty()) return emptyList()

        val rawLastEnd = rawTimings.lastOrNull()?.endMs ?: 0L
        val diffMs = if (totalDurationMs > rawLastEnd) (totalDurationMs - rawLastEnd) else 0L

        val istiadhaMs = reciter.istiadhaDurationMs
        val basmalahMs = reciter.basmalahDurationMs
        val result = mutableListOf<VerseTiming>()

        when (surahNumber) {
            1 -> {
                // Surah Al-Fatiha:
                // Audio begins with Isti'adha ("أَعُوذُ بِٱللَّهِ مِنَ ٱلشَّيْطَٰنِ ٱلرَّجِيمِ"),
                // then Ayah 1 is "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ".
                val effectiveIstiadha = if (diffMs in 2500L..9000L) diffMs else istiadhaMs
                result.add(VerseTiming(AYAH_ISTIADHA, 0L, effectiveIstiadha))

                rawTimings.forEach { raw ->
                    val shiftedStart = effectiveIstiadha + raw.startMs
                    val shiftedEnd = effectiveIstiadha + raw.endMs
                    result.add(VerseTiming(raw.ayahNumber, shiftedStart, shiftedEnd))
                }
            }
            9 -> {
                // Surah At-Tawbah:
                // Revealed without Basmalah. Audio starts directly with Ayah 1 at 0ms.
                rawTimings.forEach { raw ->
                    result.add(VerseTiming(raw.ayahNumber, raw.startMs, raw.endMs))
                }
            }
            else -> {
                // Surahs 2..8 and 10..114:
                // In standard audio recitations (e.g. Al-Hashr, Al-Baqarah, etc.),
                // the reciter recites ONLY Tasmiyah / Basmalah ("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ") at the beginning!
                // No Isti'adha is recited in these surahs.
                val effectiveBasmalah = if (diffMs in 3500L..10000L) {
                    diffMs
                } else {
                    basmalahMs
                }

                result.add(VerseTiming(AYAH_BASMALAH, 0L, effectiveBasmalah))

                rawTimings.forEach { raw ->
                    val shiftedStart = effectiveBasmalah + raw.startMs
                    val shiftedEnd = effectiveBasmalah + raw.endMs
                    result.add(VerseTiming(raw.ayahNumber, shiftedStart, shiftedEnd))
                }
            }
        }

        return result
    }

    fun calculateEstimatedTimings(
        surahNumber: Int,
        reciter: Reciter,
        ayahs: List<Ayah>,
        totalDurationMs: Long
    ): List<VerseTiming> {
        if (ayahs.isEmpty()) return emptyList()

        val istiadhaMs = reciter.istiadhaDurationMs
        val basmalahMs = reciter.basmalahDurationMs
        val result = mutableListOf<VerseTiming>()

        when (surahNumber) {
            1 -> {
                // Al-Fatiha: Isti'adha, then Ayah 1 is Basmalah.
                val effectiveDuration = if (totalDurationMs > (istiadhaMs + 5000L)) {
                    totalDurationMs
                } else {
                    istiadhaMs + (ayahs.size * 5800L)
                }

                result.add(VerseTiming(AYAH_ISTIADHA, 0L, istiadhaMs))

                val remainingDuration = maxOf(effectiveDuration - istiadhaMs, ayahs.size * 2500L)
                val weights = ayahs.map { ayah ->
                    val cleanText = ayah.textArabic.replace(" ", "")
                    maxOf(cleanText.length, 10).toLong()
                }
                val totalWeight = weights.sum().coerceAtLeast(1L)

                var currentMs = istiadhaMs
                ayahs.forEachIndexed { index, ayah ->
                    val start = currentMs
                    val end = if (index == ayahs.lastIndex) {
                        effectiveDuration
                    } else {
                        val frac = weights[index].toDouble() / totalWeight.toDouble()
                        val dur = (remainingDuration * frac).toLong().coerceAtLeast(2000L)
                        (start + dur).coerceAtMost(effectiveDuration)
                    }
                    currentMs = end
                    result.add(VerseTiming(ayah.numberInSurah, start, maxOf(end, start + 1200L)))
                }
            }
            9 -> {
                // At-Tawbah: Starts directly at Ayah 1 (No Basmalah, no Istiadha in audio)
                val effectiveDuration = if (totalDurationMs > 5000L) {
                    totalDurationMs
                } else {
                    ayahs.size * 5200L
                }

                val weights = ayahs.map { ayah ->
                    val cleanText = ayah.textArabic.replace(" ", "")
                    maxOf(cleanText.length, 10).toLong()
                }
                val totalWeight = weights.sum().coerceAtLeast(1L)

                var currentMs = 0L
                ayahs.forEachIndexed { index, ayah ->
                    val start = currentMs
                    val end = if (index == ayahs.lastIndex) {
                        effectiveDuration
                    } else {
                        val frac = weights[index].toDouble() / totalWeight.toDouble()
                        val dur = (effectiveDuration * frac).toLong().coerceAtLeast(1000L)
                        (start + dur).coerceAtMost(effectiveDuration)
                    }
                    currentMs = end
                    result.add(VerseTiming(ayah.numberInSurah, start, maxOf(end, start + 1000L)))
                }
            }
            else -> {
                // Surahs 2..8 and 10..114: Reciter recites ONLY Basmalah before Ayah 1
                val effectiveBasmalah = basmalahMs
                val effectiveDuration = if (totalDurationMs > (effectiveBasmalah + 5000L)) {
                    totalDurationMs
                } else {
                    effectiveBasmalah + (ayahs.size * 4500L)
                }

                result.add(VerseTiming(AYAH_BASMALAH, 0L, effectiveBasmalah))

                val remainingDuration = maxOf(effectiveDuration - effectiveBasmalah, ayahs.size * 2000L)
                val weights = ayahs.map { ayah ->
                    val cleanText = ayah.textArabic.replace(" ", "")
                    maxOf(cleanText.length, 10).toLong()
                }
                val totalWeight = weights.sum().coerceAtLeast(1L)

                var currentMs = effectiveBasmalah
                ayahs.forEachIndexed { index, ayah ->
                    val start = currentMs
                    val end = if (index == ayahs.lastIndex) {
                        effectiveDuration
                    } else {
                        val frac = weights[index].toDouble() / totalWeight.toDouble()
                        val dur = (remainingDuration * frac).toLong().coerceAtLeast(1200L)
                        (start + dur).coerceAtMost(effectiveDuration)
                    }
                    currentMs = end
                    result.add(VerseTiming(ayah.numberInSurah, start, maxOf(end, start + 1000L)))
                }
            }
        }

        return result
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
