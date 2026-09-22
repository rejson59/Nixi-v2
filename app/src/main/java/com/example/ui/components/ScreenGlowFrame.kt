package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Neon border glow shown when NIXI enters manual screen control / vision mode.
 * Glowing frames the screen in NIXI's signature luminous orb colors.
 */
@Composable
fun ScreenGlowFrame(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "screen_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val borderWidth = 5.dp.toPx()
        val glowWidth = 14.dp.toPx()

        // Outer soft glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x998F6FFF),
                    Color(0x3300E5FF),
                    Color.Transparent
                ),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension / 1.5f
            ),
            size = size
        )

        // Inner frame stroke
        drawRect(
            color = Color(0xFF8F6FFF).copy(alpha = glowAlpha),
            topLeft = Offset(borderWidth / 2f, borderWidth / 2f),
            size = Size(size.width - borderWidth, size.height - borderWidth),
            style = Stroke(width = borderWidth)
        )

        // Corner cyan highlights
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = glowAlpha),
                    Color(0xFF8F6FFF).copy(alpha = glowAlpha * 0.7f),
                    Color(0xFF00E5FF).copy(alpha = glowAlpha)
                )
            ),
            topLeft = Offset(glowWidth / 2f, glowWidth / 2f),
            size = Size(size.width - glowWidth, size.height - glowWidth),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
