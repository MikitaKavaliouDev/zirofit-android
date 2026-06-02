package com.ziro.fit.ui.discovery

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ziro.fit.model.ExploreEvent
import com.ziro.fit.model.TrainerSummary
import com.ziro.fit.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class MapFilterMode(val label: String) {
    ALL("All"),
    TRAINERS("Trainers"),
    EVENTS("Events")
}

data class TrainerCluster(
    val latitude: Double,
    val longitude: Double,
    val trainers: List<TrainerSummary>
)

data class EventCluster(
    val latitude: Double,
    val longitude: Double,
    val events: List<ExploreEvent>
)

sealed class MapSelectedItem {
    data class TrainerCluster(val cluster: com.ziro.fit.ui.discovery.TrainerCluster) : MapSelectedItem()
    data class EventCluster(val cluster: com.ziro.fit.ui.discovery.EventCluster) : MapSelectedItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerMapScreen(
    trainers: List<TrainerSummary>,
    onTrainerClick: (String) -> Unit,
    events: List<ExploreEvent> = emptyList(),
    onEventClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Filter and search state
    var filterMode by remember { mutableStateOf(MapFilterMode.ALL) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<MapSelectedItem?>(null) }

    // Filter and search applied trainer list
    val filteredTrainers = remember(trainers, filterMode, searchQuery) {
        if (filterMode == MapFilterMode.EVENTS) {
            emptyList()
        } else {
            val query = searchQuery.trim().lowercase()
            if (query.isBlank()) trainers
            else trainers.filter { it.name.lowercase().contains(query) }
        }
    }

    // Filter and search applied event list
    val filteredEvents = remember(events, filterMode, searchQuery) {
        if (filterMode == MapFilterMode.TRAINERS) {
            emptyList()
        } else {
            val query = searchQuery.trim().lowercase()
            if (query.isBlank()) events
            else events.filter { it.title.lowercase().contains(query) }
        }
    }

    val clusters = remember(filteredTrainers) {
        filteredTrainers
            .mapNotNull { trainer ->
                trainer.profile?.locations?.firstOrNull()?.let { location ->
                    if (location.latitude != null && location.longitude != null) {
                        Triple(trainer, location.latitude, location.longitude)
                    } else null
                }
            }
            .groupBy { (_, lat, lng) ->
                "${lat.toBigDecimal().setScale(4, java.math.RoundingMode.HALF_UP)}_${lng.toBigDecimal().setScale(4, java.math.RoundingMode.HALF_UP)}"
            }
            .map { (_, items) ->
                val first = items.first()
                TrainerCluster(
                    latitude = first.second,
                    longitude = first.third,
                    trainers = items.map { it.first }
                )
            }
    }

    // Cluster events by location
    val eventClusters = remember(filteredEvents) {
        filteredEvents
            .mapNotNull { event ->
                event.latitude?.let { lat ->
                    event.longitude?.let { lng ->
                        Triple(event, lat, lng)
                    }
                }
            }
            .groupBy { (_, lat, lng) ->
                "${lat.toBigDecimal().setScale(4, java.math.RoundingMode.HALF_UP)}_${lng.toBigDecimal().setScale(4, java.math.RoundingMode.HALF_UP)}"
            }
            .map { (_, items) ->
                val first = items.first()
                EventCluster(
                    latitude = first.second,
                    longitude = first.third,
                    events = items.map { it.first }
                )
            }
    }

    val defaultLocation = LatLng(51.5074, -0.1278)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Cache for marker bitmaps
    val markerBitmapCache = remember { mutableStateMapOf<String, BitmapDescriptor>() }

    LaunchedEffect(selectedItem) {
        showBottomSheet = selectedItem != null
    }

    // Load marker bitmaps
    LaunchedEffect(clusters) {
        clusters.forEach { cluster ->
            val trainer = cluster.trainers.first()
            if (!markerBitmapCache.containsKey(trainer.id)) {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        createMarkerBitmap(context, trainer, cluster.trainers.size)
                    }
                    bitmap?.let {
                        markerBitmapCache[trainer.id] = BitmapDescriptorFactory.fromBitmap(it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val mapProperties = remember {
            MapProperties(mapType = MapType.NORMAL, isMyLocationEnabled = false)
        }
        val mapUiSettings = remember {
            MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = false
            )
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings
        ) {
            clusters.forEach { cluster ->
                val firstTrainer = cluster.trainers.first()
                val markerBitmap = markerBitmapCache[firstTrainer.id]

                Marker(
                    state = MarkerState(position = LatLng(cluster.latitude, cluster.longitude)),
                    title = if (cluster.trainers.size > 1) {
                        "${cluster.trainers.size} Trainers"
                    } else {
                        firstTrainer.name
                    },
                    snippet = firstTrainer.profile?.locations?.firstOrNull()?.address,
                    icon = markerBitmap ?: BitmapDescriptorFactory.defaultMarker(
                        if (cluster.trainers.size > 1) BitmapDescriptorFactory.HUE_VIOLET else BitmapDescriptorFactory.HUE_AZURE
                    ),
                    onClick = {
                        selectedItem = MapSelectedItem.TrainerCluster(cluster)
                        false
                    }
                )
            }

            // Event markers
            eventClusters.forEach { cluster ->
                val firstEvent = cluster.events.first()

                Marker(
                    state = MarkerState(position = LatLng(cluster.latitude, cluster.longitude)),
                    title = if (cluster.events.size > 1) {
                        "${cluster.events.size} Events"
                    } else {
                        firstEvent.title
                    },
                    snippet = firstEvent.locationName,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (cluster.events.size > 1) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_ORANGE
                    ),
                    onClick = {
                        selectedItem = MapSelectedItem.EventCluster(cluster)
                        false
                    }
                )
            }
        }

        // Top bar with search bar and filter controls
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            if (showSearchBar) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Search trainers or events...", color = StrongTextSecondary)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = StrongTextSecondary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = StrongSecondaryBackground,
                            unfocusedContainerColor = StrongSecondaryBackground,
                            focusedBorderColor = StrongBlue,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Filter button
                    Box {
                        FloatingActionButton(
                            onClick = { showFilterMenu = true },
                            modifier = Modifier.size(40.dp),
                            containerColor = StrongSecondaryBackground,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", modifier = Modifier.size(20.dp))
                        }

                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            containerColor = StrongSecondaryBackground
                        ) {
                            MapFilterMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = mode.label,
                                            color = if (filterMode == mode) StrongBlue else Color.White,
                                            fontWeight = if (filterMode == mode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        filterMode = mode
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search toggle button
                    FloatingActionButton(
                        onClick = { showSearchBar = true },
                        modifier = Modifier.size(40.dp),
                        containerColor = StrongSecondaryBackground,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
                    }

                    // Filter button
                    Box {
                        FloatingActionButton(
                            onClick = { showFilterMenu = true },
                            modifier = Modifier.size(40.dp),
                            containerColor = StrongSecondaryBackground,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", modifier = Modifier.size(20.dp))
                        }

                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            containerColor = StrongSecondaryBackground
                        ) {
                            MapFilterMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = mode.label,
                                            color = if (filterMode == mode) StrongBlue else Color.White,
                                            fontWeight = if (filterMode == mode) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        filterMode = mode
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showBottomSheet && selectedItem != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    selectedItem = null
                },
                sheetState = sheetState,
                containerColor = StrongSecondaryBackground,
                dragHandle = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.Gray.copy(alpha = 0.4f))
                        )
                    }
                }
            ) {
                when (val item = selectedItem) {
                    is MapSelectedItem.TrainerCluster -> {
                        val cluster = item.cluster
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (cluster.trainers.size > 1) {
                                        "${cluster.trainers.size} Specialists"
                                    } else {
                                        "Specialist"
                                    },
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        showBottomSheet = false
                                        selectedItem = null
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            cluster.trainers.forEach { trainer ->
                                TrainerListItem(
                                    trainer = trainer,
                                    onClick = {
                                        onTrainerClick(trainer.id)
                                        showBottomSheet = false
                                        selectedItem = null
                                    }
                                )
                            }
                        }
                    }
                    is MapSelectedItem.EventCluster -> {
                        val cluster = item.cluster
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (cluster.events.size > 1) {
                                        "${cluster.events.size} Events"
                                    } else {
                                        "Event"
                                    },
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = {
                                        showBottomSheet = false
                                        selectedItem = null
                                    }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            cluster.events.forEach { event ->
                                EventMapCard(
                                    event = event,
                                    onClick = {
                                        onEventClick(event.id)
                                        showBottomSheet = false
                                        selectedItem = null
                                    }
                                )
                            }
                        }
                    }
                    null -> {}
                }
            }
        }
    }
}

