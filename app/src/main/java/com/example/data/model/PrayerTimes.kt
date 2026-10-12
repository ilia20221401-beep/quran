package com.example.data.model

import java.util.Calendar
import java.util.Locale
import kotlin.math.*

data class CityInfo(
    val nameFa: String,
    val nameEn: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneOffset: Double = 3.5, // Iran Standard Time default (+3.5h)
    val province: String = "",
    val isCapital: Boolean = false
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
    val nextPrayerTime: String,
    val nextPrayerRemainingFormatted: String,
    val remainingHours: Int,
    val remainingMinutes: Int,
    val remainingSeconds: Int,
    val nextPrayerProgress: Float // 0f to 1f
)

object PrayerCities {
    val defaultCity: CityInfo = IranCitiesData.tehran
    val popularCities: List<CityInfo> = IranCitiesData.popularCities
    val allCities: List<CityInfo> = IranCitiesData.allCities
    val holyCities: List<CityInfo> = IranCitiesData.holyCities
    val provincialCapitals: List<CityInfo> = IranCitiesData.allCities.filter { it.isCapital }
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

        // Standard NOAA Astronomical Calculation for Solar Position
        // Fractional year in radians
        val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1.0 + (calendar.get(Calendar.HOUR_OF_DAY) - 12.0) / 24.0)

        // NOAA Equation of Time in minutes
        val eqTimeMinutes = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2.0 * gamma) - 0.040849 * sin(2.0 * gamma))

        // Solar declination in radians
        val declRad = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
                0.006758 * cos(2.0 * gamma) + 0.000907 * sin(2.0 * gamma) -
                0.002697 * cos(3.0 * gamma) + 0.00148 * sin(3.0 * gamma)

        // Effective timezone offset in hours
        val effectiveTzOffset = city.timeZoneOffset

        // Solar Noon (Dhuhr) in hours:
        // solar_noon = (720.0 - 4.0 * longitude - eqTimeMinutes + 60.0 * tzOffset) / 60.0
        val noon = (720.0 - 4.0 * city.longitude - eqTimeMinutes + 60.0 * effectiveTzOffset) / 60.0

        val latRad = Math.toRadians(city.latitude)

        // Helper to get time offset in hours for a given solar altitude angle
        fun timeForAngle(angDeg: Double): Double {
            val angRad = Math.toRadians(angDeg)
            val cosH = (sin(angRad) - sin(latRad) * sin(declRad)) / (cos(latRad) * cos(declRad))
            if (cosH > 1.0 || cosH < -1.0) return 0.0
            return Math.toDegrees(acos(cosH.coerceIn(-1.0, 1.0))) / 15.0
        }

        fun normH(h: Double): Double {
            return ((h % 24.0) + 24.0) % 24.0
        }

        // Angles according to Tehran Institute of Geophysics & standard Islamic astronomy:
        val fajrOffset = timeForAngle(-17.7)
        val sunriseOffset = timeForAngle(-0.833)
        val sunsetOffset = timeForAngle(-0.833)
        val maghribOffset = timeForAngle(-4.5)
        val ishaOffset = timeForAngle(-14.0)

        // Asr calculation (standard shadow rule)
        val asrAngle = Math.toDegrees(atan(1.0 / (1.0 + tan(abs(latRad - declRad)))))
        val asrOffset = timeForAngle(asrAngle)

        val fajrTime = normH(noon - fajrOffset)
        val sunriseTime = normH(noon - sunriseOffset)
        val dhuhrTime = normH(noon)
        val asrTime = normH(noon + asrOffset)
        val sunsetTime = normH(noon + sunsetOffset)
        val maghribTime = normH(noon + maghribOffset)
        val ishaTime = normH(noon + ishaOffset)

        // Islamic midnight (halfway between sunset/maghrib and next day's fajr)
        val midnightDiff = ((fajrTime + 24.0) - maghribTime) / 2.0
        val midnightTime = normH(maghribTime + midnightDiff)

        fun formatHours(h: Double): String {
            val normalized = normH(h)
            val hours = normalized.toInt()
            val minutes = ((normalized - hours) * 60.0).roundToInt()
            val finalHours = if (minutes == 60) (hours + 1) % 24 else hours
            val finalMins = if (minutes == 60) 0 else minutes
            return String.format(Locale.US, "%02d:%02d", finalHours, finalMins)
        }

        // Device clock current time in hours (0.00 to 23.9999)
        val nowHours = calendar.get(Calendar.HOUR_OF_DAY) +
                calendar.get(Calendar.MINUTE) / 60.0 +
                calendar.get(Calendar.SECOND) / 3600.0 +
                calendar.get(Calendar.MILLISECOND) / 3600000.0

        // Determine next prayer and countdown
        val prayers = listOf(
            Triple("اذان صبح", fajrTime, formatHours(fajrTime)),
            Triple("طلوع آفتاب", sunriseTime, formatHours(sunriseTime)),
            Triple("اذان ظهر", dhuhrTime, formatHours(dhuhrTime)),
            Triple("عصر", asrTime, formatHours(asrTime)),
            Triple("غروب آفتاب", sunsetTime, formatHours(sunsetTime)),
            Triple("اذان مغرب", maghribTime, formatHours(maghribTime)),
            Triple("عشاء", ishaTime, formatHours(ishaTime)),
            Triple("نیمه‌شب شرعی", midnightTime, formatHours(midnightTime))
        ).sortedBy { it.second }

        var nextPrayer = prayers.firstOrNull { it.second > nowHours }
        val remainingHoursVal: Double
        val totalInterval: Double

        if (nextPrayer != null) {
            remainingHoursVal = nextPrayer.second - nowHours
            val prevIndex = prayers.indexOf(nextPrayer) - 1
            val prevTime = if (prevIndex >= 0) prayers[prevIndex].second else prayers.last().second - 24.0
            totalInterval = max(0.1, nextPrayer.second - prevTime)
        } else {
            // All today's prayers passed; next is Fajr tomorrow morning
            nextPrayer = prayers.first()
            remainingHoursVal = (nextPrayer.second + 24.0) - nowHours
            totalInterval = max(0.1, (nextPrayer.second + 24.0) - prayers.last().second)
        }

        val totalSeconds = (remainingHoursVal * 3600.0).roundToInt().coerceAtLeast(0)
        val remH = totalSeconds / 3600
        val remM = (totalSeconds % 3600) / 60
        val remS = totalSeconds % 60

        val remFormatted = when {
            remH > 0 -> "$remH ساعت و $remM دقیقه و $remS ثانیه"
            remM > 0 -> "$remM دقیقه و $remS ثانیه"
            else -> "$remS ثانیه"
        }
        val progress = (1.0 - (remainingHoursVal / totalInterval)).toFloat().coerceIn(0.02f, 0.98f)

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
            nextPrayerTime = nextPrayer.third,
            nextPrayerRemainingFormatted = remFormatted,
            remainingHours = remH,
            remainingMinutes = remM,
            remainingSeconds = remS,
            nextPrayerProgress = progress
        )
    }
}
