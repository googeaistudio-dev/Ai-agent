package com.example.ui.settings

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.domain.model.AIProviderConfig
import com.example.domain.model.AIProviderType
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.providerConfig.collectAsState()
    val voiceSettings by viewModel.voiceSettings.collectAsState()
    val floatingMascotEnabled by viewModel.floatingMascotEnabled.collectAsState()
    val reduceMotion by viewModel.reduceMotion.collectAsState()
    val testStatus by viewModel.connectionTestStatus.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()
    val discoveredModels by viewModel.discoveredModels.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()

    var providerType by remember(config.providerType) { mutableStateOf(config.providerType) }
    var baseUrlInput by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var apiKeyInput by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var selectedModel by remember(config.modelId) { mutableStateOf(config.modelId) }
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    var useGoogleSearch by remember(config.useGoogleSearch) { mutableStateOf(config.useGoogleSearch) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    var continuousListening by remember(voiceSettings.continuousListening) { mutableStateOf(voiceSettings.continuousListening) }
    var selectedLang by remember(voiceSettings.language) { mutableStateOf(voiceSettings.language) }
    var speechRate by remember(voiceSettings.speechRate) { mutableFloatStateOf(voiceSettings.speechRate) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "AI ও সিস্টেম সেটিংস (Settings)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "API Key, URL, মডেল নেম এবং ভয়েস কনফিগারেশন",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 1-Click AI Setup Presets Banner
        GlassCard(borderColor = NeonPurple) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Quick Setup", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "আর্টিফিসিয়াল ইন্টেলিজেন্ট দ্রুত সেটআপ (AI Presets)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "এক ট্যাপে API + URL + Model Name সেট করুন:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Google Gemini Preset Button
                    Button(
                        onClick = {
                            providerType = AIProviderType.GEMINI_DIRECT
                            baseUrlInput = "https://generativelanguage.googleapis.com/"
                            selectedModel = "gemini-2.5-flash"
                            useGoogleSearch = true

                            val envKey = try {
                                val k = BuildConfig.GEMINI_API_KEY
                                if (k.isNotBlank() && k != "MY_GEMINI_API_KEY") k else ""
                            } catch (_: Throwable) { "" }

                            val savedKey = viewModel.getKeyForProvider(AIProviderType.GEMINI_DIRECT)
                            val keyToUse = apiKeyInput.ifBlank { savedKey.ifBlank { envKey } }
                            apiKeyInput = keyToUse

                            val updated = config.copy(
                                providerType = AIProviderType.GEMINI_DIRECT,
                                baseUrl = "https://generativelanguage.googleapis.com/",
                                modelId = "gemini-2.5-flash",
                                apiKey = keyToUse,
                                useGoogleSearch = true
                            )
                            viewModel.saveProviderConfig(updated)
                            saveSuccessMessage = "✓ Google Gemini 2.5 Flash কনফিগারেশন সংরক্ষিত হয়েছে!"
                            viewModel.testConnection()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Google Gemini\n(2.5 Flash)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }

                    // OpenRouter Preset Button
                    Button(
                        onClick = {
                            providerType = AIProviderType.OPENAI_COMPATIBLE
                            baseUrlInput = "https://openrouter.ai/api/v1"
                            selectedModel = "google/gemini-2.0-flash-001"

                            val savedKey = viewModel.getKeyForProvider(AIProviderType.OPENAI_COMPATIBLE)
                            val keyToUse = apiKeyInput.ifBlank { savedKey }
                            apiKeyInput = keyToUse

                            val updated = config.copy(
                                providerType = AIProviderType.OPENAI_COMPATIBLE,
                                baseUrl = "https://openrouter.ai/api/v1",
                                modelId = "google/gemini-2.0-flash-001",
                                apiKey = keyToUse
                            )
                            viewModel.saveProviderConfig(updated)
                            saveSuccessMessage = "✓ OpenRouter API কনফিগারেশন সংরক্ষিত হয়েছে!"
                            if (keyToUse.isNotBlank()) viewModel.testConnection()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "OpenRouter\nAPI",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }

                    // Groq AI Fast Preset Button
                    Button(
                        onClick = {
                            providerType = AIProviderType.OPENAI_COMPATIBLE
                            baseUrlInput = "https://api.groq.com/openai/v1"
                            selectedModel = "llama-3.3-70b-versatile"

                            val savedKey = viewModel.getKeyForProvider(AIProviderType.OPENAI_COMPATIBLE)
                            val keyToUse = apiKeyInput.ifBlank { savedKey }
                            apiKeyInput = keyToUse

                            val updated = config.copy(
                                providerType = AIProviderType.OPENAI_COMPATIBLE,
                                baseUrl = "https://api.groq.com/openai/v1",
                                modelId = "llama-3.3-70b-versatile",
                                apiKey = keyToUse
                            )
                            viewModel.saveProviderConfig(updated)
                            saveSuccessMessage = "✓ Groq AI কনফিগারেশন সংরক্ষিত হয়েছে!"
                            if (keyToUse.isNotBlank()) viewModel.testConnection()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, NeonPurple, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "Groq AI\n(Fast)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Provider Details Card (API + URL + Model Name)
        GlassCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = "AI", tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI প্রোভাইডার কনফিগারেশন (API + URL + Model)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Provider Selector Tab
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurface)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (providerType == AIProviderType.GEMINI_DIRECT) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                providerType = AIProviderType.GEMINI_DIRECT
                                if (baseUrlInput.contains("openrouter") || baseUrlInput.contains("openai") || baseUrlInput.contains("groq")) {
                                    baseUrlInput = "https://generativelanguage.googleapis.com/"
                                }
                                if (!selectedModel.startsWith("gemini-")) {
                                    selectedModel = "gemini-2.5-flash"
                                }
                                val savedKey = viewModel.getKeyForProvider(AIProviderType.GEMINI_DIRECT)
                                if (savedKey.isNotBlank()) {
                                    apiKeyInput = savedKey
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Google Gemini REST",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (providerType == AIProviderType.GEMINI_DIRECT) NeonCyan else TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (providerType == AIProviderType.OPENAI_COMPATIBLE) NeonPurple.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                providerType = AIProviderType.OPENAI_COMPATIBLE
                                if (baseUrlInput.contains("generativelanguage")) {
                                    baseUrlInput = "https://openrouter.ai/api/v1"
                                }
                                if (selectedModel.startsWith("gemini-")) {
                                    selectedModel = "google/gemini-2.0-flash-001"
                                }
                                val savedKey = viewModel.getKeyForProvider(AIProviderType.OPENAI_COMPATIBLE)
                                if (savedKey.isNotBlank()) {
                                    apiKeyInput = savedKey
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OpenAI Compatible",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (providerType == AIProviderType.OPENAI_COMPATIBLE) NeonPurple else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. API Base URL Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Link, contentDescription = "URL", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "API Base URL:", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    Text(
                        text = "Reset Default",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        modifier = Modifier.clickable {
                            baseUrlInput = if (providerType == AIProviderType.GEMINI_DIRECT) {
                                "https://generativelanguage.googleapis.com/"
                            } else {
                                "https://openrouter.ai/api/v1"
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = baseUrlInput,
                    onValueChange = { baseUrlInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("base_url_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CyberSurface,
                        unfocusedContainerColor = CyberSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Model Name Field & Dropdown
                Text(
                    text = "মডেল নেম (Model Name):",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = modelDropdownExpanded,
                    onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedModel,
                        onValueChange = { selectedModel = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = CyberSurface,
                            unfocusedContainerColor = CyberSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = modelDropdownExpanded,
                        onDismissRequest = { modelDropdownExpanded = false },
                        modifier = Modifier.background(CyberSurface)
                    ) {
                        discoveredModels.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = model.displayName, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text(text = model.id, color = NeonCyan, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    selectedModel = model.id
                                    modelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. API Key Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = "Key", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "API Key (Keystore সুরক্ষিত):", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    // Auto-fill from injected secret button
                    Text(
                        text = "স্বয়ংক্রিয় কি ব্যবহার করুন",
                        fontSize = 11.sp,
                        color = HologramGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            val envKey = try {
                                val k = BuildConfig.GEMINI_API_KEY
                                if (k.isNotBlank() && k != "MY_GEMINI_API_KEY") k else ""
                            } catch (_: Throwable) { "" }
                            if (envKey.isNotBlank()) {
                                apiKeyInput = envKey
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input"),
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Text(
                            text = if (showApiKey) "Hide" else "Show",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable { showApiKey = !showApiKey }
                                .padding(8.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = CyberSurface,
                        unfocusedContainerColor = CyberSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Google Search Grounding toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Google Search Grounding",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "সঠিক ও হালনাগাদ তথ্য আনতে গুগল সার্চ ডেটা যুক্ত",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = useGoogleSearch,
                        onCheckedChange = { useGoogleSearch = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons: Test Connection & Save
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val updated = config.copy(
                                providerType = providerType,
                                baseUrl = baseUrlInput.trim(),
                                apiKey = apiKeyInput.trim(),
                                modelId = selectedModel.trim(),
                                useGoogleSearch = useGoogleSearch
                            )
                            viewModel.saveProviderConfig(updated)
                            viewModel.testConnection()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                            .testTag("test_connection_button")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(color = NeonCyan, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        } else {
                            Text(text = "টেস্ট কানেকশন", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            val updated = config.copy(
                                providerType = providerType,
                                baseUrl = baseUrlInput.trim(),
                                apiKey = apiKeyInput.trim(),
                                modelId = selectedModel.trim(),
                                useGoogleSearch = useGoogleSearch
                            )
                            viewModel.saveProviderConfig(updated)
                            saveSuccessMessage = "✓ আপনার API Key ও সেটিংস সফলভাবে সংরক্ষিত হয়েছে!"
                            if (apiKeyInput.isNotBlank()) {
                                viewModel.testConnection()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_config_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "সংরক্ষণ করুন", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (!saveSuccessMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(HologramGreen.copy(alpha = 0.15f))
                            .border(1.dp, HologramGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = saveSuccessMessage.orEmpty(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HologramGreen
                        )
                    }
                }

                if (!testStatus.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = testStatus.orEmpty(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (testStatus?.startsWith("✓") == true) HologramGreen else AlertRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Voice Settings Card
        GlassCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice", tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ভয়েস ইঞ্জিন সেটিংস (Voice & Listening)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Continuous Listening Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "অবিরাম লিসেনিং (Continuous Listening)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "কোনো ওয়েক-ওয়ার্ড ছাড়া স্বতঃস্ফূর্ত কথা শুনবে",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = continuousListening,
                        onCheckedChange = {
                            continuousListening = it
                            val updated = voiceSettings.copy(continuousListening = it)
                            viewModel.saveVoiceSettings(updated)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Language selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "ভাষা (Language):", fontSize = 13.sp, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedLang == "bn-BD") NeonCyan.copy(alpha = 0.2f) else CyberSurface)
                                .border(1.dp, if (selectedLang == "bn-BD") NeonCyan else CyberCardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedLang = "bn-BD"
                                    viewModel.saveVoiceSettings(voiceSettings.copy(language = "bn-BD"))
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "বাংলা (Bangla)", fontSize = 12.sp, color = if (selectedLang == "bn-BD") NeonCyan else TextSecondary)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedLang == "en-US") NeonCyan.copy(alpha = 0.2f) else CyberSurface)
                                .border(1.dp, if (selectedLang == "en-US") NeonCyan else CyberCardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedLang = "en-US"
                                    viewModel.saveVoiceSettings(voiceSettings.copy(language = "en-US"))
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "English", fontSize = 12.sp, color = if (selectedLang == "en-US") NeonCyan else TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Speech Rate Slider
                Text(
                    text = "কথা বলার গতি (Speech Rate: ${String.format("%.1fx", speechRate)})",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Slider(
                    value = speechRate,
                    onValueChange = {
                        speechRate = it
                        viewModel.saveVoiceSettings(voiceSettings.copy(speechRate = it))
                    },
                    valueRange = 0.7f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Echo Prevention Guard Notice
                Text(
                    text = "🛡️ স্ব-ট্রিগারিং রোধ: নাফসির কথা বলা অবস্থায় এবং শেষ হওয়ার পরবর্তী ৭০০ মিলিসেকেন্ড লিসেনিং নিঃশব্দ থাকে।",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Floating Mascot Overlay & Motion Card
        GlassCard {
            Column {
                Text(
                    text = "ম্যাসকট ও ইন্টারফেস (Mascot & Interface)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Floating Mascot on Screen switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ভাসমান সহকারী (Floating Overlay Mascot)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "অন্যান্য অ্যাপ ব্যবহারের সময়ও ম্যাসকট পর্দায় থাকবে",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Switch(
                        checked = floatingMascotEnabled,
                        onCheckedChange = {
                            if (it && !Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                context.startActivity(intent)
                            }
                            viewModel.setFloatingMascotEnabled(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reduce motion toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "অ্যানিমেশন হ্রাস (Reduce Motion)",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )

                    Switch(
                        checked = reduceMotion,
                        onCheckedChange = { viewModel.setReduceMotion(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.3f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accessibility Automation Service Card
        val isAccessibilityEnabled by viewModel.isAccessibilityActive.collectAsState()
        GlassCard(borderColor = if (isAccessibilityEnabled) HologramGreen else WarningAmber) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isAccessibilityEnabled) HologramGreen else WarningAmber)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "অ্যাক্সেসিবিলিটি সার্ভিস (Accessibility Service)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = if (isAccessibilityEnabled) "সক্রিয় (Active)" else "নিষ্ক্রিয় (Off)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAccessibilityEnabled) HologramGreen else WarningAmber
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "মোবাইলের অন্য অ্যাপে কাজ করা, স্ক্রিনে স্বয়ংক্রিয় বোতাম চাপা ও ফলাফল যাচাই করতে Accessibility সার্ভিস সক্রিয় করুন।",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAccessibilityEnabled) CyberSurface else WarningAmber
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isAccessibilityEnabled) "✓ অ্যাক্সেসিবিলিটি সেটিংস যাচাই করুন" else "👉 অ্যাক্সেসিবিলিটি সেটিংস থেকে Nafsi AI চালু করুন",
                        color = if (isAccessibilityEnabled) HologramGreen else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostics Card
        GlassCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = "Logs", tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ডায়াগনস্টিক লগ (Recent Diagnostics)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "${diagnostics.size} logs",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF030712))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        if (diagnostics.isEmpty()) {
                            Text(text = "No recent diagnostics.", fontSize = 11.sp, color = TextSecondary)
                        } else {
                            diagnostics.forEach { log ->
                                Text(
                                    text = log,
                                    fontSize = 10.sp,
                                    color = Color(0xFF38BDF8),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
