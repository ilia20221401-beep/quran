package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AudioPlayerState
import com.example.audio.RepeatMode
import com.example.data.model.AYAH_BASMALAH
import com.example.data.model.AYAH_ISTIADHA
import java.util.Locale

@Composable
fun AudioPlayerDialog(
    playerState: AudioPlayerState,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Long) -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSelectReciter: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetRepeatMode: (RepeatMode) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val surah = playerState.currentSurah ?: return

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val actualProgress = if (playerState.totalDurationMs > 0) {
        playerState.currentPositionMs.toFloat() / playerState.totalDurationMs.toFloat()
    } else 0f

    val sliderValue = if (isDraggingSlider) dragProgress else actualProgress.coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(28.dp))
                .testTag("full_audio_player"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_audio_player")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن پنجره",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "پخش تلاوت قرآن کریم",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Stop button in header
                    IconButton(
                        onClick = {
                            onStop()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("player_stop_header")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "توقف کامل",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Surah Calligraphy Plate
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = surah.nameArabic,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = when (playerState.activeAyahNumber) {
                                    AYAH_ISTIADHA -> "در حال تلاوت استعاذه (اعوذ بالله من الشیطان الرجیم)"
                                    AYAH_BASMALAH -> "در حال تلاوت تسمیه (بسم الله الرحمن الرحیم)"
                                    null -> "سوره ${surah.namePersian} • ${surah.numberOfAyahs} آیه • ${surah.revelationType}"
                                    else -> "در حال تلاوت آیه ${playerState.activeAyahNumber} از ${surah.numberOfAyahs} • جزء ${surah.juz}"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (playerState.activeAyahNumber != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (playerState.activeAyahNumber != null) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reciter Selector Button
                OutlinedButton(
                    onClick = onSelectReciter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_reciter_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SpatialAudio,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "قاری: ${playerState.currentReciter.namePersian}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Error Message if any
                playerState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.error
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Slider scrubber
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        isDraggingSlider = true
                        dragProgress = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        val targetMs = (dragProgress * playerState.totalDurationMs).toLong()
                        onSeek(targetMs)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audio_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                // Time indicators (current & total)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayCurrentMs = if (isDraggingSlider) {
                        (dragProgress * playerState.totalDurationMs).toLong()
                    } else playerState.currentPositionMs

                    Text(
                        text = formatTime(displayCurrentMs),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Text(
                        text = formatTime(playerState.totalDurationMs),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Controls: Previous, Rewind 10, Play/Pause, Forward 10, Next
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Surah
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "سوره قبلی",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Rewind 10s
                    IconButton(
                        onClick = onRewind10,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "۱۰ ثانیه عقب",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Big Play/Pause Button
                    FilledIconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("player_play_pause_big"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (playerState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else if (playerState.isPlaying) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "توقف",
                                modifier = Modifier.size(34.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "پخش",
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    // Forward 10s
                    IconButton(
                        onClick = onForward10,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "۱۰ ثانیه جلو",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Next Surah
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "سوره بعدی",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Repeat Mode & Playback Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val nextRepeatMode = when (playerState.repeatMode) {
                        RepeatMode.REPEAT_NONE -> RepeatMode.REPEAT_ONE
                        RepeatMode.REPEAT_ONE -> RepeatMode.AUTO_PLAY_NEXT
                        RepeatMode.AUTO_PLAY_NEXT -> RepeatMode.REPEAT_NONE
                    }

                    OutlinedButton(
                        onClick = { onSetRepeatMode(nextRepeatMode) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = when (playerState.repeatMode) {
                                RepeatMode.REPEAT_ONE -> Icons.Default.RepeatOne
                                RepeatMode.AUTO_PLAY_NEXT -> Icons.Default.Repeat
                                RepeatMode.REPEAT_NONE -> Icons.Default.Stop
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = playerState.repeatMode.titleFa,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                            val isSelected = playerState.playbackSpeed == speed
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetSpeed(speed) },
                                label = {
                                    Text(
                                        text = "${speed}x",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sleep Timer (تایمر خواب خودکار)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (playerState.sleepTimerMinutes > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (playerState.sleepTimerRemainingSeconds > 0) {
                                val m = playerState.sleepTimerRemainingSeconds / 60
                                val s = playerState.sleepTimerRemainingSeconds % 60
                                "خاموشی در: ${String.format(Locale.US, "%02d:%02d", m, s)}"
                            } else "تایمر خواب:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (playerState.sleepTimerMinutes > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0 to "غیرفعال", 15 to "۱۵دقیقه", 30 to "۳۰دقیقه", 60 to "۶۰دقیقه").forEach { (mins, label) ->
                            val isSel = playerState.sleepTimerMinutes == mins
                            FilterChip(
                                selected = isSel,
                                onClick = { onSetSleepTimer(mins) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
