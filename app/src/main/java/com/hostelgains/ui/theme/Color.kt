package com.hostelgains.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Hostel-Gains palette. One set per system theme; screens read the active set through the
 * composable tokens below (e.g. [Canvas0], [Ignite]), so every page switches automatically.
 */
@Immutable
data class HgColors(
    val isDark: Boolean,
    val canvas: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val outline: Color,
    val outlineStrong: Color,
    val textHigh: Color,
    val textMid: Color,
    val textLow: Color,
    val ignite: Color,
    val igniteDeep: Color,
    val onIgnite: Color,
    val protein: Color,
    val carb: Color,
    val fat: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val stageTop: Color,
    val stageBottom: Color,
    val stageGrid: Color,
    val stageTrack: Color,
    val stageChip: Color,
    val stageCable: Color,
    val clay: Color
)

val DarkHgColors = HgColors(
    isDark = true,
    canvas = Color(0xFF0C0E1A),
    surface1 = Color(0xFF151A2E),
    surface2 = Color(0xFF1E2440),
    surface3 = Color(0xFF262D4A),
    outline = Color(0xFF262D4A),
    outlineStrong = Color(0xFF3B4468),
    textHigh = Color(0xFFFFFFFF),
    textMid = Color(0xFF8F9BB3),
    textLow = Color(0xFF64748B),
    ignite = Color(0xFFC8F542),
    igniteDeep = Color(0xFFAEE42A),
    onIgnite = Color(0xFF0C0E1A),
    protein = Color(0xFFC8F542),
    carb = Color(0xFF8B7BFF),
    fat = Color(0xFFFF7A59),
    success = Color(0xFFC8F542),
    warning = Color(0xFFFF7A59),
    danger = Color(0xFFFF4D4F),
    info = Color(0xFF8B7BFF),
    stageTop = Color(0xFF151A2E),
    stageBottom = Color(0xFF0C0E1A),
    stageGrid = Color.White.copy(alpha = 0.09f),
    stageTrack = Color.White.copy(alpha = 0.12f),
    stageChip = Color.Black.copy(alpha = 0.38f),
    stageCable = Color(0xFFCBD1DB),
    clay = Color(0xFFD5D9E1)
)

val LightHgColors = HgColors(
    isDark = false,
    canvas = Color(0xFFF4F5F8),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEFF1F5),
    surface3 = Color(0xFFE3E6EC),
    outline = Color(0xFFE3E6EC),
    outlineStrong = Color(0xFFC8CDD7),
    textHigh = Color(0xFF12141A),
    textMid = Color(0xFF535B6B),
    textLow = Color(0xFF878FA0),
    ignite = Color(0xFFE8562A),
    igniteDeep = Color(0xFFC9441C),
    onIgnite = Color(0xFFFFFFFF),
    protein = Color(0xFFE8603F),
    carb = Color(0xFFD99100),
    fat = Color(0xFF1C8FCC),
    success = Color(0xFF12935F),
    warning = Color(0xFFB57500),
    danger = Color(0xFFD5384C),
    info = Color(0xFF1A7DBD),
    stageTop = Color(0xFFFAFBFD),
    stageBottom = Color(0xFFE2E6ED),
    stageGrid = Color.Black.copy(alpha = 0.08f),
    stageTrack = Color.Black.copy(alpha = 0.10f),
    stageChip = Color.White.copy(alpha = 0.82f),
    stageCable = Color(0xFF5E6675),
    clay = Color(0xFFAEB6C4)
)

val LocalHgColors = staticCompositionLocalOf { DarkHgColors }

// ── Composable tokens (read the active palette) ─────────────────────────────

val Canvas0: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.canvas
val Surface1: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.surface1
val Surface2: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.surface2
val Surface3: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.surface3
val Outline: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.outline
val OutlineStrong: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.outlineStrong
val TextHigh: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.textHigh
val TextMid: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.textMid
val TextLow: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.textLow
val Ignite: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.ignite
val IgniteDeep: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.igniteDeep
val OnIgnite: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.onIgnite
val ProteinColor: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.protein
val CarbColor: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.carb
val FatColor: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.fat
val Success: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.success
val Warning: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.warning
val Danger: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.danger
val Info: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.info
val StageTop: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.stageTop
val StageBottom: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.stageBottom
val StageGrid: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.stageGrid
val StageTrack: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.stageTrack
val StageChip: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.stageChip
val StageCable: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.stageCable
val Clay: Color @Composable @ReadOnlyComposable get() = LocalHgColors.current.clay

// ── Equipment materials: identical in both themes (real-world gym colours) ───
val Steel = Color(0xFF9AA3B2)
val Rubber = Color(0xFF2F343E)
val RubberFace = Color(0xFF3C424E)
val FrameMetal = Color(0xFF454C5A)
val Upholstery = Color(0xFF2A2E37)

// ── Legacy Brand Aliases ──────────────────────────────────────────────────
val Volt = Color(0xFFC6FF3D)
val Ink = Color(0xFF0B0B0D)
val Ember = Color(0xFFFF5A36)
val Panel = Color(0xFF151519)
val Surface = Color(0xFF1E1E24)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val Muted = Color(0xFF8E8E93)
val CyanAccent = Color(0xFF38BDF8)
val BorderStroke = Color(0xFF334155)
