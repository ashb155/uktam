package com.example.indicoffline

import android.app.Application
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

data class ConversationMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val originalText: String,
    val translatedText: String,
    val speakerLang: String,
    val transcriptionTimeMs: Long = 0L,
    val translationTimeMs: Long = 0L
)

class TranslationViewModel(application: Application) : AndroidViewModel(application) {

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

    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
    }

    private val _isHapticsEnabled = MutableStateFlow(true)
    val isHapticsEnabled: StateFlow<Boolean> = _isHapticsEnabled.asStateFlow()

    fun setHapticsEnabled(enabled: Boolean) {
        _isHapticsEnabled.value = enabled
    }

    private val _showNerdStats = MutableStateFlow(false)
    val showNerdStats: StateFlow<Boolean> = _showNerdStats.asStateFlow()

    fun setShowNerdStats(enabled: Boolean) {
        _showNerdStats.value = enabled
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

    val targetLang: String
        get() = if (_srcLang.value == "hi") "Kannada" else "Hindi"

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            asrEngine.loadLanguage("hi")
            
            val appCtx = getApplication<Application>()
            val downloadedInitially = _isModelDownloaded.value
            
            if (!downloadedInitially) {
                android.util.Log.d("LlamaTest", "Downloading model...")
                ModelDownloader.downloadModel(appCtx) { progress ->
                    android.util.Log.d("LlamaTest", "Download progress: $progress%")
                    _downloadProgress.value = progress
                }
                _isModelDownloaded.value = true
            } else {
                _downloadProgress.value = 100
            }
            val modelPath = ModelDownloader.getModelFile(appCtx).absolutePath
            llamaCtx = LlamaWrapper.loadModel(modelPath)
            android.util.Log.d("LlamaTest", if (llamaCtx != 0L) "Model loaded" else "Model load FAILED")
            if (llamaCtx != 0L) {
                _isModelReady.value = true
            }
        }
    }

    fun switchLanguage(lang: String) {
        _srcLang.value = lang
        _transcription.value = ""
        viewModelScope.launch(Dispatchers.IO) {
            asrEngine.loadLanguage(lang)
        }
    }

    fun startRecording(audioCapturer: AudioCapturer) {
        audioCapturer.startRecording()
        _isRecording.value = true
        _transcription.value = "Listening..."
    }

    fun stopRecordingAndProcess(audioCapturer: AudioCapturer, onTtsMissing: (String) -> Unit) {
        _isRecording.value = false
        _isTranslating.value = true
        _transcription.value = "Processing..."
        
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
                val translated = translate(resultText, _srcLang.value, targetLang)
                val transTime = System.currentTimeMillis() - transStart
                android.util.Log.d("LlamaTest", "Translation: '$translated'")
                
                withContext(Dispatchers.Main) {
                    _isTranslating.value = false
                    _transcription.value = ""
                    
                    val newMessage = ConversationMessage(
                        originalText = resultText,
                        translatedText = translated,
                        speakerLang = _srcLang.value,
                        transcriptionTimeMs = asrTime,
                        translationTimeMs = transTime
                    )
                    _conversationHistory.value += newMessage
                    
                    speak(translated, targetLang, onTtsMissing)
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

    fun speakTranslation(text: String, targetLanguage: String, onTtsMissing: (String) -> Unit) {
        speak(text, targetLanguage, onTtsMissing)
    }

    private fun speak(text: String, targetLang: String, onTtsMissing: (String) -> Unit) {
        if (!isTtsReady || tts == null) return
        val locale = if (targetLang == "Hindi") Locale("hi", "IN") else Locale("kn", "IN")
        val result = tts?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            onTtsMissing(targetLang)
            return
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private suspend fun translate(text: String, srcLang: String, targetLang: String): String {
        if (llamaCtx == 0L) return "Translation model not loaded"
        return withContext(Dispatchers.IO) {
            val toEnglishPrompt = "<bos><start_of_turn>user\nTranslate the text below to English.\n\n$text<end_of_turn>\n<start_of_turn>model\n"
            val englishBridge = LlamaWrapper.completion(llamaCtx, toEnglishPrompt).trim()
            android.util.Log.d("LlamaTest", "English bridge: '$englishBridge'")

            val toTargetPrompt = "<bos><start_of_turn>user\nTranslate the text below to $targetLang.\n\n$englishBridge<end_of_turn>\n<start_of_turn>model\n"
            val finalTranslation = LlamaWrapper.completion(llamaCtx, toTargetPrompt).trim()
            
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
