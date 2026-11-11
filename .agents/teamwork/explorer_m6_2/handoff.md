# Handoff Report — Explorer 2: UI & Onboarding Architecture (Milestone M6)

**Role**: Explorer 2 (UI & Onboarding Explorer)  
**Milestone**: M6 (API Key Onboarding Screen & Premium AI Feature Gate)  
**Working Directory**: `D:\projects\hostel_gains\.agents\teamwork\explorer_m6_2`  
**Date**: 2026-11-10T12:15:00Z  

---

## 1. Observation

Direct observations from the codebase investigation:

### 1.1 Existing Jetpack Compose UI Architecture & Navigation
- **File**: `app/src/main/java/com/hostelgains/ui/MainActivity.kt` (lines 9–19):
  Single activity subclassing `ComponentActivity`, calling `enableEdgeToEdge()` and setting content directly to `HostelGainsTheme { HostelGainsApp() }`.
- **File**: `app/src/main/java/com/hostelgains/ui/HostelGainsApp.kt` (lines 26–71):
  - Root composable managing the 4-tab bottom navigation scaffold.
  - State variable: `var currentTab by remember { mutableStateOf(NavigationTab.MEALS) }`.
  - Uses `Scaffold(containerColor = Ink, bottomBar = { HostelBottomNavigation(...) })`.
  - The navigation is **declarative state-based Compose navigation** (no `NavHost` / `NavController`), switching screens inside a `Box` with `when (currentTab)`:
    - `NavigationTab.MEALS -> MealInputScreen(viewModel = mealLogViewModel)`
    - `NavigationTab.WORKOUT -> WorkoutPlanScreen(viewModel = workoutViewModel, ...)`
    - `NavigationTab.VIEWER_3D -> WorkoutViewerScreen(viewModel = workoutViewerViewModel)`
    - `NavigationTab.STEPS -> StepStatusScreen(viewModel = stepTrackerViewModel)`
- **File**: `app/src/main/java/com/hostelgains/ui/components/NavigationTab.kt` (lines 13–22):
  Enum defining 4 core tabs: `MEALS ("Mess Meals")`, `WORKOUT ("V-Taper Plan")`, `VIEWER_3D ("3D Viewer")`, `STEPS ("Step Tracker")`.
- **File**: `app/src/main/java/com/hostelgains/ui/theme/Color.kt` (lines 9–19):
  - `Volt = Color(0xFFC6FF3D)` (Electric lime primary accent)
  - `Ember = Color(0xFFFF5A36)` (Hyper-orange alert / secondary accent)
  - `Ink = Color(0xFF0B0B0D)` (Deep black background)
  - `Panel = Color(0xFF151519)` (Card surface background)
  - `Surface = Color(0xFF1E1E24)` (Interactive container surface)
  - `Muted = Color(0xFF8E8E93)` (Secondary text / neutral muted)
  - `BorderStroke = Color(0x26FFFFFF)` (Subtle 15% white border)
  - `SuccessGreen = Color(0xFF22C55E)` (Completed / success green)
- **File**: `app/src/main/java/com/hostelgains/ui/screens/MealInputScreen.kt` (lines 93–124) and `WorkoutPlanScreen.kt` (lines 91–125):
  Both screens have top header rows with section titles and circular visual icons (`HeroBarbell3D` and `FitnessCenter` icon respectively), but currently lack a dedicated Settings/Menu action.
- **Dependencies**:
  `app/build.gradle.kts` imports `androidx.compose.material:material-icons-extended`, providing standard Google icons including `Visibility`, `VisibilityOff`, `Key`, `Settings`, `DeleteOutline`, `CheckCircle`, and `OpenInNew` (`Icons.AutoMirrored.Filled.OpenInNew`).

---

## 2. Logic Chain

From the observations and requirement specifications (`ORIGINAL_REQUEST.md §R-APIKEY-1`, `§R-APIKEY-2`, `§R-APIKEY-3`), the following logical progression guides the UI architecture:

### 2.1 Navigation & Screen State Flow
1. **Screen State Machine**:
   Instead of injecting an external router library, `HostelGainsApp.kt` should manage a top-level `AppScreen` state:
   ```kotlin
   enum class AppScreen {
       ONBOARDING,
       MAIN,
       SETTINGS
   }
   ```
