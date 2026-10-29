package com.eduard.crvlauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp

/** Rounded "glass" card with a vertical gradient, thin border and press feedback. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    top: Color,
    bottom: Color,
    border: Color,
    modifier: Modifier = Modifier,
    radius: Dp = 22.u,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .scale(if (pressed) 0.98f else 1f)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(1.5.u, border.copy(alpha = 0.75f), shape)
            .let { m ->
                if (onClick != null) {
                    m.combinedClickable(interaction, indication = null, onLongClick = onLongClick, onClick = onClick)
                } else m
            },
        content = content,
    )
}

@Composable
fun Label(
    text: String,
    size: Number,
    color: Color = Palette.TextPrimary,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(color = color, fontSize = size.us, fontWeight = weight),
    )
}

/** Dark navy background with the large soft blue swooshes of the design. */
@Composable
fun AppBackground() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Palette.Background)
        drawCircle(
            Brush.radialGradient(
                listOf(Palette.BackgroundGlow.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(size.width * 0.62f, size.height * -0.05f),
                radius = size.width * 0.55f,
            ),
            radius = size.width * 0.55f,
            center = Offset(size.width * 0.62f, size.height * -0.05f),
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Palette.BackgroundGlow.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(size.width * 0.95f, size.height * 1.05f),
                radius = size.width * 0.45f,
            ),
            radius = size.width * 0.45f,
            center = Offset(size.width * 0.95f, size.height * 1.05f),
        )
    }
}

/** Generic Android-Auto-style mark (blue chevron) — drawn, not the official asset. */
@Composable
fun AutoMark(size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val left = Path().apply {
            moveTo(w * 0.5f, h * 0.05f); lineTo(w * 0.05f, h * 0.92f); lineTo(w * 0.5f, h * 0.70f); close()
        }
        val right = Path().apply {
            moveTo(w * 0.5f, h * 0.05f); lineTo(w * 0.95f, h * 0.92f); lineTo(w * 0.5f, h * 0.70f); close()
        }
        drawPath(left, Color(0xFF4F8DFF))
        drawPath(right, Color(0xFF2F6BE6))
        val inner = Path().apply {
            moveTo(w * 0.5f, h * 0.48f); lineTo(w * 0.28f, h * 0.84f); lineTo(w * 0.5f, h * 0.74f)
            lineTo(w * 0.72f, h * 0.84f); close()
        }
        drawPath(inner, Color(0xFFDCE7FF))
    }
}

/** Generic CarPlay-style mark (green tile with play ring). */
@Composable
fun CarPlayMark(size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF3CE06B), Color(0xFF16B347))),
            size = Size(s, s),
            cornerRadius = CornerRadius(s * 0.24f),
        )
        drawCircle(Color.White, radius = s * 0.30f, style = Stroke(width = s * 0.07f))
        val tri = Path().apply {
            moveTo(s * 0.44f, s * 0.36f); lineTo(s * 0.66f, s * 0.5f); lineTo(s * 0.44f, s * 0.64f); close()
        }
        drawPath(tri, Color.White)
    }
}

@Composable
fun CircleOutline(size: Dp, color: Color, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .border(2.u, color, CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** A short, low-key message ("Sin conexión a internet") shown at the bottom of the screen. */
data class Notice(val text: String, val icon: ImageVector, val id: Long = System.nanoTime())

@Composable
fun BoxScope.NoticePill(notice: Notice?) {
    // Keep the last text while fading out, so it doesn't blank mid-animation.
    var shown by remember { mutableStateOf(notice) }
    if (notice != null) shown = notice
    AnimatedVisibility(
        notice != null,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(600)),
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.u),
    ) {
        val n = shown ?: return@AnimatedVisibility
        Row(
            Modifier
                .clip(RoundedCornerShape(30.u))
                .background(Color(0xE6101C38))
                .border(1.u, Color(0x552F5BB0), RoundedCornerShape(30.u))
                .padding(horizontal = 28.u, vertical = 14.u),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(n.icon, null, tint = Palette.TextSecondary, modifier = Modifier.size(30.u))
            Spacer(Modifier.width(14.u))
            Label(n.text, 24, color = Color(0xFFD5DEF5))
        }
    }
}
