package com.example.indicoffline

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ModelDownloader {
    private val MODEL_URLS = listOf(
        "https://huggingface.co/fischerman/sarvam-translate-gguf/resolve/main/sarvam-translate.Q4_K_S.gguf",
        "https://huggingface.co/mradermacher/sarvam-translate-GGUF/resolve/main/sarvam-translate.Q4_K_S.gguf"
    )
    private const val MODEL_FILENAME = "sarvam-translate.Q4_K_S.gguf"

    fun getModelFile(context: Context): File {
        val externalFile = File(context.getExternalFilesDir(null), MODEL_FILENAME)
        if (externalFile.exists()) return externalFile
        return File(context.filesDir, MODEL_FILENAME)
    }

    fun isModelDownloaded(context: Context): Boolean {
        return getModelFile(context).exists()
    }

    fun enqueueDownload(context: Context) {
        val workManager = androidx.work.WorkManager.getInstance(context)
        
        val inputData = androidx.work.workDataOf(
            "MODEL_URLS" to MODEL_URLS.toTypedArray(),
            "MODEL_FILENAME" to MODEL_FILENAME
        )

        val workRequest = androidx.work.OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(inputData)
            .addTag("model_download")
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            "model_download_work",
            androidx.work.ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}