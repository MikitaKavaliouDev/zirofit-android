package com.ziro.fit.ui.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.ziro.fit.model.AppMode
import com.ziro.fit.model.User
import com.ziro.fit.viewmodel.SettingsViewModel
import com.ziro.fit.viewmodel.SettingsUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val settingsViewModel: SettingsViewModel = mockk()
    private val uiState = MutableStateFlow(SettingsUiState())

    private var currentMode = AppMode.TRAINER
    private var navigatedRoute: String? = null

    @Before
    fun setup() {
        navigatedRoute = null
        currentMode = AppMode.TRAINER
        every { settingsViewModel.uiState } returns uiState
        every { settingsViewModel.setCustomModeEnabled(any()) } answers {
            uiState.value = uiState.value.copy(isCustomModeEnabled = firstArg())
        }
        every { settingsViewModel.setDailyTargetsEnabled(any()) } answers {
            uiState.value = uiState.value.copy(dailyTargetsEnabled = firstArg())
        }
        every { settingsViewModel.setVoiceFeedbackEnabled(any()) } answers {
            uiState.value = uiState.value.copy(voiceFeedbackEnabled = firstArg())
        }
        every { settingsViewModel.setRoutinesEnabled(any()) } answers {
            uiState.value = uiState.value.copy(routinesEnabled = firstArg())
        }
    }

    @Test
    fun `profile header shows TRAINER badge for trainer role user`() {
        val user = User(id = "1", name = "Test User", email = "test@test.com", role = "trainer", username = null, hasCompletedOnboarding = true, subscriptionStatus = null, profilePhotoPath = null, isFreeAccessModeEnabled = false, tier = null)
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.TRAINER,
                onModeSwitch = {},
                onNavigateToSubScreen = { navigatedRoute = it },
                onLogout = {},
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel(user)
            )
        }

        composeTestRule.onNodeWithText("PRO").assertExists()
        composeTestRule.onNodeWithText("Test User").assertExists()
    }

    @Test
    fun `profile header shows PERSONAL badge for client role user`() {
        val user = User(id = "2", name = "Client User", email = "client@test.com", role = "client", username = null, hasCompletedOnboarding = true, subscriptionStatus = null, profilePhotoPath = null, isFreeAccessModeEnabled = false, tier = null)
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.PERSONAL,
                onModeSwitch = {},
                onNavigateToSubScreen = { navigatedRoute = it },
                onLogout = {},
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel(user)
            )
        }

        composeTestRule.onNodeWithText("PERSONAL").assertExists()
        composeTestRule.onNodeWithText("Client User").assertExists()
    }

    @Test
    fun `profile header QR button navigates to qr_code for trainers`() {
        val user = User(id = "1", name = "Trainer", email = "trainer@test.com", role = "trainer", username = null, hasCompletedOnboarding = true, subscriptionStatus = null, profilePhotoPath = null, isFreeAccessModeEnabled = false, tier = null)
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.TRAINER,
                onModeSwitch = {},
                onNavigateToSubScreen = { navigatedRoute = it },
                onLogout = {},
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel(user)
            )
        }

        composeTestRule.onNodeWithContentDescription("QR Code").performClick()
        assert(navigatedRoute == "qr_code") { "Expected qr_code but got $navigatedRoute" }
    }

    @Test
    fun `profile header click navigates to profile`() {
        val user = User(id = "1", name = "Trainer", email = "trainer@test.com", role = "trainer", username = null, hasCompletedOnboarding = true, subscriptionStatus = null, profilePhotoPath = null, isFreeAccessModeEnabled = false, tier = null)
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.TRAINER,
                onModeSwitch = {},
                onNavigateToSubScreen = { navigatedRoute = it },
                onLogout = {},
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel(user)
            )
        }

        composeTestRule.onNodeWithText("Trainer").performClick()
        assert(navigatedRoute == "profile") { "Expected profile but got $navigatedRoute" }
    }

    @Test
    fun `preferences section contains all trainer nav items`() {
        val user = User(id = "1", name = "Trainer", email = "trainer@test.com", role = "trainer", username = null, hasCompletedOnboarding = true, subscriptionStatus = null, profilePhotoPath = null, isFreeAccessModeEnabled = false, tier = null)
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.TRAINER,
                onModeSwitch = {},
                onNavigateToSubScreen = { navigatedRoute = it },
                onLogout = {},
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel(user)
            )
        }

        composeTestRule.onNodeWithText("Preferences").assertExists()
        composeTestRule.onNodeWithText("Appearance").assertExists()
        composeTestRule.onNodeWithText("AI Coach Settings").assertExists()
        composeTestRule.onNodeWithText("Language").assertExists()
        composeTestRule.onNodeWithText("Notifications").assertExists()
        composeTestRule.onNodeWithText("Permissions").assertExists()
        composeTestRule.onNodeWithText("Data & Privacy").assertExists()
        composeTestRule.onNodeWithText("Privacy & Security").assertExists()
        composeTestRule.onNodeWithText("Dashboard Prompts").assertExists()
        composeTestRule.onNodeWithText("Custom App Mode").assertExists()
    }

    @Test
    fun `experimental section toggles are functional`() {
        val user = User(id = "1", name = "Trainer", email = "trainer@test.com", role = "trainer", username = null, hasCompletedOnboarding = true, subscriptionStatus = null, profilePhotoPath = null, isFreeAccessModeEnabled = false, tier = null)
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.TRAINER,
                onModeSwitch = {},
                onNavigateToSubScreen = { navigatedRoute = it },
                onLogout = {},
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel(user)
            )
        }

        composeTestRule.onNodeWithText("Experimental").assertExists()
        composeTestRule.onNodeWithText("Daily Targets").assertExists()
        composeTestRule.onNodeWithText("Voice Feedback").assertExists()
        composeTestRule.onNodeWithText("Routines").assertExists()
    }

    @Test
    fun `business section contains trainer-specific items`() {
        val user = User(id = "1", name = "Trainer", email = "trainer@test.com", role = "trainer", username = null, hasCompletedOnboarding = true, subscriptionStatus = null, profilePhotoPath = null, isFreeAccessModeEnabled = false, tier = null)
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.TRAINER,
                onModeSwitch = {},
                onNavigateToSubScreen = { navigatedRoute = it },
                onLogout = {},
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel(user)
            )
        }

        composeTestRule.onNodeWithText("Business").assertExists()
        composeTestRule.onNodeWithText("QR Business Card").assertExists()
        composeTestRule.onNodeWithText("Storefront Settings").assertExists()
        composeTestRule.onNodeWithText("Subscription").assertExists()
        composeTestRule.onNodeWithText("Payouts").assertExists()
        composeTestRule.onNodeWithText("What's New").assertExists()
        composeTestRule.onNodeWithText("Getting Started Guide").assertExists()
    }

    @Test
    fun `client mode shows Resources instead of Business`() {
        val user = User(id = "2", name = "Client", email = "client@test.com", role = "client", username = null, hasCompletedOnboarding = true, subscriptionStatus = null, profilePhotoPath = null, isFreeAccessModeEnabled = false, tier = null)
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.PERSONAL,
                onModeSwitch = {},
                onNavigateToSubScreen = { navigatedRoute = it },
                onLogout = {},
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel(user)
            )
        }

        composeTestRule.onNodeWithText("Resources").assertExists()
        composeTestRule.onNodeWithText("QR Business Card").assertDoesNotExist()
        composeTestRule.onNodeWithText("Payouts").assertDoesNotExist()
    }

    @Test
    fun `legal section contains terms and privacy`() {
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.TRAINER,
                onModeSwitch = {},
                onNavigateToSubScreen = { navigatedRoute = it },
                onLogout = {},
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel()
            )
        }

        composeTestRule.onNodeWithText("Legal").assertExists()
        composeTestRule.onNodeWithText("Terms of Service").assertExists()
        composeTestRule.onNodeWithText("Privacy Policy").assertExists()
    }

    @Test
    fun `footer contains version and brand links`() {
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.TRAINER,
                onModeSwitch = {},
                onNavigateToSubScreen = { navigatedRoute = it },
                onLogout = {},
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel()
            )
        }

        composeTestRule.onNodeWithText("Version 1.0.0").assertExists()
        composeTestRule.onNodeWithText("Acknowledgements").assertExists()
        composeTestRule.onNodeWithText("Terms").assertExists()
        composeTestRule.onNodeWithText("Privacy").assertExists()
    }

    @Test
    fun `sign out button exists and triggers callback`() {
        var loggedOut = false
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.TRAINER,
                onModeSwitch = {},
                onNavigateToSubScreen = {},
                onLogout = { loggedOut = true },
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel()
            )
        }

        composeTestRule.onNodeWithText("Sign Out").performClick()
        assert(loggedOut) { "Expected logout to be called" }
    }

    @Test
    fun `navigate to sub screen for AI Coach item`() {
        val user = User(id = "1", name = "Trainer", email = "trainer@test.com", role = "trainer", username = null, hasCompletedOnboarding = true, subscriptionStatus = null, profilePhotoPath = null, isFreeAccessModeEnabled = false, tier = null)
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.TRAINER,
                onModeSwitch = {},
                onNavigateToSubScreen = { navigatedRoute = it },
                onLogout = {},
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel(user)
            )
        }

        composeTestRule.onNodeWithText("AI Coach Settings").performClick()
        assert(navigatedRoute == "settings/ai_coach") { "Expected settings/ai_coach but got $navigatedRoute" }
    }

    @Test
    fun `navigate to Privacy and Security`() {
        val user = User(id = "1", name = "Trainer", email = "trainer@test.com", role = "trainer", username = null, hasCompletedOnboarding = true, subscriptionStatus = null, profilePhotoPath = null, isFreeAccessModeEnabled = false, tier = null)
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.TRAINER,
                onModeSwitch = {},
                onNavigateToSubScreen = { navigatedRoute = it },
                onLogout = {},
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel(user)
            )
        }

        composeTestRule.onNodeWithText("Privacy & Security").performClick()
        assert(navigatedRoute == "settings/privacy_security") { "Expected settings/privacy_security but got $navigatedRoute" }
    }

    @Test
    fun `navigate to Dashboard Prompts`() {
        val user = User(id = "1", name = "Trainer", email = "trainer@test.com", role = "trainer", username = null, hasCompletedOnboarding = true, subscriptionStatus = null, profilePhotoPath = null, isFreeAccessModeEnabled = false, tier = null)
        composeTestRule.setContent {
            SettingsScreen(
                currentMode = AppMode.TRAINER,
                onModeSwitch = {},
                onNavigateToSubScreen = { navigatedRoute = it },
                onLogout = {},
                settingsViewModel = settingsViewModel,
                userViewModel = mockkUserViewModel(user)
            )
        }

        composeTestRule.onNodeWithText("Dashboard Prompts").performClick()
        assert(navigatedRoute == "settings/dashboard_prompts") { "Expected settings/dashboard_prompts but got $navigatedRoute" }
    }

    private fun mockkUserViewModel(user: User? = null): com.ziro.fit.viewmodel.UserViewModel {
        val vm: com.ziro.fit.viewmodel.UserViewModel = mockk()
        every { vm.user } returns user
        every { vm.isLoading } returns false
        return vm
    }
}
