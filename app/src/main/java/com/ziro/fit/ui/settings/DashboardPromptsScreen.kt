package com.ziro.fit.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ziro.fit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardPromptsScreen(
    coachBannerEnabled: Boolean,
    checkInBannerEnabled: Boolean,
    onCoachBannerToggle: (Boolean) -> Unit,
    onCheckInBannerToggle: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Prompts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Need a Coach? banner toggle
            SettingsBannerToggle(
                icon = Icons.Default.SupportAgent,
                iconColor = StrongBlue,
                label = "Need a Coach?",
                description = "Show banner if no active trainer",
                checked = coachBannerEnabled,
                onCheckedChange = onCoachBannerToggle
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = StrongDivider
            )

            // Weekly Check-in banner toggle
            SettingsBannerToggle(
                icon = Icons.Default.CheckCircle,
                iconColor = StrongGreen,
                label = "Weekly Check-in",
                description = "Show banner for training updates",
                checked = checkInBannerEnabled,
                onCheckedChange = onCheckInBannerToggle
            )
        }
    }
}

@Composable
private fun SettingsBannerToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
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
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = StrongTextSecondary
            )
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
