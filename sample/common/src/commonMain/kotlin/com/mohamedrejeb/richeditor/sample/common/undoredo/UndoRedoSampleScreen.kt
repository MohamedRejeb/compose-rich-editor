package com.mohamedrejeb.richeditor.sample.common.undoredo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UndoRedoSampleScreen(navigateBack: () -> Unit) {
    val state = rememberRichTextState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Undo / Redo") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Type, format, and use Ctrl/Cmd+Z / Ctrl/Cmd+Shift+Z or the buttons.",
                style = MaterialTheme.typography.bodyMedium,
            )

            // Prevent the editor from losing focus when clicking any toolbar button
            // (same workaround used by the other sample screens).
            val noFocusModifier = Modifier.focusProperties { canFocus = false }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = noFocusModifier,
                    onClick = { state.history.undo() },
                    enabled = state.history.canUndo,
                ) { Text("Undo") }
                Button(
                    modifier = noFocusModifier,
                    onClick = { state.history.redo() },
                    enabled = state.history.canRedo,
                ) { Text("Redo") }
                Button(
                    modifier = noFocusModifier,
                    onClick = { state.history.clear() },
                ) { Text("Clear history") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = noFocusModifier,
                    onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                ) { Text("Bold") }
                Button(
                    modifier = noFocusModifier,
                    onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                ) { Text("Italic") }
                Button(
                    modifier = noFocusModifier,
                    onClick = {
                        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    },
                ) { Text("Underline") }
                Button(
                    modifier = noFocusModifier,
                    onClick = { state.toggleOrderedList() },
                ) { Text("Ordered List") }
                Button(
                    modifier = noFocusModifier,
                    onClick = { state.toggleUnorderedList() },
                ) { Text("Bulleted List") }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedRichTextEditor(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            )
        }
    }
}
