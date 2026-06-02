package com.ziro.fit.ui.aicoach

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.ziro.fit.model.GoalSuggestion
import com.ziro.fit.viewmodel.AICoachUiState
import com.ziro.fit.viewmodel.AICoachViewModel
import com.ziro.fit.viewmodel.AICoachStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICoachScreen(
    navController: NavController,
    viewModel: AICoachViewModel = hiltViewModel(),
    onNavigateToProgram: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.generatedProgramId) {
        uiState.generatedProgramId?.let { programId ->
             // Maybe wait for user to click "View Program" instead of auto navigating?
             // The UI shows a success state with a button.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Coach") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isLoading) viewModel.cancelGeneration()
                        navController.popBackStack()
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isLoading) {
                        IconButton(onClick = { viewModel.cancelGeneration() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            AnimatedContent(targetState = uiState.step, label = "wizard_step") { step ->
                when (step) {
                    AICoachStep.INPUT -> GoalInputView(
                        userInput = uiState.userInput,
                        onInputChange = viewModel::onUserInputChange,
                        onAnalyze = viewModel::analyzeGoal,
                        isLoading = uiState.isLoading,
                        error = uiState.error
                    )
                    AICoachStep.SELECTION -> GoalSelectionView(
                        suggestions = uiState.suggestions,
                        onSelect = viewModel::selectSuggestion,
                        isLoading = uiState.isLoading
                    )
                    AICoachStep.METRICS -> MetricsInputView(
                        requiredMetrics = uiState.requiredMetrics,
                        metricValues = uiState.metricValues,
                        onUpdateMetric = viewModel::updateMetric,
                        onGenerate = viewModel::generateProgram,
                        isLoading = uiState.isLoading,
                        error = uiState.error
                    )
                    AICoachStep.GENERATING -> LoadingView(
                        message = "Designing your perfect program...",
                        onCancel = { viewModel.cancelGeneration() }
                    )
                    AICoachStep.SUCCESS -> SuccessView(
                        onViewProgram = {
                            uiState.generatedProgramId?.let { onNavigateToProgram(it) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GoalInputView(
    userInput: String,
    onInputChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "What is your main fitness goal?",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Be as vague or specific as you like. We'll help you refine it.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = userInput,
            onValueChange = onInputChange,
            label = { Text("I want to...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )
        
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onAnalyze,
            enabled = userInput.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Analyze Goal")
            }
        }
    }
}

@Composable
fun GoalSelectionView(
    suggestions: List<GoalSuggestion>,
    onSelect: (GoalSuggestion) -> Unit,
    isLoading: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Here's what we understood",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Select the best match to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(suggestions) { suggestion ->
                Card(
                    onClick = { onSelect(suggestion) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = suggestion.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = suggestion.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        SuggestionChip(
                            onClick = { /* No-op */ },
                            label = { Text(suggestion.focus) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricsInputView(
    requiredMetrics: List<String>,
    metricValues: Map<String, String>,
    onUpdateMetric: (String, String) -> Unit,
    onGenerate: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Just a few details",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "We need these numbers to tailor the intensity.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(requiredMetrics) { metric ->
                // Basic check for units or type if name contains hint, otherwise standard text
                val label = metric.replace("_", " ").capitalize()
                OutlinedTextField(
                    value = metricValues[metric] ?: "",
                    onValueChange = { onUpdateMetric(metric, it) },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        if (error != null) {
            Text(text = error, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = onGenerate,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Generate Program")
            }
        }
    }
}

@Composable
fun LoadingView(
    message: String,
    onCancel: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = message, style = MaterialTheme.typography.titleMedium)
        
        if (onCancel != null) {
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
        }
    }
}

@Composable
fun SuccessView(onViewProgram: () -> Unit) {
     Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Success",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(96.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Program Generated!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your personalized plan is ready.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onViewProgram,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View Program")
        }
    }
}
