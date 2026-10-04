package com.sentry.safety.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.sentry.safety.camera.CameraSafetyManager
import com.sentry.safety.data.models.DetectedObject
import com.sentry.safety.data.models.Direction
import com.sentry.safety.data.models.RiskLevel
import com.sentry.safety.data.models.RiskResult
import com.sentry.safety.ui.theme.SentryBorderDark
import com.sentry.safety.ui.theme.SentryCardDark
import com.sentry.safety.ui.theme.SentryDangerRed
import com.sentry.safety.ui.theme.SentryNeonCyan
import com.sentry.safety.ui.theme.SentryWarningAmber
import com.sentry.safety.ui.theme.TextMuted
import com.sentry.safety.ui.theme.TextPrimary

@Composable
fun LiveCameraOverlay(
    cameraManager: CameraSafetyManager,
    isCameraActive: Boolean,
    detections: List<DetectedObject>,
    riskResult: RiskResult,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F141C))
            .border(1.dp, SentryBorderDark, RoundedCornerShape(16.dp))
    ) {
        // 1. Camera View or HUD Background
        if (isCameraActive && cameraManager.isCameraAvailable) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        cameraManager.startCamera(lifecycleOwner, this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Simulated HUD Grid for Demo / Standby
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1117)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCameraActive) "VISION SENSORS RUNNING" else "CAMERA STANDBY",
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 2. Sector Guidelines (LEFT | CENTER | RIGHT)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 38% divider
            drawLine(
                color = Color(0x3300E5FF),
                start = Offset(width * 0.38f, 0f),
                end = Offset(width * 0.38f, height),
                strokeWidth = 1.dp.toPx()
            )
            // 62% divider
            drawLine(
                color = Color(0x3300E5FF),
                start = Offset(width * 0.62f, 0f),
                end = Offset(width * 0.62f, height),
                strokeWidth = 1.dp.toPx()
            )

            // Bounding box overlays for detected objects
            for (obj in detections) {
                val box = obj.boundingBox
                val leftPx = box.left * width
                val topPx = box.top * height
                val boxWidthPx = box.width() * width
                val boxHeightPx = box.height() * height

                val boxColor = when (riskResult.riskLevel) {
                    RiskLevel.HIGH -> SentryDangerRed
                    RiskLevel.WARNING -> SentryWarningAmber
                    else -> SentryNeonCyan
                }

                drawRect(
                    color = boxColor,
                    topLeft = Offset(leftPx, topPx),
                    size = Size(boxWidthPx, boxHeightPx),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
        }

        // 3. Top Sector Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectorTag(
                title = "LEFT",
                isActive = detections.any { it.direction == Direction.LEFT }
            )
            SectorTag(
                title = "CENTER",
                isActive = detections.any { it.direction == Direction.CENTER }
            )
            SectorTag(
                title = "RIGHT",
                isActive = detections.any { it.direction == Direction.RIGHT }
            )
        }

        // 4. Object Bounding Box Labels
        for (obj in detections) {
            val dirX = when (obj.direction) {
                Direction.LEFT -> Alignment.TopStart
                Direction.CENTER -> Alignment.TopCenter
                Direction.RIGHT -> Alignment.TopEnd
                Direction.NONE -> Alignment.TopCenter
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 34.dp),
                contentAlignment = dirX
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC111827))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${obj.label} ${(obj.confidence * 100).toInt()}% [${obj.trajectory.name}]",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SentryNeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 5. Bottom Live Risk Badge Overlay
        AnimatedVisibility(
            visible = riskResult.riskLevel >= RiskLevel.CAUTION,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (riskResult.riskLevel == RiskLevel.HIGH) Color(0xEEFF1744) else Color(0xEEFFAB00))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${riskResult.riskLevel.name}: ${riskResult.visualWarning.replace('\n', ' ')}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun SectorTag(
    title: String,
    isActive: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) Color(0x6600E5FF) else Color(0x33000000))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) SentryNeonCyan else TextMuted,
            letterSpacing = 1.sp
        )
    }
}
