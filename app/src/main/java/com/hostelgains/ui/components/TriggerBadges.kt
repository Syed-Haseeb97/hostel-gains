package com.hostelgains.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.ui.theme.Ember
import com.hostelgains.ui.theme.Ink
import com.hostelgains.ui.theme.Volt

@Composable
fun HighCarbBadge(
    modifier: Modifier = Modifier,
    text: String = "⚡ HIGH-CARB ACTIVE (+1 Set to Lats & Squats)"
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Volt.copy(alpha = 0.15f))
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ProteinDeficitBadge(
    modifier: Modifier = Modifier,
    text: String = "⚠️ PROTEIN-DEFICIT ACTIVE (Capped at 3 Sets)"
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Ember.copy(alpha = 0.15f))
            .border(width = 1.dp, color = Ember, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Ember)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = Ember,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun AiRebalancedBadge(
    modifier: Modifier = Modifier,
    text: String = "🤖 AI REBALANCED (Volume Redistributed)"
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Volt.copy(alpha = 0.20f))
            .border(width = 1.5.dp, color = Volt, shape = RoundedCornerShape(8.dp))
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}
