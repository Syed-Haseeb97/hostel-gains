package com.hostelgains.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled._360
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hostelgains.ui.components.ChoiceChip
import com.hostelgains.ui.components.HgCard
import com.hostelgains.ui.components.SegmentedControl
import com.hostelgains.ui.components.StatusPill
import com.hostelgains.ui.theme.Canvas0
import com.hostelgains.ui.theme.Ignite
import com.hostelgains.ui.theme.OnIgnite
import com.hostelgains.ui.theme.Outline
import com.hostelgains.ui.theme.StageChip
import com.hostelgains.ui.theme.StageTrack
import com.hostelgains.ui.theme.Surface2
import com.hostelgains.ui.theme.TextHigh
import com.hostelgains.ui.theme.TextLow
import com.hostelgains.ui.theme.TextMid
import com.hostelgains.ui.three.Exercise3DState
import com.hostelgains.ui.three.Exercise3DView
import com.hostelgains.ui.three.Tempo
import com.hostelgains.ui.three.motionFor
import com.hostelgains.ui.three.rememberExercise3DState
import com.hostelgains.ui.three.repPhase
import com.hostelgains.ui.viewmodel.CameraPreset
import com.hostelgains.ui.viewmodel.WorkoutViewerViewModel
import kotlin.math.abs

private val SPEEDS = listOf(0.5f, 1f, 1.5f)

