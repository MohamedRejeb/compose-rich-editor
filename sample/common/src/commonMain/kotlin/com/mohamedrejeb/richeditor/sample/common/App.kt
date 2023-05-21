package com.mohamedrejeb.richeditor.sample.common

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import com.mohamedrejeb.richeditor.sample.common.home.HomeScreen
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme

@Composable
fun App() {
    ComposeRichEditorTheme {
        Navigator(HomeScreen)
    }
}