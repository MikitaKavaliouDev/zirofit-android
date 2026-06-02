package com.ziro.fit.ui.discovery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziro.fit.model.ExploreCity
import com.ziro.fit.ui.theme.*

@Composable
fun ExploreCityHeader(
    selectedCity: ExploreCity?,
    userLocationCity: String?,
    onCityTap: () -> Unit,
    onSearchTap: () -> Unit,
    onMapTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrentLocation = selectedCity?.isCurrentLocation == true

    val displayName = when {
        isCurrentLocation -> userLocationCity ?: "Locating..."
        selectedCity != null -> selectedCity.name
        else -> "Select City"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(StrongSecondaryBackground.copy(alpha = 0.95f))
            .padding(top = 6.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left section — city button (entire left area is tappable)
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onCityTap() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Location / Pin circle icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentLocation) StrongBlue.copy(alpha = 0.12f)
                        else StrongRed.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrentLocation) Icons.Filled.MyLocation else Icons.Filled.Place,
                    contentDescription = if (isCurrentLocation) "Current location" else "City",
                    tint = if (isCurrentLocation) StrongBlue else StrongRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            // City name + subtitle
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayName,
                        color = StrongTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Select city",
                        tint = StrongTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = if (isCurrentLocation) "Current Location" else "Active Region",
                    color = StrongTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Right section — search + map icons
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = onSearchTap) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = StrongTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onMapTap) {
                Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = "Map",
                    tint = StrongTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
