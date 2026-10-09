package com.example.data.model

data class Reciter(
    val id: String,
    val namePersian: String,
    val nameArabic: String,
    val style: String,
    val primaryUrlPattern: String,
    val fallbackUrlPattern: String,
    val quranComId: Int? = null,
    val istiadhaDurationMs: Long = 4200L,
    val basmalahDurationMs: Long = 4800L
) {
    fun getAudioUrl(surahNumber: Int, useFallback: Boolean = false): String {
        val pattern = if (useFallback) fallbackUrlPattern else primaryUrlPattern
        val padded = String.format("%03d", surahNumber)
        return if (pattern.contains("%03d")) {
            String.format(pattern, surahNumber)
        } else {
            pattern.replace("{surah}", surahNumber.toString())
                .replace("{padded}", padded)
        }
    }
}

object RecitersData {
    val defaultReciter: Reciter = Reciter(
        id = "alafasy",
        namePersian = "شیخ مشاری راشد العفاسی",
        nameArabic = "مشاري راشد العفاسي",
        style = "ترتیل دلنشین و آرام‌بخش",
        primaryUrlPattern = "https://cdn.mp3quran.net/audio/mishary-alafasy/r1/%03d.mp3",
        fallbackUrlPattern = "https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/{surah}.mp3",
        quranComId = 7,
        istiadhaDurationMs = 5500L,
        basmalahDurationMs = 5800L
    )

    val allReciters: List<Reciter> = listOf(
        defaultReciter,
        Reciter(
            id = "abdulbasit",
            namePersian = "استاد عبدالباسط عبدالصمد",
            nameArabic = "عبد الباسط عبد الصمد",
            style = "ترتیل ملکوتی و اصیل",
            primaryUrlPattern = "https://cdn.mp3quran.net/audio/abdulbasit-abdulsamad/r3/%03d.mp3",
            fallbackUrlPattern = "https://download.quranicaudio.com/qdc/abdul_baset/murattal/{surah}.mp3",
            quranComId = 2,
            istiadhaDurationMs = 3500L,
            basmalahDurationMs = 5200L
        ),
        Reciter(
            id = "minshawi",
            namePersian = "استاد محمد صدیق منشاوی",
            nameArabic = "محمد صديق المنشاوي",
            style = "ترتیل حزین و ماندگار",
            primaryUrlPattern = "https://cdn.mp3quran.net/audio/muhammad-minshawi/r1/%03d.mp3",
            fallbackUrlPattern = "https://download.quranicaudio.com/qdc/siddiq_minshawi/murattal/{surah}.mp3",
            quranComId = 9,
            istiadhaDurationMs = 3500L,
            basmalahDurationMs = 5200L
        ),
        Reciter(
            id = "husary",
            namePersian = "استاد محمود خلیل الحصری",
            nameArabic = "محمود خليل الحصري",
            style = "ترتیل آموزشی و دقیق",
            primaryUrlPattern = "https://cdn.mp3quran.net/audio/mahmoud-husary/r1/%03d.mp3",
            fallbackUrlPattern = "https://download.quranicaudio.com/qdc/khalil_al_husary/murattal/{surah}.mp3",
            quranComId = 6,
            istiadhaDurationMs = 5500L,
            basmalahDurationMs = 6500L
        ),
        Reciter(
            id = "ghamdi",
            namePersian = "شیخ سعد الغامدی",
            nameArabic = "سعد الغامدي",
            style = "ترتیل روان و دلنشین",
            primaryUrlPattern = "https://cdn.mp3quran.net/audio/saad-ghamdi/r1/%03d.mp3",
            fallbackUrlPattern = "https://cdn.mp3quran.net/audio/mishary-alafasy/r1/%03d.mp3",
            quranComId = null,
            istiadhaDurationMs = 3200L,
            basmalahDurationMs = 4500L
        )
    )
}
