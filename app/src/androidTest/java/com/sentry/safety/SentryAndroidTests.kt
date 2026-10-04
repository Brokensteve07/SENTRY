package com.sentry.safety

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sentry.safety.data.models.SafetyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SentryAndroidTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testActivityLaunchAndHeader() {
        composeTestRule.onNodeWithText("SENTRY").assertExists()
        composeTestRule.onNodeWithText("AI SAFETY LAYER").assertExists()
    }

    @Test
    fun testModeSwitching() {
        composeTestRule.onNodeWithText("HOME").performClick()
        composeTestRule.onNodeWithText("HOME").assertExists()

        composeTestRule.onNodeWithText("ELDERLY").performClick()
        composeTestRule.onNodeWithText("ELDERLY").assertExists()

        composeTestRule.onNodeWithText("PEDESTRIAN").performClick()
        composeTestRule.onNodeWithText("PEDESTRIAN").assertExists()
    }

    @Test
    fun testStartProtectionToggle() {
        val startButton = composeTestRule.onNodeWithText("START PROTECTION")
        startButton.assertExists()
        startButton.performClick()
        composeTestRule.onNodeWithText("STOP PROTECTION").assertExists()
    }

    @Test
    fun testDemoScenarioTrigger() {
        // Trigger scenario 1
        composeTestRule.onNodeWithText("1. Car Right + Horn").performClick()
        composeTestRule.waitForIdle()

        // Debug panel toggle
        composeTestRule.onNodeWithText("DECISION PIPELINE & DEBUG").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Camera AI:").assertExists()
    }
}
