package com.example.indicoffline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelUrls = inputData.getStringArray("MODEL_URLS") ?: return@withContext Result.failure()
        val modelFilename = inputData.getString("MODEL_FILENAME") ?: return@withContext Result.failure()
        
        createNotificationChannel()
        
        setForeground(createForegroundInfo(0))

        var attempts = 0
        var delayMs = 5000L
        var urlIndex = 0
        
        while (attempts < 50) {
            val urlToDownload = modelUrls[urlIndex]
            val success = tryDownload(urlToDownload, modelFilename)
            
            if (success) {
                setProgress(workDataOf("PROGRESS" to 100))
                return@withContext Result.success()
            }
            
            urlIndex = (urlIndex + 1) % modelUrls.size
            attempts++
            android.util.Log.d("ModelDownloadWorker", "Retrying with $urlToDownload... attempt $attempts")
            delay(delayMs)
            delayMs = minOf(delayMs * 2, 60000L)
        }
        
        Result.failure()
    }
    
    private suspend fun tryDownload(urlToDownload: String, modelFilename: String): Boolean {
        try {
            val file = File(applicationContext.filesDir, modelFilename)
            val tempFile = File(applicationContext.filesDir, "$modelFilename.tmp")
            val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

            val url = URL(urlToDownload)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000

            if (existingBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$existingBytes-")
                android.util.Log.d("ModelDownloadWorker", "Resuming from $existingBytes bytes")
            }

            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != 206) {
                android.util.Log.e("ModelDownloadWorker", "Bad response: $responseCode")
                return false
            }

            val contentLength = connection.contentLengthLong
            var totalBytes = if (responseCode == 206) existingBytes + contentLength else contentLength
            
            if (contentLength == -1L) {
                totalBytes = 2469606195L
            }

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
                    var lastProgress = -1
                    var lastLogTime = 0L
                    
                    while (input.read(buffer).also { bytes = it } != -1) {
                        it.write(buffer, 0, bytes)
                        downloaded += bytes
                        
                        if (totalBytes > 0) {
                            val progress = ((downloaded.toDouble() / totalBytes.toDouble()) * 100).toInt()
                            
                            val currentTime = System.currentTimeMillis()
                            if (progress != lastProgress || currentTime - lastLogTime > 2000) {
                                lastProgress = progress
                                lastLogTime = currentTime
                                android.util.Log.d("ModelDownloadWorker", "Downloaded: $downloaded / $totalBytes bytes ($progress%)")
                                
                                setProgress(workDataOf("PROGRESS" to progress))
                                setForeground(createForegroundInfo(progress))
                            }
                        }
                    }
                }
            }

            val renamed = tempFile.renameTo(file)
            if (!renamed) {
                android.util.Log.e("ModelDownloadWorker", "Failed to rename temp file")
                return false
            }
            return true
        } catch (e: Exception) {
            android.util.Log.e("ModelDownloadWorker", "Download failed: ${e.message}")
            return false
        }
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val title = "Setting up your offline experience"
        val cancel = "Cancel"
        
        val intent = androidx.work.WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)

        val notification = NotificationCompat.Builder(applicationContext, "download_channel")
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(if (progress < 100) "$progress% completed" else "Finalizing...")
            .setSmallIcon(R.drawable.ic_uktam_logo)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .setProgress(100, progress, false)
            .build()
            
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(1, notification)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "download_channel",
                "Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}
