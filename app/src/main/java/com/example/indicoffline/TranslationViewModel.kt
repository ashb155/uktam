package com.example.indicoffline

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.core.content.edit

data class ConversationMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val originalText: String,
    val translatedText: String,
    val speakerLang: String,
    val isPrimaryUser: Boolean,
    val transcriptionTimeMs: Long = 0L,
    val translationTimeMs: Long = 0L
)

class TranslationViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("indic_offline_prefs", Context.MODE_PRIVATE)

    private val asrEngine = IndicAsrEngine(application.assets)
    private var llamaCtx: Long = 0L
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _isModelDownloaded = MutableStateFlow(ModelDownloader.isModelDownloaded(application))
    val isModelDownloaded: StateFlow<Boolean> = _isModelDownloaded.asStateFlow()

    private val _isDarkMode = MutableStateFlow(
        if (prefs.contains("is_dark_mode")) prefs.getBoolean("is_dark_mode", false) else null
    )
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        prefs.edit { putBoolean("is_dark_mode", isDark) }
    }

    private val _isHapticsEnabled = MutableStateFlow(prefs.getBoolean("is_haptics_enabled", true))
    val isHapticsEnabled: StateFlow<Boolean> = _isHapticsEnabled.asStateFlow()

    fun setHapticsEnabled(enabled: Boolean) {
        _isHapticsEnabled.value = enabled
        prefs.edit { putBoolean("is_haptics_enabled", enabled) }
    }

    private val _showNerdStats = MutableStateFlow(prefs.getBoolean("show_nerd_stats", false))
    val showNerdStats: StateFlow<Boolean> = _showNerdStats.asStateFlow()

    fun setShowNerdStats(enabled: Boolean) {
        _showNerdStats.value = enabled
        prefs.edit { putBoolean("show_nerd_stats", enabled) }
    }

    private val _ttsSpeechSpeed = MutableStateFlow(prefs.getFloat("tts_speech_speed", 1.0f))
    val ttsSpeechSpeed: StateFlow<Float> = _ttsSpeechSpeed.asStateFlow()

    fun setTtsSpeechSpeed(speed: Float) {
        _ttsSpeechSpeed.value = speed
        prefs.edit { putFloat("tts_speech_speed", speed) }
        tts?.setSpeechRate(speed)
    }

    private val _srcLang = MutableStateFlow("hi")
    val srcLang: StateFlow<String> = _srcLang.asStateFlow()
    
    private val _conversationHistory = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val conversationHistory: StateFlow<List<ConversationMessage>> = _conversationHistory.asStateFlow()

    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _streamingTranslation = MutableStateFlow("")
    val streamingTranslation: StateFlow<String> = _streamingTranslation.asStateFlow()

    private val _primaryLang = MutableStateFlow("hi")
    val primaryLang: StateFlow<String> = _primaryLang.asStateFlow()

    private val _secondaryLang = MutableStateFlow("kn")
    val secondaryLang: StateFlow<String> = _secondaryLang.asStateFlow()

    fun setPrimaryLang(lang: String) {
        val wasPrimary = _srcLang.value == _primaryLang.value
        _primaryLang.value = lang
        if (wasPrimary) {
            switchLanguage(lang)
        }
    }

    fun setSecondaryLang(lang: String) {
        val wasSecondary = _srcLang.value == _secondaryLang.value
        _secondaryLang.value = lang
        if (wasSecondary) {
            switchLanguage(lang)
        }
    }

    val targetLang: String
        get() = if (_srcLang.value == _primaryLang.value) _secondaryLang.value else _primaryLang.value
        
    fun getLanguageName(code: String): String {
        return when (code) {
            "hi" -> "हिन्दी"
            "kn" -> "ಕನ್ನಡ"
            "ta" -> "தமிழ்"
            "te" -> "తెలుగు"
            else -> "Unknown"
        }
    }

    fun getLanguageNameEnglish(code: String): String {
        return when (code) {
            "hi" -> "Hindi"
            "kn" -> "Kannada"
            "ta" -> "Tamil"
            "te" -> "Telugu"
            else -> "Unknown"
        }
    }

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            asrEngine.loadLanguage("hi")
            
            val appCtx = getApplication<Application>()
            
            if (!_isModelDownloaded.value) {
                android.util.Log.d("LlamaTest", "Starting WorkManager download...")
                ModelDownloader.enqueueDownload(appCtx)
                
                launch {
                    androidx.work.WorkManager.getInstance(appCtx)
                        .getWorkInfosByTagFlow("model_download")
                        .collect { workInfoList ->
                            val workInfo = workInfoList.firstOrNull { !it.state.isFinished || it.state == androidx.work.WorkInfo.State.SUCCEEDED }
                            if (workInfo != null) {
                                val progress = workInfo.progress.getInt("PROGRESS", _downloadProgress.value)
                                if (progress > _downloadProgress.value) {
                                    _downloadProgress.value = progress
                                }
                                
                                if (workInfo.state == androidx.work.WorkInfo.State.SUCCEEDED && !_isModelReady.value) {
                                    _isModelDownloaded.value = true
                                    _downloadProgress.value = 100
                                    val modelPath = ModelDownloader.getModelFile(appCtx).absolutePath
                                    if (llamaCtx == 0L) {
                                        llamaCtx = LlamaWrapper.loadModel(modelPath)
                                        if (llamaCtx != 0L) _isModelReady.value = true
                                    }
                                }
                            }
                        }
                }
            } else {
                _downloadProgress.value = 100
                val modelPath = ModelDownloader.getModelFile(appCtx).absolutePath
                if (llamaCtx == 0L) {
                    llamaCtx = LlamaWrapper.loadModel(modelPath)
                    if (llamaCtx != 0L) {
                        _isModelReady.value = true
                    }
                }
            }
        }
    }

    fun switchLanguage(lang: String) {
        _srcLang.value = lang
        _transcription.value = ""
        _streamingTranslation.value = ""
        viewModelScope.launch(Dispatchers.IO) {
            asrEngine.loadLanguage(lang)
        }
    }

    fun startRecording(audioCapturer: AudioCapturer) {
        audioCapturer.startRecording()
        _isRecording.value = true
        _transcription.value = "Listening..."
        _streamingTranslation.value = ""
    }

    fun stopRecordingAndProcess(audioCapturer: AudioCapturer, onTtsMissing: (String) -> Unit) {
        _isRecording.value = false
        _isTranslating.value = true
        _transcription.value = "Processing..."
        _streamingTranslation.value = ""
        
        viewModelScope.launch(Dispatchers.IO) {
            val audioData = audioCapturer.stopAndGetFloatArray()
            val asrStart = System.currentTimeMillis()
            val resultText = asrEngine.transcribe(audioData)
            val asrTime = System.currentTimeMillis() - asrStart
            android.util.Log.d("LlamaTest", "Transcription: '$resultText'")
            
            withContext(Dispatchers.Main) {
                _transcription.value = resultText.ifEmpty { "No speech detected." }
            }
            
            if (resultText.isNotEmpty()) {
                val transStart = System.currentTimeMillis()
                val targetLangCode = targetLang
                val translated = translate(resultText, _srcLang.value, targetLangCode)
                val transTime = System.currentTimeMillis() - transStart
                android.util.Log.d("LlamaTest", "Translation: '$translated'")
                
                withContext(Dispatchers.Main) {
                    _isTranslating.value = false
                    _transcription.value = ""
                    
                    val newMessage = ConversationMessage(
                        originalText = resultText,
                        translatedText = translated,
                        speakerLang = _srcLang.value,
                        isPrimaryUser = _srcLang.value == _primaryLang.value,
                        transcriptionTimeMs = asrTime,
                        translationTimeMs = transTime
                    )
                    _conversationHistory.value += newMessage
                    
                    speak(translated, targetLangCode, onTtsMissing)
                }
            } else {
                kotlinx.coroutines.delay(1500)
                withContext(Dispatchers.Main) {
                    _isTranslating.value = false
                    _transcription.value = ""
                }
            }
        }
    }

    fun speakTranslation(text: String, targetLangCode: String, onTtsMissing: (String) -> Unit) {
        speak(text, targetLangCode, onTtsMissing)
    }

    private fun speak(text: String, targetLangCode: String, onTtsMissing: (String) -> Unit) {
        if (!isTtsReady || tts == null) return
        val locale = when (targetLangCode) {
            "hi" -> Locale.Builder().setLanguage("hi").setRegion("IN").build()
            "kn" -> Locale.Builder().setLanguage("kn").setRegion("IN").build()
            "ta" -> Locale.Builder().setLanguage("ta").setRegion("IN").build()
            "te" -> Locale.Builder().setLanguage("te").setRegion("IN").build()
            else -> Locale.Builder().setLanguage("hi").setRegion("IN").build()
        }
        val result = tts?.setLanguage(locale)
        android.util.Log.d("TtsDebug", "setLanguage result for ${locale.language}: $result")

        val voice = tts?.voices?.find { it.locale.language == locale.language }
        val isNotInstalled = voice?.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED || isNotInstalled) {
            android.util.Log.d("TtsDebug", "TTS missing data or not supported")
            viewModelScope.launch(Dispatchers.Main) {
                val nativeName = getLanguageName(targetLangCode)
                val englishName = getLanguageNameEnglish(targetLangCode)
                onTtsMissing("$englishName ($nativeName)")
            }
            return
        }

        tts?.setSpeechRate(_ttsSpeechSpeed.value)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private suspend fun translate(text: String, srcLang: String, targetLang: String): String {
        if (llamaCtx == 0L) return "Translation model not loaded"
        return withContext(Dispatchers.IO) {
            val toEnglishPrompt = "<bos><start_of_turn>user\nTranslate the text below to English.\n\n$text<end_of_turn>\n<start_of_turn>model\n"
            val englishBridge = LlamaWrapper.completion(llamaCtx, toEnglishPrompt).trim()
            android.util.Log.d("LlamaTest", "English bridge: '$englishBridge'")

            val targetLangName = getLanguageName(targetLang)
            val toTargetPrompt = "<bos><start_of_turn>user\nTranslate the text below to $targetLangName.\n\n$englishBridge<end_of_turn>\n<start_of_turn>model\n"
            
            _streamingTranslation.value = ""
            val tokenChannel = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)
            
            val typeWriterJob = launch {
                var displayed = ""
                for (token in tokenChannel) {
                    for (char in token) {
                        displayed += char
                        _streamingTranslation.value = displayed.trimStart()
                        kotlinx.coroutines.delay(30) 
                    }
                }
            }

            val sb = java.lang.StringBuilder()
            LlamaWrapper.generateStream(llamaCtx, toTargetPrompt).collect { token ->
                if (!token.startsWith("ERROR")) {
                    sb.append(token)
                    tokenChannel.trySend(token)
                }
            }
            
            tokenChannel.close()
            typeWriterJob.join()
            
            val finalTranslation = sb.toString().trim()
            _streamingTranslation.value = finalTranslation
            
            android.util.Log.d("LlamaTest", "Translation ($srcLang -> English -> $targetLang): '$finalTranslation'")
            finalTranslation
        }
    }

    override fun onCleared() {
        super.onCleared()
        asrEngine.destroy()
        if (llamaCtx != 0L) LlamaWrapper.freeModel(llamaCtx)
        tts?.stop()
        tts?.shutdown()
    }
}
