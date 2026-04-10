package com.kinetic.trainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kinetic.trainer.data.models.*
import com.kinetic.trainer.ui.components.*
import com.kinetic.trainer.ui.theme.*
import com.kinetic.trainer.ui.viewmodels.WorkoutAssignmentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutAssignmentScreen(
    clientId: String,
    onBack: () -> Unit,
    onAssigned: () -> Unit,
    viewModel: WorkoutAssignmentViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exercises = uiState.scratchExercises
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Templates", "Clone Last", "Scratch")

    var showAddExerciseSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Assign Workout", color = TextPrimary, fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        bottomBar = {
            if (exercises.isNotEmpty()) {
                Surface(color = Background, tonalElevation = 8.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "${exercises.size} exercises added",
                            color = TextMuted, fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        StartButton(
                            text = "Assign Workout",
                            onClick = {
                                scope.launch {
                                    viewModel.saveWorkout(clientId) { onAssigned() }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Tab Row ──────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Surface1,
                contentColor = Lime,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Lime
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(title,
                                color = if (selectedTab == index) Lime else TextMuted,
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> TemplatesTab(uiState.templates) { template ->
                    viewModel.assignTemplate(template.id, clientId) { onAssigned() }
                }
                1 -> CloneLastTab()
                2 -> ScratchTab(
                    exercises = exercises,
                    onAdd = { showAddExerciseSheet = true },
                    onRemove = { viewModel.removeExercise(it) }
                )
            }
        }
    }

    if (showAddExerciseSheet) {
        AddExerciseSheet(
            onAdd = { exercise ->
                viewModel.addExercise(exercise)
                showAddExerciseSheet = false
            },
            onDismiss = { showAddExerciseSheet = false }
        )
    }
}

@Composable
private fun TemplatesTab(
    templates: List<WorkoutTemplate>,
    onSelect: (WorkoutTemplate) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(templates) { template ->
            KineticCard(modifier = Modifier.fillMaxWidth().clickable { onSelect(template) }) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(template.name, color = TextPrimary, fontSize = 15.sp,
                            fontWeight = FontWeight.Bold)
                        Text(template.description, color = TextMuted, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LimeBadge("${template.exercises.size} exercises")
                            LimeBadge("${template.durationMins} min")
                        }
                    }
                    Icon(Icons.Default.Add, null, tint = Lime)
                }
            }
        }
    }
}

@Composable
private fun CloneLastTab() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ContentCopy, null, tint = TextMuted, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Clone from last session", color = TextMuted, fontSize = 14.sp)
            Text("Connect Firestore to enable", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ScratchTab(
    exercises: List<Exercise>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (exercises.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center) {
                        Text("No exercises added yet", color = TextMuted, fontSize = 14.sp)
                    }
                }
            }
            items(exercises.size) { index ->
                val ex = exercises[index]
                KineticCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ex.name, color = TextPrimary, fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold)
                            Text("${ex.sets} sets × ${ex.repsPerSet} reps @ ${ex.targetWeightKg}kg",
                                color = TextMuted, fontSize = 12.sp)
                        }
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Default.Delete, null, tint = Error)
                        }
                    }
                }
            }
        }
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = Lime),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Add Exercise", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseSheet(onAdd: (Exercise) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var weight by remember { mutableStateOf("0") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface1
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Exercise", color = TextPrimary, fontSize = 18.sp,
                fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Exercise name", color = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Lime, unfocusedBorderColor = Surface2,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = sets, onValueChange = { sets = it },
                    label = { Text("Sets", color = TextMuted) }, modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Lime,
                        unfocusedBorderColor = Surface2, focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary))
                OutlinedTextField(value = reps, onValueChange = { reps = it },
                    label = { Text("Reps", color = TextMuted) }, modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Lime,
                        unfocusedBorderColor = Surface2, focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary))
                OutlinedTextField(value = weight, onValueChange = { weight = it },
                    label = { Text("kg", color = TextMuted) }, modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Lime,
                        unfocusedBorderColor = Surface2, focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary))
            }
            StartButton(
                text = "Add Exercise",
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(Exercise(
                            name = name.trim(),
                            sets = sets.toIntOrNull() ?: 3,
                            repsPerSet = reps.toIntOrNull() ?: 10,
                            targetWeightKg = weight.toFloatOrNull() ?: 0f
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
