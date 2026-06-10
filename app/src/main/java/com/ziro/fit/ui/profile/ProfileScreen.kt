package com.ziro.fit.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.ziro.fit.model.BookingWindowSettings
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ziro.fit.viewmodel.ProfileViewModel

data class ProfileMenuItem(
    val title: String,
    val description: String,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isTrainer: Boolean = false,
    onLogout: () -> Unit,
    onNavigateToSubScreen: (String) -> Unit,
    onSaveBookingSettings: ((Int, Int) -> Unit)? = null,
    initialBookingWindowSettings: BookingWindowSettings? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val menuItems = listOf(
        ProfileMenuItem("Core Info", "Manage your personal details", "profile/core_info"),
        ProfileMenuItem("Branding", "Customize your app appearance", "profile/branding"),
        ProfileMenuItem("Services", "Manage your offered services", "profile/services"),
        ProfileMenuItem("Packages", "Manage your packages", "profile/packages"),
        ProfileMenuItem("Availability", "Set your working hours", "profile/availability"),
        ProfileMenuItem("Transformation Photos", "Showcase client results", "profile/transformation_photos"),
        ProfileMenuItem("Testimonials", "Manage client reviews", "profile/testimonials"),
        ProfileMenuItem("Social Links", "Connect your social media", "profile/social_links"),
        ProfileMenuItem("External Links", "Add external resources", "profile/external_links"),
        ProfileMenuItem("Billing", "Manage subscription and payments", "profile/billing"),
        ProfileMenuItem("Payouts", "Manage Stripe Connect", "profile/payouts"),
        ProfileMenuItem("Revenue", "View your earnings", "profile/revenue"),
        ProfileMenuItem("Benefits", "List your service benefits", "profile/benefits"),
        ProfileMenuItem("Notifications", "View your notifications", "profile/notifications")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(menuItems) { item ->
                ListItem(
                    headlineContent = { Text(item.title) },
                    supportingContent = { Text(item.description) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToSubScreen(item.route) }
                )
                Divider()
            }
            if (isTrainer) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val initialAdvance = (initialBookingWindowSettings?.advanceNoticeHours?.toFloat() ?: 24f)
                        val initialHorizon = (initialBookingWindowSettings?.bookingHorizonHours?.toFloat() ?: 168f)
                        var advanceNotice by remember { mutableStateOf(initialAdvance) }
                        var horizon by remember { mutableStateOf(initialHorizon) }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Booking Settings", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(text = "Advance Notice: ${advanceNotice.toInt()} hours")
                            Slider(
                                value = advanceNotice,
                                onValueChange = { advanceNotice = it },
                                valueRange = 1f..72f,
                                steps = 71
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(text = "Booking Horizon: ${horizon.toInt()} hours")
                            Slider(
                                value = horizon,
                                onValueChange = { horizon = it },
                                valueRange = 1f..168f,
                                steps = 167
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(onClick = {
                                    onSaveBookingSettings?.invoke(advanceNotice.toInt(), horizon.toInt())
                                }) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }

            item {
                val uiState = viewModel.uiState.collectAsState().value
                var showDeleteConfirm by remember { mutableStateOf(false) }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("Delete Account") },
                        text = {
                            Text("This will permanently delete your account and all associated data. This action cannot be undone.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteConfirm = false
                                    viewModel.deleteAccount(onSuccess = onLogout)
                                },
                                enabled = !uiState.deleteAccountLoading
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (uiState.deleteAccountError != null) {
                    Text(
                        text = uiState.deleteAccountError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    enabled = !uiState.deleteAccountLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (uiState.deleteAccountLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account")
                }
            }
        }
    }
}
