package com.ziro.fit.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ziro.fit.ui.theme.*

data class StorefrontMenuItem(
    val id: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val route: String
)

private val storefrontMenuItems = listOf(
    StorefrontMenuItem("profile", Icons.Default.Person, "Profile Page", "Edit your trainer profile", "profile/core_info"),
    StorefrontMenuItem("services", Icons.Default.MiscellaneousServices, "Services", "Manage offered services", "profile/services"),
    StorefrontMenuItem("packages", Icons.Default.Inventory, "Packages", "Manage training packages", "profile/packages"),
    StorefrontMenuItem("branding", Icons.Default.Palette, "Branding", "Colors, logo, banner", "profile/branding"),
    StorefrontMenuItem("availability", Icons.Default.DateRange, "Availability", "Set working hours", "profile/availability"),
    StorefrontMenuItem("testimonials", Icons.Default.Star, "Testimonials", "Client reviews", "profile/testimonials"),
    StorefrontMenuItem("transformation_photos", Icons.Default.PhotoLibrary, "Transformation Photos", "Before/after results", "profile/transformation_photos"),
    StorefrontMenuItem("social_links", Icons.Default.Share, "Social Links", "Connect social media", "profile/social_links"),
    StorefrontMenuItem("external_links", Icons.Default.Link, "External Links", "Add external resources", "profile/external_links"),
    StorefrontMenuItem("benefits", Icons.Default.Task, "Benefits", "List your benefits", "profile/benefits"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorefrontSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubScreen: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storefront") },
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
            Text(
                text = "Manage your public trainer profile and offerings",
                style = MaterialTheme.typography.bodySmall,
                color = StrongTextSecondary,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
            )

            storefrontMenuItems.forEach { item ->
                StorefrontNavItem(
                    icon = item.icon,
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = { onNavigateToSubScreen(item.route) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = StrongDivider
                )
            }
        }
    }
}

@Composable
private fun StorefrontNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
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
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = StrongTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = StrongTextSecondary
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = StrongTextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
