package com.sentry.safety.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentry.safety.data.models.Direction
import com.sentry.safety.data.models.RiskLevel
import com.sentry.safety.data.models.RiskResult
import com.sentry.safety.ui.theme.SentryDangerRed
import com.sentry.safety.ui.theme.SentryWarningAmber
import com.sentry.safety.ui.theme.TextMuted
import com.sentry.safety.ui.theme.TextPrimary

@Composable
fun WarningBanner(
    riskResult: RiskResult,
    modifier: Modifier = Modifier
) {
    val isAlertActive = riskResult.riskLevel >= RiskLevel.CAUTION && riskResult.visualWarning.isNotEmpty()

    val accentColor = if (riskResult.riskLevel == RiskLevel.HIGH) SentryDangerRed else SentryWarningAmber
    val bgColor = if (riskResult.riskLevel == RiskLevel.HIGH) Color(0x33FF1744) else Color(0x33FFAB00)

    AnimatedVisibility(
        visible = isAlertActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .border(2.dp, accentColor, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = riskResult.visualWarning,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = riskResult.reason,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Direction indicator icon
                val dirIcon = when (riskResult.direction) {
                    Direction.LEFT -> Icons.Default.ArrowBack
                    Direction.RIGHT -> Icons.Default.ArrowForward
                    Direction.CENTER -> Icons.Default.ArrowUpward
                    Direction.NONE -> null
                }

                dirIcon?.let {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor)
                    ) {
                        Icon(
                            imageVector = it,
                            contentDescription = "Direction",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
