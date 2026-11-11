package com.hostelgains.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.ui.components.HostelGainsLogoBadge
import com.hostelgains.ui.theme.BorderStroke
import com.hostelgains.ui.theme.Ember
import com.hostelgains.ui.theme.Ink
import com.hostelgains.ui.theme.Muted
import com.hostelgains.ui.theme.Panel
import com.hostelgains.ui.theme.Surface
import com.hostelgains.ui.theme.TextPrimary
import com.hostelgains.ui.theme.TextSecondary
import com.hostelgains.ui.theme.Volt
import com.hostelgains.ui.viewmodel.ApiKeyViewModel

private const val AI_STUDIO_URL = "https://aistudio.google.com/app/apikey"

@Composable
fun ApiKeyOnboardingScreen(
    viewModel: ApiKeyViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Official App Logo Emblem ──
                HostelGainsLogoBadge(size = 80.dp)

                Spacer(modifier = Modifier.height(20.dp))

                // ── Heading ─────────────────────────────────────────────────
                Text(
                    text = "Unlock Premium AI",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Paste your Google AI Studio key to enable live Gemini-powered meal parsing and intelligent workout rebalancing.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Feature comparison card ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Panel)
                        .border(1.dp, BorderStroke, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FeatureRow(
                        emoji = "🥗",
                        label = "Smart Meal Parser",
                        offlineDesc = "Local Indian mess food database",
                        aiDesc = "Gemini AI — any food, any description"
                    )
                    FeatureRow(
                        emoji = "🔄",
                        label = "Workout Rebalancer",
                        offlineDesc = "Pre-mapped muscle synergy rules",
                        aiDesc = "Gemini AI — context-aware redistribution"
                    )
                    FeatureRow(
                        emoji = "⚡",
                        label = "V-Taper PPL Engine",
                        offlineDesc = "Full 6-day framework (always on)",
                        aiDesc = "Full 6-day framework (always on)"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── API key link ─────────────────────────────────────────────
                val linkText = buildAnnotatedString {
                    withStyle(SpanStyle(color = TextSecondary, fontSize = 13.sp)) {
                        append("Get your free key at ")
                    }
                    withStyle(
                        SpanStyle(
                            color = Volt,
                            fontSize = 13.sp,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("aistudio.google.com")
                    }
                }
                Text(
                    text = linkText,
                    modifier = Modifier.clickable { uriHandler.openUri(AI_STUDIO_URL) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Key input field ──────────────────────────────────────────
                OutlinedTextField(
                    value = uiState.apiKey,
                    onValueChange = viewModel::onKeyInputChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isValidating,
                    label = { Text("AI Studio API Key", color = Muted) },
                    placeholder = { Text("Paste your API key here", color = Muted.copy(alpha = 0.5f)) },
                    visualTransformation = if (uiState.isKeyVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(
                            onClick = viewModel::toggleKeyVisibility,
                            enabled = !uiState.isValidating
                        ) {
                            Icon(
                                imageVector = if (uiState.isKeyVisible)
                                    Icons.Default.VisibilityOff
                                else
                                    Icons.Default.Visibility,
                                contentDescription = if (uiState.isKeyVisible) "Hide key" else "Show key",
                                tint = Muted
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = uiState.errorMessage != null,
                    supportingText = {
                        if (uiState.errorMessage != null) {
                            Text(uiState.errorMessage!!, color = Ember, fontSize = 12.sp)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Volt,
                        unfocusedBorderColor = BorderStroke,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Volt,
                        errorBorderColor = Ember
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Primary CTA ──────────────────────────────────────────────
                Button(
                    onClick = { viewModel.validateAndSaveKey(onSuccess = onDismiss) },
                    enabled = !uiState.isValidating && uiState.apiKey.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Volt,
                        contentColor = Ink,
                        disabledContainerColor = Volt.copy(alpha = 0.3f),
                        disabledContentColor = Ink.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isValidating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Ink,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Verifying with Google AI...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        Text("✦ Verify & Unlock Premium AI", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Skip / offline mode ──────────────────────────────────────
                TextButton(
                    onClick = { viewModel.skipOnboarding(); onDismiss() },
                    enabled = !uiState.isValidating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Skip for now — Use Offline Mode",
                        color = Muted,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    emoji: String,
    label: String,
    offlineDesc: String,
    aiDesc: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeChip(label = "Offline", desc = offlineDesc, accent = Muted, modifier = Modifier.weight(1f))
            ModeChip(label = "AI ✦", desc = aiDesc, accent = Volt, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModeChip(label: String, desc: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(desc, color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
    }
}
