package com.sentry.safety.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SentryNeonCyan,
    onPrimary = SentryBgDark,
    secondary = SentryWarningAmber,
    onSecondary = SentryBgDark,
    tertiary = SentrySafeGreen,
    background = SentryBgDark,
    surface = SentrySurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun SentryTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SentryBgDark.toArgb()
            window.navigationBarColor = SentryBgDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
