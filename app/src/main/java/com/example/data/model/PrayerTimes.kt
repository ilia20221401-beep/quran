package com.example.data.model

import java.util.Calendar
import kotlin.math.*

data class CityInfo(
    val nameFa: String,
    val nameEn: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneOffset: Double = 3.5 // Iran Standard Time default (+3.5h)
)

data class PrayerTimesResult(
    val city: CityInfo,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val sunset: String,
    val maghrib: String,
    val isha: String,
    val midnight: String,
    val qiblaBearing: Double,
    val nextPrayerName: String,
    val nextPrayerRemainingFormatted: String,
    val nextPrayerProgress: Float // 0f to 1f
)

object PrayerCities {
    val defaultCity: CityInfo = CityInfo("تهران", "Tehran", 35.6892, 51.3890, 3.5)

    val popularCities: List<CityInfo> = listOf(
        defaultCity,
        CityInfo("مشهد مقدس", "Mashhad", 36.2972, 59.6067, 3.5),
        CityInfo("اصفهان", "Isfahan", 32.6546, 51.6680, 3.5),
        CityInfo("شیراز", "Shiraz", 29.5918, 52.5837, 3.5),
        CityInfo("تبریز", "Tabriz", 38.0962, 46.2738, 3.5),
        CityInfo("قم مقدسه", "Qom", 34.6401, 50.8764, 3.5),
        CityInfo("اهواز", "Ahvaz", 31.3183, 48.6706, 3.5),
        CityInfo("کرمانشاه", "Kermanshah", 34.3277, 47.0778, 3.5),
        CityInfo("رشت", "Rasht", 37.2809, 49.5924, 3.5),
        CityInfo("کرمان", "Kerman", 30.2839, 57.0834, 3.5),
        CityInfo("یزد", "Yazd", 31.8974, 54.3569, 3.5),
        CityInfo("مکه مکرمه", "Mecca", 21.4225, 39.8262, 3.0),
        CityInfo("مدینه منوره", "Medina", 24.5247, 39.5692, 3.0),
        CityInfo("نجف اشرف", "Najaf", 32.0259, 44.3462, 3.0),
        CityInfo("کربلای معلی", "Karbala", 32.6160, 44.0249, 3.0),
        CityInfo("استانبول", "Istanbul", 41.0082, 28.9784, 3.0)
    )
}

object PrayerTimesCalculator {
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LNG = 39.8262

    fun calculateQibla(lat: Double, lng: Double): Double {
        val latRad = Math.toRadians(lat)
        val lngRad = Math.toRadians(lng)
        val kaabaLatRad = Math.toRadians(KAABA_LAT)
        val kaabaLngRad = Math.toRadians(KAABA_LNG)

        val deltaLng = kaabaLngRad - lngRad
        val y = sin(deltaLng)
        val x = cos(latRad) * tan(kaabaLatRad) - sin(latRad) * cos(deltaLng)
        val qibla = Math.toDegrees(atan2(y, x))
        return (qibla + 360.0) % 360.0
    }

    fun calculatePrayerTimes(city: CityInfo, calendar: Calendar = Calendar.getInstance()): PrayerTimesResult {
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)

        // Astronomical calculations
        val d = (dayOfYear - 1) + (calendar.get(Calendar.HOUR_OF_DAY) - 12.0) / 24.0
        val m = (357.529 + 0.98560028 * d) % 360.0
        val l = (280.459 + 0.98564736 * d) % 360.0
        val lambda = (l + 1.915 * sin(Math.toRadians(m)) + 0.020 * sin(Math.toRadians(2 * m))) % 360.0

        val obliq = 23.439 - 0.00000036 * d
        val alpha = Math.toDegrees(atan2(cos(Math.toRadians(obliq)) * sin(Math.toRadians(lambda)), cos(Math.toRadians(lambda))))
        val declination = Math.toDegrees(asin(sin(Math.toRadians(obliq)) * sin(Math.toRadians(lambda))))

        val equationOfTime = (l - alpha) / 15.0 // in hours

        // Noon time (Dhuhr)
        val noon = 12.0 + city.timeZoneOffset - (city.longitude / 15.0) - equationOfTime

