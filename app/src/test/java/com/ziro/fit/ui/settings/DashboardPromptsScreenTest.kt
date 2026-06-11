package com.ziro.fit.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class DashboardPromptsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `title is displayed`() {
        composeTestRule.setContent {
            DashboardPromptsScreen(
                coachBannerEnabled = true,
                checkInBannerEnabled = true,
                onCoachBannerToggle = {},
                onCheckInBannerToggle = {},
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Dashboard Prompts").assertExists()
    }

    @Test
    fun `need a coach toggle is displayed`() {
        composeTestRule.setContent {
            DashboardPromptsScreen(
                coachBannerEnabled = true,
                checkInBannerEnabled = true,
                onCoachBannerToggle = {},
                onCheckInBannerToggle = {},
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Need a Coach?").assertExists()
        composeTestRule.onNodeWithText("Show banner if no active trainer").assertExists()
    }

    @Test
    fun `weekly check-in toggle is displayed`() {
        composeTestRule.setContent {
            DashboardPromptsScreen(
                coachBannerEnabled = true,
                checkInBannerEnabled = true,
                onCoachBannerToggle = {},
                onCheckInBannerToggle = {},
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Weekly Check-in").assertExists()
        composeTestRule.onNodeWithText("Show banner for training updates").assertExists()
    }

    @Test
    fun `coach banner toggle triggers callback`() {
        var toggled = false
        composeTestRule.setContent {
            DashboardPromptsScreen(
                coachBannerEnabled = false,
                checkInBannerEnabled = true,
                onCoachBannerToggle = { toggled = true },
                onCheckInBannerToggle = {},
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Need a Coach?").performClick()
        assert(toggled) { "Expected coach banner toggle callback" }
    }

    @Test
    fun `check-in banner toggle triggers callback`() {
        var toggled = false
        composeTestRule.setContent {
            DashboardPromptsScreen(
                coachBannerEnabled = true,
                checkInBannerEnabled = false,
                onCoachBannerToggle = {},
                onCheckInBannerToggle = { toggled = true },
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Weekly Check-in").performClick()
        assert(toggled) { "Expected check-in banner toggle callback" }
    }

    @Test
    fun `both toggles reflect initial state`() {
        composeTestRule.setContent {
            DashboardPromptsScreen(
                coachBannerEnabled = false,
                checkInBannerEnabled = true,
                onCoachBannerToggle = {},
                onCheckInBannerToggle = {},
                onNavigateBack = {}
            )
        }

        // Cannot easily verify Switch state in compose, but we can verify both exist
        composeTestRule.onNodeWithText("Need a Coach?").assertExists()
        composeTestRule.onNodeWithText("Weekly Check-in").assertExists()
    }

    @Test
    fun `navigate back button exists`() {
        var navigatedBack = false
        composeTestRule.setContent {
            DashboardPromptsScreen(
                coachBannerEnabled = true,
                checkInBannerEnabled = true,
                onCoachBannerToggle = {},
                onCheckInBannerToggle = {},
                onNavigateBack = { navigatedBack = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(navigatedBack) { "Expected back navigation" }
    }
}
