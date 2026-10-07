package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerState
import com.example.data.model.Surah
import com.example.data.model.SurahDatabase
import com.example.ui.components.SurahCard

@Composable
fun FavoritesScreen(
    favoriteIds: Set<Int>,
    playerState: AudioPlayerState,
    onSurahClick: (Surah) -> Unit,
    onPlaySurah: (Surah) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val favoriteSurahs = favoriteIds.mapNotNull { SurahDatabase.getSurah(it) }
        .sortedBy { it.number }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("favorites_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Column {
                    Text(
                        text = "سوره‌های نشانه‌گذاری شده",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "دسترسی سریع به سوره‌های محبوب و پرفضیلت",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        if (favoriteSurahs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "هنوز سوره‌ای را نشان نکرده‌اید.",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "با لمس آیکون نشانک در کنار هر سوره، می‌توانید آن را به این بخش اضافه نمایید.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(favoriteSurahs, key = { it.number }) { surah ->
                val isCurrent = playerState.currentSurah?.number == surah.number
                SurahCard(
                    surah = surah,
                    isCurrentPlaying = isCurrent,
                    isPlaying = isCurrent && playerState.isPlaying,
                    isBuffering = isCurrent && playerState.isBuffering,
                    isFavorite = true,
                    onPlayClick = { onPlaySurah(surah) },
                    onReadClick = { onSurahClick(surah) },
                    onFavoriteClick = { onToggleFavorite(surah.number) },
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
        }

        // Recommended Virtuous Surahs section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "سوره‌های پرفضیلت پیشنهادی",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        val recommendedNumbers = listOf(36, 55, 56, 67, 18, 112)
        val recommendedSurahs = recommendedNumbers.mapNotNull { SurahDatabase.getSurah(it) }

        items(recommendedSurahs, key = { "rec_${it.number}" }) { surah ->
            val isCurrent = playerState.currentSurah?.number == surah.number
            val isFav = favoriteIds.contains(surah.number)
            SurahCard(
                surah = surah,
                isCurrentPlaying = isCurrent,
                isPlaying = isCurrent && playerState.isPlaying,
                isBuffering = isCurrent && playerState.isBuffering,
                isFavorite = isFav,
                onPlayClick = { onPlaySurah(surah) },
                onReadClick = { onSurahClick(surah) },
                onFavoriteClick = { onToggleFavorite(surah.number) },
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
