package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
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
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var showFontSlider by remember { mutableStateOf(false) }
    val isPlayingThisSurah = playerState.currentSurah?.number == surah.number && playerState.isPlaying

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
                    if (playerState.currentSurah?.number == surah.number && playerState.isBuffering) {
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

                // Ayahs List
                items(ayahs, key = { it.numberInSurah }) { ayah ->
                    AyahCard(
                        ayah = ayah,
                        surahName = surah.namePersian,
                        fontSize = arabicFontSize,
                        showTranslation = showTranslation,
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ayah_card_${ayah.numberInSurah}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Verse Header with Ayah Number Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${ayah.numberInSurah}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic Text
            Text(
                text = ayah.textArabic,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.85f).sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )

            // Persian Translation
            if (showTranslation && ayah.textPersian.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.8.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ayah.textPersian,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}
