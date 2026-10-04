package com.sentry.safety.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.sentry.safety.data.models.SoundClassification
import com.sentry.safety.data.models.SoundEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

class AudioSafetyManagerImpl(
    private val context: Context
) : AudioSafetyManager {

    private val tag = "AudioSafetyManager"

    private val _isAudioActive = MutableStateFlow(false)
    override val isAudioActive: StateFlow<Boolean> = _isAudioActive.asStateFlow()

    private var _isAudioAvailable = true
    override val isAudioAvailable: Boolean
        get() = _isAudioAvailable

    private val _soundEvents = MutableStateFlow(SoundEvent())
    override val soundEvents: StateFlow<SoundEvent> = _soundEvents.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    override fun startAudioMonitoring() {
        if (_isAudioActive.value) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize.coerceAtLeast(sampleRate * 2)
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(tag, "AudioRecord could not initialize (permission or hardware). Using adaptive audio layer.")
                _isAudioAvailable = false
                _isAudioActive.value = true
                return
            }

            audioRecord?.startRecording()
            _isAudioActive.value = true
            _isAudioAvailable = true

            recordingJob = scope.launch {
                val buffer = ShortArray(1024)
                while (isActive && _isAudioActive.value) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        analyzePcmBuffer(buffer, read)
                    }
                }
            }
            Log.i(tag, "Local privacy-safe audio monitoring started.")
        } catch (e: Exception) {
            Log.e(tag, "Error starting audio monitoring: ${e.message}")
            _isAudioAvailable = false
            _isAudioActive.value = false
        }
    }

    private fun analyzePcmBuffer(buffer: ShortArray, size: Int) {
        var sumSquares = 0.0
        var zeroCrossings = 0

        for (i in 0 until size) {
            val sample = buffer[i]
            sumSquares += (sample * sample)
            if (i > 0 && ((sample >= 0 && buffer[i - 1] < 0) || (sample < 0 && buffer[i - 1] >= 0))) {
                zeroCrossings++
            }
        }

        val rms = sqrt(sumSquares / size)
        val decibels = (20 * log10(rms.coerceAtLeast(1.0))).toFloat() + 20f

        val zcr = zeroCrossings.toFloat() / size
        // Approximate frequency in Hz = (ZCR * sampleRate) / 2
        val approxFrequency = (zcr * sampleRate) / 2.0f

        val isSuddenSpike = decibels > 78f

        // Classify acoustic event signatures locally without retaining raw audio
        val soundType: SoundClassification
        val confidence: Float

        when {
            // Vehicle Horn signature: High SPL (>72dB), approx fundamental 380 - 580 Hz, sustained tone
            decibels > 72f && approxFrequency in 350f..600f -> {
                soundType = SoundClassification.HORN
                confidence = 0.89f
            }
            // Alarm signature: Continuous high-frequency oscillating tone (2 kHz - 4 kHz)
            decibels > 68f && approxFrequency in 1800f..4200f -> {
                soundType = SoundClassification.ALARM
                confidence = 0.84f
            }
            // Impact: Sudden extreme spike (>84dB) across broad spectrum
            decibels > 84f -> {
                soundType = SoundClassification.IMPACT
                confidence = 0.81f
            }
            // Glass breaking: High ZCR (>0.40) and high frequency noise burst
            decibels > 65f && approxFrequency > 4500f -> {
                soundType = SoundClassification.GLASS_BREAK
                confidence = 0.77f
            }
            // Shout/Distress: High SPL with human speech formant (600 - 1500 Hz)
            decibels > 75f && approxFrequency in 650f..1500f -> {
                soundType = SoundClassification.SHOUT
                confidence = 0.80f
            }
            else -> {
                soundType = SoundClassification.AMBIENT
                confidence = 0.50f
            }
        }

        _soundEvents.value = SoundEvent(
            soundType = soundType,
            confidence = confidence,
            decibels = decibels,
            isSuddenSpike = isSuddenSpike,
            timestamp = System.currentTimeMillis()
        )
    }

    override fun simulateSoundEvent(event: SoundEvent) {
        _soundEvents.value = event
    }

    override fun stopAudioMonitoring() {
        try {
            _isAudioActive.value = false
            recordingJob?.cancel()
            recordingJob = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.i(tag, "Audio monitoring stopped.")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping audio: ${e.message}")
        }
    }
}
