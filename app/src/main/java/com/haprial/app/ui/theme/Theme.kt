package com.haprial.app.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.moriafly.salt.ui.SaltConfigs
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.saltConfigs

enum class ThemeMode { SYSTEM, LIGHT, DARK }

// Global reactive theme state - updated from SettingsScreen
var currentThemeMode = mutableStateOf("system")

@Composable
fun HaprialTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    // Read from SharedPreferences on first composition, then use reactive state
    LaunchedEffect(Unit) {
        val prefs = ctx.getSharedPreferences("haprial_theme", Context.MODE_PRIVATE)
        currentThemeMode.value = prefs.getString("theme", "system") ?: "system"
    }

    val themeModeValue by currentThemeMode
    val themeMode = when (themeModeValue) {
        "light" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    SaltTheme(
        configs = saltConfigs(isDarkTheme = darkTheme)
    ) {
        content()
    }
}
