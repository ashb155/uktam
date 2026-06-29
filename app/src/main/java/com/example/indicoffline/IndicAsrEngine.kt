package com.example.indicoffline

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig

class IndicAsrEngine(private val assetManager: AssetManager) {
    private var recognizer: OfflineRecognizer? = null
    var currentLanguageCode = ""
        private set

    @Synchronized
    fun loadLanguage(languageCode: String) {
        if (currentLanguageCode == languageCode) return
        recognizer?.release()
        recognizer = null

        val config = OfflineRecognizerConfig(
            modelConfig = OfflineModelConfig(
                nemo = OfflineNemoEncDecCtcModelConfig(
                    model = "$languageCode/model.int8.onnx"
                ),
                tokens = "$languageCode/tokens.txt",
                numThreads = Runtime.getRuntime().availableProcessors().coerceAtMost(4),
                debug = false
            ),
            featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80)
        )

        recognizer = OfflineRecognizer(assetManager, config)
        currentLanguageCode = languageCode
    }

    @Synchronized
    fun transcribe(audioData: FloatArray): String {
        val activeRecognizer = recognizer ?: return "Engine not initialized"
        val stream = activeRecognizer.createStream()
        return try {
            stream.acceptWaveform(audioData, sampleRate = 16000)
            activeRecognizer.decode(stream)
            activeRecognizer.getResult(stream).text
        } finally {
            stream.release()
        }
    }

    @Synchronized
    fun destroy() {
        recognizer?.release()
        recognizer = null
        currentLanguageCode = ""
    }
}