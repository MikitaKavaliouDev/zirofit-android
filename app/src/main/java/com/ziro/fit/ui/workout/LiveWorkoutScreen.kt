package com.ziro.fit.ui.workout

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.abs
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import com.ziro.fit.service.MatchConfidence
import com.ziro.fit.model.SetStatus
import com.ziro.fit.model.WorkoutExerciseUi
import com.ziro.fit.model.WorkoutSetUi
import com.ziro.fit.ui.components.ExerciseBrowserContent
import com.ziro.fit.ui.theme.*
import com.ziro.fit.ui.workouts.WorkoutSuccessContent
import com.ziro.fit.model.VoiceCoachConnectionState
import com.ziro.fit.viewmodel.VoiceTrainingViewModel
import com.ziro.fit.viewmodel.VoiceTrainingUiState
import com.ziro.fit.viewmodel.WorkoutViewModel

enum class SessionFocusField { WEIGHT, REPS, RPE }
data class FocusTarget(val exerciseId: String, val setIndex: Int, val field: SessionFocusField)
enum class WorkoutInputOverlay { KEYBOARD, PLATE_CALCULATOR, RPE_PICKER, NONE }

// Superset grouping — mirrors iOS WorkoutExercise grouping by superSetId
sealed class GroupedExerciseItem {
    data class Header(val superSetId: String) : GroupedExerciseItem()
    data class Exercise(val exercise: WorkoutExerciseUi) : GroupedExerciseItem()
}

private fun buildGroupedExerciseItems(exercises: List<WorkoutExerciseUi>): List<GroupedExerciseItem> {
    val result = mutableListOf<GroupedExerciseItem>()
    val supersetGroups = mutableMapOf<String, MutableList<WorkoutExerciseUi>>()
    val standaloneExercises = mutableListOf<WorkoutExerciseUi>()
    
    exercises.forEach { exercise ->
        val groupId = exercise.superSetId
        if (groupId != null) {
            supersetGroups.getOrPut(groupId) { mutableListOf() }.add(exercise)
        } else {
            standaloneExercises.add(exercise)
        }
    }
    
    supersetGroups.forEach { (_, groupExercises) ->
        result.add(GroupedExerciseItem.Header(groupExercises.first().superSetId ?: ""))
        groupExercises.forEach { result.add(GroupedExerciseItem.Exercise(it)) }
    }
    
    standaloneExercises.forEach { result.add(GroupedExerciseItem.Exercise(it)) }
    
    return result
}

