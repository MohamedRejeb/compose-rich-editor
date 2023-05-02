package com.mohamedrejeb.richeditor.sample.common.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Blue400,
//    secondary = Green400,
//    tertiary = Red400,

    background = Color.Black,
    surface = Color.Black,
//    surfaceVariant = Gray400,
    onPrimary = Color.White,
//    onSecondary = Color.White,
//    onBackground = Color.White,
    onSurface = Color.White,
//    onSurfaceVariant = LightGray400,
//    outline = LightGray400
)

private val LightColorScheme = lightColorScheme(
    primary = Blue400,
//    secondary = Green400,
//    tertiary = Red400,

    background = Color.White,
    surface = Color.White,
//    surfaceVariant = Gray400,
    onPrimary = Color.White,
//    onSecondary = Color.White,
    onBackground = Color.Black,
//    onSurface = Black,
//    onSurfaceVariant = DarkGray400,
//    outline = LightGray400
)

@Composable
internal fun ComposeRichEditorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}