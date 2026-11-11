package com.hostelgains.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.data.update.DownloadStatus
import com.hostelgains.ui.theme.BorderStroke
import com.hostelgains.ui.theme.Ember
import com.hostelgains.ui.theme.Ink
import com.hostelgains.ui.theme.Muted
import com.hostelgains.ui.theme.Panel
import com.hostelgains.ui.theme.Surface
import com.hostelgains.ui.theme.TextPrimary
import com.hostelgains.ui.theme.TextSecondary
import com.hostelgains.ui.theme.Volt
import com.hostelgains.ui.viewmodel.UpdateUiState

/**
 * Shows an active background download banner when a newer version is being fetched.
 */
@Composable
fun UpdateBanner(
    uiState: UpdateUiState,
    modifier: Modifier = Modifier
) {
    val isDownloading = uiState.downloadStatus is DownloadStatus.Downloading
    val progress = (uiState.downloadStatus as? DownloadStatus.Downloading)?.progressPercent ?: 0

    AnimatedVisibility(
        visible = isDownloading,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Panel)
                .border(1.dp, Volt.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Volt,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = uiState.bannerMessage ?: "Updating in background...",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "$progress%",
                    color = Volt,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Volt,
                trackColor = Surface
            )
        }
    }
}

/**
 * 1-Tap Ready-to-Install Dialog after the background download finishes.
 */
@Composable
fun UpdateReadyDialog(
    uiState: UpdateUiState,
    onInstallClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    val ready = uiState.downloadStatus as? DownloadStatus.ReadyToInstall ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Volt.copy(alpha = 0.15f))
                    .border(1.dp, Volt, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = Volt,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Update Ready: ${ready.releaseInfo.tagName}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "A new update has been downloaded in the background and is ready to install.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                if (ready.releaseInfo.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = ready.releaseInfo.releaseNotes,
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onInstallClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Volt,
                    contentColor = Ink
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Install Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = Muted)
            }
        }
    )
}
