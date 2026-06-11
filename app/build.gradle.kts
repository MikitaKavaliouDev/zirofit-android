
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.ziro.fit"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.ziro.fit"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Environment config — override via local.properties or CI env vars
        val baseUrl = project.findProperty("BASE_URL") as String? ?: "http://10.0.2.2:3321"
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        val supabaseUrl = project.findProperty("SUPABASE_URL") as String? ?: "http://10.0.2.2:54321"
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        val supabaseKey = project.findProperty("SUPABASE_KEY") as String? ?: "sb_publishable_ACJWlzQHlZjBrEguHvfOxg_3BJgxAaH"
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
        val mapStyleUrl = project.findProperty("MAP_STYLE_URL") as String? ?: "https://tiles.openfreemap.org/styles/liberty"
        buildConfigField("String", "MAP_STYLE_URL", "\"$mapStyleUrl\"")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        testInstrumentationRunnerArguments += mapOf(
            "E2E_TRAINER_EMAIL" to (project.findProperty("E2E_TRAINER_EMAIL") as String? ?: "e2e-test-trainer@ziro.fit"),
            "E2E_TRAINER_PASSWORD" to (project.findProperty("E2E_TRAINER_PASSWORD") as String? ?: "test-password-123"),
            "E2E_CLIENT_EMAIL" to (project.findProperty("E2E_CLIENT_EMAIL") as String? ?: "calendar.client@test.com"),
            "E2E_CLIENT_PASSWORD" to (project.findProperty("E2E_CLIENT_PASSWORD") as String? ?: "test-password-123"),
        )
    }
    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Secure Storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.compiler)

    // E2E Testing - Kaspresso (UI Automator + Compose support)
    androidTestImplementation("com.kaspersky.android-components:kaspresso:1.5.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // MapLibre (Open-source map SDK, no API key required)
    implementation("org.maplibre.gl:android-sdk:11.5.2")

    // Location
    implementation(libs.play.services.location)

    // Supabase

    // The bom is not in libs.toml, so I'll just use individual implementations as defined in libs.toml
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.okhttp)
    
    // Firebase
    implementation(libs.firebase.messaging)

    // Browser
    implementation(libs.androidx.browser)

    // QR Code
    implementation(libs.zxing.core)

    // ElevenLabs Conversational AI SDK
    implementation("io.elevenlabs:elevenlabs-android:0.9.0")
}
      