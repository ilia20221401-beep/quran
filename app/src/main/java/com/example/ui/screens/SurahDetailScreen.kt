package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerState
import com.example.data.model.Ayah
import com.example.data.model.Surah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahDetailScreen(
    surah: Surah,
    ayahs: List<Ayah>,
    isLoading: Boolean,
    playerState: AudioPlayerState,
    isFavorite: Boolean,
    showTranslation: Boolean,
    arabicFontSize: Float,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleTranslation: () -> Unit,
    onChangeFontSize: (Float) -> Unit,
    onSeekToAyah: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var showFontSlider by remember { mutableStateOf(false) }
    var autoScrollEnabled by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    val isThisSurahSelected = playerState.currentSurah?.number == surah.number
    val isPlayingThisSurah = isThisSurahSelected && playerState.isPlaying
    val activeAyahNumber = if (isThisSurahSelected) playerState.activeAyahNumber else null

    // Auto-scroll to the currently reciting verse
    LaunchedEffect(activeAyahNumber, isPlayingThisSurah, autoScrollEnabled) {
        if (autoScrollEnabled && isPlayingThisSurah && activeAyahNumber != null) {
            val ayahIndex = ayahs.indexOfFirst { it.numberInSurah == activeAyahNumber }
            if (ayahIndex >= 0) {
                // If Bismillah banner is present at index 0, add 1 to list item position
                val targetIndex = if (surah.number != 9) ayahIndex + 1 else ayahIndex
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("surah_detail_screen")
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "سوره ${surah.namePersian} (${surah.nameArabic})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "${surah.revelationType} • ${surah.numberOfAyahs} آیه • جزء ${surah.juz}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                // Toggle Auto-scroll button
                IconButton(
                    onClick = { autoScrollEnabled = !autoScrollEnabled },
                    modifier = Modifier.testTag("toggle_autoscroll")
                ) {
                    Icon(
                        imageVector = if (autoScrollEnabled) Icons.Default.Sync else Icons.Default.SyncDisabled,
                        contentDescription = if (autoScrollEnabled) "پیمایش خودکار فعال است" else "پیمایش خودکار غیرفعال است",
                        tint = if (autoScrollEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Toggle translation button
                IconButton(onClick = onToggleTranslation) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "نمایش ترجمه فارسی",
                        tint = if (showTranslation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Adjust font size button
                IconButton(onClick = { showFontSlider = !showFontSlider }) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = "اندازه قلم",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Bookmark Surah
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "نشانه‌گذاری",
                        tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Play / Pause Surah audio
                FilledIconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("detail_play_surah"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isThisSurahSelected && playerState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else if (isPlayingThisSurah) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "توقف صوت",
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "پخش صوت تلاوت",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Live Recitation Status Strip (when this surah is currently playing)
        if (isThisSurahSelected && activeAyahNumber != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "در حال قرائت: آیه $activeAyahNumber از ${surah.numberOfAyahs}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Text(
                        text = if (autoScrollEnabled) "پیمایش خودکار فعال" else "پیمایش خودکار متوقف",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Optional Font Size Slider Panel
        if (showFontSlider) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "اندازه خط:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Slider(
                        value = arabicFontSize,
                        onValueChange = onChangeFontSize,
                        valueRange = 18f..34f,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${arabicFontSize.toInt()}sp",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "در حال بارگذاری متن آیات و ترجمه...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Bismillah Banner (All surahs except Surah At-Tawbah 9)
                if (surah.number != 9) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                                            )
                                        )
                                    )
                                    .padding(vertical = 20.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    if (showTranslation) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "به نام خداوند بخشنده مهربان",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Ayahs List with Live Recitation Highlighting
                items(ayahs, key = { it.numberInSurah }) { ayah ->
                    val isHighlighted = isThisSurahSelected && activeAyahNumber == ayah.numberInSurah

                    AyahCard(
                        ayah = ayah,
                        surahName = surah.namePersian,
                        fontSize = arabicFontSize,
                        showTranslation = showTranslation,
                        isHighlighted = isHighlighted,
                        isPlaying = isPlayingThisSurah && isHighlighted,
                        onPlayAyah = { onSeekToAyah(ayah.numberInSurah) },
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                // Bottom spacer for player
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun AyahCard(
    ayah: Ayah,
    surahName: String,
    fontSize: Float,
    showTranslation: Boolean,
    isHighlighted: Boolean,
    isPlaying: Boolean,
    onPlayAyah: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val cardBgColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 350),
        label = "ayah_card_bg"
    )

    val cardBorder = if (isHighlighted) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ayah_card_${ayah.numberInSurah}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Verse Header with Ayah Number Badge, Active Status, and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ayah Number Circle Badge
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isHighlighted) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${ayah.numberInSurah}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Highlight Recitation Badge
                    if (isHighlighted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "در حال تلاوت",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Play / Seek to this Ayah button
                    IconButton(
                        onClick = onPlayAyah,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "پخش از این آیه",
                            modifier = Modifier.size(18.dp),
                            tint = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Copy Ayah
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Quran Ayah", "${ayah.textArabic}\n\n${ayah.textPersian}\n(سوره $surahName - آیه ${ayah.numberInSurah})")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "آیه با موفقیت کپی شد", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "کپی آیه",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share Ayah
                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "${ayah.textArabic}\n\n${ayah.textPersian}\n(سوره $surahName - آیه ${ayah.numberInSurah})")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "ارسال آیه"))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "اشتراک‌گذاری آیه",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "آیه ${ayah.numberInSurah}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic Text with Highlight
            Text(
                text = ayah.textArabic,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.85f).sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                    color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )

            // Persian Translation with Highlight
            if (showTranslation && ayah.textPersian.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    color = if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.8.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ayah.textPersian,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                        color = if (isHighlighted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}
