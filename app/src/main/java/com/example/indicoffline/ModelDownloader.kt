package com.example.indicoffline

import android.content.Context
import android.app.ActivityManager
import java.io.File

object ModelDownloader {

    private fun getTotalRamGb(context: Context): Double {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
    }

    private val MODEL_URLS_Q4 = listOf(
        "https://huggingface.co/fischerman/sarvam-translate-gguf/resolve/main/sarvam-translate.Q4_K_S.gguf",
        "https://huggingface.co/mradermacher/sarvam-translate-GGUF/resolve/main/sarvam-translate.Q4_K_S.gguf"
    )

    private val MODEL_URLS_Q2 = listOf(
        "https://huggingface.co/mradermacher/sarvam-translate-GGUF/resolve/main/sarvam-translate.Q2_K.gguf",
        "https://huggingface.co/fischerman/sarvam-translate-gguf/resolve/main/sarvam-translate.Q2_K.gguf"
    )

    private fun getModelFilename(context: Context): String {
        // Only 8GB+ phones will get the Q4 model. 6GB and below get Q2.
        return if (getTotalRamGb(context) > 6.0) {
            "sarvam-translate.Q4_K_S.gguf"
        } else {
            "sarvam-translate.Q2_K.gguf"
        }
    }

    private fun getModelUrls(context: Context): Array<String> {
        // Only 8GB+ phones will get the Q4 model. 6GB and below get Q2.
        return if (getTotalRamGb(context) > 6.0) {
            MODEL_URLS_Q4.toTypedArray()
        } else {
            MODEL_URLS_Q2.toTypedArray()
        }
    }

    fun getModelFile(context: Context): File {
        val filename = getModelFilename(context)
        val externalFile = File(context.getExternalFilesDir(null), filename)
        if (externalFile.exists()) return externalFile
        return File(context.filesDir, filename)
    }

    fun isModelDownloaded(context: Context): Boolean {
        return getModelFile(context).exists()
    }

    fun enqueueDownload(context: Context) {
        val ramGb = getTotalRamGb(context)
        val selectedModel = getModelFilename(context)
        android.util.Log.d("ModelDownloader", "Device RAM detected: $ramGb GB. Selecting model: $selectedModel")

        val workManager = androidx.work.WorkManager.getInstance(context)
        
        val inputData = androidx.work.workDataOf(
            "MODEL_URLS" to getModelUrls(context),
            "MODEL_FILENAME" to selectedModel
        )

        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val workRequest = androidx.work.OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
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