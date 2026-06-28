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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
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

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.statusBarColor = android.graphics.Color.TRANSPARENT

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

@Composable
fun AsrScreen(
    viewModel: TranslationViewModel,
    audioCapturer: AudioCapturer,
    isModelReady: Boolean,
    onNavigateToSettings: () -> Unit,
    onTtsMissing: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isHapticsEnabled by viewModel.isHapticsEnabled.collectAsState()
    val srcLang by viewModel.srcLang.collectAsState()
    val transcription by viewModel.transcription.collectAsState()
    val conversationHistory by viewModel.conversationHistory.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val showNerdStats by viewModel.showNerdStats.collectAsState()
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(conversationHistory.size, isRecording, isTranslating) {
        val targetIndex = if (isRecording || isTranslating) conversationHistory.size else conversationHistory.size - 1
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
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
                    Text(
                        text = "IndicOffline",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Row {
                        val isDarkModePref by viewModel.isDarkMode.collectAsState()
                        val systemDarkTheme = isSystemInDarkTheme()
                        val isDarkMode = isDarkModePref ?: systemDarkTheme
                        
                        IconButton(onClick = { viewModel.setDarkMode(!isDarkMode) }) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Toggle Dark Mode",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
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
                        color = if (srcLang == "kn") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Kannada",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (srcLang == "kn") FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            color = if (srcLang == "kn") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
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
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isHindi) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.translatedText,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isHindi) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            
                            if (showNerdStats) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ASR: ${message.transcriptionTimeMs}ms | Translation ${message.translationTimeMs / 1000.0}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = (if (isHindi) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer).copy(alpha = 0.5f)
                                )
                            }

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

            item {
                AnimatedVisibility(
                    visible = isRecording || isTranslating,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
                ) {
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
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isHindi) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                
                                AnimatedVisibility(
                                    visible = isTranslating && transcription.isNotEmpty() && transcription != "Processing..." && transcription != "No speech detected.",
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val isButtonEnabled = isModelReady && !isTranslating
                
                FloatingActionButton(
                    onClick = {
                        if (!isButtonEnabled) return@FloatingActionButton
                        
                        if (isHapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        
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
                    modifier = Modifier
                        .size(80.dp)
                        .alpha(if (isButtonEnabled) 1f else 0.5f),
                    containerColor = when {
                        !isButtonEnabled -> MaterialTheme.colorScheme.surfaceVariant
                        isRecording -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                    contentColor = when {
                        !isButtonEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun SettingsScreen(
    viewModel: TranslationViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isHapticsEnabled by viewModel.isHapticsEnabled.collectAsState()
    val showNerdStats by viewModel.showNerdStats.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Text-to-Speech (TTS)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
            )
            
            Surface(
                onClick = {
                    val intent = Intent()
                    intent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                    intent.setPackage("com.google.android.tts")
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open TTS settings", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "TTS",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Install TTS Voice Data",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Download voices for offline playback",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
            )
            
            Surface(
                onClick = { viewModel.setHapticsEnabled(!isHapticsEnabled) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.TouchApp,
                        contentDescription = "Haptics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptic Feedback",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Vibrate on mic press",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isHapticsEnabled,
                        onCheckedChange = { viewModel.setHapticsEnabled(it) }
                    )
                }
            }
            
            Surface(
                onClick = { viewModel.setShowNerdStats(!showNerdStats) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Nerd Stats",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Performance Stats",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Show AI inference latency",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = showNerdStats,
                        onCheckedChange = { viewModel.setShowNerdStats(it) }
                    )
                }
            }
        }
    }
}