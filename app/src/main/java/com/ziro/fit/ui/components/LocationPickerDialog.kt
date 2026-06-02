package com.ziro.fit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.camera.CameraPosition
import com.ziro.fit.ui.components.maps.MapLibreMap as MapLibreMapView

@Composable
fun LocationPickerDialog(
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    onLocationSelected: (latitude: Double, longitude: Double, address: String) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultLocation = LatLng(
        initialLatitude ?: 51.5074,
        initialLongitude ?: -0.1278
    )

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedAddress by remember { mutableStateOf("") }
    var markerRef by remember { mutableStateOf<org.maplibre.android.annotations.Marker?>(null) }

    fun reverseGeocode(latLng: LatLng) {
        selectedAddress = "${latLng.latitude}, ${latLng.longitude}"
    }

    fun updateMarker(latLng: LatLng) {
        markerRef?.remove()
        markerRef = mapInstance?.addMarker(
            org.maplibre.android.annotations.MarkerOptions()
                .position(latLng)
                .title("Selected Location")
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Location",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    MapLibreMapView(
                        modifier = Modifier.fillMaxSize(),
                        styleUrl = "https://demotiles.maplibre.org/style.json",
                        onMapReady = { map ->
                            mapInstance = map
                            map.cameraPosition = CameraPosition.Builder()
                                .target(defaultLocation)
                                .zoom(12.0)
                                .build()
                        },
                        onMapClick = { latLng ->
                            selectedLocation = latLng
                            reverseGeocode(latLng)
                            updateMarker(latLng)
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (selectedLocation != null) {
                        Text(
                            text = "Selected: $selectedAddress",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        Text(
                            text = "Tap on the map to select your location",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                selectedLocation?.let { location ->
                                    onLocationSelected(
                                        location.latitude,
                                        location.longitude,
                                        selectedAddress
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = selectedLocation != null
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}
