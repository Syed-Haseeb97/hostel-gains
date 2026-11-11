package com.hostelgains.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.domain.workout.AdjustedExercise
import com.hostelgains.ui.components.AiModeBadge
import com.hostelgains.ui.components.ExpandableCoachCard
import com.hostelgains.ui.components.HighCarbBadge
import com.hostelgains.ui.components.HostelGainsLogoBadge
import com.hostelgains.ui.components.Mannequin3DView
import com.hostelgains.ui.components.ProteinDeficitBadge
import com.hostelgains.ui.theme.BorderStroke
import com.hostelgains.ui.theme.Ember
import com.hostelgains.ui.theme.Ink
import com.hostelgains.ui.theme.Muted
import com.hostelgains.ui.theme.Panel
import com.hostelgains.ui.theme.SuccessGreen
import com.hostelgains.ui.theme.Surface
import com.hostelgains.ui.theme.TextPrimary
import com.hostelgains.ui.theme.TextSecondary
import com.hostelgains.ui.theme.Volt
import com.hostelgains.ui.viewmodel.WorkoutViewModel

val PPL_DAYS_ABBR = listOf("Day 1\nPull A", "Day 2\nPush A", "Day 3\nLegs", "Day 4\nPull B", "Day 5\nPush B", "Day 6\nPump", "Day 7\nRest")

