package com.mohamedrejeb.richeditor.sample.common.slack

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import com.mohamedrejeb.richeditor.sample.common.htmleditor.HtmlEditorContent

object SlackDemoScreen: Screen {

    @Composable
    override fun Content() {
        SlackDemoContent()
    }

}