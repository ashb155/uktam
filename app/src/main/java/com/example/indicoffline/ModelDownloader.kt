package com.example.indicoffline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ModelDownloader {
    private const val MODEL_URL = "https://huggingface.co/mradermacher/sarvam-translate-GGUF/resolve/main/sarvam-translate.Q4_K_S.gguf"
    private const val MODEL_FILENAME = "sarvam-translate.Q4_K_S.gguf"

    fun getModelFile(context: Context): File {
        val externalFile = File(context.getExternalFilesDir(null), MODEL_FILENAME)
        if (externalFile.exists()) return externalFile
        return File(context.filesDir, MODEL_FILENAME)
    }

    fun isModelDownloaded(context: Context): Boolean {
        return getModelFile(context).exists()
    }

    suspend fun downloadModel(
        context: Context,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var attempts = 0
        var delayMs = 5000L
        while (attempts < 5) {
            val success = tryDownload(context, onProgress)
            if (success) return@withContext true
            attempts++
            android.util.Log.d("ModelDownloader", "Retrying... attempt $attempts")
            kotlinx.coroutines.delay(delayMs)
            delayMs = minOf(delayMs * 2, 60000L)
        }
        false
    }

    private suspend fun tryDownload(
        context: Context,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, MODEL_FILENAME)
            val tempFile = File(context.filesDir, "$MODEL_FILENAME.tmp")
            val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

            val url = URL(MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            if (existingBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$existingBytes-")
                android.util.Log.d("ModelDownloader", "Resuming from $existingBytes bytes")
            }

            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != 206) {
                android.util.Log.e("ModelDownloader", "Bad response: $responseCode")
                return@withContext false
            }

            val contentLength = connection.contentLengthLong
            val totalBytes = if (responseCode == 206) existingBytes + contentLength else contentLength

            connection.inputStream.use { input ->
                val output = if (existingBytes > 0 && responseCode == 206) {
                    java.io.FileOutputStream(tempFile, true)
                } else {
                    tempFile.outputStream()
                }
                output.use {
                    val buffer = ByteArray(65536)
                    var downloaded = if (responseCode == 206) existingBytes else 0L
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        it.write(buffer, 0, bytes)
                        downloaded += bytes
                        if (totalBytes > 0) {
                            val progress = ((downloaded * 100) / totalBytes).toInt()
                            onProgress(progress)
                        }
                    }
                }
            }

            val renamed = tempFile.renameTo(file)
            if (!renamed) {
                android.util.Log.e("ModelDownloader", "Failed to rename temp file")
                return@withContext false
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("ModelDownloader", "Download failed: ${e.message}")
            false
        }
    }
}