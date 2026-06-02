package com.ziro.fit.ui.discovery.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziro.fit.model.ExploreCity
import com.ziro.fit.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityPickerSheet(
    selectedCity: ExploreCity?,
    cities: List<ExploreCity>,
    currentCityName: String?,
    onCitySelected: (ExploreCity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StrongSecondaryBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Text(
                text = "Select City",
                color = StrongTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )

            // Current Location row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCitySelected(
                            ExploreCity(
                                id = "current_location",
                                name = currentCityName ?: "Current Location",
                                isCurrentLocation = true
                            )
                        )
                        onDismiss()
                    }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "Current Location",
                    tint = StrongBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = currentCityName ?: "Current Location",
                    color = StrongTextPrimary,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                if (selectedCity?.isCurrentLocation == true) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = StrongBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            HorizontalDivider(color = StrongDivider)

            // Cities section header
            Text(
                text = "Cities",
                color = StrongTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            // City list
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cities) { city ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCitySelected(city)
                                onDismiss()
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = city.name,
                            color = StrongTextPrimary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (selectedCity?.id == city.id && !selectedCity.isCurrentLocation) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = StrongBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
