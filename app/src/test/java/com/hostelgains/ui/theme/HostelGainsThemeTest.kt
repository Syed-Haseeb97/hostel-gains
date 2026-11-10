package com.hostelgains.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class HostelGainsThemeTest {

    @Test
    fun testColorPalette_matchesDesignTokens() {
        assertEquals(Color(0xFFC6FF3D), Volt)
        assertEquals(Color(0xFFFF5A36), Ember)
        assertEquals(Color(0xFF0B0B0D), Ink)
        assertEquals(Color(0xFF151519), Panel)
        assertEquals(Color(0xFF1E1E24), Surface)
        assertEquals(Color(0xFF8E8E93), Muted)
    }

    @Test
    fun testTypographyTokens_configuredWithFitnessHierarchy() {
        assertEquals(44f, Typography.displayLarge.fontSize.value)
        assertEquals(32f, Typography.displayMedium.fontSize.value)
        assertEquals(24f, Typography.headlineLarge.fontSize.value)
    }
}
