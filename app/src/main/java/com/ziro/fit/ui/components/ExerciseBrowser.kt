package com.ziro.fit.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ziro.fit.model.Exercise
import com.ziro.fit.ui.theme.*
import com.ziro.fit.util.search.FuzzySearchEngine

/** Normalizes a string for fuzzy matching: lowercase + strip special characters */
private fun normalize(text: String): String =
    text.lowercase().replace(Regex("[^a-z0-9\\s]"), "").trim()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExerciseBrowserContent(
    exercises: List<Exercise>,
    isLoading: Boolean,
    onSearch: (String) -> Unit,
    onAddExercises: (List<Exercise>) -> Unit,
    singleSelect: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedExercises = remember { mutableStateListOf<Exercise>() }
    
    // Filters state
    var selectedBodyPart by remember { mutableStateOf<String?>(null) }
    var selectedEquipment by remember { mutableStateOf<String?>(null) }

    val availableBodyParts = remember(exercises) { exercises.mapNotNull { it.muscleGroup }.filter { it.isNotBlank() }.distinct().sorted() }
    val availableEquipment = remember(exercises) { exercises.mapNotNull { it.equipment }.filter { it.isNotBlank() }.distinct().sorted() }

    // Client-side fuzzy search — matches despite typos, partial words, and special characters.
    // Strips special characters from both query and exercise names for clean matching.
    val searchResults = remember(searchQuery, exercises) {
        if (searchQuery.isBlank()) {
            exercises
        } else {
            val normalizedQuery = normalize(searchQuery)
            exercises.map { exercise ->
                val normalizedName = normalize(exercise.name)
                val score = when {
                    normalizedName == normalizedQuery -> 4.0  // exact match
                    normalizedName.startsWith(normalizedQuery) -> 3.0  // prefix match
                    normalizedName.contains(normalizedQuery) -> 2.0  // substring match
                    else -> FuzzySearchEngine.calculateSimilarity(normalizedQuery, normalizedName)
                }
                exercise to score
            }.filter { it.second >= 0.3 }
                .sortedByDescending { it.second }
                .map { it.first }
        }
    }

    val filteredExercises = remember(searchResults, selectedBodyPart, selectedEquipment) {
        searchResults.filter { ex ->
            (selectedBodyPart == null || ex.muscleGroup == selectedBodyPart) &&
            (selectedEquipment == null || ex.equipment == selectedEquipment)
        }
    }

    val groupedExercises = remember(filteredExercises) {
        filteredExercises.groupBy { it.name.firstOrNull()?.uppercase() ?: "#" }
    }

    Box(modifier = Modifier.fillMaxSize().background(StrongBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Exercise", 
                    style = MaterialTheme.typography.titleLarge, 
                    color = StrongTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = StrongBlue, strokeWidth = 2.dp)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search exercises...", color = StrongTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = StrongTextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = "" 
                            onSearch("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = StrongTextSecondary)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = StrongTextPrimary,
                    unfocusedTextColor = StrongTextPrimary,
                    focusedContainerColor = StrongSecondaryBackground,
                    unfocusedContainerColor = StrongSecondaryBackground,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                FilterMenu(
                    title = selectedBodyPart ?: "Any Body Part",
                    isActive = selectedBodyPart != null,
                    options = availableBodyParts,
                    onSelect = { selectedBodyPart = it },
                    onClear = { selectedBodyPart = null }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                FilterMenu(
                    title = selectedEquipment ?: "Any Equipment",
                    isActive = selectedEquipment != null,
                    options = availableEquipment,
                    onSelect = { selectedEquipment = it },
                    onClear = { selectedEquipment = null }
                )
            }

            HorizontalDivider(color = StrongDivider)

            // List
            if (filteredExercises.isEmpty() && !isLoading) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp), 
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(48.dp), tint = StrongTextSecondary.copy(alpha = 0.5f))
                        Text(
                            text = if (searchQuery.isEmpty()) "No exercises available" else "Couldn't find '${searchQuery}'?",
                            color = StrongTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "You can create a custom exercise in your library.",
                            color = StrongTextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = if (selectedExercises.isNotEmpty()) 100.dp else 20.dp)
                ) {
                    groupedExercises.forEach { (letter, exList) ->
                        stickyHeader {
                            Text(
                                text = letter,
                                color = StrongTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StrongBackground)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        items(exList, key = { it.id }) { exercise ->
                            val isSelected = selectedExercises.any { it.id == exercise.id }
                            ExerciseRowItem(
                                exercise = exercise,
                                isSelected = isSelected,
                                onClick = {
                                    if (singleSelect) {
                                        onAddExercises(listOf(exercise))
                                    } else {
                                        if (isSelected) {
                                            selectedExercises.removeAll { it.id == exercise.id }
                                        } else {
                                            selectedExercises.add(exercise)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Floating Add Button (hidden in singleSelect mode)
        AnimatedVisibility(
            visible = selectedExercises.isNotEmpty() && !singleSelect,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Button(
                onClick = { onAddExercises(selectedExercises.toList()) },
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape),
                colors = ButtonDefaults.buttonColors(containerColor = StrongBlue),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Add ${selectedExercises.size} ${if (selectedExercises.size == 1) "Exercise" else "Exercises"}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun FilterMenu(
    title: String,
    isActive: Boolean,
    options: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .background(
                    color = if (isActive) StrongBlue.copy(alpha = 0.15f) else StrongSecondaryBackground,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (isActive) Color.Transparent else StrongDivider,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = if (isActive) StrongBlue else StrongTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isActive) StrongBlue else StrongTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(StrongSecondaryBackground)
        ) {
            DropdownMenuItem(
                text = { Text("Any", color = StrongTextPrimary) },
                onClick = { 
                    onClear()
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = StrongTextPrimary) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ExerciseRowItem(
    exercise: Exercise,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) StrongBlue.copy(alpha = 0.1f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image Thumbnail
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StrongSecondaryBackground),
            contentAlignment = Alignment.Center
        ) {
            if (!exercise.videoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = exercise.videoUrl,
                    contentDescription = exercise.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = StrongTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                color = StrongTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            val details = listOfNotNull(exercise.muscleGroup, exercise.equipment).joinToString(" • ")
            if (details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = details,
                    color = StrongTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Selection Indicator
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = StrongGreen,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = StrongBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