        // Helper to get time offset for a given sun angle
        fun timeForAngle(angle: Double): Double {
            val latRad = Math.toRadians(city.latitude)
            val decRad = Math.toRadians(declination)
            val angRad = Math.toRadians(angle)
            val cosH = (sin(angRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
            if (cosH > 1.0 || cosH < -1.0) return 0.0 // Sun never reaches angle (polar regions)
            val h = Math.toDegrees(acos(cosH)) / 15.0
            return h
        }

        // Angles according to Tehran Institute of Geophysics & standard Islamic astronomy:
        val fajrOffset = timeForAngle(-17.7)
        val sunriseOffset = timeForAngle(-0.833)
        val sunsetOffset = timeForAngle(-0.833)
        val maghribOffset = timeForAngle(-4.5)
        val ishaOffset = timeForAngle(-14.0)

        // Asr calculation (Shafi/Hanafi shadow rule)
        val asrAngle = Math.toDegrees(atan(1.0 / (1.0 + tan(abs(Math.toRadians(city.latitude) - Math.toRadians(declination))))))
        val asrOffset = timeForAngle(asrAngle)

        val fajrTime = noon - fajrOffset
        val sunriseTime = noon - sunriseOffset
        val dhuhrTime = noon
        val asrTime = noon + asrOffset
        val sunsetTime = noon + sunsetOffset
        val maghribTime = noon + maghribOffset
        val ishaTime = noon + ishaOffset

        // Islamic midnight (halfway between sunset/maghrib and next day's fajr)
        val midnightTime = (maghribTime + (fajrTime + 24.0)) / 2.0 % 24.0

        fun formatHours(h: Double): String {
            val normalized = (h + 24.0) % 24.0
            val hours = normalized.toInt()
            val minutes = ((normalized - hours) * 60.0).roundToInt()
            val finalHours = if (minutes == 60) (hours + 1) % 24 else hours
            val finalMins = if (minutes == 60) 0 else minutes
            return String.format("%02d:%02d", finalHours, finalMins)
        }

        val nowHours = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0 + calendar.get(Calendar.SECOND) / 3600.0

        // Determine next prayer and countdown
        val prayers = listOf(
            Triple("اذان صبح", fajrTime, "صبح"),
            Triple("طلوع آفتاب", sunriseTime, "طلوع"),
            Triple("اذان ظهر", dhuhrTime, "ظهر"),
            Triple("عصر", asrTime, "عصر"),
            Triple("غروب آفتاب", sunsetTime, "غروب"),
            Triple("اذان مغرب", maghribTime, "مغرب"),
            Triple("عشاء", ishaTime, "عشاء"),
            Triple("نیمه‌شب شرعی", midnightTime, "نیمه‌شب")
        ).sortedBy { it.second }

        var nextPrayer = prayers.firstOrNull { it.second > nowHours }
        val remainingHours: Double
        val totalInterval: Double

        if (nextPrayer != null) {
            remainingHours = nextPrayer.second - nowHours
            val prevIndex = prayers.indexOf(nextPrayer) - 1
            val prevTime = if (prevIndex >= 0) prayers[prevIndex].second else prayers.last().second - 24.0
            totalInterval = max(0.1, nextPrayer.second - prevTime)
        } else {
            // Next is Fajr tomorrow
            nextPrayer = prayers.first()
            remainingHours = (nextPrayer.second + 24.0) - nowHours
            totalInterval = max(0.1, (nextPrayer.second + 24.0) - prayers.last().second)
        }

        val remMinsTotal = (remainingHours * 60).toInt()
        val remH = remMinsTotal / 60
        val remM = remMinsTotal % 60
        val remFormatted = if (remH > 0) "$remH ساعت و $remM دقیقه" else "$remM دقیقه"
        val progress = (1.0 - (remainingHours / totalInterval)).toFloat().coerceIn(0.05f, 0.98f)

        val qibla = calculateQibla(city.latitude, city.longitude)

        return PrayerTimesResult(
            city = city,
            fajr = formatHours(fajrTime),
            sunrise = formatHours(sunriseTime),
            dhuhr = formatHours(dhuhrTime),
            asr = formatHours(asrTime),
            sunset = formatHours(sunsetTime),
            maghrib = formatHours(maghribTime),
            isha = formatHours(ishaTime),
            midnight = formatHours(midnightTime),
            qiblaBearing = qibla,
            nextPrayerName = nextPrayer.first,
            nextPrayerRemainingFormatted = remFormatted,
            nextPrayerProgress = progress
        )
    }
}
