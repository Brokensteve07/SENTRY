package com.sentry.safety.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentry.safety.ui.theme.SentryBorderDark
import com.sentry.safety.ui.theme.SentryCardDark
import com.sentry.safety.ui.theme.SentryNeonCyan
import com.sentry.safety.ui.theme.SentryWarningAmber
import com.sentry.safety.ui.theme.TextMuted
import com.sentry.safety.ui.theme.TextPrimary

@Composable
fun DemoScenariosBar(
    onSelectScenario: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "HACKATHON DEMO CONTROLS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SentryWarningAmber,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DemoChip(title = "1. Car Right + Horn", onClick = { onSelectScenario(1) })
            DemoChip(title = "2. Obstacle Ahead", onClick = { onSelectScenario(2) })
            DemoChip(title = "3. Truck Left", onClick = { onSelectScenario(3) })
            DemoChip(title = "4. Clear / Safe", onClick = { onSelectScenario(4) })
            DemoChip(title = "5. Elderly Fall SOS", onClick = { onSelectScenario(5) })
        }
    }
}

@Composable
private fun DemoChip(
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SentryCardDark)
            .border(1.dp, SentryBorderDark, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}
