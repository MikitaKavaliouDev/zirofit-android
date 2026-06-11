package com.ziro.fit.ui.settings

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ziro.fit.ui.theme.*

private data class PermissionItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val permissions = listOf(
        PermissionItem("Camera", Icons.Default.CameraAlt, "Not determined"),
        PermissionItem("Photos", Icons.Default.PhotoLibrary, "Not determined"),
        PermissionItem("Notifications", Icons.Default.Notifications, "Enabled"),
        PermissionItem("Location", Icons.Default.LocationOn, "While Using"),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
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
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage what Ziro Fit can access on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = StrongTextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    permissions.forEachIndexed { index, perm ->
                        PermissionRow(permission = perm)
                        if (index < permissions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = StrongDivider
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StrongBlue)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open System Settings", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Permission status shown is estimated. For precise control, use your device's system settings.",
                style = MaterialTheme.typography.bodySmall,
                color = StrongTextSecondary
            )
        }
    }
}

@Composable
private fun PermissionRow(permission: PermissionItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(StrongBlue.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = permission.icon,
                contentDescription = null,
                tint = StrongBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = permission.title,
            style = MaterialTheme.typography.bodyLarge,
            color = StrongTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = permission.status,
            style = MaterialTheme.typography.bodySmall,
            color = StrongTextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}
