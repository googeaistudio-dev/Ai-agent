package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "ম্যাসকট (Home)", Icons.Default.Home)
    object Chat : Screen("chat", "কথোপকথন (Chat)", Icons.Default.Forum)
    object Automation : Screen("automation", "অটোমেশন (Tasks)", Icons.Default.AutoAwesome)
    object Settings : Screen("settings", "সেটিংস (Settings)", Icons.Default.Settings)
}
