package com.ziro.fit.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ziro.fit.model.AppMode
import com.ziro.fit.model.User
import com.ziro.fit.ui.theme.*
import com.ziro.fit.viewmodel.SettingsViewModel
import com.ziro.fit.viewmodel.UserViewModel

private data class SettingsSection(
    val header: String? = null,
    val items: List<SettingsItem>
)

private data class SettingsItem(
    val id: String,
    val icon: ImageVector? = null,
    val label: String,
    val subtitle: String? = null,
    val type: SettingsItemType,
    val isTrainerOnly: Boolean = false,
    val isClientOnly: Boolean = false
)

private enum class SettingsItemType {
    NAVIGATE,   // Arrow right chevron
    TOGGLE,     // Switch
    ACTION      // Button like sign out
}

private fun trainerPreferences(): List<SettingsItem> = listOf(
    SettingsItem("appearance", Icons.Default.Palette, "Appearance", "Theme, colors", SettingsItemType.NAVIGATE),
    SettingsItem("ai_coach", Icons.Default.SmartToy, "AI Coach Settings", "Voice coach configuration", SettingsItemType.NAVIGATE),
    SettingsItem("language", Icons.Default.Language, "Language", "App language", SettingsItemType.NAVIGATE),
    SettingsItem("notifications", Icons.Default.Notifications, "Notifications", "Push, email alerts", SettingsItemType.NAVIGATE),
    SettingsItem("permissions", Icons.Default.Lock, "Permissions", "System access", SettingsItemType.NAVIGATE),
    SettingsItem("data_privacy", Icons.Default.Shield, "Data & Privacy", "Client data sharing", SettingsItemType.NAVIGATE),
    SettingsItem("privacy_security", Icons.Default.Security, "Privacy & Security", "Account & trainer connection", SettingsItemType.NAVIGATE),
    SettingsItem("dashboard_prompts", Icons.Default.Campaign, "Dashboard Prompts", "Coach & check-in banners", SettingsItemType.NAVIGATE),
    SettingsItem("storefront", Icons.Default.Store, "Storefront", "Manage your storefront", SettingsItemType.NAVIGATE, isTrainerOnly = true),
    SettingsItem("subscription", Icons.Default.CardMembership, "Subscription", "Your plan & billing", SettingsItemType.NAVIGATE, isTrainerOnly = true),
)

private fun clientPreferences(): List<SettingsItem> = listOf(
    SettingsItem("appearance", Icons.Default.Palette, "Appearance", "Theme, colors", SettingsItemType.NAVIGATE),
    SettingsItem("ai_coach", Icons.Default.SmartToy, "AI Coach Settings", "Voice coach configuration", SettingsItemType.NAVIGATE),
    SettingsItem("language", Icons.Default.Language, "Language", "App language", SettingsItemType.NAVIGATE),
    SettingsItem("notifications", Icons.Default.Notifications, "Notifications", "Push, email alerts", SettingsItemType.NAVIGATE),
    SettingsItem("permissions", Icons.Default.Lock, "Permissions", "System access", SettingsItemType.NAVIGATE),
    SettingsItem("data_privacy", Icons.Default.Shield, "Data & Privacy", "Manage sharing with trainer", SettingsItemType.NAVIGATE, isClientOnly = true),
    SettingsItem("privacy_security", Icons.Default.Security, "Privacy & Security", "Account & trainer connection", SettingsItemType.NAVIGATE),
    SettingsItem("dashboard_prompts", Icons.Default.Campaign, "Dashboard Prompts", "Coach & check-in banners", SettingsItemType.NAVIGATE),
)

private fun businessItems(): List<SettingsItem> = listOf(
    SettingsItem("qr_code", Icons.Default.QrCodeScanner, "QR Business Card", "Share your profile", SettingsItemType.NAVIGATE, isTrainerOnly = true),
    SettingsItem("assessments", Icons.Default.Assessment, "Assessments", "Manage assessment types", SettingsItemType.NAVIGATE, isTrainerOnly = true),
    SettingsItem("custom_exercises", Icons.Default.FitnessCenter, "Custom Exercises", "Your exercise library", SettingsItemType.NAVIGATE, isTrainerOnly = true),
    SettingsItem("contact_support", Icons.Default.ContactSupport, "Contact Support", "Get help", SettingsItemType.NAVIGATE),
    SettingsItem("whats_new", Icons.Default.AutoAwesome, "What's New", "Latest features", SettingsItemType.NAVIGATE),
    SettingsItem("getting_started_guide", Icons.Default.Explore, "Getting Started Guide", "Learn the app", SettingsItemType.NAVIGATE),
)

