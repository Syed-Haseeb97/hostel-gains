package com.hostelgains.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled._3dRotation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hostelgains.domain.workout.AdjustedExercise
import com.hostelgains.domain.workout.VTaperWorkoutRepository
import com.hostelgains.ui.components.AiModeChip
import com.hostelgains.ui.components.CoachRebalanceCard
import com.hostelgains.ui.components.HgCard
import com.hostelgains.ui.components.HgTextField
import com.hostelgains.ui.components.ProgressRing
import com.hostelgains.ui.components.ScreenHeader
import com.hostelgains.ui.components.SectionLabel
import com.hostelgains.ui.components.StatusPill
import com.hostelgains.ui.theme.Canvas0
import com.hostelgains.ui.theme.Danger
import com.hostelgains.ui.theme.Ignite
import com.hostelgains.ui.theme.Info
import com.hostelgains.ui.theme.OnIgnite
import com.hostelgains.ui.theme.Outline
import com.hostelgains.ui.theme.Success
import com.hostelgains.ui.theme.Surface1
import com.hostelgains.ui.theme.Surface2
import com.hostelgains.ui.theme.Surface3
import com.hostelgains.ui.theme.TextHigh
import com.hostelgains.ui.theme.TextLow
import com.hostelgains.ui.theme.TextMid
import com.hostelgains.ui.theme.Warning
import com.hostelgains.ui.three.Exercise3DView
import com.hostelgains.ui.viewmodel.WorkoutViewModel

