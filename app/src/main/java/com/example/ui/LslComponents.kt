@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Lsl
import com.example.ui.theme.Scoreboard

// ---------------------------------------------------------------------------
// Screen scaffold
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LslScreen(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Lsl.Chalk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle != null) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Lsl.Mist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Lsl.Chalk)
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Lsl.Night,
                    scrolledContainerColor = Lsl.Night
                )
            )
        },
        floatingActionButton = floatingActionButton,
        containerColor = Lsl.Night,
        content = content
    )
}

// ---------------------------------------------------------------------------
// Surfaces
// ---------------------------------------------------------------------------

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = Lsl.Deck,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .clip(shape)
        .background(color)
    Column(
        modifier = (if (onClick != null) base.pressable(onClick = onClick) else base)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = Lsl.Mist)
        trailing?.invoke()
    }
}

// ---------------------------------------------------------------------------
// Press feedback: a small squash + haptic tick, the app's single touch language
// ---------------------------------------------------------------------------

fun Modifier.pressable(
    enabled: Boolean = true,
    haptic: Boolean = true,
    pressedScale: Float = 0.96f,
    role: Role = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "press-scale"
    )
    val hf = LocalHapticFeedback.current
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = interaction,
            indication = ripple(color = Lsl.Chalk),
            enabled = enabled,
            role = role,
            onLongClick = onLongClick?.let { lc ->
                {
                    if (haptic) hf.performHapticFeedback(HapticFeedbackType.LongPress)
                    lc()
                }
            },
            onClick = {
                if (haptic) hf.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        )
}

// ---------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    color: Color = Lsl.Flame,
    contentColor: Color = Lsl.FlameInk,
    height: Dp = 52.dp
) {
    val bg by animateColorAsState(if (enabled) color else Lsl.DeckRaised, label = "btn-bg")
    val fg by animateColorAsState(if (enabled) contentColor else Lsl.Fade, label = "btn-fg")
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = fg)
    }
}

@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    tint: Color = Lsl.Chalk,
    height: Dp = 44.dp
) {
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(Lsl.DeckRaised)
            .pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val c = if (enabled) tint else Lsl.Fade
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = c, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = c, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------
// Choice chip
// ---------------------------------------------------------------------------

@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Lsl.Flame,
    leading: (@Composable () -> Unit)? = null
) {
    val bg by animateColorAsState(if (selected) accent else Lsl.DeckRaised, tween(160), label = "chip-bg")
    val fg by animateColorAsState(if (selected) Lsl.Night else Lsl.Chalk, tween(160), label = "chip-fg")
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .pressable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------------------
// Ball chip: one delivery on the over track / timeline
// ---------------------------------------------------------------------------

data class BallTone(val bg: Color, val fg: Color)

fun ballTone(label: String): BallTone = when {
    label.startsWith("W") -> BallTone(Lsl.Tape, Lsl.Night)
    label.endsWith("wd") || label.endsWith("nb") -> BallTone(Lsl.Violet.copy(alpha = 0.22f), Lsl.Violet)
    label.endsWith("lb") || label.endsWith("b") -> BallTone(Lsl.Violet.copy(alpha = 0.12f), Lsl.Violet)
    label == "6" -> BallTone(Lsl.Sky, Lsl.Night)
    label == "4" -> BallTone(Lsl.Gold, Lsl.Night)
    label == "0" -> BallTone(Lsl.DeckRaised, Lsl.Mist)
    else -> BallTone(Lsl.Chalk.copy(alpha = 0.12f), Lsl.Chalk)
}

@Composable
fun BallChip(label: String, modifier: Modifier = Modifier, size: Dp = 34.dp) {
    val tone = ballTone(label)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tone.bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (label == "0") "•" else label,
            color = tone.fg,
            fontFamily = Scoreboard,
            fontWeight = FontWeight.Bold,
            fontSize = when {
                label.length <= 1 -> (size.value * 0.5f).sp
                label.length <= 3 -> (size.value * 0.36f).sp
                else -> (size.value * 0.3f).sp
            },
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// Team monogram: a stable colour per team so lists are scannable
// ---------------------------------------------------------------------------

private val monogramColors = listOf(
    Color(0xFFFF7A2F), Color(0xFFFFC53D), Color(0xFF5CC8FF), Color(0xFF7EE08A),
    Color(0xFFB69CFF), Color(0xFFFF8FB1), Color(0xFF4FD8C4), Color(0xFFF2B880)
)

fun teamColor(name: String): Color =
    monogramColors[(name.lowercase().hashCode() and 0x7fffffff) % monogramColors.size]

fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

@Composable
fun Monogram(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp, color: Color = teamColor(name)) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initialsOf(name),
            color = color,
            fontFamily = Scoreboard,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp
        )
    }
}

// ---------------------------------------------------------------------------
// Empty state – always ends with the action that fills it
// ---------------------------------------------------------------------------

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Lsl.DeckRaised),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Lsl.Flame, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Lsl.Chalk, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = Lsl.Mist,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            PrimaryButton(text = actionText, onClick = onAction)
        }
    }
}

// ---------------------------------------------------------------------------
// Dialogs
// ---------------------------------------------------------------------------

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Lsl.Deck,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = Lsl.Chalk) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium, color = Lsl.Mist) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmText, color = if (destructive) Lsl.Tape else Lsl.Flame, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Lsl.Mist) }
        }
    )
}

@Composable
fun lslFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Lsl.Flame,
    unfocusedBorderColor = Lsl.Line,
    focusedLabelColor = Lsl.Flame,
    unfocusedLabelColor = Lsl.Mist,
    focusedTextColor = Lsl.Chalk,
    unfocusedTextColor = Lsl.Chalk,
    cursorColor = Lsl.Flame,
    focusedLeadingIconColor = Lsl.Flame,
    unfocusedLeadingIconColor = Lsl.Mist,
    focusedContainerColor = Lsl.Deck,
    unfocusedContainerColor = Lsl.Deck,
    focusedPlaceholderColor = Lsl.Fade,
    unfocusedPlaceholderColor = Lsl.Fade
)

/** Small label + value pair used in stat rows. */
@Composable
fun Stat(value: String, label: String, modifier: Modifier = Modifier, valueColor: Color = Lsl.Chalk, alignEnd: Boolean = false) {
    Column(modifier = modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(value, fontFamily = Scoreboard, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = valueColor, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Lsl.Mist, maxLines = 1)
    }
}

fun Double.fmt(digits: Int = 2): String = String.format(java.util.Locale.US, "%.${digits}f", this)

/** True when the user turned animations off in system settings. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }
}

/** Counts up from zero the first time it is shown (instantly when motion is reduced). */
@Composable
fun CountUpText(
    value: Int,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    durationMs: Int = 900,
    format: (Int) -> String = { "$it" }
) {
    val reduce = rememberReduceMotion()
    val anim = remember { Animatable(if (reduce) value.toFloat() else 0f) }
    LaunchedEffect(value) {
        if (reduce) anim.snapTo(value.toFloat())
        else anim.animateTo(value.toFloat(), tween(durationMs, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
    }
    Text(format(anim.value.toInt()), style = style, color = color, modifier = modifier, maxLines = 1)
}