@Composable
private fun SupersetHeader(superSetId: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = null,
            tint = StrongBlue,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "SUPERSET",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = StrongBlue,
            letterSpacing = 1.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveWorkoutScreen(
    viewModel: WorkoutViewModel,
    voiceCoachViewModel: VoiceTrainingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val voiceState by voiceCoachViewModel.uiState.collectAsState()

     var showExerciseSheet by remember { mutableStateOf(false) }
     var showCancelDialog by remember { mutableStateOf(false) }
     var showFinishDialog by remember { mutableStateOf(false) }
     var showUnloggedSetsDialog by remember { mutableStateOf(false) }
    
    val exerciseSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val context = LocalContext.current

    // Advanced Input System State
    var focusedTarget by remember { mutableStateOf<FocusTarget?>(null) }
    var inputOverlay by remember { mutableStateOf(WorkoutInputOverlay.NONE) }
    var activeInputText by remember { mutableStateOf("") }

    // LazyListState for programmatic scrolling
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { viewModel.onPermissionsResult() }
    )
    
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = data?.get(0) ?: ""
            viewModel.parseVoiceCommand(text)
        }
    }

    // Voice Coach: RECORD_AUDIO permission
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceCoachViewModel.start(context as Activity)
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
             if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            launcher.launch(permissionsToRequest.toTypedArray())
        }
        viewModel.loadExercises()
        voiceCoachViewModel.configure()
    }

    LaunchedEffect(state.activeSession, state.isLoading, state.workoutSuccessStats) {
        if (state.activeSession == null && !state.isLoading && state.workoutSuccessStats == null) {
            onNavigateBack()
        }
    }

    fun syncInput() {
        focusedTarget?.let { target ->
            val exercise = state.activeSession?.exercises?.find { it.exerciseId == target.exerciseId }
            val set = exercise?.sets?.getOrNull(target.setIndex)
            if (set != null) {
                val weight = if (target.field == SessionFocusField.WEIGHT) activeInputText else set.weight
                val reps = if (target.field == SessionFocusField.REPS) activeInputText else set.reps
                viewModel.updateSetInput(target.exerciseId, target.setIndex, weight, reps)
            }
        }
    }

    fun triggerInput(target: FocusTarget) {
        syncInput()
        focusedTarget = target
        val exercise = state.activeSession?.exercises?.find { it.exerciseId == target.exerciseId }
        val set = exercise?.sets?.getOrNull(target.setIndex)
        activeInputText = if (target.field == SessionFocusField.WEIGHT) set?.weight ?: "" else set?.reps ?: ""
        inputOverlay = if (target.field == SessionFocusField.RPE) WorkoutInputOverlay.RPE_PICKER else WorkoutInputOverlay.KEYBOARD
    }

    fun handleNext() {
        syncInput()
        focusedTarget?.let { target ->
            val exercise = state.activeSession?.exercises?.find { it.exerciseId == target.exerciseId }
            val currentSet = exercise?.sets?.getOrNull(target.setIndex)

            if (target.field == SessionFocusField.WEIGHT) {
                triggerInput(FocusTarget(target.exerciseId, target.setIndex, SessionFocusField.REPS))
            } else {
                // Log the set automatically when moving past REPS
                if (currentSet != null) {
                    val updatedSet = currentSet.copy(
                        weight = if (target.field == SessionFocusField.WEIGHT) activeInputText else currentSet.weight,
                        reps = if (target.field == SessionFocusField.REPS) activeInputText else currentSet.reps,
                        status = currentSet.status
                    )
                    viewModel.logSet(target.exerciseId, updatedSet, currentSet.status)
                }

                if (exercise != null && target.setIndex + 1 < exercise.sets.size) {
                    triggerInput(FocusTarget(target.exerciseId, target.setIndex + 1, SessionFocusField.WEIGHT))
                } else {
                    inputOverlay = WorkoutInputOverlay.NONE
                    focusedTarget = null
                }
            }
        }
    }

    val isBlank = state.activeSession?.exercises?.all { ex -> ex.sets.none { it.isCompleted } } ?: true

    Scaffold(
        containerColor = StrongBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(StrongBackground)
        ) {
            // Base layer tap to dismiss keyboard
            Box(modifier = Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                syncInput()
                inputOverlay = WorkoutInputOverlay.NONE
                focusedTarget = null
            })

            Column(modifier = Modifier.fillMaxSize()) {
                LiveWorkoutHeader(
                    templateName = state.activeSession?.title ?: "Workout",
                    elapsedSeconds = state.elapsedSeconds,
                    isTimerRunning = true, // Default to true as explicit pause state is managed implicitly
                    isRestActive = state.isRestActive,
                    restSecondsRemaining = state.restSecondsRemaining,
                    restTotalSeconds = state.restTotalSeconds,
                    onMinimize = {
                        syncInput()
                        onNavigateBack()
                    }
                )

                 if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = StrongBlue)
                    }
                 } else {
                     val exercises = state.activeSession?.exercises ?: emptyList()
                     val groupedItems = if (exercises.isEmpty()) emptyList() else buildGroupedExerciseItems(exercises)
                     val exerciseToGroupedIndexMap = remember(exercises, groupedItems) {
                         if (exercises.isEmpty()) emptyMap() else run {
                             val map = mutableMapOf<String, Int>()
                             var groupedIndex = 0
                             exercises.forEach { exercise ->
                                 while (groupedIndex < groupedItems.size) {
                                     when (val item = groupedItems[groupedIndex]) {
                                         is GroupedExerciseItem.Exercise -> {
                                             if (item.exercise.exerciseId == exercise.exerciseId) {
                                                 map[exercise.exerciseId] = groupedIndex
                                                 groupedIndex++
                                                 break
                                             }
                                             groupedIndex++
                                         }
                                         is GroupedExerciseItem.Header -> {
                                             groupedIndex++
                                         }
                                     }
                                 }
                             }
                             map
                         }
                     }

                     // Scroll to focused exercise when focusedTarget changes
                     LaunchedEffect(focusedTarget) {
                         focusedTarget?.let { target ->
                             val index = exerciseToGroupedIndexMap[target.exerciseId]
                             if (index != null) {
                                 listState.animateScrollToItem(index, scrollOffset = -200)
                             }
                         }
                     }

                     LazyColumn(
                         modifier = Modifier.fillMaxSize(),
                         contentPadding = PaddingValues(bottom = 140.dp, top = 10.dp),
                         verticalArrangement = Arrangement.spacedBy(20.dp),
                         state = listState
                     ) {
                    if (exercises.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.Gray.copy(alpha = 0.3f))
                                Spacer(Modifier.height(20.dp))
                                Text("Start by adding an exercise", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                                Spacer(Modifier.height(20.dp))
                                Button(onClick = { showExerciseSheet = true }, colors = ButtonDefaults.buttonColors(containerColor = StrongBlue), shape = RoundedCornerShape(12.dp)) {
                                    Text("Add Exercise")
                                }
                            }
                        }
                    } else {

                        itemsIndexed(groupedItems) { _, item ->
                                when (item) {
                                    is GroupedExerciseItem.Header -> SupersetHeader(superSetId = item.superSetId)
                                    is GroupedExerciseItem.Exercise -> ExerciseCard(
                                        exercise = item.exercise,
                                        focusedTarget = focusedTarget,
                                        activeInputText = activeInputText,
                                        onTriggerInput = { triggerInput(it) },
                                        onSetToggle = { set -> 
                                            syncInput()
                                            viewModel.logSet(item.exercise.exerciseId, set) 
                                        },
                                        onAddSet = { viewModel.addSetToExercise(item.exercise.exerciseId) },
                                        onRemove = { /* ViewModel remove exercise not exposed yet, mock for now */ },
                                        onToggleSuperset = { viewModel.toggleSuperset(item.exercise.exerciseId) }
                                    )
                                }
                            }
                            
                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Button(
                                        onClick = { showExerciseSheet = true },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = StrongBlue.copy(alpha = 0.1f), contentColor = StrongBlue),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Add Exercise", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Button(
                                        onClick = { showCancelDialog = true }, 
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = StrongRed.copy(alpha = 0.1f), contentColor = StrongRed),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Cancel Workout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Bottom Controls (Hidden if keyboard open)
            if (inputOverlay == WorkoutInputOverlay.NONE) {
                LiveWorkoutControls(
                    isTimerRunning = true,
                    onTogglePause = { /* Implement pause logic if added to ViewModel */ },
                     onFinish = { 
                         if (isBlank) showCancelDialog = true 
                         else if (viewModel.hasUnloggedSets()) showUnloggedSetsDialog = true 
                         else showFinishDialog = true 
                     },
                    isBlank = isBlank,
                    onMicTap = {
                        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        }
                        speechRecognizerLauncher.launch(intent)
                    }
                )
            }

            // ── Voice Coach Floating Button ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                VoiceCoachButton(
                    connectionState = voiceState.connectionState,
                    onTap = {
                        when (voiceState.connectionState) {
                            VoiceCoachConnectionState.DISCONNECTED -> {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    voiceCoachViewModel.toggle(context as Activity)
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            VoiceCoachConnectionState.ERROR -> {
                                voiceCoachViewModel.toggle(context as Activity)
                            }
                            VoiceCoachConnectionState.CONNECTED -> {
                                voiceCoachViewModel.stop()
                            }
                            VoiceCoachConnectionState.CONNECTING -> {
                                // No action while connecting
                            }
                        }
                    }
                )
            }

            // ── Voice Coach Overlay ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                VoiceCoachOverlay(
                    visible = voiceState.connectionState == VoiceCoachConnectionState.CONNECTED,
                    connectionState = voiceState.connectionState,
                    agentState = voiceState.agentState,
                    audioLevel = voiceState.audioLevel,
                    messages = voiceState.messages,
                    onDisconnect = { voiceCoachViewModel.stop() },
                    onDismiss = { voiceCoachViewModel.stop() }
                )
            }

            // Voice Command Overlay
            if (state.latestCommand != null) {
                val command = state.latestCommand!!
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.4f)).padding(16.dp).zIndex(400f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 120.dp),
                        colors = CardDefaults.cardColors(containerColor = StrongSecondaryBackground)
                    ) {
                         Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text("Voice Command Detected", color = Color.Gray, fontSize = 14.sp)
                                    // Show confidence indicator
                                    val confidenceColor = when (command.matchConfidence) {
                                        MatchConfidence.EXACT -> StrongGreen
                                        MatchConfidence.STARTS_WITH -> Color(0xFFFF9800)
                                        MatchConfidence.FUZZY -> Color(0xFFFF5722)
                                        MatchConfidence.NONE -> Color.Gray
                                    }
                                    val confidenceLabel = when (command.matchConfidence) {
                                        MatchConfidence.EXACT -> "✓ Exact match"
                                        MatchConfidence.STARTS_WITH -> "≈ Starts with"
                                        MatchConfidence.FUZZY -> "? Fuzzy match"
                                        MatchConfidence.NONE -> ""
                                    }
                                    if (confidenceLabel.isNotEmpty()) {
                                        Text(
                                            text = confidenceLabel,
                                            fontSize = 11.sp,
                                            color = confidenceColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(command.exercise ?: "Active Exercise", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StrongTextPrimary)
                                    if (command.exercise != null) {
                                        Button(
                                            onClick = { viewModel.showVoiceCorrectionPicker() },
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .height(32.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Blue.copy(alpha = 0.1f),
                                                contentColor = StrongBlue
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Change", fontSize = 12.sp)
                                        }
                                    }
                                }

                                IconButton(onClick = { viewModel.dismissVoiceCommand() }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                if (command.sets != null) {
                                    Column { Text("Sets", color=Color.Gray, fontSize=12.sp); Text("${command.sets}", fontSize=16.sp, color=StrongTextPrimary) }
                                }
                                if (command.reps != null) {
                                    Column { Text("Reps", color=Color.Gray, fontSize=12.sp); Text("${command.reps}", fontSize=16.sp, color=StrongTextPrimary) }
                                }
                                if (command.weight != null) {
                                    Column { Text("Weight", color=Color.Gray, fontSize=12.sp); Text("${command.weight} kg", fontSize=16.sp, color=StrongTextPrimary) }
                                }
                            }
                            Button(onClick = { viewModel.confirmVoiceCommand() }, modifier = Modifier.fillMaxWidth(), colors=ButtonDefaults.buttonColors(containerColor=StrongBlue)) {
                                Text("Confirm & Log")
                            }
                        }
                    }
                }
            }

            // Advanced Input Overlay
            if (inputOverlay != WorkoutInputOverlay.NONE) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(modifier = Modifier.fillMaxSize().clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        syncInput()
                        inputOverlay = WorkoutInputOverlay.NONE
                        focusedTarget = null
                    })
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(StrongSecondaryBackground)
                            .padding(bottom = 20.dp, top = 8.dp)
                    ) {
                        when (inputOverlay) {
                            WorkoutInputOverlay.KEYBOARD -> {
                                CustomNumericKeyboard(
                                    text = activeInputText,
                                    onTextChange = { activeInputText = it },
                                    onNext = { handleNext() },
                                    onDismiss = { 
                                        syncInput()
                                        inputOverlay = WorkoutInputOverlay.NONE
                                        focusedTarget = null
                                    },
                                    onAction = {
                                        inputOverlay = if (focusedTarget?.field == SessionFocusField.WEIGHT) WorkoutInputOverlay.PLATE_CALCULATOR else WorkoutInputOverlay.RPE_PICKER
                                    },
                                    isWeight = focusedTarget?.field == SessionFocusField.WEIGHT
                                )
                            }
                            WorkoutInputOverlay.PLATE_CALCULATOR -> {
                                PlateCalculatorOverlay(
                                    weightText = activeInputText,
                                    onWeightChange = { activeInputText = it },
                                    onBack = { inputOverlay = WorkoutInputOverlay.KEYBOARD }
                                )
                            }
                            WorkoutInputOverlay.RPE_PICKER -> {
                                val currentRpe = focusedTarget?.let { target ->
                                    state.activeSession?.exercises?.find { it.exerciseId == target.exerciseId }?.sets?.getOrNull(target.setIndex)?.rpe
                                }
                                RPEPickerOverlay(
                                    currentRpe = currentRpe,
                                    onRpeSelected = { rpe ->
                                        focusedTarget?.let { target ->
                                            viewModel.updateSetRpe(target.exerciseId, target.setIndex, rpe)
                                        }
                                        syncInput()
                                        inputOverlay = WorkoutInputOverlay.NONE
                                        focusedTarget = null
                                    },
                                    onDismiss = {
                                        syncInput()
                                        inputOverlay = WorkoutInputOverlay.NONE
                                        focusedTarget = null
                                    }
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }

            // PR Toast Overlay — mirrors iOS showNewRecordsToast (shown on session end)
            AnimatedVisibility(
                visible = state.showNewRecordsToast,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.zIndex(500f)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏆", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "New Personal Record!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (state.newRecords.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            state.newRecords.take(3).forEach { record ->
                                Text(
                                    text = record.exerciseName.ifEmpty { "Exercise" },
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            if (state.newRecords.size > 3) {
                                Text("+${state.newRecords.size - 3} more", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        } else if ((state.workoutSuccessStats?.recordsBroken ?: 0) > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${state.workoutSuccessStats?.recordsBroken} PR${if ((state.workoutSuccessStats?.recordsBroken ?: 0) > 1) "s" else ""} broken!",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.dismissNewRecordsToast() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Awesome!", color = Color.White)
                        }
                    }
                }
            }

            if (showCancelDialog) {
                AlertDialog(
                    onDismissRequest = { showCancelDialog = false },
                    title = { Text("Cancel Workout?") },
                    text = { Text("This will permanently discard your progress and logged sets for this session.") },
                    confirmButton = {
                        TextButton(onClick = { 
                            showCancelDialog = false
                            viewModel.cancelWorkout() 
                        }) {
                            Text("Discard", color = StrongRed)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCancelDialog = false }) {
                            Text("No", color = StrongTextPrimary)
                        }
                    },
                    containerColor = StrongSurface,
                    titleContentColor = StrongTextPrimary,
                    textContentColor = StrongTextSecondary
                )
            }

             if (showFinishDialog) {
                 AlertDialog(
                     onDismissRequest = { showFinishDialog = false },
                     title = { Text("Finish Workout?") },
                     text = { Text("Are you ready to log this session? Unfinished valid sets will be completed automatically.") },
                     confirmButton = {
                         Button(
                             onClick = { 
                                 showFinishDialog = false
                                 viewModel.finishWorkout() 
                             },
                             colors = ButtonDefaults.buttonColors(containerColor = StrongGreen)
                         ) {
                             Text("Finish Workout")
                         }
                     },
                     dismissButton = {
                         TextButton(onClick = { showFinishDialog = false }) {
                             Text("Cancel", color = StrongTextPrimary)
                         }
                     },
                     containerColor = StrongSurface,
                     titleContentColor = StrongTextPrimary,
                     textContentColor = StrongTextSecondary
                 )
             }

             if (showUnloggedSetsDialog) {
                 AlertDialog(
                     onDismissRequest = { showUnloggedSetsDialog = false },
                     title = { Text("Complete Unfinished Sets?") },
                     text = { Text("You have sets that haven't been logged. How would you like to proceed?") },
                     confirmButton = {
                         Button(
                             onClick = { 
                                 showUnloggedSetsDialog = false
                                 viewModel.completeUnloggedSets()
                                 showFinishDialog = true
                             },
                             colors = ButtonDefaults.buttonColors(containerColor = StrongBlue)
                         ) {
                             Text("Complete")
                         }
                     },
                     dismissButton = {
                         TextButton(
                             onClick = { 
                                 showUnloggedSetsDialog = false
                                 viewModel.discardUnloggedSets()
                                 showFinishDialog = true
                             }
                         ) {
                             Text("Discard", color = StrongRed)
                         }
                     },
                     containerColor = StrongSurface,
                     titleContentColor = StrongTextPrimary,
                     textContentColor = StrongTextSecondary
                 )
             }

            state.error?.let { errorMessage ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    title = { Text("Error") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK", color = StrongBlue)
                        }
                    },
                    containerColor = StrongSurface,
                    titleContentColor = StrongRed,
                    textContentColor = StrongTextSecondary
                )
            }

            if (showExerciseSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showExerciseSheet = false },
                    sheetState = exerciseSheetState,
                    containerColor = StrongSurface
                ) {
                    ExerciseBrowserContent(
                        exercises = state.availableExercises,
                        isLoading = state.isExercisesLoading,
                        onSearch = viewModel::loadExercises,
                        onAddExercises = { exercises ->
                            viewModel.addExercisesToSession(exercises)
                            showExerciseSheet = false
                        }
                    )
                }
            }

            if (state.workoutSuccessStats != null) {
                ModalBottomSheet(
                    onDismissRequest = { 
                        viewModel.onSessionCompletedNavigated()
                    },
                    dragHandle = null,
                    containerColor = StrongSurface
                ) {
                    WorkoutSuccessContent(
                        stats = state.workoutSuccessStats!!,
                        onDone = {
                            viewModel.onSessionCompletedNavigated()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LiveWorkoutHeader(
    templateName: String,
    elapsedSeconds: Long,
    isTimerRunning: Boolean,
    isRestActive: Boolean,
    restSecondsRemaining: Int,
    restTotalSeconds: Int,
    onMinimize: () -> Unit
) {
    val timeString = if (elapsedSeconds >= 3600) {
        String.format("%d:%02d:%02d", elapsedSeconds / 3600, (elapsedSeconds % 3600) / 60, elapsedSeconds % 60)
    } else {
         String.format("%d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
    }

    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val offset: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }
    var lastTime by remember { mutableStateOf(0L) }
    var maxVelocity by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StrongBackground)
            .offset { IntOffset(x = 0, y = offset.value.toInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        lastTime = System.currentTimeMillis()
                        maxVelocity = 0f
                    },
                    onDragEnd = {
                        val thresholdPx = with(density) { 120.dp.toPx() }
                        val velocityThresholdPx = with(density) { 500.dp.toPx() }
                        val shouldMinimize = offset.value > thresholdPx || maxVelocity > velocityThresholdPx

                        if (shouldMinimize) {
                            // Haptic feedback
                            if (vibrator.hasVibrator()) {
                                vibrator.vibrate(VibrationEffect.createOneShot(20, 120))
                            }
                            // Animate off-screen quickly
                            coroutineScope.launch {
                                offset.animateTo(
                                    targetValue = with(density) { 2000.dp.toPx() },
                                    animationSpec = tween(durationMillis = 150, easing = LinearEasing)
                                )
                                onMinimize()
                            }
                        } else {
                            // Snap back with spring
                            coroutineScope.launch {
                                offset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f)
                                )
                            }
                        }
                    }
                ) { change, dragAmount ->
                    val currentTime = System.currentTimeMillis()
                    val timeDelta = currentTime - lastTime
                    if (timeDelta > 0) {
                        val velocity = dragAmount * 1000f / timeDelta
                        if (abs(velocity) > maxVelocity) {
                            maxVelocity = abs(velocity)
                        }
                    }
                    lastTime = currentTime

                    if (dragAmount > 0) {
                        coroutineScope.launch {
                            offset.snapTo(offset.value + dragAmount)
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(width = 40.dp, height = 5.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(Color.Gray.copy(alpha = 0.3f))
                .align(Alignment.CenterHorizontally)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PERSONAL SESSION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(templateName.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StrongTextPrimary, maxLines = 1)
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(StrongSecondaryBackground)
                    .border(1.dp, StrongTextPrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRestActive) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFFFA500), modifier = Modifier.size(14.dp))
                        Text(formatSeconds(restSecondsRemaining.toLong()), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA500))
                    } else {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = StrongTextPrimary.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                        Text(timeString, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StrongTextPrimary)
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isTimerRunning) StrongGreen else Color(0xFFFFA500)))
                    }
                }
            }
        }

        if (isRestActive && restTotalSeconds > 0) {
            RestProgressBar(
                progress = restSecondsRemaining.toFloat() / restTotalSeconds.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun RestProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFA500),
    backgroundColor: Color = Color.Gray.copy(alpha = 0.2f)
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "restProgress"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = animatedProgress)
                .background(color)
        )
    }
}

