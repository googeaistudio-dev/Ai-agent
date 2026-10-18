package com.example.ui.mascot

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.MascotState
import com.example.ui.theme.AlertRed
import com.example.ui.theme.HologramGreen
import com.example.ui.theme.MascotRingGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanLight
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NafsiMascot(
    state: MascotState,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    rmsLevel: Float = 0f,
    reduceMotion: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MascotAnimations")

    // Vertical floating hover
    val hoverOffset by if (!reduceMotion) {
        infiniteTransition.animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Hover"
        )
    } else {
        infiniteTransition.animateFloat(0f, 0f, infiniteRepeatable(tween(1000)), "ZeroHover")
    }

    // Halo rotation
    val haloAngle by if (!reduceMotion) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(6000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Halo"
        )
    } else {
        infiniteTransition.animateFloat(0f, 0f, infiniteRepeatable(tween(1000)), "ZeroHalo")
    }

    // Pulse glow for speaking/listening
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )

    // Mouth animation for speaking
    val mouthOpen by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(260, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Mouth"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f + hoverOffset

            // 1. Outer Neon Glow & Orbital Ring
            val ringRadiusX = w * 0.48f * glowPulse
            val ringRadiusY = h * 0.22f * glowPulse

            // Orbital Ring Back Arc
            drawOval(
                brush = Brush.sweepGradient(
                    colors = listOf(NeonCyan, NeonMagenta, NeonPurple, NeonCyan),
                    center = Offset(cx, cy)
                ),
                topLeft = Offset(cx - ringRadiusX, cy - ringRadiusY),
                size = Size(ringRadiusX * 2, ringRadiusY * 2),
                style = Stroke(width = w * 0.035f)
            )

            // 2. Ear Fins (Translucent Cat-like Cyber Fins)
            val earWidth = w * 0.22f
            val earHeight = h * 0.28f

            // Left Ear
            val leftEarPath = Path().apply {
                moveTo(cx - w * 0.28f, cy - h * 0.12f)
                lineTo(cx - w * 0.38f, cy - h * 0.44f)
                lineTo(cx - w * 0.10f, cy - h * 0.32f)
                close()
            }
            drawPath(
                path = leftEarPath,
                brush = Brush.verticalGradient(
                    listOf(NeonCyanLight.copy(alpha = 0.9f), NeonPurple.copy(alpha = 0.7f)),
                    startY = cy - h * 0.44f,
                    endY = cy - h * 0.12f
                )
            )
            drawPath(
                path = leftEarPath,
                color = NeonCyan,
                style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
            )

            // Right Ear
            val rightEarPath = Path().apply {
                moveTo(cx + w * 0.28f, cy - h * 0.12f)
                lineTo(cx + w * 0.38f, cy - h * 0.44f)
                lineTo(cx + w * 0.10f, cy - h * 0.32f)
                close()
            }
            drawPath(
                path = rightEarPath,
                brush = Brush.verticalGradient(
                    listOf(NeonCyanLight.copy(alpha = 0.9f), NeonPurple.copy(alpha = 0.7f)),
                    startY = cy - h * 0.44f,
                    endY = cy - h * 0.12f
                )
            )
            drawPath(
                path = rightEarPath,
                color = NeonCyan,
                style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
            )

            // 3. Main Spherical Body Shell (Glossy Pearl White with Neon Rim Glow)
            val bodyRadius = w * 0.36f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFF94A3B8)),
                    center = Offset(cx - bodyRadius * 0.3f, cy - bodyRadius * 0.35f),
                    radius = bodyRadius * 1.2f
                ),
                radius = bodyRadius,
                center = Offset(cx, cy)
            )

            // Body Outer Cyber Ring / Rim Light
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(NeonCyan, NeonMagenta, NeonCyan),
                    center = Offset(cx, cy)
                ),
                radius = bodyRadius,
                center = Offset(cx, cy),
                style = Stroke(width = w * 0.022f)
            )

            // 4. Ear Pods / Headset Nodes
            drawCircle(
                color = Color(0xFF1E293B),
                radius = w * 0.08f,
                center = Offset(cx - bodyRadius * 0.95f, cy)
            )
            drawCircle(
                color = NeonCyan,
                radius = w * 0.055f,
                center = Offset(cx - bodyRadius * 0.95f, cy),
                style = Stroke(width = 3f)
            )

            drawCircle(
                color = Color(0xFF1E293B),
                radius = w * 0.08f,
                center = Offset(cx + bodyRadius * 0.95f, cy)
            )
            drawCircle(
                color = NeonCyan,
                radius = w * 0.055f,
                center = Offset(cx + bodyRadius * 0.95f, cy),
                style = Stroke(width = 3f)
            )

            // 5. Visor (Glossy Black Pill Face Screen)
            val visorWidth = bodyRadius * 1.48f
            val visorHeight = bodyRadius * 1.15f
            val visorTopLeft = Offset(cx - visorWidth / 2f, cy - visorHeight / 2f + h * 0.02f)
            val cornerRadius = CornerRadius(visorHeight * 0.45f, visorHeight * 0.45f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF020617)),
                    startY = visorTopLeft.y,
                    endY = visorTopLeft.y + visorHeight
                ),
                topLeft = visorTopLeft,
                size = Size(visorWidth, visorHeight),
                cornerRadius = cornerRadius
            )

            // Visor Glass Reflection Highlight Arc
            val glassPath = Path().apply {
                moveTo(visorTopLeft.x + visorWidth * 0.15f, visorTopLeft.y + visorHeight * 0.22f)
                cubicTo(
                    visorTopLeft.x + visorWidth * 0.35f, visorTopLeft.y + visorHeight * 0.12f,
                    visorTopLeft.x + visorWidth * 0.65f, visorTopLeft.y + visorHeight * 0.12f,
                    visorTopLeft.x + visorWidth * 0.85f, visorTopLeft.y + visorHeight * 0.22f
                )
            }
            drawPath(
                path = glassPath,
                color = Color.White.copy(alpha = 0.25f),
                style = Stroke(width = w * 0.025f, cap = StrokeCap.Round)
            )

            // 6. Facial Expressions (Eyes & Mouth according to MascotState)
            val eyeCenterY = cy - visorHeight * 0.04f
            val leftEyeX = cx - visorWidth * 0.24f
            val rightEyeX = cx + visorWidth * 0.24f
            val eyeBaseRadius = visorHeight * 0.20f

            val eyeColor = when (state) {
                MascotState.ERROR -> AlertRed
                MascotState.WORKING -> NeonCyanLight
                MascotState.SUCCESS -> HologramGreen
                MascotState.PAUSED -> Color(0xFF64748B)
                else -> NeonCyan
            }

            when (state) {
                MascotState.IDLE -> {
                    // Big glowing friendly oval eyes with pupil gleam
                    drawOval(
                        color = eyeColor,
                        topLeft = Offset(leftEyeX - eyeBaseRadius * 0.8f, eyeCenterY - eyeBaseRadius),
                        size = Size(eyeBaseRadius * 1.6f, eyeBaseRadius * 2f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = eyeBaseRadius * 0.35f,
                        center = Offset(leftEyeX - eyeBaseRadius * 0.2f, eyeCenterY - eyeBaseRadius * 0.35f)
                    )

                    drawOval(
                        color = eyeColor,
                        topLeft = Offset(rightEyeX - eyeBaseRadius * 0.8f, eyeCenterY - eyeBaseRadius),
                        size = Size(eyeBaseRadius * 1.6f, eyeBaseRadius * 2f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = eyeBaseRadius * 0.35f,
                        center = Offset(rightEyeX - eyeBaseRadius * 0.2f, eyeCenterY - eyeBaseRadius * 0.35f)
                    )

                    // Subtle happy curved mouth
                    val mouthPath = Path().apply {
                        moveTo(cx - w * 0.04f, cy + visorHeight * 0.24f)
                        quadraticBezierTo(cx, cy + visorHeight * 0.32f, cx + w * 0.04f, cy + visorHeight * 0.24f)
                    }
                    drawPath(mouthPath, eyeColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
                }

                MascotState.LISTENING -> {
                    // Squinting happy arc eyes (^ ^)
                    val leftArc = Path().apply {
                        moveTo(leftEyeX - eyeBaseRadius, eyeCenterY + eyeBaseRadius * 0.2f)
                        quadraticBezierTo(leftEyeX, eyeCenterY - eyeBaseRadius * 0.9f, leftEyeX + eyeBaseRadius, eyeCenterY + eyeBaseRadius * 0.2f)
                    }
                    drawPath(leftArc, eyeColor, style = Stroke(width = w * 0.035f, cap = StrokeCap.Round))

                    val rightArc = Path().apply {
                        moveTo(rightEyeX - eyeBaseRadius, eyeCenterY + eyeBaseRadius * 0.2f)
                        quadraticBezierTo(rightEyeX, eyeCenterY - eyeBaseRadius * 0.9f, rightEyeX + eyeBaseRadius, eyeCenterY + eyeBaseRadius * 0.2f)
                    }
                    drawPath(rightArc, eyeColor, style = Stroke(width = w * 0.035f, cap = StrokeCap.Round))

                    // Audio wave bars around left & right ears
                    val waveHeight1 = (12f + rmsLevel * 4f).coerceIn(8f, 32f)
                    val waveHeight2 = (20f + rmsLevel * 6f).coerceIn(12f, 44f)
                    drawLine(
                        color = NeonCyan,
                        start = Offset(cx - bodyRadius * 1.15f, cy - waveHeight1),
                        end = Offset(cx - bodyRadius * 1.15f, cy + waveHeight1),
                        strokeWidth = 4f, cap = StrokeCap.Round
                    )
                    drawLine(
                        color = NeonCyanLight,
                        start = Offset(cx - bodyRadius * 1.25f, cy - waveHeight2),
                        end = Offset(cx - bodyRadius * 1.25f, cy + waveHeight2),
                        strokeWidth = 4f, cap = StrokeCap.Round
                    )
                    drawLine(
                        color = NeonCyan,
                        start = Offset(cx + bodyRadius * 1.15f, cy - waveHeight1),
                        end = Offset(cx + bodyRadius * 1.15f, cy + waveHeight1),
                        strokeWidth = 4f, cap = StrokeCap.Round
                    )
                    drawLine(
                        color = NeonCyanLight,
                        start = Offset(cx + bodyRadius * 1.25f, cy - waveHeight2),
                        end = Offset(cx + bodyRadius * 1.25f, cy + waveHeight2),
                        strokeWidth = 4f, cap = StrokeCap.Round
                    )
                }

                MascotState.SPEAKING -> {
                    // Expressive eyes with open talking mouth
                    drawOval(
                        color = eyeColor,
                        topLeft = Offset(leftEyeX - eyeBaseRadius * 0.8f, eyeCenterY - eyeBaseRadius),
                        size = Size(eyeBaseRadius * 1.6f, eyeBaseRadius * 2f)
                    )
                    drawOval(
                        color = eyeColor,
                        topLeft = Offset(rightEyeX - eyeBaseRadius * 0.8f, eyeCenterY - eyeBaseRadius),
                        size = Size(eyeBaseRadius * 1.6f, eyeBaseRadius * 2f)
                    )

                    // Talking open mouth
                    val mouthY = cy + visorHeight * 0.25f
                    drawOval(
                        brush = Brush.verticalGradient(listOf(NeonMagenta, NeonCyan)),
                        topLeft = Offset(cx - w * 0.05f, mouthY - mouthOpen / 2f),
                        size = Size(w * 0.10f, mouthOpen)
                    )
                }

                MascotState.THINKING -> {
                    // Looking up thoughtfully
                    drawCircle(
                        color = eyeColor,
                        radius = eyeBaseRadius * 0.9f,
                        center = Offset(leftEyeX, eyeCenterY - eyeBaseRadius * 0.4f)
                    )
                    drawCircle(
                        color = eyeColor,
                        radius = eyeBaseRadius * 0.9f,
                        center = Offset(rightEyeX, eyeCenterY - eyeBaseRadius * 0.4f)
                    )

                    // Rotating thought particles around head
                    for (i in 0 until 3) {
                        val angle = (haloAngle + i * 120f) * (Math.PI.toFloat() / 180f)
                        val px = cx + cos(angle) * (bodyRadius * 0.8f)
                        val py = cy - bodyRadius * 0.7f + sin(angle) * (bodyRadius * 0.3f)
                        drawCircle(color = NeonCyanLight, radius = 5f, center = Offset(px, py))
                    }
                }

                MascotState.WORKING -> {
                    // Focused determined eyes
                    val leftWorkEye = Path().apply {
                        moveTo(leftEyeX - eyeBaseRadius, eyeCenterY - eyeBaseRadius * 0.3f)
                        lineTo(leftEyeX + eyeBaseRadius, eyeCenterY + eyeBaseRadius * 0.2f)
                        lineTo(leftEyeX - eyeBaseRadius * 0.3f, eyeCenterY + eyeBaseRadius * 0.6f)
                        close()
                    }
                    drawPath(leftWorkEye, NeonCyan)

                    val rightWorkEye = Path().apply {
                        moveTo(rightEyeX + eyeBaseRadius, eyeCenterY - eyeBaseRadius * 0.3f)
                        lineTo(rightEyeX - eyeBaseRadius, eyeCenterY + eyeBaseRadius * 0.2f)
                        lineTo(rightEyeX + eyeBaseRadius * 0.3f, eyeCenterY + eyeBaseRadius * 0.6f)
                        close()
                    }
                    drawPath(rightWorkEye, NeonCyan)

                    // Holographic HUD code panels floating beside mascot
                    val hudLeft = cx - bodyRadius * 1.55f
                    val hudRight = cx + bodyRadius * 1.15f
                    val hudY = cy - h * 0.15f
                    drawRoundRect(
                        color = NeonCyan.copy(alpha = 0.2f),
                        topLeft = Offset(hudLeft, hudY),
                        size = Size(w * 0.32f, h * 0.24f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = NeonCyan,
                        topLeft = Offset(hudLeft, hudY),
                        size = Size(w * 0.32f, h * 0.24f),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(1.5f)
                    )

                    // Mini HUD lines inside panel
                    for (row in 1..3) {
                        drawLine(
                            color = NeonCyanLight.copy(alpha = 0.8f),
                            start = Offset(hudLeft + 8f, hudY + row * 14f),
                            end = Offset(hudLeft + w * 0.24f, hudY + row * 14f),
                            strokeWidth = 2.5f
                        )
                    }

                    // Right HUD panel
                    drawRoundRect(
                        color = NeonCyan.copy(alpha = 0.2f),
                        topLeft = Offset(hudRight, hudY),
                        size = Size(w * 0.32f, h * 0.24f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = NeonCyan,
                        topLeft = Offset(hudRight, hudY),
                        size = Size(w * 0.32f, h * 0.24f),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(1.5f)
                    )
                    for (row in 1..3) {
                        drawLine(
                            color = NeonCyanLight.copy(alpha = 0.8f),
                            start = Offset(hudRight + 8f, hudY + row * 14f),
                            end = Offset(hudRight + w * 0.22f, hudY + row * 14f),
                            strokeWidth = 2.5f
                        )
                    }
                }

                MascotState.SUCCESS -> {
                    // Joyful curved squint eyes
                    val leftArc = Path().apply {
                        moveTo(leftEyeX - eyeBaseRadius, eyeCenterY + eyeBaseRadius * 0.2f)
                        quadraticBezierTo(leftEyeX, eyeCenterY - eyeBaseRadius * 0.9f, leftEyeX + eyeBaseRadius, eyeCenterY + eyeBaseRadius * 0.2f)
                    }
                    drawPath(leftArc, HologramGreen, style = Stroke(width = w * 0.035f, cap = StrokeCap.Round))

                    val rightArc = Path().apply {
                        moveTo(rightEyeX - eyeBaseRadius, eyeCenterY + eyeBaseRadius * 0.2f)
                        quadraticBezierTo(rightEyeX, eyeCenterY - eyeBaseRadius * 0.9f, rightEyeX + eyeBaseRadius, eyeCenterY + eyeBaseRadius * 0.2f)
                    }
                    drawPath(rightArc, HologramGreen, style = Stroke(width = w * 0.035f, cap = StrokeCap.Round))

                    // Big smile
                    val smilePath = Path().apply {
                        moveTo(cx - w * 0.06f, cy + visorHeight * 0.20f)
                        quadraticBezierTo(cx, cy + visorHeight * 0.38f, cx + w * 0.06f, cy + visorHeight * 0.20f)
                    }
                    drawPath(smilePath, HologramGreen, style = Stroke(width = 5f, cap = StrokeCap.Round))

                    // Twinkle star beside head
                    val starX = cx + bodyRadius * 0.9f
                    val starY = cy - bodyRadius * 0.6f
                    drawLine(HologramGreen, Offset(starX - 12f, starY), Offset(starX + 12f, starY), 3f)
                    drawLine(HologramGreen, Offset(starX, starY - 12f), Offset(starX, starY + 12f), 3f)
                }

                MascotState.ERROR -> {
                    // Angry / alert X-eyes or alert brows
                    drawLine(AlertRed, Offset(leftEyeX - 12f, eyeCenterY - 12f), Offset(leftEyeX + 12f, eyeCenterY + 12f), 5f, StrokeCap.Round)
                    drawLine(AlertRed, Offset(leftEyeX - 12f, eyeCenterY + 12f), Offset(leftEyeX + 12f, eyeCenterY - 12f), 5f, StrokeCap.Round)

                    drawLine(AlertRed, Offset(rightEyeX - 12f, eyeCenterY - 12f), Offset(rightEyeX + 12f, eyeCenterY + 12f), 5f, StrokeCap.Round)
                    drawLine(AlertRed, Offset(rightEyeX - 12f, eyeCenterY + 12f), Offset(rightEyeX + 12f, eyeCenterY - 12f), 5f, StrokeCap.Round)
                }

                MascotState.PAUSED -> {
                    // Sleepy horizontal slit eyes
                    drawLine(Color(0xFF64748B), Offset(leftEyeX - eyeBaseRadius, eyeCenterY), Offset(leftEyeX + eyeBaseRadius, eyeCenterY), 4f, StrokeCap.Round)
                    drawLine(Color(0xFF64748B), Offset(rightEyeX - eyeBaseRadius, eyeCenterY), Offset(rightEyeX + eyeBaseRadius, eyeCenterY), 4f, StrokeCap.Round)
                }

                MascotState.PERMISSION_REQUIRED -> {
                    drawOval(
                        color = Color(0xFFFFB020),
                        topLeft = Offset(leftEyeX - eyeBaseRadius * 0.7f, eyeCenterY - eyeBaseRadius * 0.7f),
                        size = Size(eyeBaseRadius * 1.4f, eyeBaseRadius * 1.4f)
                    )
                    drawOval(
                        color = Color(0xFFFFB020),
                        topLeft = Offset(rightEyeX - eyeBaseRadius * 0.7f, eyeCenterY - eyeBaseRadius * 0.7f),
                        size = Size(eyeBaseRadius * 1.4f, eyeBaseRadius * 1.4f)
                    )
                }
            }

            // 7. Foreground Orbital Ring Segment (Passing in front of the body)
            val frontRingPath = Path().apply {
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        Offset(cx - ringRadiusX, cy - ringRadiusY),
                        Size(ringRadiusX * 2, ringRadiusY * 2)
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
            }
            drawPath(
                path = frontRingPath,
                brush = Brush.horizontalGradient(listOf(NeonCyan, NeonMagenta, NeonCyanLight)),
                style = Stroke(width = w * 0.035f, cap = StrokeCap.Round)
            )
        }
    }
}
