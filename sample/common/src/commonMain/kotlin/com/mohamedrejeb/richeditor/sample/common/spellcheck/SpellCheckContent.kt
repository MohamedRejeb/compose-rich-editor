package com.mohamedrejeb.richeditor.sample.common.spellcheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.richeditor.compose.spellcheck.SpellCheckState
import com.mohamedrejeb.richeditor.compose.spellcheck.ui.SpellCheckedRichTextEditor
import com.mohamedrejeb.richeditor.compose.spellcheck.rememberSpellCheckState
import com.mohamedrejeb.richeditor.sample.common.components.RichTextStyleRow
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme

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

            val spellChecker by rememberSampleSpellChecker()
            val spellCheckState: SpellCheckState = rememberSpellCheckState(spellChecker)

            LaunchedEffect(Unit) {
                spellCheckState.richTextState.setHtml(
                    """
                    <p><b>RichTextEditor</b> is a <i>composable</i> that allows you to edit <u>rich text</u> content.</p>
                    """.trimIndent()
                )
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
                    state = spellCheckState.richTextState,
                )

                SpellCheckedRichTextEditor(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                    textStyle = TextStyle.Default.copy(color = Color.White),
                    cursorBrush = SolidColor(Color.White),
                    spellCheckState = spellCheckState,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (spellChecker == null) {
                        CircularProgressIndicator(modifier = Modifier.size(25.dp))
                        Text(" Loading Dictionary...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Loaded",
                        )
                        Text(" Spell Check Ready!")
                    }
                }
            }
        }
    }
}