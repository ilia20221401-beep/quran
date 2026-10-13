package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.CityInfo
import com.example.data.model.IranCitiesData
import com.example.data.model.PrayerCities
import com.example.data.model.PrayerTimesCalculator
import com.example.data.model.PrayerTimesResult
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var selectedCity by remember { mutableStateOf(PrayerCities.defaultCity) }
    var showCityDialog by remember { mutableStateOf(false) }

    // GPS Location State
    var isLocatingGps by remember { mutableStateOf(false) }
    var isGpsActive by remember { mutableStateOf(false) }
    var gpsAccuracyMeters by remember { mutableFloatStateOf(0f) }

    val applyLocation: (Location) -> Unit = { location ->
        val lat = location.latitude
        val lng = location.longitude
        gpsAccuracyMeters = location.accuracy

        // Instant offline lookup for closest city without network blocking
        val nearestCity = IranCitiesData.allCities.minByOrNull { city ->
            val dLat = city.latitude - lat
            val dLng = city.longitude - lng
            dLat * dLat + dLng * dLng
        }

        val cityName = nearestCity?.nameFa ?: "موقعیت شما"
        val nameFa = "موقعیت GPS ($cityName)"
        val nameEn = String.format(Locale.US, "%.3f° N, %.3f° E", lat, lng)
        val tzOffset = nearestCity?.timeZoneOffset ?: 3.5

        selectedCity = CityInfo(
            nameFa = nameFa,
            nameEn = nameEn,
            latitude = lat,
            longitude = lng,
            timeZoneOffset = tzOffset,
            province = nearestCity?.province ?: ""
        )
        isGpsActive = true
        isLocatingGps = false
        Toast.makeText(context, "موقعیت شما با ماهواره GPS ثبت شد: $cityName", Toast.LENGTH_SHORT).show()
    }

    val requestLocationUpdates: () -> Unit = {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            Toast.makeText(context, "سرویس موقعیت‌یابی در دسترس نیست", Toast.LENGTH_SHORT).show()
            isLocatingGps = false
        } else {
            val isGpsEnabled = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (e: Exception) { false }
            val isNetworkEnabled = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { false }

            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(context, "لطفاً GPS دستگاه را روشن کنید", Toast.LENGTH_LONG).show()
                isLocatingGps = false
            } else {
                isLocatingGps = true

                // Try last known locations across all providers for instantaneous response
                var bestLastLocation: Location? = null
                try {
                    val lastGps = if (isGpsEnabled) locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
                    val lastNet = if (isNetworkEnabled) locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
                    val lastPassive = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
                        } else {
                            locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                        }
                    } catch (e: Exception) { null }

                    val candidates = listOfNotNull(lastGps, lastNet, lastPassive)
                    bestLastLocation = candidates.maxByOrNull { it.time }
                } catch (e: SecurityException) {
                    // Ignore
                }

                if (bestLastLocation != null) {
                    applyLocation(bestLastLocation)
                }

                // Request fresh real-time coordinates from all available providers
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        applyLocation(location)
                        try {
                            locationManager.removeUpdates(this)
                        } catch (e: Exception) {
                            // ignore
                        }
                    }

                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                var registeredAny = false
                try {
                    if (isGpsEnabled) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            2000L,
                            10f,
                            listener,
                            android.os.Looper.getMainLooper()
                        )
                        registeredAny = true
                    }
                    if (isNetworkEnabled) {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            2000L,
                            10f,
                            listener,
                            android.os.Looper.getMainLooper()
                        )
                        registeredAny = true
                    }
                } catch (e: SecurityException) {
                    isLocatingGps = false
                    Toast.makeText(context, "خطای دسترسی به موقعیت GPS", Toast.LENGTH_SHORT).show()
                }

                if (!registeredAny && bestLastLocation == null) {
                    isLocatingGps = false
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            requestLocationUpdates()
        } else {
            isLocatingGps = false
            Toast.makeText(
                context,
                "برای تشخیص دقیق قبله و اوقات با GPS، لطفاً دسترسی موقعیت را تأیید کنید",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val onDetectGpsClick: () -> Unit = {
        val fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineCheck == PackageManager.PERMISSION_GRANTED || coarseCheck == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Compass sensor logic with change threshold to prevent excessive recomposition
    var deviceAzimuth by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        val listener = object : SensorEventListener {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val degrees = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    val newAzimuth = (degrees + 360f) % 360f
                    if (kotlin.math.abs(newAzimuth - deviceAzimuth) >= 1.0f) {
                        deviceAzimuth = newAzimuth
                    }
                } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    val newAzimuth = (event.values[0] + 360f) % 360f
                    if (kotlin.math.abs(newAzimuth - deviceAzimuth) >= 1.0f) {
                        deviceAzimuth = newAzimuth
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensor != null) {
            sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val calendar = remember(selectedCity) {
        Calendar.getInstance()
    }

    val prayerTimes = remember(selectedCity) {
        PrayerTimesCalculator.calculatePrayerTimes(selectedCity, calendar)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "اوقات شرعی و قبله‌نما",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "شهر: ${selectedCity.nameFa} • ${selectedCity.nameEn}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onDetectGpsClick,
                        modifier = Modifier.testTag("detect_gps_icon_button")
                    ) {
                        if (isLocatingGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = if (isGpsActive) Icons.Default.GpsFixed else Icons.Default.MyLocation,
                                contentDescription = "موقعیت‌یابی با GPS",
                                tint = if (isGpsActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { showCityDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "تغییر شهر", style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize().testTag("prayer_times_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Qibla Compass Section
            item {
                QiblaCompassCard(
                    prayerTimes = prayerTimes,
                    deviceAzimuth = deviceAzimuth,
                    isGpsActive = isGpsActive,
                    gpsAccuracyMeters = gpsAccuracyMeters,
                    onDetectGpsClick = onDetectGpsClick,
                    isLocatingGps = isLocatingGps
                )
            }

            // All Prayer Times Table
            item {
                PrayerTimesTableCard(prayerTimes = prayerTimes)
            }

            // Informational footer
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "محاسبات اوقات شرعی طبق استانداردهای دقیق ژئوفیزیک با احتساب عرض و طول جغرافیایی شهر محاسبه گردیده است.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 19.sp
                            )
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showCityDialog) {
        CitySelectionDialog(
            currentCity = selectedCity,
            onSelect = {
                selectedCity = it
                isGpsActive = false
                showCityDialog = false
            },
            onDetectGps = {
                showCityDialog = false
                onDetectGpsClick()
            },
            onDismiss = { showCityDialog = false }
        )
    }
}

@Composable
private fun QiblaCompassCard(
    prayerTimes: PrayerTimesResult,
    deviceAzimuth: Float,
    isGpsActive: Boolean,
    gpsAccuracyMeters: Float,
    onDetectGpsClick: () -> Unit,
    isLocatingGps: Boolean
) {
    val distanceKm = remember(prayerTimes.city.latitude, prayerTimes.city.longitude) {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(21.4225 - prayerTimes.city.latitude)
        val dLng = Math.toRadians(39.8262 - prayerTimes.city.longitude)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(prayerTimes.city.latitude)) * cos(Math.toRadians(21.4225)) *
                sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        (earthRadius * c).roundToInt()
    }

    val rawDiff = (prayerTimes.qiblaBearing.toFloat() - deviceAzimuth + 360f) % 360f
    val angleDiff = if (rawDiff > 180f) 360f - rawDiff else rawDiff
    val isAlignedWithQibla = angleDiff <= 4f

    val context = LocalContext.current
    LaunchedEffect(isAlignedWithQibla) {
        if (isAlignedWithQibla) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator?.vibrate(70)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "قبله‌نما به سمت کعبه مشرفه",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${prayerTimes.qiblaBearing.roundToInt()}°",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // GPS Location Status Banner & Direct trigger
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isGpsActive) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDetectGpsClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLocatingGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF2E7D32)
                            )
                        } else {
                            Icon(
                                imageVector = if (isGpsActive) Icons.Default.GpsFixed else Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = if (isGpsActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isGpsActive) "موقعیت ماهواره‌ای GPS فعال است" else "تشخیص قبله با مکان‌یاب ماهواره‌ای GPS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGpsActive) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            Text(
                                text = if (isGpsActive) "فاصله تا کعبه: $distanceKm کیلومتر • دقت: ±${gpsAccuracyMeters.roundToInt()} متر"
                                else "برای محاسبه زاویه دقیق قبله بر اساس مکان فعلی، کلیک کنید",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isGpsActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    if (isGpsActive && !isLocatingGps) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = "بروزرسانی",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF1B5E20),
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (!isGpsActive && !isLocatingGps) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = "فعال‌سازی GPS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Compass Dial
            val animatedRotation by animateFloatAsState(
                targetValue = -deviceAzimuth,
                animationSpec = tween(durationMillis = 180),
                label = "compass_rotation"
            )

            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(
                        width = if (isAlignedWithQibla) 3.5.dp else 2.dp,
                        color = if (isAlignedWithQibla) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Dial Canvas with North/South/East/West markers
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(animatedRotation)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f - 16.dp.toPx()

                    // Draw 12 hour ticks
                    for (i in 0 until 12) {
                        val angleRad = Math.toRadians((i * 30.0) - 90.0)
                        val start = Offset(
                            center.x + (radius - 8.dp.toPx()) * cos(angleRad).toFloat(),
                            center.y + (radius - 8.dp.toPx()) * sin(angleRad).toFloat()
                        )
                        val end = Offset(
                            center.x + radius * cos(angleRad).toFloat(),
                            center.y + radius * sin(angleRad).toFloat()
                        )
                        drawLine(
                            color = if (i == 0) Color(0xFFE53935) else Color.Gray.copy(alpha = 0.6f),
                            start = start,
                            end = end,
                            strokeWidth = if (i == 0) 3.dp.toPx() else 1.5.dp.toPx()
                        )
                    }
                }

                // Kaaba Direction Pointer Arrow (pointing to qibla bearing)
                val qiblaRelativeAngle = (prayerTimes.qiblaBearing.toFloat() + animatedRotation) % 360f

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(qiblaRelativeAngle)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val needleLength = size.width / 2f - 30.dp.toPx()

                    // Green Kaaba needle pointer
                    val needlePath = Path().apply {
                        moveTo(center.x, center.y - needleLength)
                        lineTo(center.x - 12.dp.toPx(), center.y)
                        lineTo(center.x + 12.dp.toPx(), center.y)
                        close()
                    }
                    drawPath(
                        path = needlePath,
                        brush = Brush.verticalGradient(
                            colors = if (isAlignedWithQibla) listOf(Color(0xFF1B5E20), Color(0xFF4CAF50))
                            else listOf(Color(0xFF2E7D32), Color(0xFF81C784))
                        )
                    )

                    // South counter-pointer
                    val southPath = Path().apply {
                        moveTo(center.x, center.y + needleLength * 0.6f)
                        lineTo(center.x - 8.dp.toPx(), center.y)
                        lineTo(center.x + 8.dp.toPx(), center.y)
                        close()
                    }
                    drawPath(
                        path = southPath,
                        color = Color.LightGray.copy(alpha = 0.7f)
                    )

                    // Center pivot circle
                    drawCircle(
                        color = if (isAlignedWithQibla) Color(0xFF1B5E20) else Color(0xFF2E7D32),
                        radius = 8.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = center
                    )
                }

                // Kaaba Icon badge at top
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "🕋",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Guidance & Alignment confirmation banner
            if (isAlignedWithQibla) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9),
                    border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "✅ تبریک! جهت گوشی شما دقیقاً رو به قبله است.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val rotateDirectionText = if (rawDiff > 180f) {
                    "گوشی را ${(360f - rawDiff).roundToInt()}° به چپ بچرخانید ↺"
                } else {
                    "گوشی را ${rawDiff.roundToInt()}° به راست بچرخانید ↻"
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$rotateDirectionText تا نشانگر سبز دقیقاً رو به کعبه قرار گیرد",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerTimesTableCard(prayerTimes: PrayerTimesResult) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "جدول کامل اوقات شرعی امروز",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrayerTimeRowItem("اذان صبح", prayerTimes.fajr, Icons.Default.Brightness4, isHighlight = prayerTimes.nextPrayerName == "اذان صبح")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PrayerTimeRowItem("طلوع آفتاب", prayerTimes.sunrise, Icons.Default.WbSunny, isHighlight = prayerTimes.nextPrayerName == "طلوع آفتاب")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PrayerTimeRowItem("اذان ظهر", prayerTimes.dhuhr, Icons.Default.Brightness5, isHighlight = prayerTimes.nextPrayerName == "اذان ظهر")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PrayerTimeRowItem("غروب آفتاب", prayerTimes.sunset, Icons.Default.WbTwilight, isHighlight = prayerTimes.nextPrayerName == "غروب آفتاب")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PrayerTimeRowItem("اذان مغرب", prayerTimes.maghrib, Icons.Default.NightsStay, isHighlight = prayerTimes.nextPrayerName == "اذان مغرب")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            PrayerTimeRowItem("نیمه‌شب شرعی", prayerTimes.midnight, Icons.Default.Bedtime, isHighlight = prayerTimes.nextPrayerName == "نیمه‌شب شرعی")
        }
    }
}

