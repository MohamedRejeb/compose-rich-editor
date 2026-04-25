package com.mohamedrejeb.richeditor.sample.common.markdowneditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.SampleScaffold
import com.mohamedrejeb.richeditor.sample.common.ui.theme.SampleAccents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownEditorContent(
    navigateBack: () -> Unit
) {
    var isMarkdownToRichText by remember { mutableStateOf(false) }

    var markdown by remember {
        mutableStateOf(TextFieldValue(""))
    }
    val richTextState = rememberRichTextState()

    LaunchedEffect(Unit) {
        richTextState.config.linkColor = Color(0xFF1d9bd1)
        richTextState.config.linkTextDecoration = TextDecoration.None
        richTextState.config.codeSpanColor = Color(0xFFd7882d)
        richTextState.config.codeSpanBackgroundColor = Color.Transparent
        richTextState.config.codeSpanStrokeColor = Color(0xFF494b4d)
        richTextState.config.unorderedListIndent = 38
        richTextState.config.orderedListIndent = 40
    }

    LaunchedEffect(richTextState.annotatedString, isMarkdownToRichText) {
        if (!isMarkdownToRichText) {
            markdown = TextFieldValue(richTextState.toMarkdown())
        }
    }

    SampleScaffold(
        title = "Markdown editor",
        navigateBack = navigateBack,
        actions = {
            IconButton(onClick = { isMarkdownToRichText = !isMarkdownToRichText }) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "Swap direction",
                )
            }
        },
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            DirectionPicker(
                isMarkdownToRichText = isMarkdownToRichText,
                onDirectionChange = { isMarkdownToRichText = it },
            )

            Box(modifier = Modifier.weight(1f)) {
                if (isMarkdownToRichText) {
                    MarkdownToRichText(
                        markdown = markdown,
                        onMarkdownChange = {
                            markdown = it
                            richTextState.setMarkdown(it.text)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    RichTextToMarkdown(
                        richTextState = richTextState,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DirectionPicker(
    isMarkdownToRichText: Boolean,
    onDirectionChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Text(
            text = "Direction",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = SampleAccents.Amber,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !isMarkdownToRichText,
                onClick = { onDirectionChange(false) },
                label = { Text("Rich text → Markdown") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SampleAccents.Amber.copy(alpha = 0.18f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
            FilterChip(
                selected = isMarkdownToRichText,
                onClick = { onDirectionChange(true) },
                label = { Text("Markdown → Rich text") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SampleAccents.Amber.copy(alpha = 0.18f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}
