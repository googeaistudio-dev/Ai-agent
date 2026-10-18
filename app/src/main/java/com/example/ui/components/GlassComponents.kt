package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ActionStep
import com.example.domain.model.AgentTask
import com.example.domain.model.StepStatus
import com.example.domain.model.TaskStatus
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.HologramGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanLight
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderColor: Color = CyberCardBorder,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(CyberCard)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun AudioWaveVisualizer(
    isListening: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "WaveAnimation")

    Row(
        modifier = modifier.height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 5) {
            val animHeight by transition.animateFloat(
                initialValue = 6f,
                targetValue = if (isListening) (14f + (rmsLevel * 3f) + (i * 3f)).coerceIn(10f, 26f) else 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(250 + i * 70, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "WaveBar$i"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animHeight.dp)
                    .clip(CircleShape)
                    .background(if (isListening) NeonCyan else Color(0xFF334155))
            )
        }
    }
}

@Composable
fun TaskExecutionCard(
    task: AgentTask,
    onCancelTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        borderColor = when (task.status) {
            TaskStatus.RUNNING -> NeonCyan
            TaskStatus.SUCCESS -> HologramGreen
            TaskStatus.FAILED -> AlertRed
            else -> CyberCardBorder
        }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (task.status) {
                        TaskStatus.RUNNING -> CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        TaskStatus.SUCCESS -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = HologramGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        TaskStatus.FAILED -> Icon(
                            Icons.Default.Error,
                            contentDescription = "Failed",
                            tint = AlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                        else -> Icon(
                            Icons.Default.HourglassTop,
                            contentDescription = "Pending",
                            tint = WarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সক্রিয় কাজ (Active Task)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (task.status == TaskStatus.RUNNING) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AlertRed.copy(alpha = 0.2f))
                            .border(1.dp, AlertRed, RoundedCornerShape(8.dp))
                            .clickable { onCancelTask() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "বাতিল (Cancel)",
                            fontSize = 11.sp,
                            color = AlertRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = task.goal,
                style = MaterialTheme.typography.bodyMedium,
                color = NeonCyanLight,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Steps Progress list
            task.steps.forEachIndexed { index, step ->
                StepProgressRow(step = step, isCurrent = index == task.activeStepIndex && task.status == TaskStatus.RUNNING)
                if (index < task.steps.size - 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            if (!task.resultMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = task.resultMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.status == TaskStatus.SUCCESS) HologramGreen else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun StepProgressRow(step: ActionStep, isCurrent: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (indicatorColor, statusIcon) = when (step.status) {
            StepStatus.COMPLETED -> Pair(HologramGreen, "✓")
            StepStatus.EXECUTING -> Pair(NeonCyan, "⋯")
            StepStatus.FAILED -> Pair(AlertRed, "✕")
            else -> Pair(Color(0xFF475569), "${step.index + 1}")
        }

        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(indicatorColor.copy(alpha = 0.2f))
                .border(1.dp, indicatorColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = statusIcon,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = indicatorColor
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.bengaliLabel.ifBlank { "${step.action.label} ${step.target ?: ""}" },
                fontSize = 12.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) NeonCyan else TextPrimary
            )
            if (!step.result.isNullOrBlank()) {
                Text(
                    text = step.result,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
