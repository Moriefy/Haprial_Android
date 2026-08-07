package com.haprial.app.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.moriafly.salt.ui.SaltConfigs
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.saltConfigs

enum class ThemeMode { SYSTEM, LIGHT, DARK }

// 全局主题状态 — SettingsScreen 修改后立即生效
var currentThemeMode = mutableStateOf("system")

@Composable
fun HaprialTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current

    // 首次加载时从 SharedPreferences 读取
    LaunchedEffect(Unit) {
        val prefs = ctx.getSharedPreferences("haprial_theme", Context.MODE_PRIVATE)
        currentThemeMode.value = prefs.getString("theme", "system") ?: "system"
    }

    // 直接读取 reactive state，不用 LaunchedEffect 中转
    val themeModeValue by currentThemeMode

    val darkTheme = when (themeModeValue) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    SaltTheme(
        configs = saltConfigs(isDarkTheme = darkTheme)
    ) {
        content()
    }
}
