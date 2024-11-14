package com.mohamedrejeb.richeditor.sample.common.spellcheck

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.RichTextStyleRow
import com.mohamedrejeb.richeditor.sample.common.richeditor.SpellCheck
import com.mohamedrejeb.richeditor.sample.common.richeditor.SpellCheckDropdown
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellCheckContent() {
    val navigator = LocalNavigator.currentOrThrow

    ComposeRichEditorTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Compose Spell Check") },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValue ->

            val richTextState = rememberRichTextState()

            var spellCheckWord by remember { mutableStateOf<RichSpan?>(null) }
            var expanded by remember { mutableStateOf(false) }
            var menuPosition by remember { mutableStateOf(Offset.Zero) }

            LaunchedEffect(Unit) {
                richTextState.setHtml(
                    """
            <p><b>RichTextEditor</b> is a <i>composable</i> that allows you to edit <u>rich text</u> content.</p>
            """.trimIndent()
                )
            }

            fun clearSpellCheck() {
                spellCheckWord = null
                expanded = false
                menuPosition = Offset.Zero
            }

            Column(
                modifier = Modifier
                    .padding(paddingValue)
                    .windowInsetsPadding(WindowInsets.ime)
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                RichTextStyleRow(
                    modifier = Modifier.fillMaxWidth(),
                    state = richTextState,
                )

                BasicRichTextEditor(
                    modifier = Modifier.fillMaxWidth(),
                    state = richTextState,
                    textStyle = TextStyle.Default.copy(color = Color.White),
                    cursorBrush = SolidColor(Color.White),
                    onRichSpanClick = { span, click ->
                        if (span.richSpanStyle is SpellCheck) {
                            println("On Click: $span")
                            spellCheckWord = span
                            menuPosition = click
                            expanded = true
                        }
                    },
                )

                SpellCheckDropdown(
                    spellCheckWord,
                    menuPosition,
                    dismiss = ::clearSpellCheck,
                    correctSpelling = { span, correction ->
                        println("Correcting spelling to: $correction")
                        richTextState.replaceTextRange(span.textRange, correction)
                        clearSpellCheck()
                    }
                )
            }
        }
    }
}