package com.ziro.fit

import android.app.Application
import com.ziro.fit.util.HapticManager
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import javax.inject.Inject

@HiltAndroidApp
class ZiroFitApp : Application() {
    @Inject
    lateinit var hapticManager: HapticManager

    companion object {
        var globalHapticManager: HapticManager? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize MapLibre (required before any MapView is created).
        // No API key needed — uses free MapLibre demo tiles.
        MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)
        globalHapticManager = hapticManager
    }
}
