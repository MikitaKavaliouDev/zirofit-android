package com.ziro.fit.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class VoiceSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `title is displayed`() {
        composeTestRule.setContent {
            VoiceSettingsScreen(
                onNavigateBack = {},
                onOpenAICoach = {}
            )
        }

        composeTestRule.onNodeWithText("AI Coach Settings").assertExists()
    }

    @Test
    fun `voice mode selection is displayed`() {
        composeTestRule.setContent {
            VoiceSettingsScreen(
                onNavigateBack = {},
                onOpenAICoach = {}
            )
        }

        composeTestRule.onNodeWithText("Active Voice Feature").assertExists()
    }

    @Test
    fun `command dictation card is shown`() {
        composeTestRule.setContent {
            VoiceSettingsScreen(
                onNavigateBack = {},
                onOpenAICoach = {}
            )
        }

        composeTestRule.onNodeWithText("Command Dictation").assertExists()
        composeTestRule.onNodeWithText("Log exercises, weight, and reps via speech").assertExists()
    }

    @Test
    fun `conversational AI coach card is shown`() {
        composeTestRule.setContent {
            VoiceSettingsScreen(
                onNavigateBack = {},
                onOpenAICoach = {}
            )
        }

        composeTestRule.onNodeWithText("Conversational AI Coach").assertExists()
        composeTestRule.onNodeWithText("Verbal advice and motivation from an AI coach").assertExists()
    }

    @Test
    fun `open AI Coach button triggers callback`() {
        var opened = false
        composeTestRule.setContent {
            VoiceSettingsScreen(
                onNavigateBack = {},
                onOpenAICoach = { opened = true }
            )
        }

        composeTestRule.onNodeWithText("Open AI Coach").performClick()
        assert(opened) { "Expected open AI Coach callback" }
    }

    @Test
    fun `navigate back button exists`() {
        var navigatedBack = false
        composeTestRule.setContent {
            VoiceSettingsScreen(
                onNavigateBack = { navigatedBack = true },
                onOpenAICoach = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(navigatedBack) { "Expected back navigation" }
    }
}