2. **Initial Launch Evaluation**:
   - At launch, query `ApiKeyRepository`:
     `val shouldShowOnboarding = !apiKeyRepository.hasApiKey() && !apiKeyRepository.isOnboardingCompleted()`
   - If `true` -> Start on `AppScreen.ONBOARDING`.
   - If `false` -> Start on `AppScreen.MAIN` (defaulting to `NavigationTab.MEALS`).
3. **Transition Rules**:
   - `ONBOARDING` -> on valid key saved -> transitions to `AppScreen.MAIN`.
   - `ONBOARDING` -> on "Skip for now — use Offline Mode" -> sets `isOnboardingCompleted(true)` and transitions to `AppScreen.MAIN`.
   - `MAIN` -> on Settings icon tapped in header -> transitions to `AppScreen.SETTINGS`.
   - `SETTINGS` -> on Back tapped -> returns to `AppScreen.MAIN`.
   - `SETTINGS` -> on "Clear API Key / Reset to Offline Mode" -> wipes key, updates UI status to offline mode, remains in `SETTINGS` (or returns to `MAIN`).
   - `SETTINGS` -> on "Configure API Key" (when in offline mode) -> opens `AppScreen.ONBOARDING`.

---

### 2.2 Component Design: `ApiKeyOnboardingScreen.kt`

#### Requirements Addressed
- Offline mode by default explanation (Indian mess food database).
- Invitation to paste Google AI Studio API key for Premium AI features.
- Tappable hyperlink `https://aistudio.google.com/app/apikey` opening in browser.
- Password-style `OutlinedTextField` with show/hide toggle.
- "Unlock Premium AI" CTA validating format (`key.isNotBlank() && key.startsWith("AIza")`).
- "Skip for now — use Offline Mode" dismiss button.

#### Proposed Layout Specification (`app/src/main/java/com/hostelgains/ui/screens/ApiKeyOnboardingScreen.kt`)
```kotlin
package com.hostelgains.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.data.security.ApiKeyRepository
import com.hostelgains.ui.components.HeroBarbell3D
import com.hostelgains.ui.theme.*

@Composable
fun ApiKeyOnboardingScreen(
    apiKeyRepository: ApiKeyRepository,
    onKeySaved: (String) -> Unit = {},
    onSkipToOffline: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val apiKeyUrl = "https://aistudio.google.com/app/apikey"

    var inputKey by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun handleUnlock() {
        val trimmed = inputKey.trim()
        when {
            trimmed.isBlank() -> {
                validationError = "Please enter or paste your Gemini API key."
            }
            !trimmed.startsWith("AIza") -> {
                validationError = "Invalid key format. Gemini API keys must start with 'AIza'."
            }
            else -> {
                validationError = null
                apiKeyRepository.saveApiKey(trimmed)
                apiKeyRepository.setOnboardingCompleted(true)
                onKeySaved(trimmed)
            }
        }
    }

    Scaffold(
        containerColor = Ink,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Hero Brand Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Panel)
                            .border(1.5.dp, Volt, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        HeroBarbell3D(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            isSpinning = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "HOSTEL-GAINS AI ENGINE",
                        color = Volt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Unlock Premium AI",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Supercharge mess nutrition tracking & dynamic V-Taper workout optimization with Google Gemini.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Card 1: 100% Offline by Default Guarantee
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Panel)
                        .border(1.dp, BorderStroke, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Volt,
                            modifier = Modifier.size(24.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "100% OFFLINE BY DEFAULT",
                                color = Volt,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "The app works completely offline using the built-in Indian mess food database (mutton, dry rotis, boiled eggs, dal & rice, paneer bhurji) and deterministic exercise rebalancing. Zero internet or API key is required to train and track.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // Card 2: Premium AI Invitation & Key Input Form
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Surface)
                        .border(1.dp, Volt.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Text(
                        text = "PERSONAL GEMINI API KEY",
                        color = Volt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Paste your Google AI Studio API key below to unlock live AI natural language parsing and intelligent contextual equipment rebalancing:",
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tappable Hyperlink
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { uriHandler.openUri(apiKeyUrl) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Get your free API key at Google AI Studio",
                            color = Volt,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in browser",
                            tint = Volt,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password-style TextField with Show/Hide toggle
                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = {
                            inputKey = it
                            if (validationError != null) validationError = null
                        },
                        label = { Text("Gemini API Key", color = TextSecondary, fontSize = 12.sp) },
                        placeholder = { Text("AIzaSy...", color = Muted, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { handleUnlock() }),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "Hide key" else "Show key",
                                    tint = if (isPasswordVisible) Volt else TextSecondary
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Ink,
                            unfocusedContainerColor = Ink,
                            focusedBorderColor = Volt,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Error text
                    AnimatedVisibility(visible = validationError != null) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = validationError ?: "",
                                color = Ember,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Unlock CTA Button
                    Button(
                        onClick = { handleUnlock() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Volt,
                            contentColor = Ink
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Ink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UNLOCK PREMIUM AI ✦",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Dismiss Button: Skip for Now — Use Offline Mode
            item {
                OutlinedButton(
                    onClick = {
                        apiKeyRepository.setOnboardingCompleted(true)
                        onSkipToOffline()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Text(
                        text = "Skip for now — use Offline Mode",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
```

