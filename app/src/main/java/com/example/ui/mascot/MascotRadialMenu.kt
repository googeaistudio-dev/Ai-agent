package com.example.ui.mascot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MascotRadialMenu(
    isMenuOpen: Boolean,
    isListening: Boolean,
    onToggleListening: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val radius = 80.dp

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = isMenuOpen,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                // Settings button (top right: angle ~ -45 deg)
                MenuActionButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    color = NeonCyan,
                    angleDeg = -60f,
                    distanceDp = 76f,
                    onClick = onOpenSettings
                )

                // Chat button (right: angle ~ 0 deg)
                MenuActionButton(
                    icon = Icons.Default.Forum,
                    contentDescription = "Chat",
                    color = NeonPurple,
                    angleDeg = -10f,
                    distanceDp = 76f,
                    onClick = onOpenChat
                )

                // Mic toggle button (bottom right: angle ~ 45 deg)
                MenuActionButton(
                    icon = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Toggle Mic",
                    color = if (isListening) NeonCyan else Color.Gray,
                    angleDeg = 40f,
                    distanceDp = 76f,
                    onClick = onToggleListening
                )

                // Close button (bottom right: angle ~ 85 deg)
                MenuActionButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close Menu",
                    color = AlertRed,
                    angleDeg = 90f,
                    distanceDp = 76f,
                    onClick = onCloseMenu
                )
            }
        }
    }
}

@Composable
private fun MenuActionButton(
    icon: ImageVector,
    contentDescription: String,
    color: Color,
    angleDeg: Float,
    distanceDp: Float,
    onClick: () -> Unit
) {
    val rad = Math.toRadians(angleDeg.toDouble())
    val offsetX = (distanceDp * cos(rad)).toInt()
    val offsetY = (distanceDp * sin(rad)).toInt()

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX, offsetY) }
            .size(44.dp)
            .clip(CircleShape)
            .background(CyberSurface.copy(alpha = 0.95f))
            .border(1.5.dp, color, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
    }
}
