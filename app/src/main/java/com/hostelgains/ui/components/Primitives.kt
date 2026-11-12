package com.hostelgains.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hostelgains.ui.theme.IgniteDeep
import com.hostelgains.ui.theme.Danger
import com.hostelgains.ui.theme.Ignite
import com.hostelgains.ui.theme.OnIgnite
import com.hostelgains.ui.theme.Outline
import com.hostelgains.ui.theme.OutlineStrong
import com.hostelgains.ui.theme.Success
import com.hostelgains.ui.theme.Surface1
import com.hostelgains.ui.theme.Surface2
import com.hostelgains.ui.theme.Surface3
import com.hostelgains.ui.theme.TextHigh
import com.hostelgains.ui.theme.TextLow
import com.hostelgains.ui.theme.TextMid

/** Standard elevated card used across every screen. */
@Composable
fun HgCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    containerColor: Color = Surface1,
    borderColor: Color = Outline,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .padding(contentPadding),
        content = content
    )
}

/** Large screen title with optional eyebrow and trailing actions. */
@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextLow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = TextHigh,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = trailing
        )
    }
}

/** Small section heading row: label on the left, optional action on the right. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = TextHigh,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

/** Compact tinted pill for status labels. */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    maxLines: Int = 1
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Shows whether Gemini AI is active or the app is running the offline engine. */
@Composable
fun AiModeChip(
    isPremium: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isPremium) Success else TextMid
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(if (isPremium) Success.copy(alpha = 0.12f) else Surface2)
            .border(1.dp, if (isPremium) Success.copy(alpha = 0.35f) else Outline, CircleShape)
            .clickable(enabled = !isPremium, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isPremium) Icons.Filled.AutoAwesome else Icons.Filled.Bolt,
            contentDescription = null,
            tint = if (isPremium) Success else Ignite,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (isPremium) "Gemini on" else "Offline · Unlock AI",
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/** Animated circular progress ring with centered content. */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 12.dp,
    trackColor: Color = Surface3,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "ring"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.minDimension - stroke, size.minDimension - stroke)
            val topLeft = Offset(
                (size.width - size.minDimension) / 2f + inset,
                (size.height - size.minDimension) / 2f + inset
            )
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            if (animated > 0.001f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.75f), color, color)),
                    startAngle = 135f,
                    sweepAngle = 270f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

/** Horizontal labelled progress bar used for macros. */
@Composable
fun MacroBar(
    label: String,
    value: Float,
    target: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val fraction = if (target > 0f) (value / target).coerceIn(0f, 1f) else 0f
    val animated by animateFloatAsState(fraction, tween(600), label = "macro")
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
                    .align(Alignment.CenterVertically)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = TextMid,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${value.toInt()}",
                style = MaterialTheme.typography.titleSmall,
                color = TextHigh
            )
            Text(
                text = " / ${target.toInt()} g",
                style = MaterialTheme.typography.bodySmall,
                color = TextLow
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Surface3)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** Small stat tile; place several in a Row with Modifier.weight(1f). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetricTile(
    icon: ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    unit: String? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(10.dp))
        // Number and unit are separate so the number never truncates on narrow phones.
        // FlowRow lets the unit drop under the number when the tile is too narrow.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = TextHigh,
                maxLines = 1,
                modifier = Modifier.alignByBaseline()
            )
            if (unit != null) {
                Text(
                    unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMid, maxLines = 1)
    }
}

/** Filled primary action. */
@Composable
fun HgPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    containerColor: Color = Ignite,
    contentColor: Color = OnIgnite
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Surface3,
            disabledContentColor = TextLow
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

/** Outlined secondary action. */
@Composable
fun HgSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = TextHigh,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, OutlineStrong),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Equal-width segmented control. Each option takes Modifier.weight(1f). */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(if (selected) Surface3 else Color.Transparent, label = "segBg")
            val fg by animateColorAsState(if (selected) TextHigh else TextMid, label = "segFg")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable(role = Role.Tab) { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = fg, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

/** Selectable pill chip for horizontal pickers. */
@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null
) {
    val bg by animateColorAsState(if (selected) Ignite else Surface2, label = "chipBg")
    val fg by animateColorAsState(if (selected) OnIgnite else TextHigh, label = "chipFg")
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) Ignite else Outline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, tint = if (selected) OnIgnite else Ignite, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Themed text field. */
@Composable
fun HgTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        placeholder = { Text(placeholder, color = TextLow, style = MaterialTheme.typography.bodyMedium) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextHigh),
        singleLine = singleLine,
        maxLines = maxLines,
        isError = isError,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Surface2,
            unfocusedContainerColor = Surface2,
            disabledContainerColor = Surface2,
            focusedBorderColor = Ignite,
            unfocusedBorderColor = Color.Transparent,
            errorBorderColor = Danger,
            cursorColor = Ignite,
            focusedTextColor = TextHigh,
            unfocusedTextColor = TextHigh
        )
    )
}

/** Brand mark: barbell monogram drawn in code so it scales on every density. */
@Composable
fun HostelGainsMark(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val ink = OnIgnite
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.linearGradient(listOf(Ignite, IgniteDeep))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(size * 0.2f)) {
            val w = this.size.width
            val h = this.size.height
            val bar = h * 0.11f
            val cy = h / 2f
            drawRoundRect(
                color = ink,
                topLeft = Offset(w * 0.12f, cy - bar / 2f),
                size = Size(w * 0.76f, bar),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(bar / 2f)
            )
            val plates = listOf(0.06f to 0.62f, 0.2f to 0.82f)
            plates.forEach { (x, ph) ->
                val pw = w * 0.11f
                val plateH = h * ph
                listOf(x, 1f - x - 0.11f).forEach { px ->
                    drawRoundRect(
                        color = ink,
                        topLeft = Offset(w * px, cy - plateH / 2f),
                        size = Size(pw, plateH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(pw / 2.5f)
                    )
                }
            }
        }
    }
}

