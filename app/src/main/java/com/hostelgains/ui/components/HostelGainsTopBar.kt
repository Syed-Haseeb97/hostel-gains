package com.hostelgains.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.app.R
import com.hostelgains.ui.theme.Canvas0
import com.hostelgains.ui.theme.Danger
import com.hostelgains.ui.theme.Ignite
import com.hostelgains.ui.theme.OnIgnite
import com.hostelgains.ui.theme.Outline
import com.hostelgains.ui.theme.Success
import com.hostelgains.ui.theme.Surface1
import com.hostelgains.ui.theme.Surface2
import com.hostelgains.ui.theme.TextHigh
import com.hostelgains.ui.theme.TextLow
import com.hostelgains.ui.theme.TextMid
import com.hostelgains.ui.theme.Warning

/**
 * Top brand bar featuring the Sir Whiskers Gym Cat mascot,
 * title, and interactive Gemini AI status chip.
 */
@Composable
fun HostelGainsTopBar(
    statusMessage: String,
    isKeyValid: Boolean,
    onAiBadgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeBg by animateColorAsState(
        if (isKeyValid) Success.copy(alpha = 0.15f) else Warning.copy(alpha = 0.18f),
        label = "aiBadgeBg"
    )
    val badgeBorder by animateColorAsState(
        if (isKeyValid) Success.copy(alpha = 0.45f) else Warning.copy(alpha = 0.65f),
        label = "aiBadgeBorder"
    )
    val badgeFg by animateColorAsState(
        if (isKeyValid) Success else Warning,
        label = "aiBadgeFg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Canvas0)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Gym Cat Mascot Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, Ignite, CircleShape)
            ) {
                Image(
                    painter = painterResource(R.drawable.gym_cat),
                    contentDescription = "Sir Whiskers Gym Cat Mascot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Hostel-Gains",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = TextHigh
                )
                Text(
                    text = "Eat smart · Train smart",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Interactive AI Status Pill Badge
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(badgeBg)
                .border(1.dp, badgeBorder, CircleShape)
                .clickable(role = Role.Button, onClick = onAiBadgeClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isKeyValid) Icons.Filled.AutoAwesome else Icons.Filled.Key,
                contentDescription = null,
                tint = badgeFg,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = badgeFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * High-visibility reminder banner displayed when Gemini API key is missing or invalid.
 */
@Composable
fun ApiKeyReminderBanner(
    onEnterKey: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Warning.copy(alpha = 0.12f))
            .border(1.dp, Warning.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = Warning,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Connect Gemini AI Key for live coaching",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Warning,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Ignite)
                    .clickable(role = Role.Button, onClick = onEnterKey)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Connect",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnIgnite
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = TextLow,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
