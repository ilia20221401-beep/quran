package com.example.data.remote

import com.example.data.model.Ayah
import com.example.data.model.SurahDatabase
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

class QuranRepository {
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

    // In-memory cache for loaded surahs
    private val versesCache = mutableMapOf<Int, List<Ayah>>()

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
}
