package com.sentry.safety.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentry.safety.data.models.PhoneOrientation
import com.sentry.safety.data.models.RiskLevel
import com.sentry.safety.ui.components.DebugPanel
import com.sentry.safety.ui.components.DemoScenariosBar
import com.sentry.safety.ui.components.ElderlySosDialog
import com.sentry.safety.ui.components.LiveCameraOverlay
import com.sentry.safety.ui.components.ModeSelector
import com.sentry.safety.ui.components.RiskGauge
import com.sentry.safety.ui.components.SensorStatusRow
import com.sentry.safety.ui.components.WarningBanner
import com.sentry.safety.ui.theme.SentryBgDark
import com.sentry.safety.ui.theme.SentryBorderDark
import com.sentry.safety.ui.theme.SentryCardDark
import com.sentry.safety.ui.theme.SentryDangerRed
import com.sentry.safety.ui.theme.SentryNeonCyan
import com.sentry.safety.ui.theme.SentrySafeGreen
import com.sentry.safety.ui.theme.SentryWarningAmber
import com.sentry.safety.ui.theme.TextMuted
import com.sentry.safety.ui.theme.TextPrimary

@Composable
fun MainScreen(
    viewModel: SentryViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val isProtected by viewModel.isProtectionActive.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val sensorStatus by viewModel.sensorStatus.collectAsState()
    val fusedContext by viewModel.fusedContext.collectAsState()
    val riskResult by viewModel.riskResult.collectAsState()
    val isDebugExpanded by viewModel.isDebugExpanded.collectAsState()
    val showSosDialog by viewModel.showSosDialog.collectAsState()
    val sosCountdown by viewModel.sosCountdown.collectAsState()

    // Elderly SOS Workflow
    if (showSosDialog) {
        ElderlySosDialog(
            countdownSeconds = sosCountdown,
            onCancel = { viewModel.cancelSos() },
            onGetHelp = { viewModel.confirmSosHelp() }
        )
    }

    Scaffold(
        containerColor = SentryBgDark,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==========================================
            // TOP HEADER: LOGO & STATUS BADGE
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "SENTRY",
                        tint = SentryNeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SENTRY",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "AI SAFETY LAYER",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SentryNeonCyan,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                // Protection Status Badge
                val statusColor = if (isProtected) {
                    when (riskResult.riskLevel) {
                        RiskLevel.HIGH -> SentryDangerRed
                        RiskLevel.WARNING, RiskLevel.CAUTION -> SentryWarningAmber
                        RiskLevel.NORMAL -> SentrySafeGreen
                    }
                } else TextMuted

                val statusText = if (isProtected) "PROTECTED" else "STANDBY"

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SentryCardDark)
                        .border(1.dp, SentryBorderDark, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // WARNING BANNER (When Active)
            // ==========================================
            WarningBanner(
                riskResult = riskResult,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ==========================================
            // LIVE CAMERA HUD OVERLAY
            // ==========================================
            LiveCameraOverlay(
                cameraManager = viewModel.cameraManager,
                isCameraActive = isProtected,
                detections = fusedContext.detectedObjects,
                riskResult = riskResult
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // RISK SCORE GAUGE
            // ==========================================
            RiskGauge(
                score = riskResult.riskScore,
                level = riskResult.riskLevel
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // LIVE SENSOR SUBSYSTEMS ROW
            // ==========================================
            SensorStatusRow(status = sensorStatus)

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // START / STOP PROTECTION ACTION BUTTON
            // ==========================================
            Button(
                onClick = { viewModel.toggleProtection() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isProtected) Color(0xFF263238) else SentryNeonCyan
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = if (isProtected) SentryDangerRed else Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isProtected) "STOP PROTECTION" else "START PROTECTION",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = if (isProtected) TextPrimary else Color.Black
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // MODE SELECTION
            // ==========================================
            ModeSelector(
                selectedMode = currentMode,
                onModeSelected = { viewModel.setMode(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // HACKATHON DEMO CONTROLS BAR
            // ==========================================
            DemoScenariosBar(
                onSelectScenario = { viewModel.playDemoScenario(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // PHONE ORIENTATION TOGGLE (FOR DEMO/TESTING)
            // ==========================================
            val isFacingAway = fusedContext.motionContext.orientation == PhoneOrientation.FACING_AWAY
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SentryCardDark)
                    .border(1.dp, SentryBorderDark, RoundedCornerShape(12.dp))
                    .clickable { viewModel.setPhoneFacingRoad(isFacingAway) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PHONE ORIENTATION SIMULATOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = if (isFacingAway) "Facing Away / Inattentive" else "Facing Road / Attentive",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFacingAway) SentryWarningAmber else SentrySafeGreen
                    )
                }
                Icon(
                    imageVector = if (isFacingAway) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null,
                    tint = if (isFacingAway) SentryWarningAmber else SentrySafeGreen
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // DEVELOPER DEBUG / EXPLANATION PANEL
            // ==========================================
            DebugPanel(
                fusedContext = fusedContext,
                riskResult = riskResult,
                isExpanded = isDebugExpanded,
                onToggleExpand = { viewModel.toggleDebug() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
