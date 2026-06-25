package com.example.indicoffline

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val audioCapturer = AudioCapturer()
    private val viewModel: TranslationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AsrScreen(
                        viewModel = viewModel,
                        audioCapturer = audioCapturer,
                        onTtsMissing = { lang ->
                            Toast.makeText(this@MainActivity, "Please install $lang TTS voice data.", Toast.LENGTH_LONG).show()
                            val installIntent = Intent()
                            installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                            try {
                                startActivity(installIntent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AsrScreen(
    viewModel: TranslationViewModel,
    audioCapturer: AudioCapturer,
    onTtsMissing: (String) -> Unit
) {
    val context = LocalContext.current

    val srcLang by viewModel.srcLang.collectAsState()
    val transcription by viewModel.transcription.collectAsState()
    val translation by viewModel.translation.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    
    val targetLang = viewModel.targetLang

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
        Text(
            text = "Speaking: ${if (srcLang == "hi") "Hindi" else "Kannada"} → $targetLang",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { viewModel.switchLanguage("hi") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (srcLang == "hi") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            ) { Text("Hindi") }

            Button(
                onClick = { viewModel.switchLanguage("kn") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (srcLang == "kn") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            ) { Text("Kannada") }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = transcription, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (isTranslating) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            Text("Translating...", style = MaterialTheme.typography.bodySmall)
        } else if (translation.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Translation: $translation", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.speakTranslation(onTtsMissing) }) {
                    Text("Speak")
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                if (!hasMicPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    if (isRecording) {
                        viewModel.stopRecordingAndProcess(audioCapturer, onTtsMissing)
                    } else {
                        viewModel.startRecording(audioCapturer)
                    }
                }
            },
            modifier = Modifier.size(120.dp),
            enabled = !isTranslating
        ) {
            Text(if (isRecording) "Stop" else "Record")
        }
    }
}