package com.sentry.safety.risk

import com.sentry.safety.data.models.FusedContext
import com.sentry.safety.data.models.RiskResult
import kotlinx.coroutines.flow.StateFlow

interface RiskRule {
    val name: String
    val weight: Float
    fun evaluate(context: FusedContext): RuleEvaluation
}

data class RuleEvaluation(
    val scoreContribution: Int,
    val matched: Boolean,
    val reason: String = ""
)

interface RiskEngine {
    val currentRiskResult: StateFlow<RiskResult>

    fun evaluate(fusedContext: FusedContext): RiskResult
    fun registerRule(rule: RiskRule)
}
