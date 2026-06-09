package com.ziro.fit.ui.components.maps

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap

/**
 * Remembers a MapView and wires its lifecycle (onCreate/onStart/onResume/onPause/onStop/onDestroy)
 * to the current Compose lifecycle owner. Prevents memory leaks and map recreation on recomposition.
 */
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    return mapView
}

/**
 * MapLibre-based map composable that replaces Google Maps.
 *
 * Uses [MapView] wrapped in [AndroidView] with lifecycle-aware management.
 * The map instance is exposed via [onMapReady] once the style has loaded.
 *
 * Style URLs (no API key required):
 * - "https://demotiles.maplibre.org/style.json" (MapLibre demo tiles)
 * - "https://tiles.openfreemap.org/styles/liberty" (OpenFreeMap)
 * - Style.MAPBOX_STREETS (built-in, works offline)
 * - Style.LIGHT
 *
 * Override at call site: styleUrl = BuildConfig.MAP_STYLE_URL
 */
@Composable
fun MapLibreMap(
    modifier: Modifier = Modifier,
    styleUrl: String = "https://tiles.openfreemap.org/styles/liberty",
    onMapClick: ((org.maplibre.android.geometry.LatLng) -> Unit)? = null,
    onMapReady: (MapLibreMap) -> Unit = {}
) {
    val mapView = rememberMapViewWithLifecycle()
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    map.setStyle(styleUrl) {
                        mapInstance = map
                        onMapReady(map)
                    }
                }
            }
        }
    )

    // Wire click listener reactively
    DisposableEffect(mapInstance, onMapClick) {
        val currentMap = mapInstance ?: return@DisposableEffect onDispose {}
        if (onMapClick != null) {
            val clickListener = MapLibreMap.OnMapClickListener { latLng ->
                onMapClick(latLng)
                true
            }
            currentMap.addOnMapClickListener(clickListener)
            onDispose {
                currentMap.removeOnMapClickListener(clickListener)
            }
        } else {
            // Clear any existing click listeners when onClick becomes null
            onDispose {}
        }
    }
}