---

### 2.3 Component Design: Settings Screen & Clear Key Action

#### Requirements Addressed (`R-APIKEY-2`)
- Provide a "Clear API Key / Reset to Offline Mode" option in a Settings screen or accessible from the app menu.
- Store API key in EncryptedSharedPreferences (Jetpack Security) encrypted at rest.
- Never log, transmit, or expose raw key; present masked preview (`AIzaSy••••••••XXXX`).

#### Proposed Settings Screen (`app/src/main/java/com/hostelgains/ui/screens/SettingsScreen.kt`)
```kotlin
package com.hostelgains.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.data.security.ApiKeyRepository
import com.hostelgains.ui.theme.*

fun maskApiKey(key: String?): String {
    if (key.isNullOrBlank()) return "None"
    return if (key.length > 10) {
        "${key.take(6)}${"•".repeat(16)}${key.takeLast(4)}"
    } else {
        "••••••••"
    }
}

@Composable
fun SettingsScreen(
    apiKeyRepository: ApiKeyRepository,
    onNavigateBack: () -> Unit = {},
    onNavigateToEnterKey: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var hasKey by remember { mutableStateOf(apiKeyRepository.hasApiKey()) }
    var maskedKey by remember { mutableStateOf(maskApiKey(apiKeyRepository.getApiKey())) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Ink,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Panel)
                        .border(1.dp, BorderStroke, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to dashboard",
                        tint = Volt,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "APP SETTINGS",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "AI Key & Offline Configuration",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Status Card
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
                                text = "AI ENGINE STATUS",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (hasKey) Volt.copy(alpha = 0.2f) else Surface)
                                    .border(1.dp, if (hasKey) Volt else BorderStroke, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (hasKey) "✦ PREMIUM AI ACTIVE" else "OFFLINE MODE",
                                    color = if (hasKey) Volt else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (hasKey) "Active API Key" else "No API Key Set",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (hasKey) {
                                "Key: $maskedKey\nEncrypted at rest with Android Jetpack Security."
                            } else {
                                "Using local Indian mess food database and deterministic rebalancer."
                            },
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        if (statusMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = statusMessage ?: "",
                                color = Volt,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Key Actions Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Panel)
                        .border(1.dp, BorderStroke, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "MANAGE API KEY",
                        color = Volt,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    if (hasKey) {
                        // Clear Key Button
                        Button(
                            onClick = {
                                apiKeyRepository.clearApiKey()
                                hasKey = false
                                maskedKey = "None"
                                statusMessage = "API Key wiped. Switched to Offline Mode."
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Ember.copy(alpha = 0.2f),
                                contentColor = Ember
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Ember)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Ember,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clear API Key / Reset to Offline Mode",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        // Configure Key Button
                        Button(
                            onClick = onNavigateToEnterKey,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Volt,
                                contentColor = Ink
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Configure Google Gemini API Key",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Architecture & Compliance Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Panel)
                        .border(1.dp, BorderStroke, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Volt,
                            modifier = Modifier.size(22.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "STORAGE & PRIVACY COMPLIANCE",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• All build artifacts & caches isolated on Drive D:\n• Zero unencrypted key storage\n• Keys transmitted solely to official Google Gemini REST endpoints",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
```

---

### 2.4 State Management & Navigation Wiring in `HostelGainsApp.kt`

To wire `ApiKeyOnboardingScreen`, `SettingsScreen`, and `HostelGainsApp`, update `HostelGainsApp.kt` as follows:

