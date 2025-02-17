package com.mohamedrejeb.richeditor.sample.common

import androidx.compose.runtime.*
import com.mohamedrejeb.richeditor.sample.common.navigation.NavGraph
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme

@Composable
fun App() {
    ComposeRichEditorTheme {
        NavGraph()
    }
}