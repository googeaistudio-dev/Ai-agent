package com.example.ui.mascot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MascotState
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun InteractiveMascotStudio(
    currentState: MascotState,
    rmsLevel: Float,
    isListening: Boolean,
    onStateSelect: (MascotState) -> Unit,
    onToggleListening: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuickCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var isMinimized by remember { mutableStateOf(false) }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val mascotScale by animateDpAsState(
        targetValue = if (isMinimized) 90.dp else if (isPressed) 160.dp else 190.dp,
        animationSpec = spring(),
        label = "MascotSize"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CyberCard)
            .border(1.dp, CyberCardBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // State Title & Indicator Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Nafsi AI Companion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${currentState.bengaliLabel} (${currentState.englishLabel})",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonCyan
                )
            }

            // Size Mode Toggle (Minimized / Normal)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                    .clickable { isMinimized = !isMinimized }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isMinimized) "Expand (বড়)" else "Minimize (ছোট)",
                    fontSize = 11.sp,
                    color = NeonPurple,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mascot Interactive Drag & Tap Canvas Box
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyberSurface.copy(alpha = 0.8f), Color(0xFF060A14)),
                        radius = 400f
                    )
                )
                .border(1.dp, Color(0x2200E5FF), RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            // Snap toward center on release
                            offsetX = 0f
                            offsetY = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(-120f, 120f)
                        offsetY = (offsetY + dragAmount.y).coerceIn(-60f, 60f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isMenuOpen = !isMenuOpen
                    },
                contentAlignment = Alignment.Center
            ) {
                NafsiMascot(
                    state = currentState,
                    size = mascotScale,
                    rmsLevel = rmsLevel,
                    modifier = Modifier.testTag("floating_mascot_avatar")
                )

                // Radial Quick Action Menu Overlay
                MascotRadialMenu(
                    isMenuOpen = isMenuOpen,
                    isListening = isListening,
                    onToggleListening = {
                        onToggleListening()
                        isMenuOpen = false
                    },
                    onOpenChat = {
                        onOpenChat()
                        isMenuOpen = false
                    },
                    onOpenSettings = {
                        onOpenSettings()
                        isMenuOpen = false
                    },
                    onCloseMenu = { isMenuOpen = false }
                )
            }

            // Drag / Tap Instruction overlay
            Text(
                text = "ট্যাপ করুন মেনুর জন্য • ড্র্যাগ করে সরান",
                fontSize = 11.sp,
                color = TextSecondary.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // State Switcher Carousel
        ScrollableTabRow(
            selectedTabIndex = MascotState.values().indexOf(currentState),
            edgePadding = 4.dp,
            containerColor = Color.Transparent,
            divider = {},
            indicator = {}
        ) {
            MascotState.values().forEach { state ->
                val isSelected = state == currentState
                Tab(
                    selected = isSelected,
                    onClick = { onStateSelect(state) },
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else CyberSurface)
                        .border(
                            1.dp,
                            if (isSelected) NeonCyan else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = state.englishLabel,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NeonCyan else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Bengali Voice Test Tiles
        Text(
            text = "দ্রুত নির্দেশ পরীক্ষা (Quick Actions)",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionChip(
                label = "ক্যালকুলেটর ২+২",
                sublabel = "Calculator 2+2",
                onClick = { onQuickCommand("ক্যালকুলেটর খোলো এবং দুই যোগ দুই করো") },
                modifier = Modifier.weight(1f)
            )
            QuickActionChip(
                label = "স্ক্রিন পরিদর্শন",
                sublabel = "Inspect Screen",
                onClick = { onQuickCommand("এই স্ক্রিনে কী লেখা আছে?") },
                modifier = Modifier.weight(1f)
            )
            QuickActionChip(
                label = "সেটিংস খোলো",
                sublabel = "Open Settings",
                onClick = { onQuickCommand("Open Settings") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    sublabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CyberSurface)
            .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = sublabel,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}
