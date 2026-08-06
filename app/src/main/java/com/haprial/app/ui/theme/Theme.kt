package com.haprial.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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

@Composable
fun HaprialTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}
