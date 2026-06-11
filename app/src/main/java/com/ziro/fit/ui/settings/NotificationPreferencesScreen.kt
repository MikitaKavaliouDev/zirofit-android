package com.ziro.fit.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ziro.fit.ui.theme.*
import com.ziro.fit.viewmodel.NotificationPreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationPreferencesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // ── Channels ─────────────────────────────────────────────
            SectionHeader(text = "Channels")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose how you receive notifications",
                style = MaterialTheme.typography.bodySmall,
                color = StrongTextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground)
            ) {
                Column {
                    NotificationToggle(
                        title = "Push Notifications",
                        description = "Receive push notifications on your device",
                        checked = uiState.pushNotifications,
                        onCheckedChange = { viewModel.setPushNotifications(it) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = StrongDivider
                    )
                    NotificationToggle(
                        title = "Email Notifications",
                        description = "Receive email notifications",
                        checked = uiState.emailNotifications,
                        onCheckedChange = { viewModel.setEmailNotifications(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Activity ─────────────────────────────────────────────
            SectionHeader(text = "Activity")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage notifications for your activity",
                style = MaterialTheme.typography.bodySmall,
                color = StrongTextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground)
            ) {
                Column {
                    NotificationToggle(
                        title = "Workout Reminders",
                        description = "Get reminded about scheduled workouts",
                        checked = uiState.workoutReminders,
                        onCheckedChange = { viewModel.setWorkoutReminders(it) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = StrongDivider
                    )
                    NotificationToggle(
                        title = "Booking Updates",
                        description = "Updates about your bookings and sessions",
                        checked = uiState.bookingAlerts,
                        onCheckedChange = { viewModel.setBookingAlerts(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Cross-Profile Alerts ─────────────────────────────────
            SectionHeader(text = "Cross-Profile Alerts")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose whether to see notifications for your other profile while active in a different mode.",
                style = MaterialTheme.typography.bodySmall,
                color = StrongTextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground)
            ) {
                Column {
                    NotificationToggle(
                        title = "Trainer alerts in Client Mode",
                        description = "See trainer-related notifications while in client mode",
                        checked = uiState.trainerAlertsInClientMode,
                        onCheckedChange = { viewModel.setTrainerAlertsInClientMode(it) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = StrongDivider
                    )
                    NotificationToggle(
                        title = "Client alerts in Trainer Mode",
                        description = "See client-related notifications while in trainer mode",
                        checked = uiState.clientAlertsInTrainerMode,
                        onCheckedChange = { viewModel.setClientAlertsInTrainerMode(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = StrongTextSecondary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun NotificationToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = StrongTextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = StrongTextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
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
