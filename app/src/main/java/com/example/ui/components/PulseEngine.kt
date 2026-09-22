package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pulse Engine — Jetpack Compose implementation of the NIXI Animated Orb.
 * Faithfully reproduces the layered CSS/React shader effects with real-time
 * audio volume reactivity and low CPU utilization.
 */
@Composable
fun PulseEngine(
    size: Dp = 240.dp,
    amplitude: Float = 0f,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_engine_transition")

    // o-pulse-engine-live (4.2s ease-in-out infinite)
    val liveScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_scale"
    )

    // o-pulse-engine-pulse (1.9s ease-in-out infinite)
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    // o-pulse-engine-spin (9s linear infinite)
    val energyRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "energy_spin"
    )

    // o-pulse-engine-spin reverse (17s linear infinite)
    val energy2Rotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 17000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "energy2_spin"
    )

    // o-pulse-engine-core (1.9s ease-in-out infinite)
    val corePulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )
    val corePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_alpha"
    )

    // o-pulse-engine-shimmer (6s ease-in-out infinite)
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.75f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    // Speech amplitude reactive boost
    val voiceScaleBoost = if (isSpeaking || isListening) amplitude * 0.22f else 0f
    val effectiveScale = (liveScale + voiceScaleBoost).coerceIn(0.9f, 1.4f)

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .scale(effectiveScale)
            .testTag("nixi_pulse_orb")
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. HALO (outer luminous glow)
        Box(
            modifier = Modifier
                .fillMaxSize(1.64f)
                .scale(haloScale + voiceScaleBoost * 0.5f)
                .alpha(haloAlpha)
                .blur(20.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xBF8F6FFF), // rgba(143,111,255, 0.75)
                            Color(0x4D8F6FFF), // rgba(143,111,255, 0.3)
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // 2. MAIN ORB BODY & ENERGY LAYERS
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        ) {
            // Background sphere with deep radial gradient & inner illumination
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(this.size.width * 0.5f, this.size.height * 0.45f)
                val highlightOffset = Offset(this.size.width * 0.31f, this.size.height * 0.27f)

                // Outer base gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE0D6FF),
                            Color(0xFF7A5CFF),
                            Color(0xFF1A0066)
                        ),
                        center = centerOffset,
                        radius = this.size.width * 0.65f
                    )
                )

                // Top-left soft specular glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x66FFFFFF),
                            Color.Transparent
                        ),
                        center = highlightOffset,
                        radius = this.size.width * 0.3f
                    )
                )
            }

            // Energy 1 (Rotating conic-like sweep)
            Box(
                modifier = Modifier
                    .fillMaxSize(1.6f)
                    .align(Alignment.Center)
                    .rotate(energyRotation)
                    .blur(16.dp)
                    .alpha(0.85f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweep = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            Color(0x8CEFE9FF),
                            Color.Transparent,
                            Color(0x668F6FFF),
                            Color.Transparent,
                            Color(0x4DEFE9FF),
                            Color.Transparent
                        )
                    )
                    drawCircle(brush = sweep)
                }
            }

            // Energy 2 (Reverse rotating conic sweep)
            Box(
                modifier = Modifier
                    .fillMaxSize(1.72f)
                    .align(Alignment.Center)
                    .rotate(energy2Rotation)
                    .blur(20.dp)
                    .alpha(0.70f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweep2 = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            Color(0x738F6FFF),
                            Color.Transparent,
                            Color(0x59EFE9FF),
                            Color.Transparent
                        )
                    )
                    drawCircle(brush = sweep2)
                }
            }

            // Core (pulsing glowing center)
            Box(
                modifier = Modifier
                    .fillMaxSize(0.40f)
                    .align(Alignment.Center)
                    .scale(corePulseScale + voiceScaleBoost * 0.6f)
                    .alpha(corePulseAlpha)
                    .blur(7.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xF2EFE9FF),
                                Color(0x73EFE9FF),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Shimmer band
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(16f)
            ) {
                val canvasW = this.size.width
                val canvasH = this.size.height
                val bandWidth = canvasW * 0.46f
                val bandLeft = canvasW * shimmerOffset
                val shimmerAlpha = if (shimmerOffset in -0.1f..1.1f) 0.5f else 0.0f
                if (shimmerAlpha > 0f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x66FFFFFF),
                                Color.Transparent
                            ),
                            startX = bandLeft,
                            endX = bandLeft + bandWidth
                        ),
                        topLeft = Offset(bandLeft, -canvasH * 0.25f),
                        size = Size(bandWidth, canvasH * 1.5f),
                        blendMode = BlendMode.Screen
                    )
                }
            }
        }

        // 3. RIM & SPECULAR HIGHLIGHTS
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 1.dp, color = Color(0x33FFFFFF), shape = CircleShape)
        ) {
            // Specular highlight 1 (top-left ellipse)
            Canvas(
                modifier = Modifier
                    .fillMaxSize(0.35f)
                    .align(Alignment.TopStart)
                    .offset(x = size * 0.12f, y = size * 0.07f)
                    .rotate(-24f)
                    .blur(3.dp)
            ) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x80FFFFFF),
                            Color(0x1FFFFFFF),
                            Color.Transparent
                        )
                    )
                )
            }

            // Specular highlight 2 (sharp top pin point)
            Canvas(
                modifier = Modifier
                    .fillMaxSize(0.12f)
                    .align(Alignment.TopStart)
                    .offset(x = size * 0.19f, y = size * 0.11f)
                    .rotate(-24f)
                    .blur(1.5.dp)
            ) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x99FFFFFF), Color.Transparent)
                    )
                )
            }

            // Specular highlight 3 (subtle bottom-right rim glow)
            Canvas(
                modifier = Modifier
                    .fillMaxSize(0.30f)
                    .align(Alignment.BottomEnd)
                    .offset(x = -size * 0.08f, y = -size * 0.09f)
                    .rotate(-24f)
                    .blur(6.dp)
            ) {
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x608F6FFF), Color.Transparent)
                    )
                )
            }
        }
    }
}
