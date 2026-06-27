package com.example.indicoffline

import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.indicoffline.ui.theme.IndicofflineTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation(
    viewModel: TranslationViewModel,
    audioCapturer: AudioCapturer,
    isModelDownloaded: Boolean,
    isModelReady: Boolean
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val isDarkModePref by viewModel.isDarkMode.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()
    val isDarkMode = isDarkModePref ?: systemDarkTheme

    IndicofflineTheme(darkTheme = isDarkMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = if (!isModelDownloaded) "download" else "asr"
            ) {
                composable("download") {
                    DownloadScreen(viewModel = viewModel)
                }
                composable("asr") {
                    AsrScreen(
                        viewModel = viewModel,
                        audioCapturer = audioCapturer,
                        isModelReady = isModelReady,
                        onNavigateToSettings = { navController.navigate("settings") },
                        onTtsMissing = { lang ->
                            Toast.makeText(context, "Please install $lang TTS voice data.", Toast.LENGTH_LONG).show()
                            val installIntent = Intent()
                            installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                            installIntent.setPackage("com.google.android.tts")
                            try {
                                context.startActivity(installIntent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
