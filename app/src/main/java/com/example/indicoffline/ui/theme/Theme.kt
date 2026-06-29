package com.example.indicoffline.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = DarkOnPrimary,
    onBackground = DarkOnBackground,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkOnBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = LightOnPrimary,
    onBackground = LightOnBackground,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightOnBackground
)

@Suppress("DEPRECATION")
@Composable
fun animateColorScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(durationMillis = 300, easing = FastOutSlowInEasing)
    val primary by animateColorAsState(target.primary, spec, label = "")
    val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, spec, label = "")
    val secondary by animateColorAsState(target.secondary, spec, label = "")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, spec, label = "")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, spec, label = "")
    val tertiary by animateColorAsState(target.tertiary, spec, label = "")
    val background by animateColorAsState(target.background, spec, label = "")
    val surface by animateColorAsState(target.surface, spec, label = "")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "")
    val onBackground by animateColorAsState(target.onBackground, spec, label = "")
    val onSurface by animateColorAsState(target.onSurface, spec, label = "")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, spec, label = "")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, spec, label = "")
    val error by animateColorAsState(target.error, spec, label = "")
    val onError by animateColorAsState(target.onError, spec, label = "")

    return ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = target.inversePrimary,
        secondary = secondary,
        onSecondary = target.onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = target.onTertiary,
        tertiaryContainer = target.tertiaryContainer,
        onTertiaryContainer = target.onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = target.surfaceTint,
        inverseSurface = target.inverseSurface,
        inverseOnSurface = target.inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = target.errorContainer,
        onErrorContainer = target.onErrorContainer,
        outline = target.outline,
        outlineVariant = target.outlineVariant,
        scrim = target.scrim
    )
}

@Composable
fun IndicofflineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    val animatedColorScheme = animateColorScheme(colorScheme)

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = Typography,
        content = content
    )
}