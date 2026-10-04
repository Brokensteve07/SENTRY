package com.sentry.safety.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentry.safety.data.models.FusedContext
import com.sentry.safety.data.models.RiskResult
import com.sentry.safety.ui.theme.SentryBorderDark
import com.sentry.safety.ui.theme.SentryCardDark
import com.sentry.safety.ui.theme.SentryDangerRed
import com.sentry.safety.ui.theme.SentryNeonCyan
import com.sentry.safety.ui.theme.SentrySafeGreen
import com.sentry.safety.ui.theme.SentrySurfaceDark
import com.sentry.safety.ui.theme.SentryWarningAmber
import com.sentry.safety.ui.theme.TextMuted
import com.sentry.safety.ui.theme.TextPrimary

@Composable
fun DebugPanel(
    fusedContext: FusedContext,
    riskResult: RiskResult,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SentrySurfaceDark)
            .border(1.dp, SentryBorderDark, RoundedCornerShape(14.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Debug",
                    tint = SentryNeonCyan,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "DECISION PIPELINE & DEBUG",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SentryNeonCyan,
                    letterSpacing = 1.sp
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle",
                tint = TextMuted
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Divider(color = SentryBorderDark)
                Spacer(modifier = Modifier.height(10.dp))

                val threatObj = fusedContext.primaryThreatObject
                DebugRow(
                    label = "Camera AI:",
                    value = if (threatObj != null) "${threatObj.label} (${(threatObj.confidence * 100).toInt()}%)" else "None"
                )
                DebugRow(
                    label = "Position:",
                    value = threatObj?.direction?.name ?: "N/A"
                )
                DebugRow(
                    label = "Movement:",
                    value = threatObj?.trajectory?.name ?: "N/A"
                )
                DebugRow(
                    label = "Audio Class:",
                    value = "${fusedContext.soundEvent.soundType.name} (${(fusedContext.soundEvent.confidence * 100).toInt()}%) - ${fusedContext.soundEvent.decibels.toInt()} dB"
                )
                DebugRow(
                    label = "Motion State:",
                    value = fusedContext.motionContext.motionState.name
                )
                DebugRow(
                    label = "Phone Orientation:",
                    value = fusedContext.motionContext.orientation.name
                )
                DebugRow(
                    label = "User Inattention:",
                    value = if (fusedContext.inattentionDetected) "YES (Facing Away while walking)" else "NO"
                )
                DebugRow(
                    label = "Risk Calculation:",
                    value = "${riskResult.riskScore} / 100 (${riskResult.riskLevel.name})",
                    highlight = true
                )
                DebugRow(
                    label = "Decision:",
                    value = riskResult.recommendedAction
                )
                DebugRow(
                    label = "Warning Emitted:",
                    value = if (riskResult.voiceWarning.isNotEmpty()) riskResult.voiceWarning else "None"
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DebugRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) SentryWarningAmber else TextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}
