package com.hostelgains.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hostelgains.ui.components.HgCard
import com.hostelgains.ui.components.HgPrimaryButton
import com.hostelgains.ui.components.HgSecondaryButton
import com.hostelgains.ui.components.HgTextField
import com.hostelgains.ui.components.HostelGainsMark
import com.hostelgains.ui.theme.Canvas0
import com.hostelgains.ui.theme.Danger
import com.hostelgains.ui.theme.Ignite
import com.hostelgains.ui.theme.Surface2
import com.hostelgains.ui.theme.TextHigh
import com.hostelgains.ui.theme.TextMid
import com.hostelgains.ui.viewmodel.ApiKeyViewModel

private const val AI_STUDIO_URL = "https://aistudio.google.com/app/apikey"

@Composable
fun ApiKeyOnboardingScreen(
    viewModel: ApiKeyViewModel,
    onDismiss: () -> Unit
) {
    val ui by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas0)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HostelGainsMark(size = 64.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                "Unlock Gemini AI",
                style = MaterialTheme.typography.headlineLarge,
                color = TextHigh,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Add your free Google AI Studio key for smarter meal parsing and workout rebalancing. You can skip this and use the offline engine.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMid,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            HgCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Feature(Icons.Filled.Restaurant, "Understands any mess menu", "Free-text meals become accurate macros.")
                    Feature(Icons.Filled.Balance, "Context-aware rebalancing", "Volume moves intelligently when equipment is busy.")
                    Feature(Icons.Filled.Lock, "Stays on your phone", "The key is stored encrypted on this device only.")
                }
            }

            Spacer(Modifier.height(20.dp))

            HgTextField(
                value = ui.apiKey,
                onValueChange = viewModel::onKeyInputChanged,
                placeholder = "Paste your API key",
                singleLine = true,
                maxLines = 1,
                enabled = !ui.isValidating,
                isError = ui.errorMessage != null,
                visualTransformation = if (ui.isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (ui.apiKey.isNotBlank()) viewModel.validateAndSaveKey(onSuccess = onDismiss)
                }),
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleKeyVisibility, enabled = !ui.isValidating) {
                        Icon(
                            if (ui.isKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (ui.isKeyVisible) "Hide key" else "Show key",
                            tint = TextMid
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            ui.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = Danger, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { uriHandler.openUri(AI_STUDIO_URL) }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Get a free key from Google AI Studio", style = MaterialTheme.typography.labelLarge, color = Ignite)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Ignite, modifier = Modifier.size(16.dp))
            }

            Spacer(Modifier.height(16.dp))
            HgPrimaryButton(
                text = if (ui.isValidating) "Verifying with Google…" else "Verify & unlock",
                icon = Icons.Filled.AutoAwesome,
                onClick = { viewModel.validateAndSaveKey(onSuccess = onDismiss) },
                enabled = ui.apiKey.isNotBlank(),
                loading = ui.isValidating,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            HgSecondaryButton(
                text = "Continue offline",
                onClick = { viewModel.skipOnboarding(); onDismiss() },
                enabled = !ui.isValidating,
                contentColor = TextMid,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Surface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Ignite, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextHigh)
            Text(body, style = MaterialTheme.typography.bodySmall, color = TextMid)
        }
    }
}
