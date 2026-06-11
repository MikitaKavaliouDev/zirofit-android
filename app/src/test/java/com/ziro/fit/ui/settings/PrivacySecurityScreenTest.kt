package com.ziro.fit.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class PrivacySecurityScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `title is displayed`() {
        composeTestRule.setContent {
            PrivacySecurityScreen(
                isClient = false,
                onNavigateBack = {},
                onUnlinkTrainer = {},
                onDeleteAccount = {}
            )
        }

        composeTestRule.onNodeWithText("Privacy & Security").assertExists()
    }

    @Test
    fun `delete account option is shown for non-client`() {
        composeTestRule.setContent {
            PrivacySecurityScreen(
                isClient = false,
                onNavigateBack = {},
                onUnlinkTrainer = {},
                onDeleteAccount = {}
            )
        }

        composeTestRule.onNodeWithText("Security").assertExists()
        composeTestRule.onNodeWithText("Delete Account").assertExists()
        composeTestRule.onNodeWithText("Trainer Connection").assertDoesNotExist()
    }

    @Test
    fun `unlink trainer option is shown only for clients`() {
        composeTestRule.setContent {
            PrivacySecurityScreen(
                isClient = true,
                onNavigateBack = {},
                onUnlinkTrainer = {},
                onDeleteAccount = {}
            )
        }

        composeTestRule.onNodeWithText("Trainer Connection").assertExists()
        composeTestRule.onNodeWithText("Unlink from Trainer").assertExists()
        composeTestRule.onNodeWithText("Delete Account").assertExists()
    }

    @Test
    fun `delete account shows confirmation dialog`() {
        composeTestRule.setContent {
            PrivacySecurityScreen(
                isClient = false,
                onNavigateBack = {},
                onUnlinkTrainer = {},
                onDeleteAccount = {}
            )
        }

        composeTestRule.onNodeWithText("Delete Account").performClick()
        composeTestRule.onNodeWithText("Delete Account?").assertExists()
        composeTestRule.onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun `delete dialog confirms and triggers callback`() {
        var deleted = false
        composeTestRule.setContent {
            PrivacySecurityScreen(
                isClient = false,
                onNavigateBack = {},
                onUnlinkTrainer = {},
                onDeleteAccount = { deleted = true }
            )
        }

        composeTestRule.onNodeWithText("Delete Account").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        assert(deleted) { "Expected delete callback to be invoked" }
    }

    @Test
    fun `unlink dialog shows and confirms`() {
        var unlinked = false
        composeTestRule.setContent {
            PrivacySecurityScreen(
                isClient = true,
                onNavigateBack = {},
                onUnlinkTrainer = { unlinked = true },
                onDeleteAccount = {}
            )
        }

        composeTestRule.onNodeWithText("Unlink from Trainer").performClick()
        composeTestRule.onNodeWithText("Unlink from Trainer?").assertExists()

        composeTestRule.onNodeWithText("Unlink").performClick()
        assert(unlinked) { "Expected unlink callback to be invoked" }
    }

    @Test
    fun `unlink dialog can be cancelled`() {
        var unlinked = false
        composeTestRule.setContent {
            PrivacySecurityScreen(
                isClient = true,
                onNavigateBack = {},
                onUnlinkTrainer = { unlinked = true },
                onDeleteAccount = {}
            )
        }

        composeTestRule.onNodeWithText("Unlink from Trainer").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        assert(!unlinked) { "Expected unlink callback NOT to be invoked after cancel" }
    }

    @Test
    fun `navigate back button exists`() {
        var navigatedBack = false
        composeTestRule.setContent {
            PrivacySecurityScreen(
                isClient = false,
                onNavigateBack = { navigatedBack = true },
                onUnlinkTrainer = {},
                onDeleteAccount = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(navigatedBack) { "Expected back navigation" }
    }
}
