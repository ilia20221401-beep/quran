package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import com.example.data.model.Reciter
import com.example.data.model.RecitersData
import com.example.data.model.Surah
import com.example.data.model.SurahDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatMode(val titleFa: String) {
    REPEAT_NONE("بدون تکرار"),
    REPEAT_ONE("تکرار همین سوره"),
    AUTO_PLAY_NEXT("پخش پیوسته سوره‌ها")
}

data class AudioPlayerState(
    val currentSurah: Surah? = null,
    val currentReciter: Reciter = RecitersData.defaultReciter,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: RepeatMode = RepeatMode.AUTO_PLAY_NEXT,
    val sleepTimerMinutes: Int = 0,
    val sleepTimerRemainingSeconds: Long = 0L,
    val errorMessage: String? = null
)

class QuranAudioManager(private val context: Context) {
    private enum class InternalState {
        IDLE,
        PREPARING,
        PREPARED,
        PLAYING,
        PAUSED,
        ERROR
    }

    private var mediaPlayer: MediaPlayer? = null
    private var internalState = InternalState.IDLE
    private var isUsingFallback = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    private fun safeReleasePlayer() {
        stopProgressTracker()
        try {
            mediaPlayer?.let { player ->
                if (try { player.isPlaying } catch (e: Exception) { false }) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            // Ignore release exceptions
        }
        mediaPlayer = null
        internalState = InternalState.IDLE
    }

    private fun createAndPreparePlayer(audioUrl: String): Boolean {
        safeReleasePlayer()

        return try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                try {
                    setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                } catch (e: Exception) {
                    // Ignore
                }

                setOnPreparedListener { mp ->
                    internalState = InternalState.PREPARED
                    val duration = try { mp.duration.toLong() } catch (e: Exception) { 0L }
                    _playerState.value = _playerState.value.copy(
                        isBuffering = false,
                        isPlaying = true,
                        totalDurationMs = duration,
                        errorMessage = null
                    )
                    applySpeed(_playerState.value.playbackSpeed)
                    try {
                        mp.start()
                        internalState = InternalState.PLAYING
                        startProgressTracker()
                    } catch (e: Exception) {
                        internalState = InternalState.ERROR
                        _playerState.value = _playerState.value.copy(
                            isPlaying = false,
                            isBuffering = false,
                            errorMessage = "خطا در شروع پخش صوت"
                        )
                    }
                }

                setOnCompletionListener {
                    internalState = InternalState.PREPARED
                    handlePlaybackCompletion()
                }

                setOnErrorListener { _, what, extra ->
                    stopProgressTracker()
                    internalState = InternalState.ERROR

                    val currentSurah = _playerState.value.currentSurah
                    val currentReciter = _playerState.value.currentReciter
                    if (!isUsingFallback && currentSurah != null) {
                        isUsingFallback = true
                        val fallbackUrl = currentReciter.getAudioUrl(currentSurah.number, useFallback = true)
                        scope.launch {
                            createAndPreparePlayer(fallbackUrl)
                        }
                    } else {
                        _playerState.value = _playerState.value.copy(
                            isPlaying = false,
                            isBuffering = false,
                            errorMessage = "خطا در اتصال به سرور تلاوت (کد: $what). لطفاً اتصال اینترنت را بررسی نمایید."
                        )
                    }
                    true
                }
            }

            internalState = InternalState.PREPARING
            val headers = mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android) QuranApp/1.0")
            player.setDataSource(context, Uri.parse(audioUrl), headers)
            player.prepareAsync()
            mediaPlayer = player
            true
        } catch (e: Exception) {
            safeReleasePlayer()
            internalState = InternalState.ERROR
            _playerState.value = _playerState.value.copy(
                isBuffering = false,
                isPlaying = false,
                errorMessage = "خطا در شروع: ${e.localizedMessage ?: "عدم اتصال"}"
            )
            false
        }
    }

    fun playSurah(surah: Surah, reciter: Reciter = _playerState.value.currentReciter) {
        isUsingFallback = false
        val audioUrl = reciter.getAudioUrl(surah.number, useFallback = false)
        _playerState.value = _playerState.value.copy(
            currentSurah = surah,
            currentReciter = reciter,
            isBuffering = true,
            isPlaying = false,
            currentPositionMs = 0L,
            totalDurationMs = 0L,
            errorMessage = null
        )

        createAndPreparePlayer(audioUrl)
    }

    fun pause() {
        stopProgressTracker()
        try {
            val player = mediaPlayer
            if (player != null && (try { player.isPlaying } catch (e: Exception) { false } || internalState == InternalState.PLAYING)) {
                player.pause()
            }
        } catch (e: Exception) {
            // Ignore
        }
        internalState = InternalState.PAUSED
        _playerState.value = _playerState.value.copy(isPlaying = false, isBuffering = false)
    }

    fun resume() {
        val player = mediaPlayer
        if (player != null && (internalState == InternalState.PAUSED || internalState == InternalState.PREPARED)) {
            try {
                applySpeed(_playerState.value.playbackSpeed)
                player.start()
                internalState = InternalState.PLAYING
                _playerState.value = _playerState.value.copy(isPlaying = true, isBuffering = false)
                startProgressTracker()
                return
            } catch (e: Exception) {
                internalState = InternalState.ERROR
            }
        }
        val surah = _playerState.value.currentSurah ?: SurahDatabase.getSurah(1)
        surah?.let { playSurah(it) }
    }

    fun stop() {
        stopProgressTracker()
        safeReleasePlayer()
        _playerState.value = _playerState.value.copy(
            isPlaying = false,
            isBuffering = false,
            currentPositionMs = 0L,
            currentSurah = null
        )
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        val isAudioActuallyPlaying = try { player?.isPlaying == true } catch (e: Exception) { false }
        if (isAudioActuallyPlaying || _playerState.value.isPlaying || _playerState.value.isBuffering) {
            pause()
        } else {
            resume()
        }
    }

    fun seekTo(positionMs: Long) {
        if (internalState == InternalState.PLAYING || internalState == InternalState.PAUSED || internalState == InternalState.PREPARED) {
            try {
                mediaPlayer?.seekTo(positionMs.toInt())
                _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
            } catch (e: Exception) {
                // Ignore seek exception
            }
        }
    }

    fun seekForward10s() {
        val current = _playerState.value.currentPositionMs
        val total = _playerState.value.totalDurationMs
        val target = (current + 10_000L).coerceAtMost(if (total > 0) total else current + 10_000L)
        seekTo(target)
    }

    fun seekRewind10s() {
        val current = _playerState.value.currentPositionMs
        val target = (current - 10_000L).coerceAtLeast(0L)
        seekTo(target)
    }

    fun playNext() {
        val current = _playerState.value.currentSurah ?: return
        val nextNumber = if (current.number < 114) current.number + 1 else 1
        val nextSurah = SurahDatabase.getSurah(nextNumber) ?: return
        playSurah(nextSurah)
    }

    fun playPrevious() {
        val current = _playerState.value.currentSurah ?: return
        val prevNumber = if (current.number > 1) current.number - 1 else 114
        val prevSurah = SurahDatabase.getSurah(prevNumber) ?: return
        playSurah(prevSurah)
    }

    fun setReciter(reciter: Reciter) {
        if (_playerState.value.currentReciter.id == reciter.id) return
        val currentSurah = _playerState.value.currentSurah
        _playerState.value = _playerState.value.copy(currentReciter = reciter)
        if (currentSurah != null && (_playerState.value.isPlaying || _playerState.value.isBuffering)) {
            playSurah(currentSurah, reciter)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        applySpeed(speed)
    }

    private fun applySpeed(speed: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null && (internalState == InternalState.PLAYING || internalState == InternalState.PAUSED || internalState == InternalState.PREPARED)) {
                val params = mediaPlayer?.playbackParams ?: PlaybackParams()
                params.speed = speed
                mediaPlayer?.playbackParams = params
            }
        } catch (e: Exception) {
            // Ignore speed exception
        }
    }

    fun setRepeatMode(mode: RepeatMode) {
        _playerState.value = _playerState.value.copy(repeatMode = mode)
    }

    private fun handlePlaybackCompletion() {
        val state = _playerState.value
        when (state.repeatMode) {
            RepeatMode.REPEAT_ONE -> {
                state.currentSurah?.let { playSurah(it) }
            }
            RepeatMode.AUTO_PLAY_NEXT -> {
                playNext()
            }
            RepeatMode.REPEAT_NONE -> {
                _playerState.value = state.copy(isPlaying = false, currentPositionMs = 0L)
                stopProgressTracker()
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                try {
                    val mp = mediaPlayer
                    if (mp != null && internalState == InternalState.PLAYING && mp.isPlaying) {
                        val current = mp.currentPosition.toLong()
                        val total = mp.duration.toLong()
                        _playerState.value = _playerState.value.copy(
                            currentPositionMs = current,
                            totalDurationMs = total
                        )
                    }
                } catch (e: Exception) {
                    // Ignore
                }
                delay(350)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private var sleepTimerJob: Job? = null

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playerState.value = _playerState.value.copy(sleepTimerMinutes = 0, sleepTimerRemainingSeconds = 0L)
            return
        }

        var remainingSeconds = (minutes * 60).toLong()
        _playerState.value = _playerState.value.copy(
            sleepTimerMinutes = minutes,
            sleepTimerRemainingSeconds = remainingSeconds
        )

        sleepTimerJob = scope.launch {
            while (isActive && remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                _playerState.value = _playerState.value.copy(sleepTimerRemainingSeconds = remainingSeconds)
            }
            if (isActive) {
                pause()
                _playerState.value = _playerState.value.copy(sleepTimerMinutes = 0, sleepTimerRemainingSeconds = 0L)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _playerState.value = _playerState.value.copy(sleepTimerMinutes = 0, sleepTimerRemainingSeconds = 0L)
    }

    fun release() {
        cancelSleepTimer()
        safeReleasePlayer()
    }
}
