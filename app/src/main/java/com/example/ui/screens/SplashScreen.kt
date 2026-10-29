package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldenStar
import com.example.ui.theme.QuranAmiriFontFamily
import com.example.ui.theme.VazirmatnFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val StarCoordinates = listOf(
    Offset(0.12f, 0.15f) to 2.2f,
    Offset(0.85f, 0.18f) to 3.0f,
    Offset(0.25f, 0.32f) to 1.8f,
    Offset(0.78f, 0.38f) to 2.4f,
    Offset(0.08f, 0.55f) to 1.6f,
    Offset(0.92f, 0.60f) to 2.8f,
    Offset(0.18f, 0.75f) to 2.5f,
    Offset(0.82f, 0.82f) to 1.9f,
    Offset(0.50f, 0.10f) to 2.0f,
    Offset(0.35f, 0.88f) to 2.2f
)

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isFinishedCalled by remember { mutableStateOf(false) }

    // Screen-level fade out animator
    val screenAlpha = remember { Animatable(1f) }

    val safeFinish: () -> Unit = {
        if (!isFinishedCalled) {
            isFinishedCalled = true
            coroutineScope.launch {
                screenAlpha.animateTo(0f, tween(250))
                onFinished()
            }
        }
    }

    // Allow user to press back to immediately enter the app
    BackHandler(enabled = !isFinishedCalled) { safeFinish() }

    // Stage-based animation controllers
    val logoScale = remember { Animatable(0.3f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleTranslation = remember { Animatable(20f) }
    val basmalahAlpha = remember { Animatable(0f) }
    val verseCardAlpha = remember { Animatable(0f) }
    val progressAlpha = remember { Animatable(0f) }
    val progressVal = remember { Animatable(0f) }

    // Smooth ambient transitions
    val infiniteTransition = rememberInfiniteTransition(label = "splash_ambient")
    val mandalaRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mandala_rotation"
    )
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(38000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counter_rotation"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )
    val emblemFloat by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emblem_float"
    )

    // Snappy, non-blocking concurrent entrance sequence (total ~1.8 seconds)
    LaunchedEffect(Unit) {
        // Stage 1: Central Emblem entrance
        launch {
            logoAlpha.animateTo(1f, tween(350))
        }
        launch {
            logoScale.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 260f))
        }

        delay(100)
        // Stage 2: Bismillah calligraphy smooth fade-in
        launch {
            basmalahAlpha.animateTo(1f, tween(400))
        }

        delay(100)
        // Stage 3: Title and Subtitle rise up
        launch {
            titleAlpha.animateTo(1f, tween(400))
            titleTranslation.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 280f))
        }

        delay(100)
        // Stage 4: Spiritual Verse of Light card
        launch {
            verseCardAlpha.animateTo(1f, tween(400))
        }

        delay(80)
        // Stage 5: Progress bar loading
        launch {
            progressAlpha.animateTo(1f, tween(250))
        }

        // Progress bar smoothly fills up over 1100ms
        progressVal.animateTo(1f, tween(1100, easing = LinearEasing))
        delay(150)
        safeFinish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F3A2A), // Deep Emerald
                        Color(0xFF092017), // Forest Sanctuary
                        Color(0xFF04100B)  // Obsidian Night
                    ),
                    radius = 1600f
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { safeFinish() }
            .testTag("splash_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        // Background Stars & Sparkle Canvas (zero-allocation)
        TwinklingStarsCanvas(glowPulse = glowPulse)

        // Sacred Islamic Geometric Rosette & Mandala (zero-allocation)
        IslamicMandalaCanvas(
            rotationAngle = mandalaRotation,
            counterAngle = counterRotation,
            glowPulse = glowPulse,
            modifier = Modifier.size(360.dp)
        )

        // Skip button on top corner for instant entry
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { safeFinish() }
                .testTag("splash_skip_button"),
            color = Color.White.copy(alpha = 0.14f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ورود به برنامه",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = VazirmatnFontFamily,
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "ورود سریع",
                    tint = GoldenStar,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Center Content Column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Radiant Emblem & Holy Book
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                // Multi-tier golden halo aura
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    // Outer aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GoldenStar.copy(alpha = 0.35f * glowPulse),
                                Color(0xFF145A3E).copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        ),
                        radius = radius * 1.15f
                    )
                    // Golden ring
                    drawCircle(
                        color = GoldenStar.copy(alpha = 0.5f * glowPulse),
                        radius = radius * 0.92f,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Inner dotted ring
                    drawCircle(
                        color = Color(0xFFE5C365).copy(alpha = 0.4f),
                        radius = radius * 0.84f,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    )
                }

                // Inner medallion circle
                Surface(
                    modifier = Modifier
                        .size(105.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, Brush.linearGradient(listOf(GoldenStar, Color(0xFFB8860B))), CircleShape),
                    color = Color(0xFF0F2E21),
                    shadowElevation = 10.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF1B533E),
                                        Color(0xFF0D2C1F)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Illuminated Quran Icon with subtle breathing offset
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "قرآن کریم",
                            tint = GoldenStar,
                            modifier = Modifier
                                .size(54.dp)
                                .rotate(emblemFloat)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Bismillah Calligraphy (Amiri Quran font)
            Text(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = QuranAmiriFontFamily,
                    fontSize = 24.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBE8A6),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(basmalahAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // App Title with Gold Gradient
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .padding(top = titleTranslation.value.dp)
            ) {
                Text(
                    text = "قـــــرآنِ کَـــــریـم",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "همراه با تلاوت قاریان برجسته جهان اسلام و ترجمه روان فارسی",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = VazirmatnFontFamily,
                        fontSize = 13.5.sp,
                        color = Color(0xFFC7EBD7).copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Spiritual Verse Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(verseCardAlpha.value),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0F2E21).copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            GoldenStar.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "«أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ»",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = QuranAmiriFontFamily,
                            fontSize = 18.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "آگاه باشید که با یاد خدا دل‌ها آرام می‌گیرد (رعد: ۲۸)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = VazirmatnFontFamily,
                            fontSize = 12.sp,
                            color = Color(0xFFE6EEE8).copy(alpha = 0.8f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Smooth golden progress bar & status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(progressAlpha.value)
            ) {
                LinearProgressIndicator(
                    progress = { progressVal.value },
                    modifier = Modifier
                        .width(160.dp)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = GoldenStar,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "در حال آماده‌سازی کلام نور...",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = VazirmatnFontFamily,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.5.sp
                    )
                )
            }
        }

        // Bottom Islamic branding stamp
        Text(
            text = "نسخه جامع و برگزیده قرآن کریم",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = VazirmatnFontFamily,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.5.sp
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

/**
 * Draws the rotating 8-pointed Islamic Star (Khatam Sulayman) using pure GPU primitives without GC allocations.
 */
@Composable
private fun IslamicMandalaCanvas(
    rotationAngle: Float,
    counterAngle: Float,
    glowPulse: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension / 2f * 0.95f
        val innerRadius = outerRadius * 0.65f

        // Draw counter-rotating outer ring with rays
        rotate(counterAngle, center) {
            val rayCount = 16
            for (i in 0 until rayCount) {
                val angleRad = Math.toRadians((i * (360.0 / rayCount)).toDouble())
                val start = Offset(
                    (center.x + (outerRadius * 0.82f) * cos(angleRad)).toFloat(),
                    (center.y + (outerRadius * 0.82f) * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    (center.x + outerRadius * cos(angleRad)).toFloat(),
                    (center.y + outerRadius * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = GoldenStar.copy(alpha = 0.22f * glowPulse),
                    start = start,
                    end = end,
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            drawCircle(
                color = Color(0xFF56D19B).copy(alpha = 0.16f),
                radius = outerRadius * 0.85f,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
                )
            )
        }

        // Draw primary rotating 8-pointed Islamic Star with drawRect (Zero allocations)
        rotate(rotationAngle, center) {
            val starSize = innerRadius * 0.82f
            val squareWidth = starSize * 2f
            val topLeft = Offset(center.x - starSize, center.y - starSize)
            val rectSize = Size(squareWidth, squareWidth)

            // First square
            drawRect(
                color = GoldenStar.copy(alpha = 0.20f * glowPulse),
                topLeft = topLeft,
                size = rectSize,
                style = Stroke(width = 1.4.dp.toPx())
            )

            // Second square rotated 45 degrees
            rotate(45f, center) {
                drawRect(
                    color = GoldenStar.copy(alpha = 0.20f * glowPulse),
                    topLeft = topLeft,
                    size = rectSize,
                    style = Stroke(width = 1.4.dp.toPx())
                )
            }

            // Central concentric decorative ring
            drawCircle(
                color = GoldenStar.copy(alpha = 0.28f * glowPulse),
                radius = starSize * 0.72f,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

/**
 * Draws gently twinkling starlight particles across the night sky.
 */
@Composable
private fun TwinklingStarsCanvas(
    glowPulse: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        StarCoordinates.forEachIndexed { index, (coord, sizeRadius) ->
            val actualPos = Offset(coord.x * size.width, coord.y * size.height)
            val alphaMultiplier = if (index % 2 == 0) glowPulse else (1.55f - glowPulse)
            drawCircle(
                color = GoldenStar.copy(alpha = (0.35f * alphaMultiplier).coerceIn(0.1f, 0.85f)),
                radius = sizeRadius.dp.toPx(),
                center = actualPos
            )
        }
    }
}
