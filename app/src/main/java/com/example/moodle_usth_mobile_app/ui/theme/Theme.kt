package com.example.moodle_usth_mobile_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.moodle_usth_mobile_app.data.local.ThemeMode

/** Chế độ sáng/tối thực tế sau khi áp lựa chọn của người dùng (Theo hệ thống / Sáng / Tối). */
@Composable
fun isAppInDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun MoodleusthmobileappTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = Color.White,
            secondary = colors.green,
            tertiary = colors.amber,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.card,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.cardBorder,
            onSurfaceVariant = colors.textSecondary,
            surfaceContainerHigh = colors.card,
            outline = colors.cardBorder
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = Color.White,
            secondary = colors.green,
            onSecondary = Color.White,
            tertiary = colors.amber,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.card,
            onSurface = colors.textPrimary,
            surfaceContainerHigh = colors.card,
            outline = colors.cardBorder
        )
    }

    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
