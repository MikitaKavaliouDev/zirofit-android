package com.ziro.fit.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ziro.fit.ui.theme.*

private data class OnboardingStep(
    val icon: ImageVector,
    val iconColor: Color,
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GettingStartedGuideScreen(
    isTrainer: Boolean,
    onNavigateBack: () -> Unit
) {
    val trainerSteps = listOf(
        OnboardingStep(
            icon = Icons.Default.Store,
            iconColor = StrongBlue,
            title = "Set Up Your Profile",
            description = "Create your trainer profile, add your credentials, and set up your services"
        ),
        OnboardingStep(
            icon = Icons.Default.People,
            iconColor = Color(0xFF8B5CF6),
            title = "Manage Clients",
            description = "Add clients, assign programs, and track their progress"
        ),
        OnboardingStep(
            icon = Icons.Default.FitnessCenter,
            iconColor = Color(0xFF10B981),
            title = "Create Programs",
            description = "Build workout programs and assign them to your clients"
        ),
        OnboardingStep(
            icon = Icons.Default.Assessment,
            iconColor = Color(0xFFF59E0B),
            title = "Track Analytics",
            description = "Monitor client performance with detailed analytics"
        ),
        OnboardingStep(
            icon = Icons.Default.AutoAwesome,
            iconColor = Color(0xFFEC4899),
            title = "Engage & Grow",
            description = "Use AI Coach and smart features to grow your business"
        )
    )

    val clientSteps = listOf(
        OnboardingStep(
            icon = Icons.Default.FitnessCenter,
            iconColor = Color(0xFF10B981),
            title = "Your Dashboard",
            description = "View your workouts, progress, and upcoming sessions"
        ),
        OnboardingStep(
            icon = Icons.Default.SmartToy,
            iconColor = Color(0xFF8B5CF6),
            title = "AI Coach",
            description = "Get personalized coaching and voice-guided workouts"
        ),
        OnboardingStep(
            icon = Icons.Default.CalendarMonth,
            iconColor = StrongBlue,
            title = "Schedule & Book",
            description = "Book sessions with your trainer and manage your calendar"
        ),
        OnboardingStep(
            icon = Icons.Default.CheckCircle,
            iconColor = Color(0xFF10B981),
            title = "Check-ins",
            description = "Log your progress with regular check-in updates"
        ),
        OnboardingStep(
            icon = Icons.Default.TrendingUp,
            iconColor = Color(0xFFF59E0B),
            title = "Track Progress",
            description = "Monitor your improvement with detailed analytics"
        )
    )

    val steps = if (isTrainer) trainerSteps else clientSteps
    var currentStep by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Getting Started Guide") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                steps.forEachIndexed { index, _ ->
                    val color = if (index == currentStep) StrongBlue
                        else if (index < currentStep) StrongBlue.copy(alpha = 0.5f)
                        else StrongDivider
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                }
            }

            // Content area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val step = steps[currentStep]

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(step.iconColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = step.iconColor,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = step.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = StrongTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = StrongTextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Navigation buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentStep < steps.size - 1) {
                    Button(
                        onClick = { currentStep++ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StrongBlue)
                    ) {
                        Text("Next", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = { currentStep = steps.size - 1 }) {
                        Text("Skip", color = StrongTextSecondary)
                    }
                } else {
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StrongBlue
                        )
                    ) {
                        Text(
                            text = if (isTrainer) "Start Coaching" else "Get Started",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
