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

class AudioCapturer {
    private val sampleRate = 16000
    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false
    private var shortBuffer = ShortArray(16000 * 10)
    private var bufferSize = 0
    private val bufferLock = Any()
    private var recordingJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording) return
        synchronized(bufferLock) {
            bufferSize = 0
            if (shortBuffer.size < 16000 * 10) {
                shortBuffer = ShortArray(16000 * 10)
            }
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )
        audioRecord = record

        record.startRecording()
        isRecording = true

        recordingJob = coroutineScope.launch {
            val readBuffer = ShortArray(1024)
            while (isActive && isRecording) {
                val readResult = record.read(readBuffer, 0, readBuffer.size)
                if (readResult > 0) {
                    synchronized(bufferLock) {
                        if (bufferSize + readResult > shortBuffer.size) {
                            val newSize = (shortBuffer.size * 2).coerceAtLeast(bufferSize + readResult)
                            val newBuffer = ShortArray(newSize)
                            System.arraycopy(shortBuffer, 0, newBuffer, 0, bufferSize)
                            shortBuffer = newBuffer
                        }
                        System.arraycopy(readBuffer, 0, shortBuffer, bufferSize, readResult)
                        bufferSize += readResult
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

        val floatArray = synchronized(bufferLock) {
            val arr = FloatArray(bufferSize)
            for (i in 0..bufferSize) {
                arr[i] = shortBuffer[i] / 32768.0f
            }
            arr
        }

        return@withContext floatArray
    }
}