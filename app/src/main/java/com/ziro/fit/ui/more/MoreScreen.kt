package com.ziro.fit.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziro.fit.model.AppMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    currentMode: AppMode,
    onModeSwitch: (AppMode) -> Unit,
    onNavigateToAssessments: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToCheckIns: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToMyEvents: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "App Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
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
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = currentMode == AppMode.PERSONAL,
                                onClick = { onModeSwitch(AppMode.PERSONAL) },
                                label = { Text(AppMode.PERSONAL.displayName, fontSize = 14.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
            item {
                MoreMenuItem(
                    icon = Icons.Default.Event,
                    title = "Explore Events",
                    subtitle = "Discover and join upcoming events",
                    onClick = onNavigateToEvents
                )
                HorizontalDivider()
            }
            item {
                MoreMenuItem(
                    icon = Icons.Default.Build,
                    title = "My Events",
                    subtitle = "Manage your created events",
                    onClick = onNavigateToMyEvents
                )
                HorizontalDivider()
            }
            item {
                MoreMenuItem(
                    icon = Icons.Default.Build,
                    title = "Assessments Library",
                    subtitle = "Manage custom assessment types",
                    onClick = onNavigateToAssessments
                )
                HorizontalDivider()
            }
            item {
                MoreMenuItem(
                    icon = Icons.Default.DateRange, // Reusing DateRange as it fits Bookings
                    title = "Bookings",
                    subtitle = "Manage your bookings",
                    onClick = onNavigateToBookings
                )
                HorizontalDivider()
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                MoreMenuItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Logout",
                    subtitle = "Sign out of your account",
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
