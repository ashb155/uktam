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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.background

class MainActivity : ComponentActivity() {

    private val audioCapturer = AudioCapturer()
    private val viewModel: TranslationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isModelReady by viewModel.isModelReady.collectAsState()
            val isModelDownloaded by viewModel.isModelDownloaded.collectAsState()
            
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!isModelDownloaded) {
                        DownloadScreen(viewModel = viewModel)
                    } else {
                        AsrScreen(
                            viewModel = viewModel,
                            audioCapturer = audioCapturer,
                            isModelReady = isModelReady,
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
}

@Composable
fun AsrScreen(
    viewModel: TranslationViewModel,
    audioCapturer: AudioCapturer,
    isModelReady: Boolean,
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
            enabled = isModelReady && !isTranslating,
            modifier = Modifier.size(120.dp)
        ) {
            Text(if (isRecording) "Stop" else "Record")
        }
        
        if (!isModelReady) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Initializing AI Engine...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun DownloadScreen(viewModel: TranslationViewModel) {
    val progress by viewModel.downloadProgress.collectAsState()
    val isModelReady by viewModel.isModelReady.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.size(160.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                )
                
                Text(
                    text = "$progress%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = if (progress < 100) "Downloading Translation Core" else "Initializing Engine",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "This 2.3 GB offline model is downloaded only once.\nPlease keep the app open.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}