package com.example.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MascotState
import com.example.ui.MainViewModel
import com.example.ui.components.AudioWaveVisualizer
import com.example.ui.components.GlassCard
import com.example.ui.components.TaskExecutionCard
import com.example.ui.mascot.InteractiveMascotStudio
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.HologramGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mascotState by viewModel.mascotState.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isContinuousListening by viewModel.isContinuousListening.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()
    val partialText by viewModel.partialText.collectAsState()
    val activeTask by viewModel.activeTask.collectAsState()

    val isAccessibilityEnabled by viewModel.isAccessibilityActive.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NAFSI AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonCyan,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "স্বায়ত্তশাসিত ভয়েস ও ভিজ্যুয়াল এজেন্ট",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Continuous Listening Toggle Button
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isContinuousListening) NeonCyan.copy(alpha = 0.2f) else CyberSurface)
                    .border(1.dp, if (isContinuousListening) NeonCyan else Color.Gray, CircleShape)
                    .clickable { viewModel.toggleContinuousListening() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("toggle_listening_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isContinuousListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Listening status",
                        tint = if (isContinuousListening) NeonCyan else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isContinuousListening) "Listening: ON" else "Paused",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isContinuousListening) NeonCyan else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // AI Model & Key Connection Status Banner
        val providerConfig by viewModel.providerConfig.collectAsState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSurface)
                .border(
                    1.dp,
                    if (providerConfig.apiKey.isNotBlank()) HologramGreen.copy(alpha = 0.4f) else WarningAmber.copy(alpha = 0.6f),
                    RoundedCornerShape(12.dp)
                )
                .clickable { onNavigateToSettings() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (providerConfig.apiKey.isNotBlank()) HologramGreen else WarningAmber)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (providerConfig.apiKey.isNotBlank()) "AI প্রস্তুত: ${providerConfig.modelId}" else "AI সেটআপ প্রয়োজন (API Key + URL + Model)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (providerConfig.apiKey.isNotBlank()) TextPrimary else WarningAmber
                )
            }
            Text(
                text = "কনফিগার করুন >",
                fontSize = 11.sp,
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Accessibility Service Alert Banner if not enabled
        if (!isAccessibilityEnabled) {
            GlassCard(
                borderColor = WarningAmber,
                modifier = Modifier.clickable {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Accessibility,
                        contentDescription = "Accessibility",
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "অ্যাক্সেসিবিলিটি সার্ভিস প্রয়োজন",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                        Text(
                            text = "অন্যান্য অ্যাপে স্পর্শ ও স্বয়ংক্রিয় কাজ করতে সার্ভিসটি চালু করুন। (ট্যাপ করুন)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Real-time Voice Recognition Banner
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AudioWaveVisualizer(isListening = isListening, rmsLevel = rmsLevel)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isListening) "শুনছি... (Listening)" else if (isContinuousListening) "প্রস্তুত (Ready for speech)" else "লিসেনিং স্থগিত (Paused)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isListening) NeonCyan else TextSecondary
                    )
                    Text(
                        text = if (partialText.isNotBlank()) "\"$partialText\"" else "কথা বলুন: 'ক্যালকুলেটর খোলো', '২ যোগ ২'...",
                        fontSize = 13.sp,
                        color = if (partialText.isNotBlank()) NeonCyan else TextPrimary,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Task Card if executing
        activeTask?.let { task ->
            TaskExecutionCard(
                task = task,
                onCancelTask = { viewModel.cancelActiveTask() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Interactive Mascot Studio (Floating companion preview, drag, touch, radial menu, states)
        InteractiveMascotStudio(
            currentState = mascotState,
            rmsLevel = rmsLevel,
            isListening = isListening,
            onStateSelect = { viewModel.setMascotState(it) },
            onToggleListening = { viewModel.toggleContinuousListening() },
            onOpenChat = onNavigateToChat,
            onOpenSettings = onNavigateToSettings,
            onQuickCommand = { viewModel.onUserCommand(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Automation Test Runner Card
        GlassCard {
            Column {
                Text(
                    text = "স্বয়ংক্রিয় টেস্ট (Automated Verification Test)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ক্যালকুলেটরে ২+২ হিসাব করে ৪ ফলাফল যাচাই করার পূর্ণাঙ্গ সাইকেল পরীক্ষা করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.runCalculatorDemo() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_calculator_demo_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ক্যালকুলেটর ২+২ অটোমেশন চালান",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
