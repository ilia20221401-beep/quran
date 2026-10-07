package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.AudioPlayerState
import com.example.data.model.JuzDatabase
import com.example.data.model.JuzInfo
import com.example.data.model.Surah
import com.example.data.model.SurahDatabase
import com.example.ui.components.SurahCard
import com.example.ui.viewmodel.SurahFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahListScreen(
    surahs: List<Surah>,
    lastReadSurah: Surah?,
    searchQuery: String,
    selectedFilter: SurahFilter,
    playerState: AudioPlayerState,
    favoriteIds: Set<Int>,
    onSearchChange: (String) -> Unit,
    onFilterSelect: (SurahFilter) -> Unit,
    onSurahClick: (Surah) -> Unit,
    onPlaySurah: (Surah) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onSelectReciter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedListTab by remember { mutableIntStateOf(0) } // 0: Surahs, 1: Juz

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("surah_list_screen"),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Hero Banner Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.quran_hero_banner_1791396872226),
                    contentDescription = "قرآن کریم",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark & Emerald gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "قرآن کریم با صوت قاریان برجسته جهان اسلام",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }

                        // Reciter button in header
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.clickable { onSelectReciter() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SpatialAudio,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = playerState.currentReciter.namePersian.substringAfter("استاد ").substringAfter("شیخ "),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    // Quick recitation callout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "۱۱۴ سوره کامل • تلاوت ترتیل",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Text(
                            text = "۳۰ جزء",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }
        }

        // Last Read / Quick Resume Card
        if (lastReadSurah != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clickable { onSurahClick(lastReadSurah) }
                        .testTag("last_read_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "آخرین سوره خوانده شده",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "سوره ${lastReadSurah.namePersian} (${lastReadSurah.nameArabic})",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = { onPlaySurah(lastReadSurah) },
                            modifier = Modifier.testTag("play_last_read")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "پخش",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // View Mode Tabs: سوره‌ها (Surahs) vs اجزاء قرآن (Juz Index)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                TabRow(
                    selectedTabIndex = selectedListTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedListTab == 0,
                        onClick = { selectedListTab = 0 },
                        text = {
                            Text(
                                text = "سوره‌ها (۱۱۴)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedListTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                    Tab(
                        selected = selectedListTab == 1,
                        onClick = { selectedListTab = 1 },
                        text = {
                            Text(
                                text = "اجزاء قرآن (۳۰)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedListTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }
        }

        if (selectedListTab == 0) {
            // Search Bar for Surahs
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_surah_input"),
                        placeholder = {
                            Text(
                                text = "جستجوی سوره (نام فارسی، عربی، شماره یا جزء)...",
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "پاک کردن",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }

            // Filter Chips Row
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SurahFilter.values()) { filter ->
                        val isSelected = filter == selectedFilter
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterSelect(filter) },
                            label = {
                                Text(
                                    text = filter.titleFa,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("filter_chip_${filter.name}")
                        )
                    }
                }
            }

            // Results count header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "فهرست سوره‌ها (${surahs.size} سوره)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Text(
                        text = "برای قرائت یا پخش لمس کنید",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // Surah Items
            items(surahs, key = { it.number }) { surah ->
                val isCurrent = playerState.currentSurah?.number == surah.number
                val isFavorite = favoriteIds.contains(surah.number)

                SurahCard(
                    surah = surah,
                    isCurrentPlaying = isCurrent,
                    isPlaying = isCurrent && playerState.isPlaying,
                    isBuffering = isCurrent && playerState.isBuffering,
                    isFavorite = isFavorite,
                    onPlayClick = { onPlaySurah(surah) },
                    onReadClick = { onSurahClick(surah) },
                    onFavoriteClick = { onToggleFavorite(surah.number) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                )
            }

            if (surahs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "هیچ سوره‌ای با این مشخصات یافت نشد.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Juz Index List (جزء ۱ تا ۳۰)
            item {
                Text(
                    text = "فهرست ۳۰ جزء قرآن کریم",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            items(JuzDatabase.allJuz, key = { it.number }) { juz ->
                JuzCard(
                    juz = juz,
                    onClick = {
                        SurahDatabase.getSurah(juz.startSurahNumber)?.let { onSurahClick(it) }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun JuzCard(
    juz: JuzInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("juz_card_${juz.number}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${juz.number}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = juz.namePersian,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = juz.nameArabic,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "آغاز از: سوره ${juz.startSurahName} (آیه ${juz.startAyahNumber}) • صفحه ${juz.startPage}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
