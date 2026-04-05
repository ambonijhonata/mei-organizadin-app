package com.tcc.androidnative.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    error = RedError,
    background = SurfaceLight,
    surface = SurfaceLight
)

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    error = RedError
)

@Composable
fun AndroidNativeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

