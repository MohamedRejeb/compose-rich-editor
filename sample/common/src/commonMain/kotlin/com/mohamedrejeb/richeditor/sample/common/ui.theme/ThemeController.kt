package com.mohamedrejeb.richeditor.sample.common.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Exposes the sample app's light/dark theme state and toggle to anywhere in the composition,
 * so screen-level chrome like [SampleScaffold] can render a toggle action without the screen
 * signature knowing anything about theming.
 */
@Immutable
class ThemeController(
    val darkTheme: Boolean,
    val toggle: () -> Unit,
)

val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("ThemeController not provided. Wrap content in ComposeRichEditorTheme.")
}
