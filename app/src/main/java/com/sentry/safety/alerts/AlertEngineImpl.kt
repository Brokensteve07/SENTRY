package com.sentry.safety.alerts

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import com.sentry.safety.data.models.Direction
import com.sentry.safety.data.models.RiskLevel
import com.sentry.safety.data.models.RiskResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class AlertEngineImpl(
    private val context: Context
) : AlertEngine, TextToSpeech.OnInitListener {

    private val tag = "AlertEngine"

    private val _lastTriggeredAlert = MutableStateFlow<RiskResult?>(null)
    override val lastTriggeredAlert: StateFlow<RiskResult?> = _lastTriggeredAlert.asStateFlow()

    private var vibrator: Vibrator? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false

    // Anti-spam cooldown & hysteresis
    private var lastSpokenTime = 0L
    private var lastSpokenText = ""
    private var lastRiskLevel = RiskLevel.NORMAL
    private val alertCooldownMs = 3800L

    override val isCooldownActive: Boolean
        get() = (System.currentTimeMillis() - lastSpokenTime) < alertCooldownMs

    init {
        initVibrator()
        initTts()
    }

    private fun initVibrator() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.w(tag, "Vibrator initialization fallback: ${e.message}")
        }
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.w(tag, "TTS initialization fallback: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            textToSpeech?.setSpeechRate(1.05f)
            textToSpeech?.setPitch(1.0f)
            Log.i(tag, "TTS initialized successfully.")
        } else {
            Log.w(tag, "TTS init returned status $status")
            ttsReady = false
        }
    }

    override fun processRiskResult(riskResult: RiskResult) {
        val currentTime = System.currentTimeMillis()

        if (riskResult.riskLevel == RiskLevel.NORMAL) {
            lastRiskLevel = RiskLevel.NORMAL
            return
        }

        // Only trigger alerts for CAUTION (subtle), WARNING, or HIGH
        val isEscalation = riskResult.riskLevel.ordinal > lastRiskLevel.ordinal
        val isSameAlert = riskResult.voiceWarning == lastSpokenText
        val cooldownPassed = (currentTime - lastSpokenTime) > alertCooldownMs

        if ((cooldownPassed || isEscalation) && riskResult.voiceWarning.isNotEmpty()) {
            _lastTriggeredAlert.value = riskResult
            lastSpokenTime = currentTime
            lastSpokenText = riskResult.voiceWarning
            lastRiskLevel = riskResult.riskLevel

            // Trigger Voice TTS
            speakWarning(riskResult.voiceWarning, isUrgent = riskResult.riskLevel == RiskLevel.HIGH)

            // Trigger Haptic
            triggerHapticAlert(riskResult.direction, riskResult.riskLevel)
        }
    }

    override fun triggerImmediateAlert(
        title: String,
        spokenWarning: String,
        direction: Direction,
        level: RiskLevel
    ) {
        val simulated = RiskResult(
            riskScore = if (level == RiskLevel.HIGH) 92 else 72,
            riskLevel = level,
            direction = direction,
            reason = title,
            recommendedAction = "WARN_USER",
            visualWarning = "⚠️ $title",
            voiceWarning = spokenWarning,
            timestamp = System.currentTimeMillis()
        )
        _lastTriggeredAlert.value = simulated
        lastSpokenTime = System.currentTimeMillis()
        lastSpokenText = spokenWarning
        lastRiskLevel = level

        speakWarning(spokenWarning, isUrgent = level == RiskLevel.HIGH)
        triggerHapticAlert(direction, level)
    }

    private fun speakWarning(text: String, isUrgent: Boolean) {
        if (!ttsReady || textToSpeech == null) {
            Log.i(tag, "Voice Alert (TTS not ready): $text")
            return
        }
        val queueMode = if (isUrgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        textToSpeech?.speak(text, queueMode, null, "sentry_alert_${System.currentTimeMillis()}")
    }

    fun triggerHapticAlert(direction: Direction, level: RiskLevel) {
        vibrator?.let { vib ->
            if (!vib.hasVibrator()) return

            val timings: LongArray
            val amplitudes: IntArray

            when (level) {
                RiskLevel.HIGH -> {
                    // Urgent aggressive pulse based on direction
                    when (direction) {
                        Direction.RIGHT -> {
                            // Two long pulses leaning right
                            timings = longArrayOf(0, 100, 60, 250, 60, 350)
                            amplitudes = intArrayOf(0, 180, 0, 240, 0, 255)
                        }
                        Direction.LEFT -> {
                            // Quick repeated warning pulses leaning left
                            timings = longArrayOf(0, 250, 60, 120, 60, 120)
                            amplitudes = intArrayOf(0, 255, 0, 200, 0, 180)
                        }
                        else -> {
                            // Central solid urgent triple buzz
                            timings = longArrayOf(0, 180, 80, 180, 80, 240)
                            amplitudes = intArrayOf(0, 220, 0, 220, 0, 255)
                        }
                    }
                }
                RiskLevel.WARNING -> {
                    timings = longArrayOf(0, 150, 100, 180)
                    amplitudes = intArrayOf(0, 180, 0, 220)
                }
                RiskLevel.CAUTION -> {
                    timings = longArrayOf(0, 80, 60, 80)
                    amplitudes = intArrayOf(0, 120, 0, 120)
                }
                RiskLevel.NORMAL -> return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vib.vibrate(effect)
                } catch (e: Exception) {
                    @Suppress("DEPRECATION")
                    vib.vibrate(timings, -1)
                }
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(timings, -1)
            }
        }
    }

    override fun stopAlerts() {
        textToSpeech?.stop()
        vibrator?.cancel()
    }

    override fun release() {
        stopAlerts()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