@Composable
fun LiveWorkoutControls(
    isTimerRunning: Boolean,
    onTogglePause: () -> Unit,
    onFinish: () -> Unit,
    isBlank: Boolean,
    onMicTap: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(StrongSecondaryBackground.copy(alpha = 0.95f))
                .border(1.dp, StrongTextPrimary.copy(alpha = 0.05f), RoundedCornerShape(30.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onMicTap,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = StrongBlue),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
            }
            
            Button(
                onClick = onTogglePause,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isTimerRunning) StrongBlue else Color(0xFFFFA500))
            ) {
                Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isTimerRunning) "Pause" else "Resume", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Button(
                onClick = onFinish,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isBlank) StrongRed.copy(alpha = 0.8f) else StrongGreen)
            ) {
                Text(if (isBlank) "Cancel" else "Finish", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ExerciseCard(
    exercise: WorkoutExerciseUi,
    focusedTarget: FocusTarget?,
    activeInputText: String,
    onTriggerInput: (FocusTarget) -> Unit,
    onSetToggle: (WorkoutSetUi) -> Unit,
    onAddSet: () -> Unit,
    onRemove: () -> Unit,
    onToggleSuperset: () -> Unit = {}
) {
    val isInSuperset = exercise.superSetId != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(StrongSecondaryBackground)
            .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.exerciseName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StrongTextPrimary)
                if (isInSuperset) {
                    Text("Superset", fontSize = 11.sp, color = StrongBlue.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Superset toggle button — mirrors iOS link/unlink gesture
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isInSuperset) StrongBlue.copy(alpha = 0.15f) else StrongBackground.copy(alpha = 0.5f))
                        .clickable { onToggleSuperset() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isInSuperset) Icons.Default.Link else Icons.Default.LinkOff,
                        contentDescription = if (isInSuperset) "Unlink from superset" else "Create superset",
                        tint = if (isInSuperset) StrongBlue else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(StrongBackground.copy(alpha = 0.5f))
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("-", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray.copy(alpha=0.5f))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    }
                }

                Box {
                    var expanded by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(StrongBackground.copy(alpha = 0.5f))
                            .clickable { expanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = StrongSecondaryBackground) {
                        DropdownMenuItem(text = { Text("Remove Exercise", color = StrongRed) }, onClick = { expanded = false; onRemove() })
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SET", modifier = Modifier.width(40.dp), color = Color.Gray.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("KG", modifier = Modifier.width(70.dp), color = Color.Gray.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Text("REPS", modifier = Modifier.width(65.dp), color = Color.Gray.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Text("RPE", modifier = Modifier.width(35.dp), color = Color.Gray.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(40.dp))
        }
        
        exercise.sets.forEachIndexed { index, set ->
            SetRow(
                exerciseId = exercise.exerciseId,
                setIndex = index,
                set = set,
                focusedTarget = focusedTarget,
                activeInputText = activeInputText,
                onTriggerInput = onTriggerInput,
                onToggle = { onSetToggle(set) }
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(StrongBlue.copy(alpha = 0.08f))
                .clickable { onAddSet() },
            contentAlignment = Alignment.Center
        ) {
            Text("+ Add Set", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StrongBlue)
        }
    }
}