private fun clientResourcesItems(): List<SettingsItem> = listOf(
    SettingsItem("my_packages", Icons.Default.Inventory, "My Packages", "Your purchased packages", SettingsItemType.NAVIGATE, isClientOnly = true),
    SettingsItem("purchase_history", Icons.Default.Receipt, "Purchase History", "Past transactions", SettingsItemType.NAVIGATE, isClientOnly = true),
    SettingsItem("contact_support", Icons.Default.ContactSupport, "Contact Support", "Get help", SettingsItemType.NAVIGATE),
    SettingsItem("whats_new", Icons.Default.AutoAwesome, "What's New", "Latest features", SettingsItemType.NAVIGATE),
    SettingsItem("getting_started_guide", Icons.Default.Explore, "Getting Started Guide", "Learn the app", SettingsItemType.NAVIGATE),
)

private fun legalItems(): List<SettingsItem> = listOf(
    SettingsItem("terms", Icons.Default.Description, "Terms of Service", null, SettingsItemType.NAVIGATE),
    SettingsItem("privacy", Icons.Default.PrivacyTip, "Privacy Policy", null, SettingsItemType.NAVIGATE),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentMode: AppMode,
    onModeSwitch: (AppMode) -> Unit,
    onNavigateToSubScreen: (route: String) -> Unit,
    onLogout: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val user = userViewModel.user
    val isLoading = userViewModel.isLoading
    val isTrainer = user?.role == "trainer" || currentMode == AppMode.TRAINER

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                ProfileHeader(
                    user = user,
                    isTrainer = isTrainer,
                    onClick = { onNavigateToSubScreen("profile") },
                    onQrClick = {
                        if (isTrainer) onNavigateToSubScreen("qr_code")
                    }
                )
            }

            item {
                AppModeSelector(
                    currentMode = currentMode,
                    onModeSwitch = onModeSwitch
                )
            }

            item {
                SectionHeader("Preferences")
            }

            val prefsItems = if (isTrainer) trainerPreferences() else clientPreferences()
            prefsItems.forEach { item ->
                item {
                    SettingsNavItem(
                        item = item,
                        onClick = { onNavigateToSubScreen("settings/${item.id}") }
                    )
                }
            }

            item {
                SettingsToggleItem(
                    icon = Icons.Default.SwapHoriz,
                    label = "Custom App Mode",
                    subtitle = "Enable manual Professional/Personal switching",
                    checked = uiState.isCustomModeEnabled,
                    onCheckedChange = { settingsViewModel.setCustomModeEnabled(it) }
                )
            }

            item {
                SectionHeader("Experimental")
            }

            item {
                SettingsToggleItem(
                    icon = Icons.Default.Today,
                    label = "Daily Targets",
                    subtitle = "Set and track daily workout goals",
                    checked = uiState.dailyTargetsEnabled,
                    onCheckedChange = { settingsViewModel.setDailyTargetsEnabled(it) }
                )
            }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.Mic,
                    label = "Voice Feedback",
                    subtitle = "Voice announcements during workouts",
                    checked = uiState.voiceFeedbackEnabled,
                    onCheckedChange = { settingsViewModel.setVoiceFeedbackEnabled(it) }
                )
            }
            item {
                SettingsToggleItem(
                    icon = Icons.Default.Repeat,
                    label = "Routines",
                    subtitle = "Structured workout routines",
                    checked = uiState.routinesEnabled,
                    onCheckedChange = { settingsViewModel.setRoutinesEnabled(it) }
                )
            }

            val resourceItems = if (isTrainer) businessItems() else clientResourcesItems()
            if (resourceItems.isNotEmpty()) {
                item {
                    SectionHeader(if (isTrainer) "Business" else "Resources")
                }
                resourceItems.forEach { item ->
                    item {
                        SettingsNavItem(
                            item = item,
                            onClick = {
                                val route = when (item.id) {
                                    "assessments" -> "assessments_library"
                                    "custom_exercises" -> "exercises_library"
                                    "contact_support" -> "settings/${item.id}"
                                    "my_packages" -> "profile/packages"
                                    "purchase_history" -> "settings/${item.id}"
                                    "qr_code" -> "settings/${item.id}"
                                    else -> "settings/${item.id}"
                                }
                                onNavigateToSubScreen(route)
                            }
                        )
                    }
                }
            }

            if (isTrainer) {
                item {
                    SettingsNavItem(
                        item = SettingsItem("payouts", Icons.Default.AccountBalance, "Payouts", "Stripe Connect", SettingsItemType.NAVIGATE),
                        onClick = { onNavigateToSubScreen("profile/payouts") }
                    )
                }
            }

            item {
                SectionHeader("Legal")
            }
            legalItems().forEach { item ->
                item {
                    SettingsNavItem(
                        item = item,
                        onClick = { onNavigateToSubScreen("settings/${item.id}") }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SignOutButton(onClick = onLogout)
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                BrandFooter(onNavigateToSubScreen = onNavigateToSubScreen)
            }

            // Bottom spacer for nav bar
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    user: User?,
    isTrainer: Boolean,
    onClick: () -> Unit,
    onQrClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (user?.profilePhotoPath != null) {
                AsyncImage(
                    model = user.profilePhotoPath,
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isTrainer) StrongBlue.copy(alpha = 0.2f)
                            else StrongGreen.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (user?.name?.firstOrNull()?.uppercase() ?: "U"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isTrainer) StrongBlue else StrongGreen
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.name ?: user?.email ?: "User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = StrongTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (user?.name != null && user.email != null) {
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = StrongTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isTrainer) StrongBlue.copy(alpha = 0.15f) else StrongGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isTrainer) "PRO" else "PERSONAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isTrainer) StrongBlue else StrongGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (isTrainer) {
                IconButton(onClick = onQrClick) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "QR Code",
                        tint = StrongBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = StrongTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AppModeSelector(
    currentMode: AppMode,
    onModeSwitch: (AppMode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "App Mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = StrongTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentMode == AppMode.TRAINER,
                    onClick = { onModeSwitch(AppMode.TRAINER) },
                    label = { Text(AppMode.TRAINER.displayName, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StrongBlue.copy(alpha = 0.2f),
                        selectedLabelColor = StrongBlue
                    )
                )
                FilterChip(
                    selected = currentMode == AppMode.PERSONAL,
                    onClick = { onModeSwitch(AppMode.PERSONAL) },
                    label = { Text(AppMode.PERSONAL.displayName, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StrongGreen.copy(alpha = 0.2f),
                        selectedLabelColor = StrongGreen
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = StrongTextSecondary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsNavItem(
    item: SettingsItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(StrongSecondaryBackground, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = StrongBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge,
                color = StrongTextPrimary
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StrongTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = StrongTextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(StrongSecondaryBackground, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StrongBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = StrongTextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StrongTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = StrongBlue,
                checkedTrackColor = StrongBlue.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun SignOutButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(StrongRed.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Sign Out",
                tint = StrongRed,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "Sign Out",
            style = MaterialTheme.typography.bodyLarge,
            color = StrongRed,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BrandFooter(
    onNavigateToSubScreen: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = StrongTextSecondary.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { onNavigateToSubScreen("settings/acknowledgements") },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Acknowledgements",
                    style = MaterialTheme.typography.labelSmall,
                    color = StrongBlue.copy(alpha = 0.8f)
                )
            }

            Text(
                text = "\u2022",
                color = StrongTextSecondary.copy(alpha = 0.3f),
                style = MaterialTheme.typography.labelSmall
            )

            TextButton(
                onClick = { onNavigateToSubScreen("settings/terms") },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Terms",
                    style = MaterialTheme.typography.labelSmall,
                    color = StrongBlue.copy(alpha = 0.8f)
                )
            }

            Text(
                text = "\u2022",
                color = StrongTextSecondary.copy(alpha = 0.3f),
                style = MaterialTheme.typography.labelSmall
            )

            TextButton(
                onClick = { onNavigateToSubScreen("settings/privacy") },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.labelSmall,
                    color = StrongBlue.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "\u00A9 2026 Ziro Fit. All rights reserved.",
            style = MaterialTheme.typography.bodySmall,
            color = StrongTextSecondary.copy(alpha = 0.4f)
        )
    }
}
