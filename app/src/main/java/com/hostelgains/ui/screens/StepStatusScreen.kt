package com.hostelgains.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.ui.components.WalkingAvatar3D
import com.hostelgains.ui.theme.BorderStroke
import com.hostelgains.ui.theme.CyanAccent
import com.hostelgains.ui.theme.Ember
import com.hostelgains.ui.theme.Ink
import com.hostelgains.ui.theme.Muted
import com.hostelgains.ui.theme.Panel
import com.hostelgains.ui.theme.Surface
import com.hostelgains.ui.theme.TextPrimary
import com.hostelgains.ui.theme.TextSecondary
import com.hostelgains.ui.theme.Volt
import com.hostelgains.ui.viewmodel.StepTrackerViewModel

@Composable
fun StepStatusScreen(
    viewModel: StepTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val progressPct = ((state.currentSteps.toFloat() / state.targetSteps.toFloat()) * 100f).coerceIn(0f, 100f).toInt()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STEP TRACKER",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Daily NEAT & Evening Recomposition Sync",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Panel)
                        .border(1.dp, BorderStroke, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = Volt,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 6:00 PM Step Deficit Warning Card (Prominently displayed when triggered)
        item {
            if (state.alertResult.isAlertTriggered) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Ember.copy(alpha = 0.15f))
                        .border(1.5.dp, Ember, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alert",
                                tint = Ember,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.alertResult.alertTitle ?: "STEP DEFICIT ALERT",
                                color = Ember,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.alertResult.alertMessage ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.addSteps(3000) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Ember,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "LOG 3,000 POST-DINNER STEPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Circular Step Dial & Animated 3D Walking Avatar Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Panel)
                    .border(1.dp, BorderStroke, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DAILY ACTIVITY TARGET",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Target: 10,000 steps",
                            color = Volt,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Circular Progress Dial with embedded 3D Avatar
                    Box(
                        modifier = Modifier.size(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Compose Canvas Circular Progress Arc
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidthPx = 14.dp.toPx()
                            val radius = (size.minDimension - strokeWidthPx) / 2f
                            val center = size / 2f

                            // Track circle
                            drawCircle(
                                color = Surface,
                                radius = radius,
                                style = Stroke(width = strokeWidthPx)
                            )

                            // Animated Sweep Arc
                            val sweepAngle = (progressPct / 100f) * 360f
                            drawArc(
                                color = if (state.currentSteps >= 5000) Volt else Ember,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                            )
                        }

                        // Embedded Animated 3D Walking Avatar in the Center
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(modifier = Modifier.size(80.dp, 80.dp)) {
                                WalkingAvatar3D(
                                    modifier = Modifier.fillMaxSize(),
                                    isWalking = state.currentSteps > 0,
                                    cadenceSpeed = 1.2f
                                )
                            }
                            Text(
                                text = "${state.currentSteps}",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 34.sp
                            )
                            Text(
                                text = "STEPS ($progressPct%)",
                                color = if (state.currentSteps >= 5000) Volt else Ember,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3 Metric Counters: Distance, Calories, Active Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StepMetricItem(
                            label = "DISTANCE",
                            value = "${state.distanceKm} km",
                            color = Volt
                        )
                        StepMetricItem(
                            label = "ENERGY",
                            value = "${state.caloriesBurned} kcal",
                            color = Ember
                        )
                        StepMetricItem(
                            label = "ACTIVE TIME",
                            value = "${state.activeMinutes} min",
                            color = CyanAccent
                        )
                    }
                }
            }
        }

        // Time Simulation & Verification Controls
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Panel)
                    .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Volt,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TIME EVALUATION SWITCHER (6:00 PM RULE TESTER)",
                        color = Volt,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Current Evaluated Time: ${String.format("%02d:%02d", state.simulatedHour, state.simulatedMinute)}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.setTime(14, 0) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(34.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                    ) {
                        Text(text = "2:00 PM (Pre)", fontSize = 10.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { viewModel.setTime(17, 59) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(34.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                    ) {
                        Text(text = "5:59 PM (Safe)", fontSize = 10.sp, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { viewModel.setTime(18, 0) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(34.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (state.simulatedHour >= 18) Ember else BorderStroke
                        )
                    ) {
                        Text(text = "6:00 PM (Alert)", fontSize = 10.sp, color = Ember, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Step Logging Buttons
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Panel)
                    .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "LOG STEP ACTIVITY",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(500, 1000, 2500, 5000).forEach { delta ->
                        Button(
                            onClick = { viewModel.addSteps(delta) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Surface,
                                contentColor = Volt
                            )
                        ) {
                            Text(
                                text = "+$delta",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { viewModel.resetSteps() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Text(
                        text = "Reset Steps to 0",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepMetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black
        )
    }
}
