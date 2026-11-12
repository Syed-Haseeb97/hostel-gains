package com.hostelgains.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.hostelgains.ui.components.HgCard
import com.hostelgains.ui.components.HgPrimaryButton
import com.hostelgains.ui.components.MetricTile
import com.hostelgains.ui.components.ProgressRing
import com.hostelgains.ui.components.ScreenHeader
import com.hostelgains.ui.components.SectionLabel
import com.hostelgains.ui.components.SegmentedControl
import com.hostelgains.ui.components.StatusPill
import com.hostelgains.ui.theme.Canvas0
import com.hostelgains.ui.theme.Danger
import com.hostelgains.ui.theme.FatColor
import com.hostelgains.ui.theme.Ignite
import com.hostelgains.ui.theme.Success
import com.hostelgains.ui.theme.Surface2
import com.hostelgains.ui.theme.TextHigh
import com.hostelgains.ui.theme.TextMid
import com.hostelgains.ui.theme.Warning
import com.hostelgains.ui.three.Exercise3DView
import com.hostelgains.ui.viewmodel.StepTrackerViewModel

private val STEP_INCREMENTS = listOf(500, 1000, 2500, 5000)

/** Preset check times for the 6 PM deficit alert, plus "Now". */
private val CHECK_TIMES = listOf(14 to 0, 17 to 59, 18 to 0)

@Composable
fun StepStatusScreen(
    viewModel: StepTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val progress = (state.currentSteps / state.targetSteps.toFloat()).coerceIn(0f, 1f)
    val goalHit = state.currentSteps >= state.targetSteps
    val ringColor = when {
        goalHit -> Success
        state.currentSteps >= 5000 -> Ignite
        else -> Warning
    }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas0),
        contentPadding = PaddingValues(top = top + 12.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(
                eyebrow = "Daily movement",
                title = "Move",
                trailing = {
                    if (state.isHardwareSensorActive) {
                        StatusPill(text = "LIVE SENSOR", color = Success)
                    } else {
                        StatusPill(text = "STANDBY", color = Ignite)
                    }
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // ── Progress hero ─────────────────────────────────────────────────────
        item {
            HgCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(
                        progress = progress,
                        color = ringColor,
                        strokeWidth = 12.dp,
                        modifier = Modifier
                            .weight(1.15f)
                            .aspectRatio(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("%,d".format(state.currentSteps), style = MaterialTheme.typography.headlineLarge, color = TextHigh)
                            Text("of %,d steps".format(state.targetSteps), style = MaterialTheme.typography.bodySmall, color = TextMid)
                            Spacer(Modifier.height(6.dp))
                            StatusPill("${(progress * 100).toInt()}%", ringColor)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    // Live walking mannequin; pauses at zero steps.
                    Box(
                        Modifier
                            .weight(0.85f)
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(18.dp))
                    ) {
                        Exercise3DView(
                            exerciseKey = "d7_active_recovery_walk",
                            targetMuscles = listOf("Calves", "Glutes"),
                            modifier = Modifier.fillMaxSize(),
                            interactive = false,
                            isPlaying = state.currentSteps > 0,
                            yawDeg = 60f,
                            pitchDeg = 10f
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile(Icons.Filled.Place, "${state.distanceKm}", "Distance", FatColor, Modifier.weight(1f), unit = "km")
                    MetricTile(Icons.Filled.LocalFireDepartment, "${state.caloriesBurned}", "Burned", Ignite, Modifier.weight(1f), unit = "kcal")
                    MetricTile(Icons.Filled.Timer, "${state.activeMinutes}", "Active", Success, Modifier.weight(1f), unit = "min")
                }
            }
        }

        // ── Alert / celebration ───────────────────────────────────────────────
        if (state.alertResult.isAlertTriggered) {
            item {
                HgCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Danger.copy(alpha = 0.08f),
                    borderColor = Danger.copy(alpha = 0.35f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Danger, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            state.alertResult.alertTitle ?: "Step deficit",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextHigh
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(state.alertResult.alertMessage ?: "", style = MaterialTheme.typography.bodyMedium, color = TextMid)
                    Spacer(Modifier.height(14.dp))
                    HgPrimaryButton(
                        text = "Log a 3,000-step walk",
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        onClick = { viewModel.addSteps(3000) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else if (goalHit) {
            item {
                HgCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Success.copy(alpha = 0.08f),
                    borderColor = Success.copy(alpha = 0.35f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Success)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Goal reached", style = MaterialTheme.typography.titleSmall, color = TextHigh)
                            Text("Great work for insulin sensitivity and recovery.", style = MaterialTheme.typography.bodySmall, color = TextMid)
                        }
                    }
                }
            }
        }

        // ── Manual logging ────────────────────────────────────────────────────
        item {
            HgCard(Modifier.fillMaxWidth()) {
                SectionLabel("Add steps") {
                    TextButton(onClick = { viewModel.resetSteps() }) { Text("Reset", color = TextMid) }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    STEP_INCREMENTS.forEach { delta ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Surface2)
                                .clickable(role = Role.Button) { viewModel.addSteps(delta) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (delta >= 1000) "+${delta / 1000f}k".replace(".0k", "k") else "+$delta",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextHigh
                            )
                        }
                    }
                }
            }
        }

        // ── Alert check time ──────────────────────────────────────────────────
        item {
            HgCard(Modifier.fillMaxWidth()) {
                Text("Deficit check", style = MaterialTheme.typography.titleMedium, color = TextHigh)
                Spacer(Modifier.height(2.dp))
                Text(
                    "An alert fires when you're under 5,000 steps at 6:00 PM. Evaluating at %02d:%02d."
                        .format(state.simulatedHour, state.simulatedMinute),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMid
                )
                Spacer(Modifier.height(12.dp))
                val selected = CHECK_TIMES.indexOfFirst { it.first == state.simulatedHour && it.second == state.simulatedMinute }
                SegmentedControl(
                    options = listOf("2:00 PM", "5:59 PM", "6:00 PM", "Now"),
                    selectedIndex = if (selected >= 0) selected else 3,
                    onSelect = { i ->
                        if (i < CHECK_TIMES.size) viewModel.setTime(CHECK_TIMES[i].first, CHECK_TIMES[i].second)
                        else viewModel.syncSystemTime()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

