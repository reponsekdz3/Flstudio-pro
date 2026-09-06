package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _liveAmplitude = MutableStateFlow(0f)
    val liveAmplitude = _liveAmplitude.asStateFlow()

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val recordedBytes = ByteArrayOutputStream()

    @SuppressLint("MissingPermission")
    fun startRecording(coroutineScope: CoroutineScope, onStarted: (Boolean) -> Unit) {
        if (_isRecording.value) return

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize <= 0) {
            onStarted(false)
            return
        }

        try {
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                onStarted(false)
                return
            }

            recordedBytes.reset()
            audioRecord?.startRecording()
            _isRecording.value = true
            onStarted(true)

            recordingJob = coroutineScope.launch(Dispatchers.IO) {
                val buffer = ShortArray(1024)
                while (isActive && _isRecording.value) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        var maxAmp = 0
                        for (i in 0 until read) {
                            val sample = buffer[i]
                            val sAbs = abs(sample.toInt())
                            if (sAbs > maxAmp) maxAmp = sAbs

                            // Write as 16-bit little endian
                            recordedBytes.write(sample.toInt() and 0xFF)
                            recordedBytes.write((sample.toInt() shr 8) and 0xFF)
                        }
                        _liveAmplitude.value = (maxAmp / 32768.0f).coerceIn(0f, 1f)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isRecording.value = false
            onStarted(false)
        }
    }

    fun stopRecording(): FloatArray {
        _isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
        _liveAmplitude.value = 0f

        // Convert recorded bytes to normalized FloatArray
        val bytes = recordedBytes.toByteArray()
        val numSamples = bytes.size / 2
        val floatSamples = FloatArray(numSamples)
        for (i in 0 until numSamples) {
            val b1 = bytes[i * 2].toInt() and 0xFF
            val b2 = bytes[i * 2 + 1].toInt()
            val sampleShort = (b2 shl 8) or b1
            floatSamples[i] = (sampleShort / 32768.0f).coerceIn(-1.0f, 1.0f)
        }
        return floatSamples
    }
}
