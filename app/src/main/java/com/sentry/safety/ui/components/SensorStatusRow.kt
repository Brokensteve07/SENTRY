package com.sentry.safety.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentry.safety.data.models.SensorStatus
import com.sentry.safety.ui.theme.SentryBorderDark
import com.sentry.safety.ui.theme.SentryCardDark
import com.sentry.safety.ui.theme.SentryDangerRed
import com.sentry.safety.ui.theme.SentrySafeGreen
import com.sentry.safety.ui.theme.SentryWarningAmber
import com.sentry.safety.ui.theme.TextMuted
import com.sentry.safety.ui.theme.TextPrimary

@Composable
fun SensorStatusRow(
    status: SensorStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SentryCardDark)
            .border(1.dp, SentryBorderDark, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SensorItem(
            name = "Camera",
            icon = Icons.Default.CameraAlt,
            isActive = status.cameraActive,
            isAvailable = status.cameraAvailable
        )
        SensorItem(
            name = "Audio",
            icon = Icons.Default.Mic,
            isActive = status.audioActive,
            isAvailable = status.audioAvailable
        )
        SensorItem(
            name = "Motion",
            icon = Icons.Default.DirectionsWalk,
            isActive = status.motionActive,
            isAvailable = status.motionAvailable
        )
        SensorItem(
            name = "Location",
            icon = Icons.Default.Navigation,
            isActive = status.locationActive,
            isAvailable = status.locationAvailable
        )
    }
}

@Composable
private fun SensorItem(
    name: String,
    icon: ImageVector,
    isActive: Boolean,
    isAvailable: Boolean
) {
    val indicatorColor = when {
        !isAvailable -> SentryWarningAmber
        isActive -> SentrySafeGreen
        else -> TextMuted
    }

    val stateText = when {
        !isAvailable -> "Degraded"
        isActive -> "Active"
        else -> "Standby"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = if (isActive) TextPrimary else TextMuted,
                modifier = Modifier.size(24.dp)
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(
            text = stateText,
            fontSize = 9.sp,
            color = indicatorColor
        )
    }
}