@Composable
private fun EventMapCard(
    event: ExploreEvent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = StrongBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StrongSecondaryBackground),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Time row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = StrongTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.startTime.take(16).replace("T", " "),
                        color = StrongTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Location row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = StrongTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = event.locationName,
                        color = StrongTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Capacity bar
                if (event.capacity != null && event.capacity > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val fillFraction = (event.enrolledCount ?: 0).toFloat() / event.capacity.toFloat()
                    val capacityColor = when {
                        fillFraction >= 0.9f -> StrongRed
                        fillFraction >= 0.7f -> ExploreOrange
                        else -> StrongBlue
                    }
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${event.enrolledCount ?: 0}/${event.capacity}",
                                color = capacityColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (event.spotsLeft in 1..5) {
                                Text(
                                    text = "Only ${event.spotsLeft} left!",
                                    color = SellingFastOrange,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { fillFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = capacityColor,
                            trackColor = CapacityBarBg
                        )
                    }
                }
            }
        }
    }
}

private suspend fun createMarkerBitmap(
    context: android.content.Context,
    trainer: TrainerSummary,
    count: Int?
): Bitmap? {
    return try {
        if (count != null && count > 1) {
            val combined = Bitmap.createBitmap(140, 150, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(combined)

            val bgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#9C27B0")
                isAntiAlias = true
            }
            val rect = android.graphics.RectF(0f, 0f, 140f, 150f)
            val radius = 20f
            canvas.drawRoundRect(rect, radius, radius, bgPaint)

            val circlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                isAntiAlias = true
            }
            canvas.drawCircle(70f, 75f, 30f, circlePaint)
            canvas.drawCircle(70f, 55f, 30f, circlePaint)
            canvas.drawCircle(70f, 35f, 30f, circlePaint)

            val badgePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#3B82F6")
                isAntiAlias = true
            }
            canvas.drawCircle(120f, 15f, 15f, badgePaint)

            val badgeTextPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 20f
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("$count", 120f, 20f, badgeTextPaint)

            combined
        } else {
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(trainer.profile?.profilePhotoPath)
                .size(120, 120)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val drawable = result.drawable
                val baseBitmap = drawable.toBitmap(120, 120)

                val combined = Bitmap.createBitmap(140, 150, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(combined)

                canvas.drawBitmap(baseBitmap, 10f, 0f, null)

                val ratingBg = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#1B2228")
                }
                canvas.drawRect(10f, 95f, 130f, 120f, ratingBg)

                val starPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#FFD700")
                    textSize = 20f
                }
                canvas.drawText("★", 15f, 112f, starPaint)

                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 22f
                    isFakeBoldText = true
                }
                canvas.drawText(String.format("%.1f", trainer.profile?.averageRating ?: 5.0), 32f, 112f, textPaint)

                combined
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
private fun TrainerListItem(
    trainer: TrainerSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = StrongBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = trainer.profile?.profilePhotoPath,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StrongSecondaryBackground),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trainer.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                trainer.profile?.certifications?.split(",")?.firstOrNull()?.let {
                    Text(
                        text = it.trim(),
                        color = StrongTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", trainer.profile?.averageRating ?: 5.0),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = StrongTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = trainer.profile?.locations?.firstOrNull()?.address ?: "Online",
                        color = StrongTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
