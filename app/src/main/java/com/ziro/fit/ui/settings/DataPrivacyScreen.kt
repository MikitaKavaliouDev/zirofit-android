package com.ziro.fit.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ziro.fit.ui.theme.*
import com.ziro.fit.viewmodel.DataPrivacyViewModel

private val RETENTION_OPTIONS = listOf(90, 180, 365, 730, 0)

private fun retentionLabel(days: Int): String = when (days) {
    90 -> "3 Months"
    180 -> "6 Months"
    365 -> "1 Year"
    730 -> "2 Years"
    0 -> "Indefinitely"
    else -> "$days days"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataPrivacyScreen(
    onNavigateBack: () -> Unit,
    viewModel: DataPrivacyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val prefs = uiState.preferences
    var retentionExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data & Privacy") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StrongBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                DataPrivacySectionHeader("Data Shared with Trainer")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your trainer needs access to your workout history to create and adjust your training programs. Other data is optional.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StrongTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Workout History (required)
                DataPrivacyToggle(
                    title = "Workout History",
                    description = "Exercises, sets, reps, weights, and session completion",
                    isOn = prefs.shareWorkoutHistory,
                    onCheckedChange = { enabled -> viewModel.setShareWorkoutHistory(enabled) },
                    required = true
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = StrongDivider
                )

                DataPrivacyToggle(
                    title = "Body Measurements",
                    description = "Weight, height, and body stats tracked in check-ins",
                    isOn = prefs.shareBodyMeasurements,
                    onCheckedChange = { enabled -> viewModel.setShareBodyMeasurements(enabled) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = StrongDivider
                )

                DataPrivacyToggle(
                    title = "Check-in Notes",
                    description = "Mood, energy levels, sleep quality, and personal notes",
                    isOn = prefs.shareCheckinNotes,
                    onCheckedChange = { enabled -> viewModel.setShareCheckinNotes(enabled) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                DataPrivacySectionHeader("Data Retention")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (prefs.dataRetentionDays == 0)
                        "Your data is kept indefinitely."
                    else
                        "Training data older than ${prefs.dataRetentionDays} days will be automatically deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StrongTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Retention Picker
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground)
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        RETENTION_OPTIONS.forEach { days ->
                            val isSelected = prefs.dataRetentionDays == days
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setDataRetentionDays(days) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setDataRetentionDays(days) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = StrongBlue,
                                        unselectedColor = StrongTextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = retentionLabel(days),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) StrongTextPrimary else StrongTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Links ──────────────────────────────────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground)
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        TextButton(
                            onClick = { /* TODO: Open URL */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Privacy Policy",
                                color = StrongBlue,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = StrongDivider
                        )
                        TextButton(
                            onClick = { /* TODO: Open URL */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "How Your Data Is Used",
                                color = StrongBlue,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Save Button ────────────────────────────────────────────
                Text(
                    text = "Changes take effect immediately. Your trainer will only see data you've chosen to share.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StrongTextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.savePreferences() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp),
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = StrongBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = StrongTextPrimary
                        )
                    } else {
                        Text("Save Settings", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Success message
                if (uiState.success) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Settings saved successfully!",
                        style = MaterialTheme.typography.bodySmall,
                        color = StrongGreen,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Error message
                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = StrongRed,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ─── Data Privacy Toggle ──────────────────────────────────────────────────────

@Composable
private fun DataPrivacySectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = StrongTextSecondary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun DataPrivacyToggle(
    title: String,
    description: String,
    isOn: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    required: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = StrongTextPrimary
                )
                if (required) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StrongBlue.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Required",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StrongBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = StrongTextSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (required) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Required",
                tint = StrongTextSecondary.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )
        } else {
            Switch(
                checked = isOn,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = StrongBlue,
                    checkedTrackColor = StrongBlue.copy(alpha = 0.3f)
                )
            )
        }
    }
}