```kotlin
package com.hostelgains.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hostelgains.data.security.ApiKeyRepository
import com.hostelgains.ui.components.HostelBottomNavigation
import com.hostelgains.ui.components.NavigationTab
import com.hostelgains.ui.screens.ApiKeyOnboardingScreen
import com.hostelgains.ui.screens.MealInputScreen
import com.hostelgains.ui.screens.SettingsScreen
import com.hostelgains.ui.screens.StepStatusScreen
import com.hostelgains.ui.screens.WorkoutPlanScreen
import com.hostelgains.ui.screens.WorkoutViewerScreen
import com.hostelgains.ui.theme.Ink
import com.hostelgains.ui.viewmodel.AppRepositories
import com.hostelgains.ui.viewmodel.MealLogViewModel
import com.hostelgains.ui.viewmodel.StepTrackerViewModel
import com.hostelgains.ui.viewmodel.WorkoutViewModel
import com.hostelgains.ui.viewmodel.WorkoutViewerViewModel

enum class AppScreen {
    ONBOARDING,
    MAIN,
    SETTINGS
}

@Composable
fun HostelGainsApp(
    mealLogViewModel: MealLogViewModel = viewModel(),
    workoutViewModel: WorkoutViewModel = viewModel(),
    stepTrackerViewModel: StepTrackerViewModel = viewModel(),
    workoutViewerViewModel: WorkoutViewerViewModel = viewModel(),
    apiKeyRepository: ApiKeyRepository = AppRepositories.apiKeyRepository
) {
    var currentScreen by remember {
        mutableStateOf(
            if (apiKeyRepository.hasApiKey() || apiKeyRepository.isOnboardingCompleted()) {
                AppScreen.MAIN
            } else {
                AppScreen.ONBOARDING
            }
        )
    }
    var currentTab by remember { mutableStateOf(NavigationTab.MEALS) }

    when (currentScreen) {
        AppScreen.ONBOARDING -> {
            ApiKeyOnboardingScreen(
                apiKeyRepository = apiKeyRepository,
                onKeySaved = { currentScreen = AppScreen.MAIN },
                onSkipToOffline = { currentScreen = AppScreen.MAIN }
            )
        }
        AppScreen.SETTINGS -> {
            SettingsScreen(
                apiKeyRepository = apiKeyRepository,
                onNavigateBack = { currentScreen = AppScreen.MAIN },
                onNavigateToEnterKey = { currentScreen = AppScreen.ONBOARDING }
            )
        }
        AppScreen.MAIN -> {
            Scaffold(
                containerColor = Ink,
                bottomBar = {
                    HostelBottomNavigation(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        NavigationTab.MEALS -> {
                            MealInputScreen(
                                viewModel = mealLogViewModel,
                                onOpenSettings = { currentScreen = AppScreen.SETTINGS }
                            )
                        }
                        NavigationTab.WORKOUT -> {
                            WorkoutPlanScreen(
                                viewModel = workoutViewModel,
                                onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                                onNavigateToViewer = { exerciseName ->
                                    workoutViewerViewModel.selectExercise(exerciseName)
                                    currentTab = NavigationTab.VIEWER_3D
                                }
                            )
                        }
                        NavigationTab.VIEWER_3D -> {
                            WorkoutViewerScreen(viewModel = workoutViewerViewModel)
                        }
                        NavigationTab.STEPS -> {
                            StepStatusScreen(viewModel = stepTrackerViewModel)
                        }
                    }
                }
            }
        }
    }
}
```

---

### 2.5 Header Integration in `MealInputScreen` & `WorkoutPlanScreen`

