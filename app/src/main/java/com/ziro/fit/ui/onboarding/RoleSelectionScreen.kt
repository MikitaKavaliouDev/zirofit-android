package com.ziro.fit.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsGymnastics
import com.ziro.fit.ui.components.LocationPickerDialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ziro.fit.viewmodel.OnboardingViewModel

enum class OnboardingStep {
    ROLE,
    DETAILS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    onOnboardingComplete: (String) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableStateOf(OnboardingStep.ROLE) }
    val avatarUri = viewModel.avatarUri
    
    val name = uiState.name
    val location = uiState.location ?: ""
    val bio = uiState.bio ?: ""
    var showLocationPicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> viewModel.setAvatar(uri) }
    )

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onOnboardingComplete(uiState.role)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentStep == OnboardingStep.ROLE) "Choose Your Path" else "Tell Us About You") },
                navigationIcon = {
                    if (currentStep == OnboardingStep.DETAILS) {
                        IconButton(onClick = { currentStep = OnboardingStep.ROLE }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier.padding(padding),
            label = "onboarding_step"
        ) { step ->
            when (step) {
                OnboardingStep.ROLE -> RoleStepContent(
                    selectedRole = uiState.role,
                    onRoleSelect = { role ->
                        viewModel.updateRole(role)
                    },
                    onContinue = { currentStep = OnboardingStep.DETAILS }
                )
                OnboardingStep.DETAILS -> DetailsStepContent(
                    uiState = uiState,
                    avatarUri = avatarUri,
                    name = name,
                    location = location,
                    bio = bio,
                    showLocationPicker = showLocationPicker,
                    photoPickerLauncher = photoPickerLauncher,
                    onNameChange = { viewModel.updateName(it) },
                    onLocationChange = { viewModel.updateLocation(it) },
                    onBioChange = { viewModel.updateBio(it) },
                    onShowLocationPicker = { showLocationPicker = true },
                    onComplete = {
                        viewModel.completeOnboarding(
                            name,
                            location.takeIf { it.isNotBlank() },
                            bio.takeIf { it.isNotBlank() }
                        )
                    }
                )
            }
        }
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            onLocationSelected = { latitude, longitude, address ->
                val locationString = "$latitude,$longitude"
                viewModel.updateLocation(locationString)
                showLocationPicker = false
            },
            onDismiss = { showLocationPicker = false }
        )
    }
}

@Composable
private fun RoleStepContent(
    selectedRole: String,
    onRoleSelect: (String) -> Unit,
    onContinue: () -> Unit
) {
    val trainerFeatures = listOf(
        "Build & Scale Your Fitness Business" to Icons.Default.CheckCircle,
        "Sell Custom Training Packages & Subscriptions" to Icons.Default.CheckCircle,
        "Manage Clients, Workouts, & Progress in One Place" to Icons.Default.CheckCircle,
        "Automated Scheduling & Payments" to Icons.Default.CheckCircle
    )
    
    val clientFeatures = listOf(
        "Find & Book Top-Tier Personal Trainers" to Icons.Default.CheckCircle,
        "Purchase Flexible Training Packages" to Icons.Default.CheckCircle,
        "Track Workouts, Nutrition & Progress" to Icons.Default.CheckCircle,
        "Seamless Data Sharing with Your Coach" to Icons.Default.CheckCircle
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "I am a...",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RoleCard(
                title = "Personal",
                icon = Icons.Default.Person,
                emoji = "💪",
                selected = selectedRole == "client",
                onClick = { onRoleSelect("client") },
                modifier = Modifier.weight(1f)
            )
            RoleCard(
                title = "Trainer",
                icon = Icons.Default.SportsGymnastics,
                emoji = "🏋️",
                selected = selectedRole == "trainer",
                onClick = { onRoleSelect("trainer") },
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (selectedRole == "trainer") "As a Trainer, you can:" else "As a Client, you can:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                val features = if (selectedRole == "trainer") trainerFeatures else clientFeatures
                features.forEach { (feature, icon) ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Continue as ${if (selectedRole == "trainer") "Trainer" else "Client"}",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun DetailsStepContent(
    uiState: com.ziro.fit.model.OnboardingFormState,
    avatarUri: android.net.Uri?,
    name: String,
    location: String,
    bio: String,
    showLocationPicker: Boolean,
    photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    onNameChange: (String) -> Unit,
    onLocationChange: (String?) -> Unit,
    onBioChange: (String?) -> Unit,
    onShowLocationPicker: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (avatarUri != null) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Upload Photo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("What should we call you?") },
            placeholder = { Text("Your full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (uiState.role == "trainer") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { onLocationChange(it) },
                    label = { Text("Where are you based?") },
                    placeholder = { Text("City, Country") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                FilledTonalIconButton(
                    onClick = onShowLocationPicker,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Pick from map"
                    )
                }
            }
            Text(
                text = "We use this to help clients find trainers near them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        OutlinedTextField(
            value = bio,
            onValueChange = onBioChange,
            label = { Text("Short Bio (Optional)") },
            placeholder = { Text(if (uiState.role == "trainer") "I specialize in strength training and..." else "I'm looking to improve my...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && (uiState.role != "trainer" || location.isNotBlank()),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (uiState.isLoading) "Setting up..." else "Complete Setup",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    icon: ImageVector,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
