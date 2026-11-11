package com.hostelgains.ui.components

import androidx.compose.ui.graphics.Color
import com.hostelgains.ui.theme.Ember
import com.hostelgains.ui.theme.Volt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HostelGainsLogoTest {

    @Test
    fun testLogoColorAccents_alignWithDesignSystem() {
        assertEquals(Color(0xFFC6FF3D), Volt)
        assertEquals(Color(0xFFFF5A36), Ember)
    }

    @Test
    fun testLauncherIconDrawables_existAcrossAllMipmaps() {
        val resDir = File("src/main/res")
        val densities = listOf("mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi")

        for (density in densities) {
            val dir = File(resDir, density)
            assertTrue("Expected $density dir to exist", dir.exists())
            assertTrue("Expected ic_launcher.png in $density", File(dir, "ic_launcher.png").exists())
            assertTrue("Expected ic_launcher_round.png in $density", File(dir, "ic_launcher_round.png").exists())
        }

        val adaptiveDir = File(resDir, "mipmap-anydpi-v26")
        assertTrue("Expected adaptive icon dir to exist", adaptiveDir.exists())
        assertTrue("Expected ic_launcher.xml in anydpi-v26", File(adaptiveDir, "ic_launcher.xml").exists())
        assertTrue("Expected ic_launcher_round.xml in anydpi-v26", File(adaptiveDir, "ic_launcher_round.xml").exists())
    }

    @Test
    fun testVectorDrawables_exist() {
        val resDir = File("src/main/res/drawable")
        assertTrue("Expected ic_launcher_background.xml", File(resDir, "ic_launcher_background.xml").exists())
        assertTrue("Expected ic_launcher_foreground.xml", File(resDir, "ic_launcher_foreground.xml").exists())
        assertTrue("Expected ic_hostel_gains_logo.xml", File(resDir, "ic_hostel_gains_logo.xml").exists())
    }
}