@Composable
fun WorkoutViewerScreen(
    viewModel: WorkoutViewerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val exercise = state.selectedExercise
    val list = state.availableExercises
    val selectedIndex = list.indexOfFirst { it.id == exercise.id }.coerceAtLeast(0)
    val viewState = rememberExercise3DState()
    val haptics = LocalHapticFeedback.current
    val chipsState = rememberLazyListState()
    var notesExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedIndex) {
        chipsState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0))
    }

    fun step(delta: Int) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val next = (selectedIndex + delta + list.size) % list.size
        viewModel.selectExercise(list[next].id)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas0)
    ) {
        val compact = maxHeight < 640.dp
        val notesMaxHeight = maxHeight * 0.3f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
                Text("FORM LAB · 3D", style = MaterialTheme.typography.labelSmall, color = TextLow)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = exercise.name,
                    style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                    color = TextHigh,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Exercise picker ───────────────────────────────────────────────
            LazyRow(
                state = chipsState,
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(list, key = { _, ex -> ex.id }) { _, ex ->
                    ChoiceChip(
                        text = ex.name,
                        selected = ex.id == exercise.id,
                        onClick = { viewModel.selectExercise(ex.id) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── 3D stage (takes all remaining height) ─────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 220.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, Outline, RoundedCornerShape(28.dp))
            ) {
                Exercise3DView(
                    exerciseKey = exercise.id,
                    targetMuscles = exercise.targetMuscles,
                    modifier = Modifier.fillMaxSize(),
                    state = viewState,
                    isPlaying = state.isPlaying,
                    speed = state.playbackSpeed,
                    yawDeg = state.yawDeg,
                    pitchDeg = state.pitchDeg,
                    autoOrbit = state.isAutoOrbit,
                    hudInset = 0.08f,
                    onCameraChange = { yaw, pitch ->
                        viewModel.onYawPitchChanged(((yaw % 360f) + 360f) % 360f, pitch)
                    }
                )

                RepHud(
                    state = viewState,
                    exerciseKey = exercise.id,
                    sets = exercise.recommendedSets,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(14.dp)
                )

                StageArrow(
                    left = true,
                    onClick = { step(-1) },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                )
                StageArrow(
                    left = false,
                    onClick = { step(1) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TempoBar(viewState, exercise.id)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Drag to rotate · Pinch to zoom · Double-tap to reset",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Playback & camera controls ────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Ignite,
                            contentColor = OnIgnite
                        )
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    SegmentedControl(
                        options = SPEEDS.map { if (it == 1f) "1×" else "$it×" },
                        selectedIndex = SPEEDS.indexOf(state.playbackSpeed),
                        onSelect = { viewModel.setSpeed(SPEEDS[it]) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    val orbitOn = state.isAutoOrbit
                    FilledIconButton(
                        onClick = {
                            if (orbitOn) {
                                // Freeze the camera where the orbit left it.
                                viewModel.onYawPitchChanged((state.yawDeg + viewState.orbit) % 360f, state.pitchDeg)
                                viewState.orbit = 0f
                            } else {
                                viewModel.toggleAutoOrbit()
                            }
                        },
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (orbitOn) Ignite.copy(alpha = 0.18f) else Surface2,
                            contentColor = if (orbitOn) Ignite else TextHigh
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled._360,
                            contentDescription = if (orbitOn) "Stop auto-rotate" else "Auto-rotate"
                        )
                    }
                }

                val presets = CameraPreset.entries
                val presetIndex = if (state.isAutoOrbit) -1 else presets.indexOfFirst {
                    abs(it.yawDeg - state.yawDeg) < 0.5f && abs(it.pitchDeg - state.pitchDeg) < 0.5f
                }
                SegmentedControl(
                    options = presets.map { it.label },
                    selectedIndex = presetIndex,
                    onSelect = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewState.orbit = 0f
                        viewState.zoom = 1f
                        viewModel.setCameraPreset(presets[it])
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Form notes (collapsible so the stage keeps priority on small phones) ──
            FormNotesCard(
                cue = exercise.formCue,
                muscles = exercise.targetMuscles,
                apparatus = exercise.apparatus,
                expanded = notesExpanded,
                onToggle = { notesExpanded = !notesExpanded },
                maxExpandedHeight = notesMaxHeight,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun RepHud(
    state: Exercise3DState,
    exerciseKey: String,
    sets: String,
    modifier: Modifier = Modifier
) {
    val kind = remember(exerciseKey) { motionFor(exerciseKey) }
    // derivedStateOf keeps recomposition to label/count changes rather than every frame.
    val label by remember(kind) { derivedStateOf { repPhase(kind, state.time).label } }
    val counter by remember(kind) {
        derivedStateOf {
            val reps = (state.time / kind.cycleSeconds).toInt()
            when (kind.tempo) {
                Tempo.ISOMETRIC -> "${state.time.toInt()} s"
                Tempo.CYCLIC -> "${reps * 2} steps"
                else -> "Rep ${reps + 1}"
            }
        }
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        StatusPill(label, Ignite)
        Spacer(Modifier.width(6.dp))
        StatusPill(counter, TextHigh)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .clip(CircleShape)
                .background(StageChip)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(sets, style = MaterialTheme.typography.labelMedium, color = TextHigh, maxLines = 1)
        }
    }
}

/** Thin bar tracking progress through the current rep; redraws without recomposing. */
@Composable
private fun TempoBar(state: Exercise3DState, exerciseKey: String) {
    val kind = remember(exerciseKey) { motionFor(exerciseKey) }
    val track = StageTrack
    val fill = Ignite
    Canvas(
        Modifier
            .fillMaxWidth(0.6f)
            .height(4.dp)
    ) {
        val p = repPhase(kind, state.time)
        val r = CornerRadius(size.height / 2f)
        drawRoundRect(track, cornerRadius = r)
        drawRoundRect(fill, size = Size(size.width * p.cycle, size.height), cornerRadius = r)
    }
}

@Composable
private fun StageArrow(left: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(StageChip)
    ) {
        Icon(
            if (left) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (left) "Previous exercise" else "Next exercise",
            tint = TextHigh
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormNotesCard(
    cue: String,
    muscles: List<String>,
    apparatus: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    maxExpandedHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    HgCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = if (expanded) 4.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Coach cue", style = MaterialTheme.typography.labelMedium, color = Ignite)
                Spacer(Modifier.height(2.dp))
                Text(
                    cue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextHigh,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse notes" else "Expand notes",
                tint = TextMid,
                modifier = Modifier.padding(8.dp)
            )
        }
        if (expanded) {
            Column(
                Modifier
                    .heightIn(max = maxExpandedHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp)
            ) {
                Text("Target muscles", style = MaterialTheme.typography.labelMedium, color = TextMid)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    muscles.forEach { StatusPill(it, Ignite) }
                }
                Spacer(Modifier.height(14.dp))
                Text("Equipment", style = MaterialTheme.typography.labelMedium, color = TextMid)
                Spacer(Modifier.height(4.dp))
                Text(apparatus, style = MaterialTheme.typography.bodyMedium, color = TextHigh)
            }
        }
    }
}

