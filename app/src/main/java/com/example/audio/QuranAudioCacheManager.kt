package com.example.audio

import android.content.Context
import com.example.data.model.Reciter
import com.example.data.model.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class QuranAudioCacheManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val audioDir: File
        get() = File(context.filesDir, "quran_audio").apply {
            if (!exists()) mkdirs()
        }

    // Active downloading tasks progress: map of surahNumber -> Float (0.0 to 1.0)
    private val _downloadProgress = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<Int, Float>> = _downloadProgress.asStateFlow()

    // Set of cached surah numbers for reactive UI
    private val _cachedSurahNumbers = MutableStateFlow<Set<Int>>(emptySet())
    val cachedSurahNumbers: StateFlow<Set<Int>> = _cachedSurahNumbers.asStateFlow()

    private val downloadingSet = ConcurrentHashMap.newKeySet<Int>()

    init {
        refreshCachedSurahs()
    }

    fun getAudioFile(reciterId: String, surahNumber: Int): File {
        return File(audioDir, "${reciterId}_${surahNumber}.mp3")
    }

    fun isCached(reciter: Reciter, surahNumber: Int): Boolean {
        val file = getAudioFile(reciter.id, surahNumber)
        return file.exists() && file.length() > 20_000L // Ensure file has valid audio data
    }

    fun refreshCachedSurahs(reciterId: String = "alafasy") {
        val list = audioDir.listFiles() ?: emptyArray()
        val set = mutableSetOf<Int>()
        for (file in list) {
            if (file.name.startsWith("${reciterId}_") && file.name.endsWith(".mp3") && file.length() > 20_000L) {
                val numPart = file.name.substringAfter("${reciterId}_").substringBefore(".mp3")
                numPart.toIntOrNull()?.let { set.add(it) }
            }
        }
        _cachedSurahNumbers.value = set
    }

    /**
     * Downloads and caches the Surah audio file to local storage.
     * Can be invoked explicitly or called in background while streaming.
     */
    suspend fun downloadAndCache(
        surah: Surah,
        reciter: Reciter,
        onProgressUpdate: ((Float) -> Unit)? = null
    ): File? = withContext(Dispatchers.IO) {
        val targetFile = getAudioFile(reciter.id, surah.number)
        if (targetFile.exists() && targetFile.length() > 20_000L) {
            refreshCachedSurahs(reciter.id)
            return@withContext targetFile
        }

        if (!downloadingSet.add(surah.number)) {
            // Already downloading
            return@withContext null
        }

        val tempFile = File(audioDir, "${reciter.id}_${surah.number}.tmp")
        try {
            updateProgress(surah.number, 0.05f)
            onProgressUpdate?.invoke(0.05f)

            val url = reciter.getAudioUrl(surah.number, useFallback = false)
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android) QuranApp/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                // Try fallback URL if primary fails
                val fallbackUrl = reciter.getAudioUrl(surah.number, useFallback = true)
                val fallbackReq = Request.Builder()
                    .url(fallbackUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android) QuranApp/1.0")
                    .build()
                val fallbackRes = client.newCall(fallbackReq).execute()
                if (!fallbackRes.isSuccessful) {
                    removeProgress(surah.number)
                    downloadingSet.remove(surah.number)
                    return@withContext null
                }
                writeResponseToFile(fallbackRes, tempFile, surah.number, onProgressUpdate)
            } else {
                writeResponseToFile(response, tempFile, surah.number, onProgressUpdate)
            }

            if (tempFile.exists() && tempFile.length() > 20_000L) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                refreshCachedSurahs(reciter.id)
                removeProgress(surah.number)
                downloadingSet.remove(surah.number)
                return@withContext targetFile
            }
        } catch (e: Exception) {
            tempFile.delete()
        } finally {
            removeProgress(surah.number)
            downloadingSet.remove(surah.number)
        }
        null
    }

    private fun writeResponseToFile(
        response: okhttp3.Response,
        tempFile: File,
        surahNumber: Int,
        onProgressUpdate: ((Float) -> Unit)?
    ) {
        val body = response.body ?: return
        val totalLength = body.contentLength()
        body.byteStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (totalLength > 0) {
                        val progress = (totalBytesRead.toFloat() / totalLength.toFloat()).coerceIn(0f, 0.99f)
                        updateProgress(surahNumber, progress)
                        onProgressUpdate?.invoke(progress)
                    }
                }
            }
        }
    }

    private fun updateProgress(surahNumber: Int, progress: Float) {
        val map = _downloadProgress.value.toMutableMap()
        map[surahNumber] = progress
        _downloadProgress.value = map
    }

    private fun removeProgress(surahNumber: Int) {
        val map = _downloadProgress.value.toMutableMap()
        map.remove(surahNumber)
        _downloadProgress.value = map
    }

    fun deleteCache(reciter: Reciter, surahNumber: Int): Boolean {
        val file = getAudioFile(reciter.id, surahNumber)
        val deleted = if (file.exists()) file.delete() else false
        refreshCachedSurahs(reciter.id)
        return deleted
    }

    fun clearAllAudioCache(): Boolean {
        var success = true
        audioDir.listFiles()?.forEach {
            if (!it.delete()) success = false
        }
        refreshCachedSurahs()
        return success
    }

    fun getTotalCacheSizeFormatted(): String {
        var bytes = 0L
        audioDir.listFiles()?.forEach { bytes += it.length() }
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(java.util.Locale.US, "%.1f مگابایت", mb)
    }
}
