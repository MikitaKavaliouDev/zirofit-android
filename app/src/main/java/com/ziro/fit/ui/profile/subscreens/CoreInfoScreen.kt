package com.ziro.fit.ui.profile.subscreens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ziro.fit.model.ProfileCoreInfo
import com.ziro.fit.model.ProfileCoreLocation
import com.ziro.fit.model.UpdateCoreInfoRequest
import com.ziro.fit.ui.theme.*
import com.ziro.fit.viewmodel.ProfileViewModel

private val WEIGHT_UNITS = listOf("kg", "lbs")
private val CURRENCIES = listOf("USD", "EUR", "GBP", "CAD", "AUD", "CHF", "JPY")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoreInfoScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val coreInfo = uiState.coreInfo

    // Editable fields
    var fullName by remember(coreInfo) { mutableStateOf(coreInfo?.fullName ?: "") }
    var username by remember(coreInfo) { mutableStateOf(coreInfo?.username ?: "") }
    var phone by remember(coreInfo) { mutableStateOf(coreInfo?.phone ?: "") }
    var aboutMe by remember(coreInfo) { mutableStateOf(coreInfo?.aboutMe ?: "") }
    var certifications by remember(coreInfo) { mutableStateOf(coreInfo?.certifications ?: "") }

    // List fields
    var specialties by remember(coreInfo) { mutableStateOf(coreInfo?.specialties ?: emptyList()) }
    var trainingTypes by remember(coreInfo) { mutableStateOf(coreInfo?.trainingTypes ?: emptyList()) }

    // Single select
    var weightUnit by remember(coreInfo) { mutableStateOf(coreInfo?.weightUnit ?: "kg") }
    var businessCurrency by remember(coreInfo) { mutableStateOf(coreInfo?.businessCurrency ?: "USD") }

    // Locations
    var locations by remember(coreInfo) { mutableStateOf(coreInfo?.locations ?: emptyList()) }
    var showAddLocation by remember { mutableStateOf(false) }
    var newLocationAddress by remember { mutableStateOf("") }

    // Avatar
    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }

    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) selectedAvatarUri = uri
    }

    LaunchedEffect(Unit) {
        viewModel.fetchCoreInfo()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.updateCoreInfo(
                                UpdateCoreInfoRequest(
                                    fullName = fullName.ifBlank { null },
                                    username = username.ifBlank { null },
                                    weightUnit = weightUnit,
                                    certifications = certifications.ifBlank { null },
                                    phone = phone.ifBlank { null },
                                    specialties = specialties,
                                    trainingTypes = trainingTypes,
                                    businessCurrency = businessCurrency,
                                    aboutMe = aboutMe.ifBlank { null },
                                    locations = locations
                                )
                            )
                        },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = StrongBlue
                            )
                        } else {
                            Text("Save", color = StrongBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // ── Avatar ────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(StrongSecondaryBackground)
                        .clickable { avatarLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedAvatarUri != null) {
                        AsyncImage(
                            model = selectedAvatarUri,
                            contentDescription = "Selected avatar",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else if (uiState.branding?.profileImageUrl != null) {
                        AsyncImage(
                            model = uiState.branding!!.profileImageUrl,
                            contentDescription = "Profile photo",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = StrongTextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Edit badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(StrongBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change photo",
                            tint = StrongTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Basic Info ────────────────────────────────────────────
            SectionLabel("Basic Information")

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── About Me ──────────────────────────────────────────────
            SectionLabel("About")
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = aboutMe,
                onValueChange = { aboutMe = it },
                label = { Text("About Me") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Professional Info ─────────────────────────────────────
            SectionLabel("Professional")
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = certifications,
                onValueChange = { certifications = it },
                label = { Text("Certifications") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Specialties ───────────────────────────────────────────
            SectionLabel("Specialties")
            Spacer(modifier = Modifier.height(4.dp))
            SubtitleText("Tap to add specialties")

            var showSpecialtyInput by remember { mutableStateOf(false) }
            var newSpecialty by remember { mutableStateOf("") }

            ChipGroup(
                items = specialties,
                onRemoveItem = { specialties = specialties - it }
            )

            if (showSpecialtyInput) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newSpecialty,
                        onValueChange = { newSpecialty = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Enter specialty") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newSpecialty.isNotBlank()) {
                                specialties = specialties + newSpecialty.trim()
                                newSpecialty = ""
                                showSpecialtyInput = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Add", tint = StrongBlue)
                    }
                }
            } else {
                TextButton(
                    onClick = { showSpecialtyInput = true },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Specialty")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Training Types ────────────────────────────────────────
            SectionLabel("Training Types")
            Spacer(modifier = Modifier.height(4.dp))
            SubtitleText("Tap to add training types")

            var showTrainingTypeInput by remember { mutableStateOf(false) }
            var newTrainingType by remember { mutableStateOf("") }

            ChipGroup(
                items = trainingTypes,
                onRemoveItem = { trainingTypes = trainingTypes - it }
            )

            if (showTrainingTypeInput) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newTrainingType,
                        onValueChange = { newTrainingType = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Enter training type") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newTrainingType.isNotBlank()) {
                                trainingTypes = trainingTypes + newTrainingType.trim()
                                newTrainingType = ""
                                showTrainingTypeInput = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Add", tint = StrongBlue)
                    }
                }
            } else {
                TextButton(
                    onClick = { showTrainingTypeInput = true },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Training Type")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Weight Unit ───────────────────────────────────────────
            SectionLabel("Weight Unit")
            Spacer(modifier = Modifier.height(4.dp))
            SubtitleText("Used for tracking client measurements")

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WEIGHT_UNITS.forEach { unit ->
                    FilterChip(
                        selected = weightUnit == unit,
                        onClick = { weightUnit = unit },
                        label = { Text(unit) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StrongBlue.copy(alpha = 0.2f),
                            selectedLabelColor = StrongBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Business Currency ─────────────────────────────────────
            SectionLabel("Business Currency")
            Spacer(modifier = Modifier.height(4.dp))
            SubtitleText("Used for services and packages")

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CURRENCIES.take(4).forEach { currency ->
                    FilterChip(
                        selected = businessCurrency == currency,
                        onClick = { businessCurrency = currency },
                        label = { Text(currency, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StrongBlue.copy(alpha = 0.2f),
                            selectedLabelColor = StrongBlue
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CURRENCIES.drop(4).forEach { currency ->
                    FilterChip(
                        selected = businessCurrency == currency,
                        onClick = { businessCurrency = currency },
                        label = { Text(currency, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StrongBlue.copy(alpha = 0.2f),
                            selectedLabelColor = StrongBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Locations ─────────────────────────────────────────────
            SectionLabel("Locations")
            Spacer(modifier = Modifier.height(4.dp))
            SubtitleText("Your training locations")

            Spacer(modifier = Modifier.height(8.dp))

            if (locations.isEmpty() && !showAddLocation) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddLocation = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = StrongTextSecondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Add Location",
                            color = StrongTextSecondary
                        )
                    }
                }
            }

            locations.forEachIndexed { index, location ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = StrongBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = location.address ?: "Location ${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StrongTextPrimary
                            )
                            if (location.latitude != null && location.longitude != null) {
                                Text(
                                    text = "${location.latitude}, ${location.longitude}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StrongTextSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { locations = locations.toMutableList().also { it.removeAt(index) } }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove location",
                                tint = StrongRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (showAddLocation) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = newLocationAddress,
                            onValueChange = { newLocationAddress = it },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                showAddLocation = false
                                newLocationAddress = ""
                            }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newLocationAddress.isNotBlank()) {
                                        locations = locations + ProfileCoreLocation(
                                            id = null,
                                            address = newLocationAddress.trim()
                                        )
                                        newLocationAddress = ""
                                        showAddLocation = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StrongBlue)
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            }

            if (locations.isNotEmpty() && !showAddLocation) {
                TextButton(onClick = { showAddLocation = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Another Location")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Error ─────────────────────────────────────────────────
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = StrongRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Bottom spacer
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── Helper Composables ───────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = StrongTextPrimary
    )
}

@Composable
private fun SubtitleText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = StrongTextSecondary
    )
}

@Composable
private fun ChipGroup(
    items: List<String>,
    onRemoveItem: (String) -> Unit
) {
    if (items.isEmpty()) {
        Text(
            text = "None added yet",
            style = MaterialTheme.typography.bodySmall,
            color = StrongTextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.padding(vertical = 4.dp)
        )
    } else {
        // Use a wrapping layout via FlowRow
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                InputChip(
                    selected = false,
                    onClick = {},
                    label = { Text(item, fontSize = 13.sp) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove $item",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onRemoveItem(item) }
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        containerColor = StrongSecondaryBackground,
                        labelColor = StrongTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}