@Composable
fun WorkoutPlanScreen(
    viewModel: WorkoutViewModel,
    onNavigateToViewer: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    isPremiumAi: Boolean = false,
    onUnlockAiClicked: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var promptInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // AI Mode Badge
            AiModeBadge(
                isPremium = isPremiumAi,
                modifier = Modifier.clickable { if (!isPremiumAi) onUnlockAiClicked() }
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "V-TAPER PPL PLAN",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Dynamic Hypertrophy & Rebalancing Engine",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                HostelGainsLogoBadge(size = 46.dp)
            }
        }

        // 7-Day PPL Selector Carousel
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PPL_DAYS_ABBR.indices.toList()) { index ->
                    val isSelected = index == state.selectedDayIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Volt else Panel)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Volt else BorderStroke,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectDay(index) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = PPL_DAYS_ABBR[index],
                            color = if (isSelected) Ink else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            lineHeight = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Active Focus & Compensatory Adjustment Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Panel)
                    .border(1.dp, BorderStroke, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.basePlan.dayName.uppercase(),
                            color = Volt,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${state.completedExerciseIds.size}/${state.adjustedPlan.exercises.size} DONE",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = state.basePlan.focusArea,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (state.basePlan.guidanceNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.basePlan.guidanceNotes,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    // Active triggers banner
                    if (state.adjustedPlan.activeTriggers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.adjustedPlan.activeTriggers.forEach { trigger ->
                                when {
                                    trigger.contains("High-Carb", true) || trigger.contains("Carbohydrate", true) -> {
                                        HighCarbBadge(modifier = Modifier.fillMaxWidth())
                                    }
                                    trigger.contains("Protein Deficit", true) -> {
                                        ProteinDeficitBadge(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI Rebalanced Coach Rationale Card (if rebalance active)
        item {
            if (state.rebalanceState.isRebalanced) {
                ExpandableCoachCard(
                    isExpanded = state.rebalanceState.isCardExpanded,
                    onToggleExpanded = { viewModel.toggleCoachCardExpanded() },
                    skippedExerciseName = state.rebalanceState.skippedExerciseName ?: "Exercise",
                    reason = state.rebalanceState.reason,
                    coachMessage = state.rebalanceState.coachMessage,
                    compensations = state.rebalanceState.compensations,
                    onResetToDefault = { viewModel.resetRebalance() }
                )
            }
        }

        // Report Skipped / Unavailable / Painful Exercise Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "DYNAMIC REBALANCER & EQUIPMENT FALLBACK",
                    color = Volt,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Report equipment busy or pain (e.g., \"Wrist hurts, skip Incline DB\", \"No Lat Pulldown machine\"):",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = {
                            Text(
                                text = "e.g., No Lat Pulldown machine today",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Ink,
                            unfocusedContainerColor = Ink,
                            focusedBorderColor = Volt,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            viewModel.rebalanceFromPrompt(promptInput)
                            promptInput = ""
                        })
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            viewModel.rebalanceFromPrompt(promptInput)
                            promptInput = ""
                        },
                        enabled = promptInput.isNotBlank(),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (promptInput.isNotBlank()) Volt else Surface)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Submit rebalance prompt",
                            tint = if (promptInput.isNotBlank()) Ink else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Section Title: Exercises
        item {
            Text(
                text = "SCHEDULED MOVEMENTS (${state.adjustedPlan.exercises.size})",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }

        // Exercise List Cards
        items(state.adjustedPlan.exercises, key = { it.baseExercise.id }) { adjExercise ->
            val isCompleted = state.completedExerciseIds.contains(adjExercise.baseExercise.id)
            val isSkipped = state.rebalanceState.isRebalanced &&
                    adjExercise.baseExercise.id == state.rebalanceState.skippedExerciseId

            ExerciseCard(
                exercise = adjExercise,
                isCompleted = isCompleted,
                isSkipped = isSkipped,
                onToggleCompleted = { viewModel.toggleExerciseCompleted(adjExercise.baseExercise.id) },
                onSkip = { viewModel.skipExercise(adjExercise.baseExercise.id) },
                onView3D = { onNavigateToViewer(adjExercise.baseExercise.name) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: AdjustedExercise,
    isCompleted: Boolean,
    isSkipped: Boolean,
    onToggleCompleted: () -> Unit,
    onSkip: () -> Unit,
    onView3D: () -> Unit
) {
    val borderColor = when {
        isSkipped -> Ember.copy(alpha = 0.4f)
        isCompleted -> SuccessGreen.copy(alpha = 0.5f)
        exercise.isCarbBonusApplied -> Volt.copy(alpha = 0.6f)
        exercise.isProteinCapApplied -> Ember.copy(alpha = 0.6f)
        else -> BorderStroke
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Completion check & Exercise Title
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) SuccessGreen else Surface)
                            .border(1.5.dp, if (isCompleted) SuccessGreen else Muted, CircleShape)
                            .clickable { if (!isSkipped) onToggleCompleted() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Ink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = exercise.baseExercise.name,
                            color = if (isCompleted || isSkipped) TextSecondary else Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            style = if (isCompleted) androidx.compose.ui.text.TextStyle(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            ) else androidx.compose.ui.text.TextStyle()
                        )
                        Text(
                            text = "${exercise.baseExercise.targetMuscle.name} · Rest ${exercise.baseExercise.restSeconds}s",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Right: Sets & Reps
                Column(horizontalAlignment = Alignment.End) {
                    if (isSkipped) {
                        Text(
                            text = "SKIPPED",
                            color = Ember,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    } else {
                        Text(
                            text = "${exercise.effectiveSets} SETS",
                            color = when {
                                exercise.isCarbBonusApplied -> Volt
                                exercise.isProteinCapApplied -> Ember
                                else -> Volt
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = exercise.effectiveReps,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Adjustment notes & Form cues
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                exercise.adjustmentNotes.forEach { note ->
                    val noteColor = when {
                        note.contains("+1 set", true) || note.contains("AI Rebalanced", true) -> Volt
                        note.contains("Capped", true) || note.contains("Deficit", true) -> Ember
                        else -> TextSecondary
                    }
                    Text(
                        text = "• $note",
                        color = noteColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Cue: ${exercise.baseExercise.formNotes}",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inline 3D Mannequin Preview Thumbnail & Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Inline 3D Mannequin Preview (compact canvas)
                Box(
                    modifier = Modifier
                        .size(width = 85.dp, height = 60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Ink)
                        .border(1.dp, BorderStroke, RoundedCornerShape(8.dp))
                        .clickable { onView3D() }
                ) {
                    Mannequin3DView(
                        exerciseName = exercise.baseExercise.name,
                        targetMuscles = listOf(exercise.baseExercise.targetMuscle.name),
                        modifier = Modifier.fillMaxSize(),
                        isPlaying = true,
                        speed = 1.0f,
                        yawDeg = -35f,
                        pitchDeg = 15f
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Inline "Skip / Can't Do" Action
                    if (!isSkipped) {
                        OutlinedButton(
                            onClick = onSkip,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ember),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Ember.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotInterested,
                                contentDescription = null,
                                tint = Ember,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Skip / Can't Do",
                                color = Ember,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 3D View Action
                    Button(
                        onClick = onView3D,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Surface,
                            contentColor = Volt
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Volt,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "3D Guide",
                            color = Volt,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