private val QUICK_ISSUES = listOf(
    "Swap squats for leg press",
    "Add 3 sets of hammer curls",
    "Dumbbell only today",
    "Shorten workout to 30 mins",
    "No lat pulldown machine today",
    "Wrist hurts on presses"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutPlanScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier,
    profileViewModel: com.hostelgains.ui.viewmodel.PhysiqueProfileViewModel? = null,
    isPremiumAi: Boolean = false,
    onUnlockAiClicked: () -> Unit = {},
    onNavigateToViewer: (exerciseId: String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val week = remember(state.targetBuild) { viewModel.getAllPlans() }
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current
    var prompt by rememberSaveable { mutableStateOf("") }
    var showBuildSelector by rememberSaveable { mutableStateOf(false) }
    val daysState = rememberLazyListState()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    if (showBuildSelector && profileViewModel != null) {
        PhysiqueSelectionScreen(
            viewModel = profileViewModel,
            onDismiss = { showBuildSelector = false },
            onConfirmed = { selectedBuild, selectedLevel ->
                viewModel.selectPhysiqueBuildAndLevel(selectedBuild, selectedLevel)
                showBuildSelector = false
            }
        )
        return
    }

    LaunchedEffect(state.selectedDayIndex) {
        daysState.animateScrollToItem((state.selectedDayIndex - 1).coerceAtLeast(0))
    }

    val exercises = state.adjustedPlan.exercises
    val doneCount = exercises.count { it.baseExercise.id in state.completedExerciseIds }
    val isRest = state.basePlan.isActiveRest
    val firstActiveExerciseId = remember(exercises, state.completedExerciseIds, state.rebalanceState) {
        exercises.firstOrNull { ex ->
            ex.baseExercise.id !in state.completedExerciseIds &&
                !(state.rebalanceState.isRebalanced && ex.baseExercise.id == state.rebalanceState.skippedExerciseId)
        }?.baseExercise?.id
    }

    fun submitPrompt(text: String) {
        if (text.isBlank()) return
        viewModel.rebalanceFromPrompt(text)
        prompt = ""
        focusManager.clearFocus()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas0),
        contentPadding = PaddingValues(top = top + 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(
                eyebrow = "${state.targetBuild.title} · ${state.experienceLevel.title.uppercase()} · ${state.targetBuild.repRangeSummary}",
                title = "Train",
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                AiModeChip(isPremium = isPremiumAi, onClick = onUnlockAiClicked)
            }
        }

        // ── Active Physique Build Pathway Card ────────────────────────────────
        item {
            HgCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clickable {
                        if (profileViewModel != null) {
                            showBuildSelector = true
                        }
                    },
                containerColor = Surface2,
                borderColor = Ignite.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(state.targetBuild.visualBadge, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    state.targetBuild.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextHigh
                                )
                                Spacer(Modifier.width(6.dp))
                                StatusPill(state.targetBuild.archetypeTag, color = Ignite)
                                Spacer(Modifier.width(4.dp))
                                StatusPill(state.experienceLevel.title, color = Info)
                                if (state.isAiGeneratedRoutine) {
                                    Spacer(Modifier.width(4.dp))
                                    StatusPill("✨ Gemini AI", color = Success)
                                }
                            }
                            Text(
                                "${state.targetBuild.repRangeSummary} · ${state.targetBuild.targetAesthetic}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMid,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (profileViewModel != null) {
                        Text(
                            "Change",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Ignite
                        )
                    }
                }
            }
        }

        // ── AI Generation Live Feedback Card ──────────────────────────────────
        if (state.isGeneratingAiRoutine) {
            item {
                HgCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    containerColor = Surface2,
                    borderColor = Ignite
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = Ignite, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Gemini 1.5 Flash AI Periodization",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextHigh
                            )
                            Text(
                                "Generating 7-day routine tailored for ${state.experienceLevel.title} level & ${state.targetBuild.title}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMid
                            )
                        }
                    }
                }
            }
        }

        // ── AI Generation Error Card ──────────────────────────────────────────
        if (state.aiGenerationError != null && !state.isGeneratingAiRoutine) {
            item {
                HgCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    containerColor = Surface2,
                    borderColor = Danger
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "AI Routine Generation Error",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Danger
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            state.aiGenerationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMid
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { viewModel.generateGeminiRoutine() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, tint = Ignite, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Retry with Gemini", color = Ignite)
                            }
                        }
                    }
                }
            }
        }

        // ── Gemini Coach Rationale Card ───────────────────────────────────────
        if (state.coachRationale.isNotBlank()) {
            item {
                HgCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    containerColor = Surface2,
                    borderColor = Success.copy(alpha = 0.5f)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Gemini Coach Rationale · ${state.experienceLevel.title}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Success
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                state.coachRationale,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextHigh
                            )
                        }
                    }
                }
            }
        }

        // ── Action Bar (Calibrate with Gemini AI) ─────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (state.isAiGeneratedRoutine) "✨ AI Periodized Routine" else "Baseline Framework",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.isAiGeneratedRoutine) Success else TextLow
                )
                if (!state.isGeneratingAiRoutine) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface2)
                            .border(1.dp, Ignite.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { viewModel.generateGeminiRoutine() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Ignite,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (state.isAiGeneratedRoutine) "Regenerate with Gemini" else "Calibrate with Gemini AI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Ignite
                        )
                    }
                }
            }
        }

        // ── Day selector ──────────────────────────────────────────────────────
        item {
            LazyRow(
                state = daysState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(week, key = { _, d -> d.dayNumber }) { index, day ->
                    DayPill(
                        number = day.dayNumber,
                        label = day.dayName.substringAfter(": ", day.dayName),
                        selected = index == state.selectedDayIndex,
                        isRest = day.isActiveRest,
                        onClick = { viewModel.selectDay(index) }
                    )
                }
            }
        }

        // ── Day overview ──────────────────────────────────────────────────────
        item {
            HgCard(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.basePlan.dayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Ignite
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            state.basePlan.focusArea,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextHigh
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    ProgressRing(
                        progress = if (exercises.isEmpty()) 0f else doneCount / exercises.size.toFloat(),
                        color = Success,
                        strokeWidth = 7.dp,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$doneCount/${exercises.size}", style = MaterialTheme.typography.titleSmall, color = TextHigh)
                            Text("done", style = MaterialTheme.typography.bodySmall, color = TextMid)
                        }
                    }
                }
                if (state.basePlan.guidanceNotes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(state.basePlan.guidanceNotes, style = MaterialTheme.typography.bodyMedium, color = TextMid)
                }
                if (state.adjustedPlan.activeTriggers.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.adjustedPlan.activeTriggers.forEach { trigger ->
                            StatusPill(trigger, triggerColor(trigger), maxLines = 2)
                        }
                    }
                }
            }
        }

        if (state.rebalanceState.isRebalanced) {
            item {
                CoachRebalanceCard(
                    isExpanded = state.rebalanceState.isCardExpanded,
                    onToggleExpanded = { viewModel.toggleCoachCardExpanded() },
                    skippedExerciseName = state.rebalanceState.skippedExerciseName ?: "exercise",
                    reason = state.rebalanceState.reason,
                    coachMessage = state.rebalanceState.coachMessage,
                    compensations = state.rebalanceState.compensations,
                    onResetToDefault = { viewModel.resetRebalance() },
                    isCustomized = state.rebalanceState.isCustomized,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // ── AI Workout Customizer prompt ──────────────────────────────────────
        if (!isRest) {
            item {
                HgCard(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                    Text("Customize Today's Workout with AI", style = MaterialTheme.typography.titleMedium, color = TextHigh)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Ask your coach to swap, add, or adjust exercises (e.g. 'swap squats for leg press', 'add 3 sets of curls', 'dumbbell only').",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMid
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HgTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            placeholder = "e.g. Swap squats for leg press",
                            singleLine = true,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitPrompt(prompt) }),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = { submitPrompt(prompt) },
                            enabled = prompt.isNotBlank(),
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Ignite,
                                contentColor = OnIgnite,
                                disabledContainerColor = Surface3,
                                disabledContentColor = TextLow
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Customize workout")
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QUICK_ISSUES.forEach { issue ->
                            Box(
                                Modifier
                                    .heightIn(min = 36.dp)
                                    .clip(CircleShape)
                                    .background(Surface2)
                                    .clickable(role = Role.Button) { submitPrompt(issue) }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(issue, style = MaterialTheme.typography.labelMedium, color = TextMid)
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionLabel(
                text = if (isRest) "Today's recovery" else "Exercises",
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Text("${exercises.size} movements", style = MaterialTheme.typography.bodySmall, color = TextMid)
            }
        }

        items(exercises, key = { it.baseExercise.id }, contentType = { "exercise_card" }) { ex ->
            val skipped = state.rebalanceState.isRebalanced && ex.baseExercise.id == state.rebalanceState.skippedExerciseId
            val isNextActive = ex.baseExercise.id == firstActiveExerciseId
            ExerciseCard(
                exercise = ex,
                isCompleted = ex.baseExercise.id in state.completedExerciseIds,
                isSkipped = skipped,
                isRest = isRest,
                animatePreview = isNextActive,
                onToggleCompleted = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleExerciseCompleted(ex.baseExercise.id)
                },
                onSkip = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.skipExercise(ex.baseExercise.id)
                },
                onView3D = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigateToViewer(ex.baseExercise.id)
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
@ReadOnlyComposable
private fun triggerColor(trigger: String): Color = when {
    trigger.contains("Carb", ignoreCase = true) -> Warning
    trigger.contains("Protein", ignoreCase = true) -> Danger
    else -> Info
}

@Composable
private fun DayPill(
    number: Int,
    label: String,
    selected: Boolean,
    isRest: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (selected) Ignite else Surface1, label = "dayBg")
    val fg by animateColorAsState(if (selected) OnIgnite else TextHigh, label = "dayFg")
    Column(
        modifier = Modifier
            .widthIn(min = 76.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, if (selected) Ignite else Outline, RoundedCornerShape(18.dp))
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "DAY $number",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) OnIgnite.copy(alpha = 0.7f) else TextLow
        )
        Spacer(Modifier.height(4.dp))
        if (isRest) {
            Icon(Icons.Filled.SelfImprovement, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        } else {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatMuscle(raw: String): String =
    raw.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(
    exercise: AdjustedExercise,
    isCompleted: Boolean,
    isSkipped: Boolean,
    isRest: Boolean,
    animatePreview: Boolean = true,
    onToggleCompleted: () -> Unit,
    onSkip: () -> Unit,
    onView3D: () -> Unit,
    modifier: Modifier = Modifier
) {
    val base = exercise.baseExercise
    val border = when {
        isCompleted -> Success.copy(alpha = 0.45f)
        isSkipped -> Outline
        else -> Outline
    }
    HgCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isSkipped) 0.6f else 1f),
        borderColor = border,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row {
            // Live 3D preview; tapping opens the full Form Lab.
            Box(
                modifier = Modifier
                    .width(92.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClickLabel = "Open 3D form guide", onClick = onView3D)
            ) {
                Exercise3DView(
                    exerciseKey = base.id,
                    targetMuscles = listOf(base.targetMuscle.name.replace('_', ' ')),
                    modifier = Modifier.fillMaxSize(),
                    interactive = false,
                    showGrid = false,
                    isPlaying = animatePreview && !isSkipped && !isCompleted
                )
                Icon(
                    Icons.Filled._3dRotation,
                    contentDescription = null,
                    tint = TextHigh,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(16.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    base.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCompleted) TextMid else TextHigh,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${formatMuscle(base.targetMuscle.name)} · Rest ${base.restSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid
                )
                Spacer(Modifier.height(8.dp))
                if (isSkipped) {
                    StatusPill("Skipped", Danger, icon = Icons.Filled.Block)
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (!isRest) {
                            Text(
                                "${exercise.effectiveSets}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Ignite
                            )
                            Text(
                                " sets",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMid,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            exercise.effectiveReps,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextHigh,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
                val notes = exercise.adjustmentNotes.filterNot { it.startsWith("Baseline") || it.startsWith("Skipped") }
                if (notes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        notes.forEach { StatusPill(it, triggerColor(it), maxLines = 2) }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            base.formNotes,
            style = MaterialTheme.typography.bodySmall,
            color = TextMid,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isSkipped) {
                CardAction(
                    text = if (isCompleted) "Done" else "Mark done",
                    icon = Icons.Filled.Check,
                    onClick = onToggleCompleted,
                    container = if (isCompleted) Success.copy(alpha = 0.16f) else Surface2,
                    content = if (isCompleted) Success else TextHigh,
                    modifier = Modifier.weight(1f)
                )
            }
            CardAction(
                text = "Form 3D",
                icon = Icons.Filled._3dRotation,
                onClick = onView3D,
                container = Surface2,
                content = TextHigh,
                modifier = Modifier.weight(1f)
            )
            if (!isSkipped && !isCompleted && !isRest) {
                // Icon-only so "Mark done" and "Form 3D" keep full labels on narrow phones.
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2)
                        .clickable(role = Role.Button, onClickLabel = "Skip exercise", onClick = onSkip),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Block, contentDescription = "Skip ${base.name}", tint = TextMid, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun CardAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
