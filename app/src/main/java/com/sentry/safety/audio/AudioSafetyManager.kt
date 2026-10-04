package com.sentry.safety.audio

import com.sentry.safety.data.models.SoundEvent
import kotlinx.coroutines.flow.StateFlow

interface AudioSafetyManager {
    val isAudioActive: StateFlow<Boolean>
    val isAudioAvailable: Boolean
    val soundEvents: StateFlow<SoundEvent>

    fun startAudioMonitoring()
    fun stopAudioMonitoring()
    fun simulateSoundEvent(event: SoundEvent)
}
