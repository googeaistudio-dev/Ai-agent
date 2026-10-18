package com.example.ui.automation

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Screenshot
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
import com.example.domain.model.ActionType
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.TaskExecutionCard
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
fun AutomationScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeTask by viewModel.activeTask.collectAsState()
    val isAccessibilityEnabled = viewModel.isAccessibilityEnabled()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "অটোমেশন কন্ট্রোল (Automation Engine)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "স্ক্রিন পরিদর্শন ও অন্যান্য অ্যাপ্লিকেশনে কাজ পরিচালনা",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // System Capabilities Status
        GlassCard {
            Column {
                Text(
                    text = "সিস্টেম সক্ষমতা (Platform Status)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Accessibility Service Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Accessibility,
                            contentDescription = "Accessibility",
                            tint = if (isAccessibilityEnabled) HologramGreen else WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Accessibility Service",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isAccessibilityEnabled) "সক্রিয় (Active) - UI স্পর্শের অনুমতি আছে" else "নিষ্ক্রিয় (ট্যাপ করে চালু করুন)",
                                fontSize = 11.sp,
                                color = if (isAccessibilityEnabled) HologramGreen else WarningAmber
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isAccessibilityEnabled) HologramGreen.copy(alpha = 0.2f) else AlertRed.copy(alpha = 0.2f))
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isAccessibilityEnabled) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isAccessibilityEnabled) HologramGreen else AlertRed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Screen Capture / MediaProjection Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Screenshot,
                            contentDescription = "Screen Capture",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Screen Vision (MediaProjection)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "প্রয়োজনে স্ক্রিনশট ও ভিজ্যুয়াল অ্যানালাইসিস",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calculator Automation Test Card
        GlassCard(borderColor = NeonPurple) {
            Column {
                Text(
                    text = "Calculator Automation Verification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "অ্যাক্সেসিবিলিটি সার্ভিসের মাধ্যমে ডিভাইসের আসল Calculator অ্যাপ চালু করবে, ২ টিপবে, + টিপবে, ২ টিপবে, = টিপবে এবং স্ক্রিনের আউটপুট ৪ হিসেবে সত্যতা যাচাই (Verify) করবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { viewModel.runCalculatorDemo() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_calculator_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "২ + ২ হিসাব করুন (Run Calculator 2+2)",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Task Card if present
        activeTask?.let { task ->
            TaskExecutionCard(task = task, onCancelTask = { viewModel.cancelActiveTask() })
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Supported Actions Directory
        GlassCard {
            Column {
                Text(
                    text = "সমর্থিত অ্যাকশন তালিকা (Supported Actions)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                ActionType.values().forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = action.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (action) {
                                ActionType.LAUNCH_APP -> "অ্যাপের নাম অনুযায়ী খোলা"
                                ActionType.TAP_ELEMENT -> "টেক্সট বা বাটন খুঁজে ক্লিক করা"
                                ActionType.TAP_COORDINATE -> "নির্দিষ্ট x, y স্থানাঙ্কে স্পর্শ"
                                ActionType.ENTER_TEXT -> "ইনপুট ফিল্ডে লেখা টাইপ করা"
                                ActionType.PRESS_BACK -> "সিস্টেম ব্যাক বাটন অনুকরণ"
                                ActionType.PRESS_HOME -> "হোম স্ক্রিনে ফিরে যাওয়া"
                                ActionType.INSPECT_SCREEN -> "স্ক্রিনের তথ্য পাঠ করা"
                                ActionType.VERIFY_RESULT -> "ফলাফল যাচাই ও সত্যতা নিশ্চিতকরণ"
                                ActionType.SCROLL -> "স্ক্রিন স্ক্রল করা"
                            },
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
