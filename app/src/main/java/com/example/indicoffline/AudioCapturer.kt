package com.example.indicoffline

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

class AudioCapturer {
    private val sampleRate = 16000
    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false
    private val shortBuffer = Collections.synchronizedList(mutableListOf<Short>())
    private var recordingJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording) return
        shortBuffer.clear()

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord = record

        record.startRecording()
        isRecording = true

        recordingJob = coroutineScope.launch {
            val readBuffer = ShortArray(1024)
            while (isActive && isRecording) {
                val readResult = record.read(readBuffer, 0, readBuffer.size)
                if (readResult > 0) {
                    synchronized(shortBuffer) {
                        for (i in 0 until readResult) {
                            shortBuffer.add(readBuffer[i])
                        }
                    }
                } else if (readResult < 0) {
                    break
                }
            }
        }
    }

    suspend fun stopAndGetFloatArray(): FloatArray = withContext(Dispatchers.IO) {
        isRecording = false
        recordingJob?.cancelAndJoin()
        recordingJob = null

        audioRecord?.apply {
            try {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            release()
        }
        audioRecord = null

        val localBuffer = synchronized(shortBuffer) {
            shortBuffer.toShortArray()
        }

        val floatArray = FloatArray(localBuffer.size)
        for (i in localBuffer.indices) {
            floatArray[i] = localBuffer[i] / 32768.0f
        }

        return@withContext floatArray
    }
}