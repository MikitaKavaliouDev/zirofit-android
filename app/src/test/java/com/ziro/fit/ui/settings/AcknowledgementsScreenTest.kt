package com.ziro.fit.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class AcknowledgementsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `title is displayed`() {
        composeTestRule.setContent {
            AcknowledgementsScreen(onNavigateBack = {})
        }

        composeTestRule.onNodeWithText("Acknowledgements").assertExists()
    }

    @Test
    fun `header text is displayed`() {
        composeTestRule.setContent {
            AcknowledgementsScreen(onNavigateBack = {})
        }

        composeTestRule.onNodeWithText("Data & Acknowledgements").assertExists()
    }

    @Test
    fun `open source section is displayed`() {
        composeTestRule.setContent {
            AcknowledgementsScreen(onNavigateBack = {})
        }

        composeTestRule.onNodeWithText("Open Source").assertExists()
    }

    @Test
    fun `key open source libraries are listed`() {
        composeTestRule.setContent {
            AcknowledgementsScreen(onNavigateBack = {})
        }

        composeTestRule.onNodeWithText("Jetpack Compose").assertExists()
        composeTestRule.onNodeWithText("Hilt").assertExists()
        composeTestRule.onNodeWithText("Retrofit").assertExists()
        composeTestRule.onNodeWithText("Coil").assertExists()
        composeTestRule.onNodeWithText("Supabase").assertExists()
    }

    @Test
    fun `data attribution section is displayed`() {
        composeTestRule.setContent {
            AcknowledgementsScreen(onNavigateBack = {})
        }

        composeTestRule.onNodeWithText("Data Attribution").assertExists()
        composeTestRule.onNodeWithText("Exercise database and illustrations provided by ExerciseDB and WGER Open Source.").assertExists()
    }

    @Test
    fun `navigate back button exists`() {
        var navigatedBack = false
        composeTestRule.setContent {
            AcknowledgementsScreen(onNavigateBack = { navigatedBack = true })
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assert(navigatedBack) { "Expected back navigation" }
    }
}
