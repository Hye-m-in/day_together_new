package com.example.day_together.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


private val LightColorScheme = lightColorScheme(
    primary = ButtonActiveBackground,
    onPrimary = ButtonActiveText,

    secondary = AnniversaryBoardBackground,
    onSecondary = TextPrimary,

    tertiary = NavIconUnselected,
    onTertiary = TextPrimary,

    background = ScreenBackground,
    onBackground = TextPrimary,

    surface = ScreenBackground,
    onSurface = TextPrimary,

    surfaceVariant = Brown100,
    onSurfaceVariant = TextPrimary,

    outline = SelectedMonthlyBorder,

    error = ErrorRed,
    onError = Color.White
)

// TODO: 다크 테마 디자인이 확정되면 DarkColorScheme을 구체적으로 정의합니다.

private val DarkColorScheme = darkColorScheme(
    primary = NavIconUnselected,
    onPrimary = TextPrimary,
    secondary = ButtonActiveBackground,
    onSecondary = ButtonActiveText,
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

@Composable
fun Day_togetherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            window.statusBarColor = colorScheme.background.toArgb()

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

