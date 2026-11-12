package com.hostelgains.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.domain.physique.PhysiqueBuild
import com.hostelgains.ui.components.HgCard
import com.hostelgains.ui.components.HgPrimaryButton
import com.hostelgains.ui.components.HgSecondaryButton
import com.hostelgains.ui.components.ProgressRing
import com.hostelgains.ui.components.ScreenHeader
import com.hostelgains.ui.components.SectionLabel
import com.hostelgains.ui.components.StatusPill
import com.hostelgains.ui.theme.Canvas0
import com.hostelgains.ui.theme.CarbColor
import com.hostelgains.ui.theme.Danger
import com.hostelgains.ui.theme.FatColor
import com.hostelgains.ui.theme.Ignite
import com.hostelgains.ui.theme.OnIgnite
import com.hostelgains.ui.theme.Outline
import com.hostelgains.ui.theme.OutlineStrong
import com.hostelgains.ui.theme.ProteinColor
import com.hostelgains.ui.theme.Success
import com.hostelgains.ui.theme.Surface1
import com.hostelgains.ui.theme.Surface2
import com.hostelgains.ui.theme.Surface3
import com.hostelgains.ui.theme.TextHigh
import com.hostelgains.ui.theme.TextLow
import com.hostelgains.ui.theme.TextMid
import com.hostelgains.ui.theme.Warning
import com.hostelgains.ui.viewmodel.PhysiqueProfileViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhysiqueSelectionScreen(
    viewModel: PhysiqueProfileViewModel,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    onConfirmed: (PhysiqueBuild) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val profile = state.currentProfile
    var showAllBuilds by remember { mutableStateOf(false) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Canvas0),
        contentPadding = PaddingValues(top = top + 16.dp, bottom = 36.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Screen Header ──────────────────────────────────────────────────────
        item {
            ScreenHeader(
                eyebrow = "Physique Architect · Tailored PPL",
                title = "Choose Your Build",
                trailing = {
                    if (onDismiss != null) {
                        Text(
                            "Done",
                            color = Ignite,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDismiss() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Select your target physique pathway. The dynamic engine will calibrate your 6-day PPL exercise payload, rep ranges, and volume to your height and weight.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMid
            )
        }

        // ── Physical Stats Card (Height, Weight, Age) ──────────────────────────
        item {
            SectionLabel("Your Physical Foundation")
            Spacer(Modifier.height(8.dp))
            HgCard {
                // Height Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Height, contentDescription = null, tint = Ignite, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Height", style = MaterialTheme.typography.titleMedium, color = TextHigh)
                    }
                    Text(
                        profile.heightFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ignite
                    )
                }
                Slider(
                    value = state.heightCm,
                    onValueChange = { viewModel.updateHeight(it) },
                    valueRange = 150f..205f,
                    steps = 54,
                    colors = SliderDefaults.colors(
                        thumbColor = Ignite,
                        activeTrackColor = Ignite,
                        inactiveTrackColor = Surface3
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Weight Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = FatColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Weight", style = MaterialTheme.typography.titleMedium, color = TextHigh)
                    }
                    Text(
                        profile.weightFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FatColor
                    )
                }
                Slider(
                    value = state.weightKg,
                    onValueChange = { viewModel.updateWeight(it) },
                    valueRange = 45f..130f,
                    steps = 84,
                    colors = SliderDefaults.colors(
                        thumbColor = FatColor,
                        activeTrackColor = FatColor,
                        inactiveTrackColor = Surface3
                    )
                )

                Spacer(Modifier.height(12.dp))

                // Age & BMI row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = ProteinColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Age: ${state.ageYears} yrs", style = MaterialTheme.typography.bodyMedium, color = TextHigh)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Surface3)
                                .clickable { if (state.ageYears > 16) viewModel.updateAge(state.ageYears - 1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", color = TextHigh, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Surface3)
                                .clickable { if (state.ageYears < 60) viewModel.updateAge(state.ageYears + 1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = TextHigh, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // BMI Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("BODY FRAME ASSESSMENT", style = MaterialTheme.typography.labelSmall, color = TextLow)
                        Text(profile.bmiCategory, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextHigh)
                    }
                    StatusPill("BMI ${profile.bmi}", color = if (profile.bmi in 18.5f..25f) Success else FatColor)
                }
            }
        }

        // ── Primary Build Selection ────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Target Physique Pathways")
                Text(
                    if (showAllBuilds) "Show Core (3)" else "Show All (${PhysiqueBuild.ALL_BUILDS.size})",
                    color = Ignite,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { showAllBuilds = !showAllBuilds }
                )
            }
        }

        val displayedBuilds = if (showAllBuilds) PhysiqueBuild.ALL_BUILDS else PhysiqueBuild.PRIMARY_BUILDS

        items(displayedBuilds, key = { it.id }) { build ->
            PhysiqueBuildCard(
                build = build,
                isSelected = build == state.selectedBuild,
                onClick = { viewModel.selectBuild(build) }
            )
        }

        // ── Gemini AI Anthropometric Calibration ────────────────────────────────
        item {
            SectionLabel("AI Anthropometric Calibration")
            Spacer(Modifier.height(8.dp))
            HgCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Ignite.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Ignite, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Google Gemini AI Calibration", style = MaterialTheme.typography.titleMedium, color = TextHigh)
                        Text(
                            "Calibrates leverage angles, volume caps, and hostel mess hacks specifically for your ${profile.heightImperialFormatted} @ ${profile.weightKg.toInt()} kg frame.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMid
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (state.isOptimizing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = Ignite, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Analyzing biomechanics & mess nutrition...", style = MaterialTheme.typography.bodyMedium, color = TextMid)
                    }
                } else if (state.optimizationResult != null) {
                    val res = state.optimizationResult!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface2)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TARGET CALORIES", style = MaterialTheme.typography.labelSmall, color = TextLow)
                            Text("${res.dailyCalories} kcal / day", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = CarbColor)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("PROTEIN TARGET", style = MaterialTheme.typography.labelSmall, color = TextLow)
                            Text("${res.proteinGrams}g / day", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ProteinColor)
                        }
                        Text(
                            "💡 Leverage Advice: ${res.leverageAdvice}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextHigh
                        )
                        Text(
                            "🍛 Hostel Mess Strategy: ${res.hostelMessStrategy}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMid
                        )
                    }
                } else {
                    HgSecondaryButton(
                        text = "Calibrate Frame With Gemini AI",
                        icon = Icons.Filled.AutoAwesome,
                        onClick = { viewModel.runGeminiOptimization() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ── Confirm Action Button ──────────────────────────────────────────────
        item {
            HgPrimaryButton(
                text = "Lock In ${state.selectedBuild.title} & Generate Routine",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = {
                    viewModel.saveProfile()
                    onConfirmed(state.selectedBuild)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )
        }
    }
}

/**
 * Distinct stylized card displaying a physique build archetype with real-life visual styling.
 */
@Composable
fun PhysiqueBuildCard(
    build: PhysiqueBuild,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(if (isSelected) Ignite else Outline, label = "buildBorder")
    val bgColor by animateColorAsState(if (isSelected) Surface1 else Surface2, label = "buildBg")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(14.dp)
    ) {
        // ── Real Visual Photo Banner (Faceless Build Reference) ───────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Surface3)
        ) {
            SubcomposeAsyncImage(
                model = build.realLifeVisualUrl,
                contentDescription = "${build.title} visual representation",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    PhysiqueSilhouetteBanner(build = build, isSelected = isSelected, modifier = Modifier.fillMaxSize())
                },
                error = {
                    PhysiqueSilhouetteBanner(build = build, isSelected = isSelected, modifier = Modifier.fillMaxSize())
                }
            )

            // Cinematic gradient overlay for dark-mode contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Canvas0.copy(alpha = 0.40f),
                                Canvas0.copy(alpha = 0.15f),
                                Canvas0.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            // Top overlay: Archetype pill & Selection radio badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Surface1.copy(alpha = 0.90f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(build.visualBadge, fontSize = 15.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        build.archetypeTag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Ignite else TextHigh
                    )
                }

                // Radio Indicator
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Ignite else Surface1.copy(alpha = 0.90f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = "Selected", tint = OnIgnite, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Bottom overlay: Title and aesthetic summary tag
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    build.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextHigh
                )
                Text(
                    build.targetAesthetic,
                    style = MaterialTheme.typography.labelSmall,
                    color = Ignite,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Short Description
        Text(
            build.shortDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = TextHigh
        )

        Spacer(Modifier.height(10.dp))

        // Target Aesthetic & Rep Range Tags
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusPill(build.repRangeSummary, color = if (isSelected) Ignite else TextLow)
            StatusPill(build.targetAesthetic, color = TextMid)
        }

        Spacer(Modifier.height(10.dp))

        // Primary Exercises Focus
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Surface3.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text("CORNERSTONE MOVEMENTS", style = MaterialTheme.typography.labelSmall, color = TextLow)
            Text(
                build.primaryExerciseFocus,
                style = MaterialTheme.typography.bodySmall,
                color = TextMid,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Procedural stylized visual silhouette depicting the physical emphasis of each build archetype.
 */
@Composable
fun PhysiqueSilhouetteBanner(
    build: PhysiqueBuild,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isSelected) Ignite else Color(0xFF6E7585)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Surface3,
                        if (isSelected) Ignite.copy(alpha = 0.12f) else Surface2,
                        Surface3
                    )
                )
            )
            .border(1.dp, if (isSelected) Ignite.copy(alpha = 0.3f) else Outline, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            when (build) {
                PhysiqueBuild.V_TAPER -> {
                    // Wide flaring shoulders & lats tapering into sharp narrow waist
                    val path = Path().apply {
                        moveTo(cx - 60f, cy - 24f) // Left shoulder
                        lineTo(cx + 60f, cy - 24f) // Right shoulder
                        lineTo(cx + 40f, cy - 6f)  // Flaring lat
                        lineTo(cx + 14f, cy + 24f) // Narrow waist
                        lineTo(cx - 14f, cy + 24f) // Narrow waist left
                        lineTo(cx - 40f, cy - 6f)  // Left lat
                        close()
                    }
                    drawPath(path, color = accentColor.copy(alpha = 0.85f))
                }
                PhysiqueBuild.TANK, PhysiqueBuild.YOKE -> {
                    // Heavy mountain traps & thick dense square torso
                    val path = Path().apply {
                        moveTo(cx - 20f, cy - 28f) // High neck/trap peak
                        lineTo(cx - 55f, cy - 18f) // Traps slope down to left shoulder
                        lineTo(cx - 50f, cy + 24f) // Thick left torso
                        lineTo(cx + 50f, cy + 24f) // Thick right torso
                        lineTo(cx + 55f, cy - 18f) // Right shoulder
                        lineTo(cx + 20f, cy - 28f) // Traps peak
                        close()
                    }
                    drawPath(path, color = accentColor.copy(alpha = 0.85f))
                }
                PhysiqueBuild.ATHLETE, PhysiqueBuild.SLEEPER -> {
                    // Balanced athletic sprinter proportion
                    val path = Path().apply {
                        moveTo(cx - 42f, cy - 22f)
                        lineTo(cx + 42f, cy - 22f)
                        lineTo(cx + 30f, cy)
                        lineTo(cx + 18f, cy + 24f)
                        lineTo(cx - 18f, cy + 24f)
                        lineTo(cx - 30f, cy)
                        close()
                    }
                    drawPath(path, color = accentColor.copy(alpha = 0.85f))
                }
                PhysiqueBuild.X_FRAME, PhysiqueBuild.GOLDEN_ERA -> {
                    // Wide shoulders + narrow waist + sweeping outer quad base
                    val path = Path().apply {
                        moveTo(cx - 54f, cy - 24f)
                        lineTo(cx + 54f, cy - 24f)
                        lineTo(cx + 16f, cy - 2f)  // Narrow waist
                        lineTo(cx + 48f, cy + 24f) // Sweeping outer quads
                        lineTo(cx - 48f, cy + 24f) // Sweeping outer quads left
                        lineTo(cx - 16f, cy - 2f)  // Narrow waist
                        close()
                    }
                    drawPath(path, color = accentColor.copy(alpha = 0.85f))
                }
            }
        }

        // Overlay text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ANATOMICAL SILHOUETTE",
                style = MaterialTheme.typography.labelSmall,
                color = TextLow,
                fontSize = 10.sp
            )
            Text(
                build.targetAesthetic,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
