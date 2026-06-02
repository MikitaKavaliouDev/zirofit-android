package com.ziro.fit.ui.discovery

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ziro.fit.model.ExploreEvent
import com.ziro.fit.ui.theme.*
import com.ziro.fit.viewmodel.EventDetailViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(eventId) {
        viewModel.loadEventDetails(eventId)
    }

    // Handle checkout redirect with Custom Chrome Tab
    LaunchedEffect(uiState.checkoutUrl) {
        uiState.checkoutUrl?.let { url ->
            try {
                val customTabsIntent = CustomTabsIntent.Builder().build()
                customTabsIntent.launchUrl(context, Uri.parse(url))
            } catch (_: Exception) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            viewModel.clearCheckoutUrl()
        }
    }

    Scaffold(
        containerColor = StrongBackground,
        topBar = {
            TopAppBar(
                title = { Text("Event Details", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    uiState.event?.let { event ->
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, event.title)
                                val text = buildString {
                                    append(event.title)
                                    if (event.description != null) append("\n\n${event.description}")
                                    append("\n\nCheck it out on ZiroFit!")
                                }
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Event"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.event == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StrongBlue)
            }
        } else {
            uiState.event?.let { event ->
                EventDetailContent(
                    event = event,
                    onEnroll = { viewModel.enroll(event) },
                    isLoading = uiState.isLoading,
                    joinSuccess = uiState.joinSuccess,
                    padding = padding
                )
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

    if (uiState.showSuccessDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSuccessDialog,
            title = { Text("You're in! 🎉") },
            text = {
                Text(
                    text = "You have successfully enrolled in ${uiState.event?.title ?: "the event"}."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissSuccessDialog) { Text("Great!") }
            }
        )
    }
}

@Composable
fun EventDetailContent(
    event: ExploreEvent,
    onEnroll: () -> Unit,
    isLoading: Boolean,
    joinSuccess: Boolean,
    padding: PaddingValues
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
    ) {
        AsyncImage(
            model = event.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = event.title,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Formatted Date & Time
            val formattedDate = remember(event.startTime) {
                try {
                    val zdt = ZonedDateTime.parse(event.startTime)
                    zdt.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
                } catch (e: Exception) {
                    event.startTime
                }
            }
            val formattedTime = remember(event.startTime) {
                try {
                    val zdt = ZonedDateTime.parse(event.startTime)
                    val endTimeStr = event.endTime
                    val start = zdt.format(DateTimeFormatter.ofPattern("h:mm a"))
                    if (endTimeStr != null) {
                        try {
                            val endZdt = ZonedDateTime.parse(endTimeStr)
                            "$start - ${endZdt.format(DateTimeFormatter.ofPattern("h:mm a"))}"
                        } catch (_: Exception) {
                            start
                        }
                    } else {
                        start
                    }
                } catch (e: Exception) {
                    ""
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = StrongBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = formattedDate, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    if (formattedTime.isNotBlank()) {
                        Text(text = formattedTime, color = StrongTextSecondary, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = StrongBlue, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = event.locationName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    if (event.address != null) {
                        Text(text = event.address, color = StrongTextSecondary, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Capacity Progress
            val progress = (event.enrolledCount?.toFloat() ?: 0f) / (event.capacity?.toFloat() ?: 1f)
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Capacity", color = Color.White, fontSize = 14.sp)
                    Text("${event.enrolledCount ?: 0}/${event.capacity ?: "∞"}", color = StrongBlue, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (progress >= 0.9f) StrongRed else StrongBlue,
                    trackColor = StrongSecondaryBackground
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("About this event", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = event.description ?: "No description provided.",
                color = StrongTextSecondary,
                lineHeight = 22.sp,
                fontSize = 15.sp
            )

            // Highlights Tags
            if (!event.highlights.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    event.highlights.take(3).forEach { highlight ->
                        Surface(
                            color = StrongBlue.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = highlight,
                                color = StrongBlue,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enhanced Host Info
            val hostName = event.hostName ?: event.trainerName ?: ""
            if (hostName.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StrongSecondaryBackground
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = event.trainer?.profile?.profilePhotoPath,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hosted by", color = StrongTextSecondary, fontSize = 12.sp)
                            Text(hostName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (event.hostId != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "View Profile",
                                    color = StrongBlue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Inline Map
            if (event.latitude != null && event.longitude != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StrongSecondaryBackground)
                        .clickable {
                            val uri = Uri.parse("geo:${event.latitude},${event.longitude}?q=${event.latitude},${event.longitude}(${Uri.encode(event.title)})")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(mapIntent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        StrongBlue.copy(alpha = 0.15f),
                                        StrongSecondaryBackground
                                    )
                                )
                            )
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = StrongBlue,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap to view on map",
                            color = StrongTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (joinSuccess) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = StrongGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StrongGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("You are enrolled!", color = Color.White)
                    }
                }
            } else {
                Button(
                    onClick = onEnroll,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StrongBlue),
                    enabled = !isLoading && (event.isBooked != true) && !event.isFull
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        val text = when {
                            event.isBooked == true -> "Already Enrolled"
                            event.isFull -> "Event Full"
                            else -> if (event.price != null && event.price > 0) "Buy Ticket (${event.price} ${event.currency})" else "Join for Free"
                        }
                        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
