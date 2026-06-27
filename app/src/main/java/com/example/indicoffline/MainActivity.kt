package com.example.indicoffline

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val audioCapturer = AudioCapturer()
    private val viewModel: TranslationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
    val conversationHistory by viewModel.conversationHistory.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(conversationHistory.size) {
        if (conversationHistory.isNotEmpty()) {
            listState.animateScrollToItem(conversationHistory.size - 1)
        }
    }

    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasMicPermission = isGranted }
    )

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (srcLang == "hi") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Hindi",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (srcLang == "hi") FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            color = if (srcLang == "hi") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.switchLanguage(if (srcLang == "hi") "kn" else "hi") },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = "Swap Languages",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = if (srcLang == "kn") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Kannada",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (srcLang == "kn") FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            color = if (srcLang == "kn") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(conversationHistory) { message ->
                val isHindi = message.speakerLang == "hi"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isHindi) Arrangement.Start else Arrangement.End
                ) {
                    Surface(
                        color = if (isHindi) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = message.originalText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isHindi) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.translatedText,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isHindi) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                val targetLanguage = if (isHindi) "Kannada" else "Hindi"
                                IconButton(
                                    onClick = { viewModel.speakTranslation(message.translatedText, targetLanguage, onTtsMissing) },
                                    modifier = Modifier.size(32.dp).background(if (isHindi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(50))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VolumeUp,
                                        contentDescription = "Speak",
                                        tint = if (isHindi) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isRecording || isTranslating) {
                item {
                    val isHindi = srcLang == "hi"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isHindi) Arrangement.Start else Arrangement.End
                    ) {
                        Surface(
                            color = if (isHindi) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(0.85f).animateContentSize(animationSpec = tween(300, easing = LinearOutSlowInEasing))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = transcription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isHindi) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                
                                AnimatedVisibility(
                                    visible = isTranslating && transcription.isNotEmpty(),
                                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                                ) {
                                    Column {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = if (isHindi) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Translating...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isHindi) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, top = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = {
                        if (!isModelReady || isTranslating) return@FloatingActionButton
                        
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
                    modifier = Modifier.size(80.dp),
                    containerColor = when {
                        !isModelReady || isTranslating -> MaterialTheme.colorScheme.surfaceVariant
                        isRecording -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                    contentColor = when {
                        !isModelReady || isTranslating -> MaterialTheme.colorScheme.onSurfaceVariant
                        isRecording -> MaterialTheme.colorScheme.onError
                        else -> MaterialTheme.colorScheme.onPrimary
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Stop" else "Record",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Initializing AI Engine...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.alpha(if (isModelReady) 0f else 1f)
                )
            }
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
                text = if (progress < 100) "Setting up your offline experience" else "Finalizing...",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "This is a one-time process, Wi-fi recommended.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}