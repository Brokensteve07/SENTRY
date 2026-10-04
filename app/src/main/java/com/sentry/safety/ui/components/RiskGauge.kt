package com.sentry.safety.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentry.safety.data.models.RiskLevel
import com.sentry.safety.ui.theme.SentryCardDark
import com.sentry.safety.ui.theme.SentryDangerRed
import com.sentry.safety.ui.theme.SentryNeonCyan
import com.sentry.safety.ui.theme.SentrySafeGreen
import com.sentry.safety.ui.theme.SentryWarningAmber
import com.sentry.safety.ui.theme.TextMuted
import com.sentry.safety.ui.theme.TextPrimary

@Composable
fun RiskGauge(
    score: Int,
    level: RiskLevel,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "riskProgress"
    )

    val targetColor = when (level) {
        RiskLevel.NORMAL -> SentrySafeGreen
        RiskLevel.CAUTION -> SentryWarningAmber
        RiskLevel.WARNING -> Color(0xFFFF6D00)
        RiskLevel.HIGH -> SentryDangerRed
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 400),
        label = "riskColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.toPx() - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(diameter, diameter)

            // Background track (240 degree arc from 150 deg to 390 deg)
            drawArc(
                color = Color(0xFF1E2638),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress active arc
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to SentryNeonCyan,
                    0.5f to SentryWarningAmber,
                    1.0f to SentryDangerRed
                ),
                startAngle = 150f,
                sweepAngle = 240f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "RISK SCORE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "$score",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                color = animatedColor,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = level.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = animatedColor,
                letterSpacing = 2.sp
            )
        }
    }
}