@Composable
fun SetRow(
    exerciseId: String,
    setIndex: Int,
    set: WorkoutSetUi,
    focusedTarget: FocusTarget?,
    activeInputText: String,
    onTriggerInput: (FocusTarget) -> Unit,
    onToggle: () -> Unit
) {
    val isWeightFocused = focusedTarget?.exerciseId == exerciseId && focusedTarget.setIndex == setIndex && focusedTarget.field == SessionFocusField.WEIGHT
    val isRepsFocused = focusedTarget?.exerciseId == exerciseId && focusedTarget.setIndex == setIndex && focusedTarget.field == SessionFocusField.REPS

    val weightText = if (isWeightFocused && activeInputText.isNotEmpty()) activeInputText else if (set.weight.isEmpty() || set.weight == "0.0" || set.weight == "0") "-" else set.weight
    val repsText = if (isRepsFocused && activeInputText.isNotEmpty()) activeInputText else if (set.reps.isEmpty() || set.reps == "0") "-" else set.reps

    val isGhostSet = set.logId == null
    val statusBadgeColor = when (set.status) {
        SetStatus.WARM_UP -> Color(0xFFFF9800)
        SetStatus.DROP_SET -> Color(0xFF9C27B0)
        SetStatus.FAILURE -> Color(0xFFF44336)
        SetStatus.NORMAL -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    set.isCompleted -> StrongGreen.copy(alpha = 0.12f)
                    isGhostSet -> Color.Gray.copy(alpha = 0.04f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (set.isCompleted) StrongGreen else Color.Gray.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(set.setNumber.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (set.isCompleted) Color.White else Color.Gray)
            }
            if (set.status != SetStatus.NORMAL) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusBadgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = set.status.indicator,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        
        Box(
            modifier = Modifier
                .width(70.dp).height(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isWeightFocused) StrongSecondaryBackground else StrongInputBackground.copy(alpha = 0.3f))
                .border(if (isWeightFocused) 2.dp else 0.dp, if (isWeightFocused) StrongBlue else Color.Transparent, RoundedCornerShape(10.dp))
                .clickable { onTriggerInput(FocusTarget(exerciseId, setIndex, SessionFocusField.WEIGHT)) },
            contentAlignment = Alignment.Center
        ) {
            Text(weightText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StrongTextPrimary)
        }
        Spacer(Modifier.weight(1f))
        
        Box(
            modifier = Modifier
                .width(65.dp).height(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isRepsFocused) StrongSecondaryBackground else StrongInputBackground.copy(alpha = 0.3f))
                .border(if (isRepsFocused) 2.dp else 0.dp, if (isRepsFocused) StrongBlue else Color.Transparent, RoundedCornerShape(10.dp))
                .clickable { onTriggerInput(FocusTarget(exerciseId, setIndex, SessionFocusField.REPS)) },
            contentAlignment = Alignment.Center
        ) {
            Text(repsText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StrongTextPrimary)
        }
        Spacer(Modifier.weight(1f))
        
        Text("RPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.width(35.dp), textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (set.isCompleted) StrongGreen.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (set.isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = StrongGreen, modifier = Modifier.size(18.dp))
                } else {
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.15f)))
                }
            }
        }
    }
}