@Composable
private fun PrayerTimeRowItem(
    title: String,
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isHighlight: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isHighlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else Color.Transparent
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
                    color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun CitySelectionDialog(
    currentCity: CityInfo,
    onSelect: (CityInfo) -> Unit,
    onDetectGps: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) } // 0: همه شهرهای ایران, 1: مراکز استان‌ها, 2: اماکن مقدسه
    var expandedProvince by remember { mutableStateOf<String?>(null) }

    val iranianCities: List<CityInfo> = remember {
        IranCitiesData.allCities.filter { it.province !in listOf("عربستان", "عراق", "سوریه", "فلسطین") }
    }

    val provincesList: List<String> = remember(iranianCities) {
        iranianCities.map { it.province }.distinct().sorted()
    }

    val filteredCities: List<CityInfo> = remember(searchQuery, selectedCategoryIndex) {
        val trimmed = searchQuery.trim()
        if (trimmed.isNotEmpty()) {
            IranCitiesData.allCities.filter {
                it.nameFa.contains(trimmed, ignoreCase = true) ||
                it.nameEn.contains(trimmed, ignoreCase = true) ||
                it.province.contains(trimmed, ignoreCase = true)
            }
        } else {
            when (selectedCategoryIndex) {
                1 -> PrayerCities.provincialCapitals
                2 -> PrayerCities.holyCities
                else -> emptyList()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "انتخاب شهر و قبله",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${iranianCities.size} شهر ایران",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "محاسبه دقیق اوقات شرعی و زاویه قبله برای تمام شهرهای کشور",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                // Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("جستجوی نام شهر یا استان...", fontSize = 13.sp)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "جستجو",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "پاک کردن",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Filter Tabs (when not actively searching)
                if (searchQuery.isEmpty()) {
                    val tabs = listOf("همه استان‌ها", "مراکز استان", "اماکن مقدسه")
                    TabRow(
                        selectedTabIndex = selectedCategoryIndex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedCategoryIndex == index,
                                onClick = { selectedCategoryIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selectedCategoryIndex == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // GPS automatic locator item at top
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDetectGps() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "موقعیت‌یابی خودکار ماهواره‌ای (GPS)",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Text(
                                        text = "تنظیم اوقات و قبله با مختصات زنده دستگاه شما",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    // Content depends on search query or selected tab
                    if (searchQuery.isNotEmpty()) {
                        if (filteredCities.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "شهری با نام «$searchQuery» یافت نشد",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        } else {
                            items(filteredCities, key = { it.nameFa + it.province + it.latitude }) { city ->
                                CityListItem(
                                    city = city,
                                    isSelected = city.nameFa == currentCity.nameFa && city.province == currentCity.province,
                                    onClick = { onSelect(city) }
                                )
                            }
                        }
                    } else if (selectedCategoryIndex == 1) {
                        // Provincial capitals
                        items(PrayerCities.provincialCapitals, key = { it.nameFa }) { city ->
                            CityListItem(
                                city = city,
                                isSelected = city.nameFa == currentCity.nameFa,
                                onClick = { onSelect(city) }
                            )
                        }
                    } else if (selectedCategoryIndex == 2) {
                        // Holy places
                        items(PrayerCities.holyCities, key = { it.nameFa }) { city ->
                            CityListItem(
                                city = city,
                                isSelected = city.nameFa == currentCity.nameFa,
                                onClick = { onSelect(city) }
                            )
                        }
                    } else {
                        // All Iranian cities grouped by province (Expandable)
                        items(provincesList, key = { it }) { province: String ->
                            val citiesInProvince: List<CityInfo> = iranianCities.filter { it.province == province }
                            val isExpanded = expandedProvince == province || currentCity.province == province

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isExpanded) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable {
                                        expandedProvince = if (isExpanded) null else province
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowLeft,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "استان $province",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                    ) {
                                        Text(
                                            text = "${citiesInProvince.size} شهر",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            if (isExpanded) {
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    citiesInProvince.forEach { city ->
                                        CityListItem(
                                            city = city,
                                            isSelected = city.nameFa == currentCity.nameFa && city.province == currentCity.province,
                                            onClick = { onSelect(city) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )
}

@Composable
private fun CityListItem(
    city: CityInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = city.nameFa,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                )
                if (city.isCapital) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "مرکز استان",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            if (city.province.isNotEmpty()) {
                Text(
                    text = "استان ${city.province} • ${city.nameEn}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

