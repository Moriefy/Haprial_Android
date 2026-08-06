package com.haprial.app.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun HaprialTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("haprial_theme", Context.MODE_PRIVATE)
    val themeMode = when (prefs.getString("theme", "system")) {
        "light" -> ThemeMode.LIGHT
        "dark" -> ThemeMode.DARK
        else -> ThemeMode.SYSTEM
    }

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}

private val LightColorScheme = lightColorScheme(
    primary = Primary, onPrimary = OnPrimary, primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer, secondary = Secondary, tertiary = Tertiary,
    surface = Surface, onSurface = OnSurface, surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant, outline = Outline, outlineVariant = OutlineVariant
)
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary, onPrimary = DarkOnPrimary, primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer, secondary = Secondary, tertiary = Tertiary,
    surface = DarkSurface, onSurface = DarkOnSurface, surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant, outline = DarkOutline, outlineVariant = DarkOutlineVariant
)