@Composable
fun CustomNumericKeyboard(
    text: String,
    onTextChange: (String) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    isWeight: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(".", "0", "DEL")
            )
            for (row in keys) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (key in row) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StrongInputBackground.copy(alpha = 0.8f))
                                .clickable {
                                    if (key == "DEL") {
                                        if (text.isNotEmpty()) onTextChange(text.dropLast(1))
                                    } else {
                                        if (key == "." && text.contains(".")) return@clickable
                                        onTextChange(text + key)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "DEL") {
                                Text("DEL", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StrongTextPrimary)
                            } else {
                                Text(key, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = StrongTextPrimary)
                            }
                        }
                    }
                }
            }
        }
        
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(8.dp)).background(StrongSecondaryBackground).clickable { onDismiss() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = StrongTextPrimary)
            }
            Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(8.dp)).background(StrongSecondaryBackground).clickable { onAction() }, contentAlignment = Alignment.Center) {
                if (isWeight) {
                    Icon(Icons.Default.Apps, contentDescription = null, tint = StrongTextPrimary)
                } else {
                    Text("RPE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StrongTextPrimary)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(8.dp)).background(StrongSecondaryBackground).clickable { 
                    val current = text.toDoubleOrNull() ?: 0.0
                    val decrement = if (isWeight) 1.25 else 1.0
                    val newVal = maxOf(0.0, current - decrement)
                    onTextChange(if (newVal == kotlin.math.floor(newVal)) newVal.toInt().toString() else newVal.toString())
                }, contentAlignment = Alignment.Center) {
                    Text("-", fontSize = 20.sp, color = StrongTextPrimary)
                }
                Box(modifier = Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(8.dp)).background(StrongSecondaryBackground).clickable { 
                    val current = text.toDoubleOrNull() ?: 0.0
                    val increment = if (isWeight) 1.25 else 1.0
                    val newVal = current + increment
                    onTextChange(if (newVal == kotlin.math.floor(newVal)) newVal.toInt().toString() else newVal.toString())
                }, contentAlignment = Alignment.Center) {
                    Text("+", fontSize = 20.sp, color = StrongTextPrimary)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(8.dp)).background(StrongBlue).clickable { onNext() }, contentAlignment = Alignment.Center) {
                Text(if (isWeight) "Next" else "Log", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

private fun formatSeconds(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}

@Composable
fun PlateCalculatorOverlay(
    weightText: String,
    onWeightChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val barWeight = 20.0
    val currentWeight = weightText.toDoubleOrNull() ?: 0.0
    val availablePlates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    fun addPlate(plate: Double) {
        val newWeight = if (currentWeight == 0.0) barWeight + (plate * 2) else currentWeight + (plate * 2)
        onWeightChange(if (newWeight == kotlin.math.floor(newWeight)) newWeight.toInt().toString() else newWeight.toString())
    }

    fun clearPlates() {
        onWeightChange(barWeight.toInt().toString())
    }

    fun calculatePlates(totalWeight: Double): List<Double> {
        var remaining = (totalWeight - barWeight) / 2.0
        val result = mutableListOf<Double>()
        for (plate in availablePlates) {
            while (remaining >= plate) {
                result.add(plate)
                remaining -= plate
            }
        }
        return result
    }

    val currentPlates = calculatePlates(currentWeight)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${if (currentWeight == kotlin.math.floor(currentWeight)) currentWeight.toInt() else currentWeight} kg",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = StrongTextPrimary
        )
        Text("Bar: ${barWeight.toInt()} kg", fontSize = 14.sp, color = StrongTextSecondary)

        Spacer(Modifier.height(20.dp))

        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp).background(Color.Gray, RoundedCornerShape(4.dp)))
            Row(modifier = Modifier.fillMaxWidth(0.6f), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(40.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        currentPlates.reversed().forEach { plate -> PlateVisual(plate) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        currentPlates.forEach { plate -> PlateVisual(plate) }
                    }
                    Box(modifier = Modifier.width(40.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Tap plates to add", fontSize = 12.sp, color = StrongTextSecondary)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            availablePlates.forEach { plate ->
                Button(
                    onClick = { addPlate(plate) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = plateColor(plate)),
                    modifier = Modifier.size(60.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (plate == kotlin.math.floor(plate)) plate.toInt().toString() else plate.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Button(
                onClick = { clearPlates() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = StrongRed),
                modifier = Modifier.size(60.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = StrongSecondaryBackground)) {
            Text("Back to Keyboard", color = StrongTextPrimary)
        }
    }
}

@Composable
fun PlateVisual(weight: Double) {
    val height = when (weight) {
        25.0 -> 90.dp
        20.0 -> 90.dp
        15.0 -> 80.dp
        10.0 -> 70.dp
        5.0 -> 60.dp
        2.5 -> 50.dp
        1.25 -> 40.dp
        else -> 40.dp
    }
    val width = when (weight) {
        25.0 -> 12.dp
        20.0 -> 10.dp
        15.0 -> 10.dp
        10.0 -> 10.dp
        5.0 -> 8.dp
        2.5 -> 8.dp
        1.25 -> 8.dp
        else -> 8.dp
    }
    Box(modifier = Modifier.width(width).height(height).background(plateColor(weight), RoundedCornerShape(4.dp)))
}

fun plateColor(weight: Double): Color {
    return when (weight) {
        25.0 -> Color.Red
        20.0 -> Color.Blue
        15.0 -> Color.Yellow
        10.0 -> Color.Green
        5.0 -> Color.White.copy(alpha = 0.8f)
        2.5 -> Color.Red.copy(alpha = 0.6f)
        1.25 -> Color.Blue.copy(alpha = 0.6f)
        else -> Color.Gray
    }
}

@Composable
fun RPEPickerOverlay(
    currentRpe: Double?,
    onRpeSelected: (Double?) -> Unit,
    onDismiss: () -> Unit
) {
    val rpeValues = listOf(10.0, 9.5, 9.0, 8.5, 8.0, 7.5, 7.0, 6.5, 6.0, 5.5, 5.0)

    fun rpeDescription(value: Double): String {
        return when (value) {
            10.0 -> "Max Effort"
            9.5 -> "Maybe 0 reps left"
            9.0 -> "1 rep left"
            8.5 -> "Maybe 1-2 reps left"
            8.0 -> "2 reps left"
            7.5 -> "Maybe 2-3 reps left"
            7.0 -> "3 reps left"
            6.5 -> "Maybe 3-4 reps left"
            6.0 -> "4+ reps left"
            5.5 -> "Warm up"
            5.0 -> "Light"
            else -> ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Select RPE", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StrongTextPrimary)
            TextButton(onClick = { onRpeSelected(null) }) {
                Text("Clear", color = StrongRed)
            }
        }

        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            items(rpeValues.size) { index ->
                val value = rpeValues[index]
                val isSelected = currentRpe == value
                Button(
                    onClick = { onRpeSelected(value) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) StrongBlue else StrongInputBackground.copy(alpha = 0.5f),
                        contentColor = if (isSelected) Color.White else StrongTextPrimary
                    ),
                    modifier = Modifier.width(80.dp).height(100.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(
                            text = if (value == kotlin.math.floor(value)) value.toInt().toString() else value.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = rpeDescription(value),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }

        Text("RPE (Rate of Perceived Exertion) helps track intensity.", fontSize = 12.sp, color = StrongTextSecondary)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = StrongSecondaryBackground)) {
            Text("Close", color = StrongTextPrimary)
        }
    }
}
