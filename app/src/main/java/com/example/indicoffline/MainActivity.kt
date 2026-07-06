package com.example.indicoffline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    private val audioCapturer = AudioCapturer()
    private val viewModel: TranslationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.setSustainedPerformanceMode(true)
        window.isNavigationBarContrastEnforced = false

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        setContent {
            val isModelReady by viewModel.isModelReady.collectAsState()
            val isModelDownloaded by viewModel.isModelDownloaded.collectAsState()
            
            AppNavigation(
                viewModel = viewModel,
                audioCapturer = audioCapturer,
                isModelDownloaded = isModelDownloaded,
                isModelReady = isModelReady
            )
        }
    }
}

