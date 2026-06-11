package com.ziro.fit.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class WhatsNewScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `title is displayed`() {
        composeTestRule.setContent {
            WhatsNewScreen(onNavigateBack = {})
        }

        composeTestRule.onNodeWithText("What's New").assertExists()
    }

    @Test
    fun `main heading is displayed`() {
        composeTestRule.setContent {
            WhatsNewScreen(onNavigateBack = {})
        }

        composeTestRule.onNodeWithText("What's New in Ziro Fit").assertExists()
    }

    @Test
    fun `feature items are displayed`() {
        composeTestRule.setContent {
            WhatsNewScreen(onNavigateBack = {})
        }

        composeTestRule.onNodeWithText("AI Coach Improvements").assertExists()
        composeTestRule.onNodeWithText("Advanced Analytics").assertExists()
        composeTestRule.onNodeWithText("Smart Notifications").assertExists()
        composeTestRule.onNodeWithText("New Themes").assertExists()
    }

    @Test
    fun `feature descriptions are displayed`() {
        composeTestRule.setContent {
            WhatsNewScreen(onNavigateBack = {})
        }

        composeTestRule.onNodeWithText("Enhanced voice recognition and more natural conversation flow").assertExists()
        composeTestRule.onNodeWithText("New charts and insights for your training progress").assertExists()
    }

    @Test
    fun `navigate back button exists`() {
        var navigatedBack = false
        composeTestRule.setContent {
            WhatsNewScreen(onNavigateBack = { navigatedBack = true })
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(navigatedBack) { "Expected back navigation" }
    }
}