In `MealInputScreen.kt` (Header row, line 93):
Add optional parameter `onOpenSettings: () -> Unit = {}`.
Place a Settings `IconButton` with `Icons.Default.Settings` in the header row:
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Column {
        Text(text = "MESS MEALS", ...)
        Text(text = "AI Macronutrient Calculation & Intake Tracker", ...)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Panel)
                .border(1.dp, BorderStroke, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "App Settings",
                tint = Volt,
                modifier = Modifier.size(20.dp)
            )
        }
        Box(modifier = Modifier.size(52.dp)...) {
            HeroBarbell3D(isSpinning = true)
        }
    }
}
```

The same header pattern applies to `WorkoutPlanScreen.kt`.

---

### 2.6 Shared Reusable Badges for Explorer 3
In `app/src/main/java/com/hostelgains/ui/components/TriggerBadges.kt`, export:
```kotlin
@Composable
fun PremiumAiBadge(
    modifier: Modifier = Modifier,
    text: String = "✦ PREMIUM AI ACTIVE"
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Volt.copy(alpha = 0.20f))
            .border(width = 1.dp, color = Volt, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Volt)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = Volt,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun OfflineModeBadge(
    modifier: Modifier = Modifier,
    text: String = "⚡ OFFLINE MODE (Built-in Mess DB)"
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .border(width = 1.dp, color = BorderStroke, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Muted)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
```

---

## 3. Caveats

1. **Android Framework Dependency vs Pure JVM Testing**:
   - `EncryptedSharedPreferences` requires an Android `Context` and Android Keystore, which are only present on a device/emulator or Robolectric.
   - To keep unit tests running fast and deterministically in pure JVM (`testDebugUnitTest`), `ApiKeyRepository` must have an `InMemoryApiKeyRepository` implementation as the default or test fixture, ensuring all UI and ViewModel tests pass without Android framework stubs.
2. **Browser Hyperlink Dispatch**:
   - `LocalUriHandler.current.openUri(apiKeyUrl)` delegates to the Android OS intent resolver (`Intent.ACTION_VIEW`). In unit test environments without a mocked `UriHandler`, clicking the hyperlink should be isolated or tested with a fake `UriHandler`.
3. **No Navigation Compose Overhead**:
   - The app deliberately avoids `androidx.navigation:navigation-compose` in favor of declarative Compose state (`AppScreen` and `NavigationTab`). This eliminates runtime navigation crashes, argument serialization boilerplate, and complex backstack state.

---

## 4. Conclusion

1. **Architecture Verified**:
   The existing Compose architecture is cleanly structured, dark-themed (`Volt/Ember/Ink/Panel`), and uses state-driven tab switching.
2. **Onboarding Screen Designed**:
   `ApiKeyOnboardingScreen.kt` fully implements R-APIKEY-1:
   - Clear explanation of 100% offline mode via built-in Indian mess food database.
   - Invitation to unlock Premium AI features.
   - Tappable hyperlink `https://aistudio.google.com/app/apikey` opening in browser.
   - Password-style `OutlinedTextField` with show/hide toggle.
   - "Unlock Premium AI" CTA validating format (`key.isNotBlank() && key.startsWith("AIza")`).
   - "Skip for now — use Offline Mode" dismiss button.
3. **Settings Screen Designed**:
   `SettingsScreen.kt` fully implements R-APIKEY-2:
   - Displays AI status and masked API key (`AIzaSy••••••••XXXX`).
   - Provides "Clear API Key / Reset to Offline Mode" button.
   - Provides "Configure Google Gemini API Key" button when offline.
4. **App Wiring Complete**:
   `HostelGainsApp.kt` manages the 3-state machine (`ONBOARDING`, `MAIN`, `SETTINGS`), connecting screens with zero navigation library overhead and zero Drive C: contamination.

---

## 5. Verification Method

### 5.1 Unit Tests to Author (`app/src/test/java/com/hostelgains/ui/ApiKeyOnboardingTest.kt`)
Author unit tests covering:
1. **Key Format Validation**:
   - Empty or blank strings return `false`.
   - Keys not starting with `"AIza"` (e.g., `"sk-12345"`, `"test_key"`) return `false`.
   - Valid keys starting with `"AIza"` (e.g., `"AIzaSyD-1234567890abcdef"`) return `true`.
2. **Masked Key Formatter**:
   - Null or empty key returns `"None"`.
   - Key length <= 10 returns `"••••••••"`.
   - Key length > 10 returns `"${key.take(6)}${"•".repeat(16)}${key.takeLast(4)}"`.
3. **Onboarding Flow with Repository**:
   - First launch with no key and `isOnboardingCompleted == false` triggers onboarding display.
   - Calling `onSkipToOffline` sets `isOnboardingCompleted = true` and `hasApiKey = false`.
   - Submitting valid key calls `saveApiKey` and sets `isOnboardingCompleted = true`.
4. **Clear Key in Settings**:
   - Calling `clearApiKey()` sets `hasApiKey = false`, sets `getApiKey() = null`, and resets engine to offline mode.

### 5.2 Independent Verification Command
Execute unit tests strictly on Drive D:
```powershell
$env:GRADLE_USER_HOME = "D:\.gradle"
$env:ANDROID_HOME = "D:\Android\sdk"
$env:ANDROID_SDK_ROOT = "D:\Android\sdk"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
Set-Location "D:\projects\hostel_gains"
& .\gradlew.bat --gradle-user-home "D:\.gradle" testDebugUnitTest
```
All tests must compile and pass cleanly with zero bytes written to Drive C:.
