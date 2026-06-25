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
import android.speech.tts.TextToSpeech
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var asrEngine: IndicAsrEngine
    private val audioCapturer = AudioCapturer()
    private var llamaCtx: Long = 0L
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }

        asrEngine = IndicAsrEngine(assets)

        lifecycleScope.launch(Dispatchers.IO) {
            asrEngine.loadLanguage("hi")
            if (!ModelDownloader.isModelDownloaded(this@MainActivity)) {
                android.util.Log.d("LlamaTest", "Downloading model...")
                ModelDownloader.downloadModel(this@MainActivity) { progress ->
                    android.util.Log.d("LlamaTest", "Download progress: $progress%")
                }
            }
            val modelPath = ModelDownloader.getModelFile(this@MainActivity).absolutePath
            llamaCtx = LlamaWrapper.loadModel(modelPath)
            android.util.Log.d("LlamaTest", if (llamaCtx != 0L) "Model loaded" else "Model load FAILED")
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AsrScreen(asrEngine, audioCapturer, ::translate, ::speak)
                }
            }
        }
    }

    private fun speak(text: String, targetLang: String) {
        if (!isTtsReady || tts == null) return
        val locale = if (targetLang == "Hindi") Locale("hi", "IN") else Locale("kn", "IN")
        val result = tts?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            android.widget.Toast.makeText(this, "Please install $targetLang TTS voice data.", android.widget.Toast.LENGTH_LONG).show()
            val installIntent = android.content.Intent()
            installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
            try {
                startActivity(installIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private suspend fun translate(text: String, srcLang: String, targetLang: String): String {
        if (llamaCtx == 0L) return "Translation model not loaded"
        return withContext(Dispatchers.IO) {
            val srcName = if (srcLang == "hi") "Hindi" else "Kannada"

            val toEnglishPrompt = "<bos><start_of_turn>user\nTranslate the following text from $srcName to English.\n$text<end_of_turn>\n<start_of_turn>model\n"
            val english = LlamaWrapper.completion(llamaCtx, toEnglishPrompt)
            android.util.Log.d("LlamaTest", "English bridge: '$english'")

            val toTargetPrompt = "<bos><start_of_turn>user\nTranslate the following text from English to $targetLang.\n$english<end_of_turn>\n<start_of_turn>model\n"
            LlamaWrapper.completion(llamaCtx, toTargetPrompt)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        asrEngine.destroy()
        if (llamaCtx != 0L) LlamaWrapper.freeModel(llamaCtx)
        tts?.stop()
        tts?.shutdown()
    }
}

@Composable
fun AsrScreen(
    asrEngine: IndicAsrEngine,
    audioCapturer: AudioCapturer,
    translate: suspend (String, String, String) -> String,
    speak: (String, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var srcLang by remember { mutableStateOf("hi") }
    val targetLang = if (srcLang == "hi") "Kannada" else "Hindi"

    var transcription by remember { mutableStateOf("Ready. Press Record to speak.") }
    var translation by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
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
                onClick = {
                    srcLang = "hi"
                    coroutineScope.launch(Dispatchers.IO) { asrEngine.loadLanguage("hi") }
                    transcription = "Switched to Hindi → Kannada"
                    translation = ""
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (srcLang == "hi") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            ) { Text("Hindi") }

            Button(
                onClick = {
                    srcLang = "kn"
                    coroutineScope.launch(Dispatchers.IO) { asrEngine.loadLanguage("kn") }
                    transcription = "Switched to Kannada → Hindi"
                    translation = ""
                },
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
                Button(onClick = { speak(translation, targetLang) }) {
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
                        isRecording = false
                        transcription = "Processing..."
                        coroutineScope.launch(Dispatchers.IO) {
                            val audioData = audioCapturer.stopAndGetFloatArray()
                            val resultText = asrEngine.transcribe(audioData)
                            android.util.Log.d("LlamaTest", "Transcription: '$resultText'")
                            withContext(Dispatchers.Main) {
                                transcription = resultText.ifEmpty { "No speech detected." }
                            }
                            if (resultText.isNotEmpty()) {
                                withContext(Dispatchers.Main) { isTranslating = true }
                                val translated = translate(resultText, srcLang, targetLang)
                                android.util.Log.d("LlamaTest", "Translation: '$translated'")
                                withContext(Dispatchers.Main) {
                                    isTranslating = false
                                    translation = translated
                                    speak(translated, targetLang)
                                }
                            }
                        }
                    } else {
                        audioCapturer.startRecording()
                        isRecording = true
                        transcription = "Listening..."
                        translation = ""
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