package org.openvault.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = NavyDark,
    primaryContainer = NavySurface,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = EmeraldGreenDark,
    onSecondary = NavyDark,
    secondaryContainer = NavyCard,
    onSecondaryContainer = EmeraldGreenDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    error = CrimsonErrorDark,
    onError = NavyDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = SurfaceLight,
    primaryContainer = SurfaceVariantLight,
    onPrimaryContainer = PrimaryBlueLight,
    secondary = EmeraldGreenLight,
    onSecondary = SurfaceLight,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = EmeraldGreenLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    error = CrimsonErrorLight,
    onError = SurfaceLight
)

@Composable
fun OpenVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
