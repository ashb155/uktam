package com.example.indicoffline

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
    val isHapticsEnabled by viewModel.isHapticsEnabled.collectAsStateWithLifecycle()
    val srcLang by viewModel.srcLang.collectAsStateWithLifecycle()
    val primaryLang by viewModel.primaryLang.collectAsStateWithLifecycle()
    val secondaryLang by viewModel.secondaryLang.collectAsStateWithLifecycle()
    val transcription by viewModel.transcription.collectAsStateWithLifecycle()
    val streamingTranslation by viewModel.streamingTranslation.collectAsStateWithLifecycle()
    val conversationHistory by viewModel.conversationHistory.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val isTranslating by viewModel.isTranslating.collectAsStateWithLifecycle()
    val showNerdStats by viewModel.showNerdStats.collectAsStateWithLifecycle()
    
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

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()) {

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
                        val isDarkModePref by viewModel.isDarkMode.collectAsStateWithLifecycle()
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
                val availableLanguages = listOf("hi", "kn", "ta", "te")
                var primaryExpanded by remember { mutableStateOf(false) }
                var secondaryExpanded by remember { mutableStateOf(false) }
                var primaryBoxWidth by remember { mutableStateOf(0) }
                var secondaryBoxWidth by remember { mutableStateOf(0) }
                val density = LocalDensity.current

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f).onGloballyPositioned { primaryBoxWidth = it.size.width }) {
                        val isPrimaryActive = srcLang == primaryLang
                        val primaryBg = if (isPrimaryActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        val primaryText = if (isPrimaryActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        
                        Surface(
                            onClick = { primaryExpanded = true },
                            enabled = !isRecording && !isTranslating,
                            shape = RoundedCornerShape(24.dp),
                            color = primaryBg,
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = viewModel.getLanguageName(primaryLang),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isPrimaryActive) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = if (isPrimaryActive) primaryText else primaryText.copy(alpha = 0.6f),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = if (isPrimaryActive) primaryText else primaryText.copy(alpha = 0.6f),
                                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                                )
                            }
                        }
                        
                        if (primaryExpanded) {
                            Popup(
                                onDismissRequest = { primaryExpanded = false },
                                properties = PopupProperties(focusable = true)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = primaryBg,
                                    modifier = Modifier.width(with(density) { primaryBoxWidth.toDp() }).animateContentSize(),
                                    shadowElevation = 0.dp,
                                    tonalElevation = 0.dp
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                                .clickable { primaryExpanded = false }
                                                .padding(vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = viewModel.getLanguageName(primaryLang),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = if (isPrimaryActive) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                textAlign = TextAlign.Center,
                                                color = if (isPrimaryActive) primaryText else primaryText.copy(alpha = 0.6f),
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.ArrowDropUp,
                                                contentDescription = "Dropdown",
                                                tint = if (isPrimaryActive) primaryText else primaryText.copy(alpha = 0.6f),
                                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            availableLanguages.forEach { lang ->
                                                val isSelected = lang == primaryLang
                                                val isDisabled = lang == secondaryLang
                                                Row(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .clickable(enabled = !isDisabled) { 
                                                            viewModel.setPrimaryLang(lang)
                                                            primaryExpanded = false
                                                        }
                                                        .padding(vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = viewModel.getLanguageName(lang),
                                                        color = if (isSelected) primaryText else if (isDisabled) primaryText.copy(alpha = 0.2f) else primaryText.copy(alpha = 0.6f),
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    ) 
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
                    
                    Box(modifier = Modifier.weight(1f).onGloballyPositioned { secondaryBoxWidth = it.size.width }) {
                        val isSecondaryActive = srcLang == secondaryLang
                        val secondaryBg = if (isSecondaryActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        val secondaryText = if (isSecondaryActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface

                        Surface(
                            onClick = { secondaryExpanded = true },
                            enabled = !isRecording && !isTranslating,
                            shape = RoundedCornerShape(24.dp),
                            color = secondaryBg,
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 0.dp,
                            tonalElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = viewModel.getLanguageName(secondaryLang),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isSecondaryActive) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = if (isSecondaryActive) secondaryText else secondaryText.copy(alpha = 0.6f),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = if (isSecondaryActive) secondaryText else secondaryText.copy(alpha = 0.6f),
                                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                                )
                            }
                        }
                        
                        if (secondaryExpanded) {
                            Popup(
                                onDismissRequest = { secondaryExpanded = false },
                                properties = androidx.compose.ui.window.PopupProperties(focusable = true)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = secondaryBg,
                                    modifier = Modifier.width(with(density) { secondaryBoxWidth.toDp() }).animateContentSize(),
                                    shadowElevation = 0.dp,
                                    tonalElevation = 0.dp
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier.fillMaxWidth()
                                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                                .clickable { secondaryExpanded = false }
                                                .padding(vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = viewModel.getLanguageName(secondaryLang),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = if (isSecondaryActive) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                textAlign = TextAlign.Center,
                                                color = if (isSecondaryActive) secondaryText else secondaryText.copy(alpha = 0.6f),
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.ArrowDropUp,
                                                contentDescription = "Dropdown",
                                                tint = if (isSecondaryActive) secondaryText else secondaryText.copy(alpha = 0.6f),
                                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            availableLanguages.forEach { lang ->
                                                val isSelected = lang == secondaryLang
                                                val isDisabled = lang == primaryLang
                                                Row(
                                                    modifier = Modifier.fillMaxWidth()
                                                        .clickable(enabled = !isDisabled) { 
                                                            viewModel.setSecondaryLang(lang)
                                                            secondaryExpanded = false
                                                        }
                                                        .padding(vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = viewModel.getLanguageName(lang),
                                                        color = if (isSelected) secondaryText else if (isDisabled) secondaryText.copy(alpha = 0.2f) else secondaryText.copy(alpha = 0.6f),
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(conversationHistory, key = { it.id }) { message ->
                val isPrimary = message.isPrimaryUser
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
                            Spacer(modifier = Modifier.height(8.dp))
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
                                
                                Spacer(modifier = Modifier.width(16.dp))
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
                androidx.compose.animation.AnimatedVisibility(
                    visible = isRecording || isTranslating,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = if (transcription.isEmpty()) {
                        fadeOut(tween(300)) + shrinkVertically(tween(300))
                    } else {
                        fadeOut(tween(0)) + shrinkVertically(tween(0))
                    }
                ) {
                    val isPrimary = srcLang == primaryLang
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isPrimary) Arrangement.Start else Arrangement.End
                    ) {
                        Surface(
                            color = if (isPrimary) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(0.85f)
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
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (streamingTranslation.isEmpty()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Translating...",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = streamingTranslation,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Spacer(modifier = Modifier.size(28.dp)) 
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
            
            androidx.compose.animation.AnimatedVisibility(
                visible = conversationHistory.isEmpty() && !isRecording && !isTranslating,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.Center).padding(bottom = 160.dp, start = 32.dp, end = 32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { 
                    Text(
                        text = "Tap the languages at the top to choose your preferred languages",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Press the mic icon and speak in your preferred language",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Translation and speech in required language will appear automatically",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Press the swap icon to switch turns",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
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

