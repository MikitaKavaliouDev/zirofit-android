package com.ziro.fit.ui.discovery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ziro.fit.model.ExploreEvent
import com.ziro.fit.ui.theme.*
import com.ziro.fit.viewmodel.EventsViewModel
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EventsListScreen(
    onBack: () -> Unit,
    onNavigateToEvent: (String) -> Unit,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore Events", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StrongBackground
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Open filters */ }) {
                        Icon(Icons.Default.FilterAlt, contentDescription = "Filter", tint = StrongBlue)
                    }
                }
            )
        },
        containerColor = StrongBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            TextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search events...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = StrongSecondaryBackground,
                    unfocusedContainerColor = StrongSecondaryBackground,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = StrongBlue,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Row: Category Chips + Free Only toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryChip("All", uiState.selectedCategory == null) { viewModel.onCategorySelected(null) }
                CategoryChip("Workouts", uiState.selectedCategory == "workouts") { viewModel.onCategorySelected("workouts") }
                CategoryChip("Seminars", uiState.selectedCategory == "seminars") { viewModel.onCategorySelected("seminars") }
                Spacer(modifier = Modifier.weight(1f))
                FreeOnlyChip(isSelected = uiState.isFreeOnly == true, onClick = viewModel::toggleFreeFilter)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading && uiState.events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StrongBlue)
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::pullToRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.groupedEvents.isNotEmpty()) {
                            uiState.sortedDateKeys.forEach { dateKey ->
                                val eventsForDate = uiState.groupedEvents[dateKey] ?: emptyList()
                                if (eventsForDate.isNotEmpty()) {
                                    stickyHeader {
                                        DateSectionHeader(dateKey = dateKey)
                                    }
                                    items(eventsForDate, key = { it.id }) { event ->
                                        EnhancedEventItem(event = event, onClick = { onNavigateToEvent(event.id) })
                                    }
                                }
                            }
                        } else {
                            items(uiState.events, key = { it.id }) { event ->
                                EnhancedEventItem(event = event, onClick = { onNavigateToEvent(event.id) })
                            }
                        }

                        if (uiState.hasMore) {
                            item {
                                LaunchedEffect(Unit) {
                                    viewModel.loadEvents()
                                }
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = StrongBlue, strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("Error") },
            text = { Text(uiState.error ?: "Unknown error") },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text("OK") }
            }
        )
    }
}

@Composable
fun CategoryChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) StrongBlue else StrongSecondaryBackground,
        tonalElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) Color.White else StrongTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FreeOnlyChip(isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) StrongGreen else StrongSecondaryBackground,
        tonalElevation = 2.dp
    ) {
        Text(
            text = "Free Only",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) Color.White else StrongTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DateSectionHeader(dateKey: String) {
    val headerText = remember(dateKey) {
        try {
            val date = LocalDate.parse(dateKey)
            val today = LocalDate.now()
            val tomorrow = today.plusDays(1)
            when (date) {
                today -> "Today"
                tomorrow -> "Tomorrow"
                else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
            }
        } catch (e: Exception) {
            dateKey
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StrongBackground,
        tonalElevation = 0.dp
    ) {
        Text(
            text = headerText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EventItem(event: ExploreEvent, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = StrongSecondaryBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
                
                // Price Badge
                Surface(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = event.priceDisplay ?: "Free",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = StrongBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = event.locationName,
                    color = StrongTextSecondary,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.startTime.take(10), // Simple date extraction
                        color = StrongBlue,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${event.enrolledCount ?: 0}/${event.capacity ?: "∞"}",
                        color = if (event.isNearCapacity == true) StrongRed else StrongTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedEventItem(event: ExploreEvent, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = StrongSecondaryBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Image with overlays
            Box {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )

                // Organizer Type Badge (top-left)
                if (event.organizerType != null || event.resolvedHostName != null) {
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
                        OrganizerTypeBadge(
                            orgType = event.organizerType,
                            hostName = event.resolvedHostName ?: "Ziro"
                        )
                    }
                }

                // Top-right badges
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Price Badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = event.priceDisplay ?: "Free",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (event.priceDisplay?.lowercase() == "free" || event.priceDisplay == null) StrongGreen else StrongBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // "Selling Out" Badge
                    if (event.isNearCapacity == true) {
                        Surface(
                            color = SellingFastOrange,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "🔥 Selling out",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Content
            Column(modifier = Modifier.padding(16.dp)) {
                // Title
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Location
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = StrongTextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.locationName,
                        color = StrongTextSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Date & Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val displayTime = remember(event.startTime) {
                        try {
                            val zdt = ZonedDateTime.parse(event.startTime)
                            zdt.format(DateTimeFormatter.ofPattern("MMM d · h:mm a"))
                        } catch (e: Exception) {
                            event.startTime.take(16).replace("T", " ")
                        }
                    }
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = StrongBlue
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = displayTime,
                        color = StrongBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Highlights Tags
                event.highlights?.takeIf { it.isNotEmpty() }?.let { highlights ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        highlights.take(2).forEach { highlight ->
                            Surface(
                                color = HighlightBlueBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = highlight,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    color = StrongBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Capacity Bar
                (event.enrolledCount to event.capacity).let { (enrolled, cap) ->
                    if (enrolled != null && cap != null && cap > 0) {
                        val ratio = enrolled.toFloat() / cap.toFloat()
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${event.spotsLeft} spots left",
                                color = StrongTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "$enrolled/$cap filled",
                                color = StrongTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val barColor = if (ratio > 0.8f) SellingFastOrange else StrongBlue
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(CapacityBarBg)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganizerTypeBadge(orgType: String?, hostName: String) {
    val (badgeColor, icon) = when (orgType) {
        "brand" -> ExploreOrange to Icons.Default.Star
        "corporate" -> ExplorePurple to Icons.Default.Business
        "gym" -> StrongBlue to Icons.Default.FitnessCenter
        else -> StrongBlue to Icons.Default.Person
    }
    Row(
        modifier = Modifier
            .background(badgeColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(10.dp),
            tint = Color.White
        )
        Text(
            text = hostName.uppercase(),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1
        )
    }
}
