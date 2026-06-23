package com.example.indicoffline

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var asrEngine: IndicAsrEngine
    private val audioCapturer = AudioCapturer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        asrEngine = IndicAsrEngine(assets)

        lifecycleScope.launch(Dispatchers.IO) {
            asrEngine.loadLanguage("hi")
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AsrScreen(asrEngine, audioCapturer)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        asrEngine.destroy()
    }
}

@Composable
fun AsrScreen(asrEngine: IndicAsrEngine, audioCapturer: AudioCapturer) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var transcription by remember { mutableStateOf("Ready. Press Record to speak.") }
    var isRecording by remember { mutableStateOf(false) }
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasMicPermission = isGranted }
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = {
                coroutineScope.launch(Dispatchers.IO) { asrEngine.loadLanguage("hi") }
                transcription = "Switched to Hindi"
            }) { Text("Hindi") }
            Button(onClick = {
                coroutineScope.launch(Dispatchers.IO) { asrEngine.loadLanguage("kn") }
                transcription = "Switched to Kannada"
            }) { Text("Kannada") }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Text(text = transcription, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                if (!hasMicPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    if (isRecording) {
                        isRecording = false
                        transcription = "Processing..."
                        coroutineScope.launch(Dispatchers.IO) {
                            val audioData = audioCapturer.stopAndGetFloatArray()
                            val resultText = asrEngine.transcribe(audioData)
                            withContext(Dispatchers.Main) {
                                transcription = resultText.ifEmpty { "No speech detected." }
                            }
                        }
                    } else {
                        audioCapturer.startRecording()
                        isRecording = true
                        transcription = "Listening..."
                    }
                }
            },
            modifier = Modifier.size(120.dp)
        ) {
            Text(if (isRecording) "Stop" else "Record")
        }
    }
}