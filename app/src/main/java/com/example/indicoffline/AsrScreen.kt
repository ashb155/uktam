package com.example.indicoffline

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun AsrScreen(
    viewModel: TranslationViewModel,
    audioCapturer: AudioCapturer,
    isModelReady: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onTtsMissing: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isHapticsEnabled by viewModel.isHapticsEnabled.collectAsState()
    val srcLang by viewModel.srcLang.collectAsState()
    val primaryLang by viewModel.primaryLang.collectAsState()
    val secondaryLang by viewModel.secondaryLang.collectAsState()
    val transcription by viewModel.transcription.collectAsState()
    val conversationHistory by viewModel.conversationHistory.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val showNerdStats by viewModel.showNerdStats.collectAsState()
    
    val listState = rememberLazyListState()

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = androidx.compose.ui.graphics.Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_uktam_logo),
                                contentDescription = "App Logo",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Uktam.ai",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
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
                        IconButton(onClick = onNavigateToAbout) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "About",
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
                        shape = CircleShape,
                        color = if (srcLang == primaryLang) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                            .clip(CircleShape)
                            .clickable { Toast.makeText(context, "More languages coming soon!", Toast.LENGTH_SHORT).show() }
                    ) {
                        Text(
                            text = viewModel.getLanguageName(primaryLang),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (srcLang == primaryLang) FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            color = if (srcLang == primaryLang) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.switchLanguage(if (srcLang == primaryLang) secondaryLang else primaryLang) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        enabled = !isRecording && !isTranslating
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = "Swap Languages",
                            tint = if (!isRecording && !isTranslating) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }
                    
                    Surface(
                        shape = CircleShape,
                        color = if (srcLang == secondaryLang) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                            .clip(CircleShape)
                            .clickable { Toast.makeText(context, "More languages coming soon!", Toast.LENGTH_SHORT).show() }
                    ) {
                        Text(
                            text = viewModel.getLanguageName(secondaryLang),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (srcLang == secondaryLang) FontWeight.Bold else FontWeight.Medium
                            ),
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            color = if (srcLang == secondaryLang) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                val isPrimary = message.speakerLang == primaryLang
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isPrimary) Arrangement.Start else Arrangement.End
                ) {
                    Surface(
                        color = if (isPrimary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = message.originalText,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = message.translatedText,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Spacer(modifier = Modifier.width(6.dp))
                                val targetLangCode = if (isPrimary) secondaryLang else primaryLang
                                IconButton(
                                    onClick = { viewModel.speakTranslation(message.translatedText, targetLangCode, onTtsMissing) },
                                    modifier = Modifier.size(28.dp).background(if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(50))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak",
                                        tint = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            
                            if (showNerdStats) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ASR: ${message.transcriptionTimeMs}ms | Translation ${message.translationTimeMs / 1000.0}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = (if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer).copy(alpha = 0.5f)
                                )
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
                    val isPrimary = srcLang == primaryLang
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isPrimary) Arrangement.Start else Arrangement.End
                    ) {
                        Surface(
                            color = if (isPrimary) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(0.85f).animateContentSize(animationSpec = tween(300, easing = LinearOutSlowInEasing))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = transcription,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                
                                AnimatedVisibility(
                                    visible = isTranslating && transcription.isNotEmpty() && transcription != "Processing..." && transcription != "No speech detected.",
                                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                                ) {
                                    Column {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Translating...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
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
                    modifier = Modifier.size(80.dp),
                    containerColor = when {
                        !isButtonEnabled -> androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.2f)
                        isRecording -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                    contentColor = when {
                        !isButtonEnabled -> androidx.compose.ui.graphics.Color.Gray
                        isRecording -> MaterialTheme.colorScheme.onError
                        else -> MaterialTheme.colorScheme.onPrimary
                    },
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = if (isButtonEnabled) 6.dp else 0.dp,
                        pressedElevation = if (isButtonEnabled) 12.dp else 0.dp
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Stop" else "Record",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
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

