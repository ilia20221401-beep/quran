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
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerState
import com.example.data.model.AYAH_BASMALAH
import com.example.data.model.AYAH_ISTIADHA
import com.example.data.model.Ayah
import com.example.data.model.Surah
import com.example.ui.components.AyahNoteDialog
import com.example.ui.components.TafsirBottomSheet
import com.example.ui.viewmodel.ReadingTheme

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
    readingTheme: ReadingTheme = ReadingTheme.DEFAULT,
    targetAyahToScroll: Int? = null,
    onClearTargetAyahToScroll: () -> Unit = {},
    isAyahBookmarked: (Int) -> Boolean = { false },
    getAyahNote: (Int) -> String = { "" },
    onToggleAyahBookmark: (Ayah) -> Unit = {},
    onSaveAyahNote: (Ayah, String) -> Unit = { _, _ -> },
    onDeleteAyahNote: (Ayah) -> Unit = {},
    onChangeReadingTheme: (ReadingTheme) -> Unit = {},
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
    var showThemeMenu by remember { mutableStateOf(false) }
    var autoScrollEnabled by remember { mutableStateOf(true) }
    var activeTafsirAyah by remember { mutableStateOf<Ayah?>(null) }
    var activeNoteAyah by remember { mutableStateOf<Ayah?>(null) }
    val listState = rememberLazyListState()

    val isThisSurahSelected = playerState.currentSurah?.number == surah.number
    val isPlayingThisSurah = isThisSurahSelected && playerState.isPlaying
    val defaultFirstAyah = when (surah.number) {
        1 -> AYAH_ISTIADHA
        9 -> 1
        else -> AYAH_BASMALAH
    }
    val activeAyahNumber = if (isThisSurahSelected) (playerState.activeAyahNumber ?: defaultFirstAyah) else null

    // Auto-scroll to the currently reciting verse
    LaunchedEffect(activeAyahNumber, isPlayingThisSurah, autoScrollEnabled) {
        if (autoScrollEnabled && isPlayingThisSurah && activeAyahNumber != null) {
            val headerCount = when (surah.number) {
                1 -> 1
                9 -> 0
                else -> 1
            }
            val targetIndex = when (activeAyahNumber) {
                AYAH_ISTIADHA -> 0
                AYAH_BASMALAH -> 0
                else -> {
                    val ayahIndex = ayahs.indexOfFirst { it.numberInSurah == activeAyahNumber }
                    if (ayahIndex >= 0) {
                        headerCount + ayahIndex
                    } else null
                }
            }
            if (targetIndex != null) {
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
            val statusText = when (activeAyahNumber) {
                AYAH_ISTIADHA -> "در حال قرائت: استعاذه (أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ)"
                AYAH_BASMALAH -> "در حال قرائت: تسمیه (بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ)"
                1 -> if (surah.number == 1) "در حال قرائت: آیه ۱ (بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ)" else "در حال قرائت: آیه ۱ از ${surah.numberOfAyahs}"
                else -> "در حال قرائت: آیه $activeAyahNumber از ${surah.numberOfAyahs}"
            }
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            maxLines = 1
                        )
                    }

                    Text(
                        text = if (autoScrollEnabled) "پیمایش فعال" else "پیمایش متوقف",
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
                // Header Items:
                // Surah 1 (Al-Fatiha): begins with Isti'adha ("أَعُوذُ بِٱللَّهِ..."), then Ayah 1 is Basmalah.
                // Surah 9 (At-Tawbah): revealed without Basmalah, audio starts directly at Ayah 1.
                // All other Surahs (2..8 and 10..114): reciter recites ONLY Tasmiyah / Basmalah ("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ") before Ayah 1.
                when (surah.number) {
                    1 -> {
                        item {
                            val isIstiadhaHighlighted = isThisSurahSelected && activeAyahNumber == AYAH_ISTIADHA
                            IstiadhaCard(
                                isHighlighted = isIstiadhaHighlighted,
                                isPlaying = isPlayingThisSurah && isIstiadhaHighlighted,
                                fontSize = arabicFontSize,
                                showTranslation = showTranslation,
                                onPlay = { onSeekToAyah(AYAH_ISTIADHA) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            )
                        }
                    }
                    9 -> {
                        // Surah At-Tawbah: Starts directly from Ayah 1 without Basmalah or Istiadha.
                    }
                    else -> {
                        item {
                            val isBasmalahHighlighted = isThisSurahSelected && activeAyahNumber == AYAH_BASMALAH
                            BasmalahCard(
                                isHighlighted = isBasmalahHighlighted,
                                isPlaying = isPlayingThisSurah && isBasmalahHighlighted,
                                fontSize = arabicFontSize,
                                showTranslation = showTranslation,
                                onPlay = { onSeekToAyah(AYAH_BASMALAH) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            )
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

@Composable
fun IstiadhaCard(
    isHighlighted: Boolean,
    isPlaying: Boolean,
    fontSize: Float,
    showTranslation: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBgColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        animationSpec = tween(durationMillis = 350),
        label = "istiadha_card_bg"
    )

    val cardBorder = if (isHighlighted) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }

    Card(
        modifier = modifier.testTag("istiadha_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row with Badge & Listen action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPlaying) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isPlaying) "در حال تلاوت استعاذه" else "استعاذه (پناه‌جویی)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("play_istiadha")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "شنیدن استعاذه",
                        tint = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Arabic text: اعوذ بالله من الشیطان الرجیم
            Text(
                text = "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize.sp,
                    color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (showTranslation) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "پناه می‌برم به خداوند از شر شیطان رانده‌شده",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun BasmalahCard(
    isHighlighted: Boolean,
    isPlaying: Boolean,
    fontSize: Float,
    showTranslation: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBgColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        animationSpec = tween(durationMillis = 350),
        label = "basmalah_card_bg"
    )

    val cardBorder = if (isHighlighted) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }

    Card(
        modifier = modifier.testTag("basmalah_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row with Badge & Listen action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPlaying) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isPlaying) "در حال تلاوت تسمیه" else "تسمیه (آغاز با نام خدا)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("play_basmalah")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "شنیدن بسم‌الله",
                        tint = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Arabic text: بسم الله الرحمن الرحیم
            Text(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (fontSize + 1f).sp,
                    color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (showTranslation) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "به نام خداوند بخشنده مهربان",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
