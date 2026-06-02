package com.ziro.fit.ui.discovery

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ziro.fit.model.*
import com.ziro.fit.ui.discovery.components.*
import com.ziro.fit.ui.theme.*
import com.ziro.fit.viewmodel.ExploreViewModel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onNavigateToEvent: (String) -> Unit,
    onNavigateToTrainer: (String) -> Unit,
    onNavigateToMap: () -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showCityPicker by remember { mutableStateOf(false) }
    var exploreTab by remember { mutableStateOf(ExploreTab.Trainers) }

    // ── Location permission + device location ──
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            scope.launch { fetchDeviceLocation(context, viewModel) }
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            locationPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            fetchDeviceLocation(context, viewModel)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StrongBackground)
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refreshContent,
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isLoading && uiState.featuredTrainers.isEmpty() && uiState.featuredEvents.isEmpty()
                && uiState.nearbyTrainers.isEmpty() && uiState.sortedDateKeys.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StrongBlue)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 75.dp, bottom = 100.dp)
                ) {
                // ── Sliding Segmented Control ──
                item {
                    ExploreSlidingSegment(
                        selectedTab = exploreTab,
                        onTabSelected = { exploreTab = it },
                        modifier = Modifier.padding(top = 10.dp, bottom = 16.dp)
                    )
                }

                // ── TRAINERS TAB ──
                if (exploreTab == ExploreTab.Trainers) {
                    // 1. Spotlight Specialist Hero Card
                    val spotlightTrainer = uiState.featuredTrainers.firstOrNull()
                        ?: uiState.nearbyTrainers.firstOrNull()
                    if (spotlightTrainer != null) {
                        item {
                            SectionHeaderWithAction(
                                title = "Featured Specialist",
                                onSeeAllClick = onNavigateToMap
                            )
                        }
                        item {
                            TrainerSpotlightHeroCard(
                                trainer = spotlightTrainer,
                                onClick = { onNavigateToTrainer(spotlightTrainer.id) },
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                        }
                    }

                    // 2. Browse by Category + Trending Tags
                    if (uiState.categories.isNotEmpty()) {
                        item {
                            Text(
                                "Browse by Category",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.categories) { category ->
                                    FilterChip(
                                        selected = uiState.selectedCategory?.id == category.id,
                                        onClick = { viewModel.selectCategory(category) },
                                        label = { Text(category.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = StrongBlue,
                                            labelColor = Color.White,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                        item {
                            TrendingTagsRow(
                                onTagClick = { onNavigateToMap() },
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }

                    // 3. Trainers Near You
                    if (uiState.nearbyTrainers.isNotEmpty()) {
                        item {
                            HStack {
                                Text(
                                    "Trainers Near You",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = onNavigateToMap) {
                                    Text("See All", color = StrongBlue, fontSize = 14.sp)
                                }
                            }
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.nearbyTrainers) { trainer ->
                                    InteractiveTrainerCard(
                                        trainer = trainer,
                                        onClick = { onNavigateToTrainer(trainer.id) }
                                    )
                                }
                            }
                        }
                    }

                    // 4. Featured Trainers
                    if (uiState.featuredTrainers.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeaderWithAction(
                                title = "Featured Trainers",
                                onSeeAllClick = onNavigateToMap
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.featuredTrainers) { trainer ->
                                    InteractiveTrainerCard(
                                        trainer = trainer,
                                        onClick = { onNavigateToTrainer(trainer.id) }
                                    )
                                }
                            }
                        }
                    }

                    // 5. Ziro Recommends
                    if (uiState.recommendedTrainers.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionHeaderWithAction(
                                title = "Ziro Recommends",
                                onSeeAllClick = onNavigateToMap
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.recommendedTrainers) { trainer ->
                                    InteractiveTrainerCard(
                                        trainer = trainer,
                                        onClick = { onNavigateToTrainer(trainer.id) }
                                    )
                                }
                            }
                        }
                    }

                    // 6. Map Spotlight Preview Card
                    val totalCount = uiState.nearbyTrainers.size + uiState.featuredTrainers.size
                    if (totalCount > 0) {
                        item {
                            MapSpotlightPreviewCard(
                                trainerCount = totalCount,
                                onOpenMap = onNavigateToMap,
                                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                            )
                        }
                    }
                }

                // ── EVENTS TAB ──
                if (exploreTab == ExploreTab.Events) {
                    // 1. Featured Events Carousel
                    if (uiState.featuredEvents.isNotEmpty()) {
                        item {
                            SectionHeaderWithAction(
                                title = "Featured Events",
                                onSeeAllClick = { }
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.featuredEvents) { event ->
                                    InteractiveEventCard(
                                        event = event,
                                        onClick = { onNavigateToEvent(event.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Category Filter
                    if (uiState.categories.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.categories) { category ->
                                    FilterChip(
                                        selected = uiState.selectedCategory?.id == category.id,
                                        onClick = { viewModel.selectCategory(category) },
                                        label = { Text(category.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = StrongBlue,
                                            labelColor = Color.White,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 2. Upcoming Events (grouped by date) / Empty State
                    if (uiState.upcomingEvents.isEmpty() && !uiState.isLoading) {
                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                            ExploreEmptyEventsView(
                                isLoading = uiState.isSubscribing,
                                onNotifyMe = { viewModel.subscribeToEventNotifications() }
                            )
                        }
                    } else {
                        val dateKeys = uiState.sortedDateKeys.ifEmpty {
                            uiState.upcomingEvents.keys.sorted()
                        }
                        dateKeys.forEach { date ->
                            stickyHeader {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(StrongBackground)
                                        .padding(16.dp, 8.dp)
                                ) {
                                    Text(
                                        formatDateHeader(date),
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            uiState.upcomingEvents[date]?.let { events ->
                                items(events) { event ->
                                    CompactEventCard(
                                        event = event,
                                        onClick = { onNavigateToEvent(event.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Close PullToRefreshBox content ──
        }

        // ── Floating City Header ──
        ExploreCityHeader(
            selectedCity = uiState.selectedCity,
            userLocationCity = uiState.userLocationCity,
            onCityTap = { showCityPicker = true },
            onSearchTap = onNavigateToMap,
            onMapTap = onNavigateToMap,
            modifier = Modifier
                .fillMaxWidth()
                .background(StrongSecondaryBackground.copy(alpha = 0.95f))
                .statusBarsPadding()
        )

    }

    // ── City Picker Sheet ──
    if (showCityPicker) {
        CityPickerSheet(
            selectedCity = uiState.selectedCity,
            cities = uiState.cities,
            currentCityName = uiState.userLocationCity,
            onCitySelected = { city ->
                viewModel.selectCity(city)
                showCityPicker = false
            },
            onDismiss = { showCityPicker = false }
        )
    }
}

// ── Shared Card Composables ──

@Composable
fun SectionHeaderWithAction(title: String, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onSeeAllClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "See All",
                color = StrongBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = StrongBlue,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun HStack(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(modifier = modifier, content = content)
}

@Composable
fun InteractiveTrainerCard(trainer: TrainerSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = trainer.profile?.profilePhotoPath,
                    contentDescription = trainer.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    StrongSecondaryBackground.copy(alpha = 0.8f)
                                ),
                                startY = 100f
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        String.format("%.1f", trainer.profile?.averageRating ?: 5.0),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (trainer.username != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(StrongBlue, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    trainer.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    trainer.profile?.certifications?.split(",")?.firstOrNull()?.trim() ?: "Pro Trainer",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                trainer.profile?.certifications?.let { certs ->
                    if (certs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            certs.split(",").firstOrNull()?.trim() ?: "Certified Trainer",
                            color = StrongBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveEventCard(event: ExploreEvent, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = event.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )

            event.priceDisplay?.let { price ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(StrongGreen, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(price, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    event.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(event.locationName, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = StrongBlue, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(formatEventDateTime(event.startTime), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            if (event.spotsLeft > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(if (event.isNearCapacity == true) StrongRed else Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("${event.spotsLeft} spots left", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun CompactEventCard(event: ExploreEvent, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(event.locationName, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = StrongBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(formatEventDateTime(event.startTime), color = Color.Gray, fontSize = 12.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                event.priceDisplay?.let { price ->
                    Text(price, color = StrongGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                if (event.spotsLeft > 0) {
                    Text("${event.spotsLeft} left", color = if (event.isNearCapacity == true) StrongRed else Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun CompactTrainerCard(trainer: TrainerSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = trainer.profile?.profilePhotoPath,
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(trainer.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 14.sp)
                trainer.profile?.certifications?.split(",")?.firstOrNull()?.let {
                    Text(it.trim(), color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(String.format("%.1f", trainer.profile?.averageRating ?: 5.0), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.LocationOn, null, tint = StrongTextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(trainer.profile?.locations?.firstOrNull()?.address ?: "Online", color = StrongTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = StrongTextSecondary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = StrongTextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
}

private fun formatDateHeader(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val month = monthNames[parts[1].toInt() - 1]
            "$month ${parts[2]}, ${parts[0]}"
        } else dateStr
    } catch (e: Exception) { dateStr }
}

private fun formatEventDateTime(dateTime: String): String {
    return try {
        val formatted = dateTime.take(16).replace("T", " ")
        formatted.substringAfter(" ")
    } catch (e: Exception) { dateTime }
}

/**
 * Fetches the device's last known location via FusedLocationProviderClient.
 * On success, passes lat/lng + reverse-geocoded city name to [ExploreViewModel.updateLocation].
 * Safe to call even without location permission — silently returns on [SecurityException].
 */
private suspend fun fetchDeviceLocation(context: Context, viewModel: ExploreViewModel) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
            fusedLocationClient.lastLocation
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        }
        if (location != null) {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = try {
                geocoder.getFromLocation(location.latitude, location.longitude, 1)
            } catch (e: Exception) { null }
            val cityName = addresses?.firstOrNull()?.locality
            viewModel.updateLocation(location.latitude, location.longitude, cityName)
        }
    } catch (_: SecurityException) {
        // Permission was revoked after check — silently ignore
    }
}
