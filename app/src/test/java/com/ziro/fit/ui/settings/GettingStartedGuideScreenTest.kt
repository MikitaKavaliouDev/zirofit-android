package com.ziro.fit.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class GettingStartedGuideScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `title is displayed`() {
        composeTestRule.setContent {
            GettingStartedGuideScreen(
                isTrainer = true,
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Getting Started Guide").assertExists()
    }

    @Test
    fun `trainer first step is displayed`() {
        composeTestRule.setContent {
            GettingStartedGuideScreen(
                isTrainer = true,
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Set Up Your Profile").assertExists()
        composeTestRule.onNodeWithText("Create your trainer profile, add your credentials, and set up your services").assertExists()
    }

    @Test
    fun `client first step is displayed`() {
        composeTestRule.setContent {
            GettingStartedGuideScreen(
                isTrainer = false,
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Your Dashboard").assertExists()
        composeTestRule.onNodeWithText("View your workouts, progress, and upcoming sessions").assertExists()
    }

    @Test
    fun `next button advances to step 2`() {
        composeTestRule.setContent {
            GettingStartedGuideScreen(
                isTrainer = true,
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Set Up Your Profile").assertExists()
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithText("Manage Clients").assertExists()
    }

    @Test
    fun `skip button jumps to last step`() {
        composeTestRule.setContent {
            GettingStartedGuideScreen(
                isTrainer = true,
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Skip").performClick()
        composeTestRule.onNodeWithText("Engage & Grow").assertExists()
    }

    @Test
    fun `last step shows start button for trainers`() {
        var started = false
        composeTestRule.setContent {
            GettingStartedGuideScreen(
                isTrainer = true,
                onNavigateBack = { started = true }
            )
        }

        // Skip to last step
        composeTestRule.onNodeWithText("Skip").performClick()
        composeTestRule.onNodeWithText("Start Coaching").assertExists()
    }

    @Test
    fun `last step shows get started button for clients`() {
        composeTestRule.setContent {
            GettingStartedGuideScreen(
                isTrainer = false,
                onNavigateBack = {}
            )
        }

        // Skip to last step
        composeTestRule.onNodeWithText("Skip").performClick()
        composeTestRule.onNodeWithText("Get Started").assertExists()
    }

    @Test
    fun `last step button navigates back`() {
        var navigatedBack = false
        composeTestRule.setContent {
            GettingStartedGuideScreen(
                isTrainer = true,
                onNavigateBack = { navigatedBack = true }
            )
        }

        // Skip to last step and click start
        composeTestRule.onNodeWithText("Skip").performClick()
        composeTestRule.onNodeWithText("Start Coaching").performClick()
        assert(navigatedBack) { "Expected back navigation on start" }
    }

    @Test
    fun `navigate back button exists`() {
        var navigatedBack = false
        composeTestRule.setContent {
            GettingStartedGuideScreen(
                isTrainer = true,
                onNavigateBack = { navigatedBack = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(navigatedBack) { "Expected back navigation" }
    }
}
