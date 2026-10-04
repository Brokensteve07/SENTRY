package com.sentry.safety.alerts

import com.sentry.safety.data.models.Direction
import com.sentry.safety.data.models.RiskLevel
import com.sentry.safety.data.models.RiskResult
import kotlinx.coroutines.flow.StateFlow

interface AlertEngine {
    val lastTriggeredAlert: StateFlow<RiskResult?>
    val isCooldownActive: Boolean

    fun processRiskResult(riskResult: RiskResult)
    fun triggerImmediateAlert(
        title: String,
        spokenWarning: String,
        direction: Direction,
        level: RiskLevel
    )
    fun stopAlerts()
    fun release()
}
