package com.mohamedrejeb.richeditor.sample.common.headings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.CopyButton
import com.mohamedrejeb.richeditor.sample.common.components.SampleScaffold
import com.mohamedrejeb.richeditor.sample.common.ui.theme.SampleAccents
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor

private val seedHtml = """
<h1>Compose Rich Editor</h1>
<p>Apply heading levels (H1-H6) to whole paragraphs. Place the caret in any line and pick a level
from the toolbar - the chip highlights the active level so you always see what's applied.</p>
<h2>Why first-class headings?</h2>
<p>Heading is a paragraph-level field on <b>RichParagraph</b>, so identity survives theme tweaks,
font customisation and round-trips through HTML or Markdown.</p>
<h3>Try it out</h3>
<ul>
    <li>Click into a paragraph, pick H1-H6 to promote it.</li>
    <li>Click <i>Normal</i> to clear the heading and return to body text.</li>
    <li>Watch the live HTML and Markdown panels below.</li>
</ul>
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HeadingsSampleScreen(navigateBack: () -> Unit) {
    val state = rememberRichTextState()

    LaunchedEffect(Unit) {
        state.setHtml(seedHtml)
    }

    val noFocus = Modifier.focusProperties { canFocus = false }

    SampleScaffold(
        title = "Headings",
        navigateBack = navigateBack,
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(4.dp))

            IntroCard()

            HeadingPicker(
                current = state.currentHeadingStyle,
                onPick = { state.setHeadingStyle(it) },
                noFocus = noFocus,
            )

            OutlinedRichTextEditor(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp),
            )

            ExportCard(
                title = "HTML output",
                accent = SampleAccents.Coral,
                content = remember(state.annotatedString) { state.toHtml() },
            )

            ExportCard(
                title = "Markdown output",
                accent = SampleAccents.Amber,
                content = remember(state.annotatedString) { state.toMarkdown() },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IntroCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Text(
            text = "How it works",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = SampleAccents.Indigo,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "RichTextState exposes setHeadingStyle and currentHeadingStyle so toolbars can " +
                "drive the active level. Heading is stored as a first-class field on each paragraph, " +
                "not detected by font fingerprinting, so styles round-trip cleanly to HTML and Markdown.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HeadingPicker(
    current: HeadingStyle,
    onPick: (HeadingStyle) -> Unit,
    noFocus: Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Text(
            text = "Heading level",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SampleAccents.Indigo,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HeadingStyle.entries.forEach { style ->
                FilterChip(
                    selected = current == style,
                    onClick = { onPick(style) },
                    label = {
                        Text(if (style == HeadingStyle.Normal) "Normal" else "H${style.level}")
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SampleAccents.Indigo.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = noFocus,
                )
            }
        }
    }
}

@Composable
private fun ExportCard(
    title: String,
    accent: Color,
    content: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier = Modifier.weight(1f),
            )
            CopyButton(content = content)
        }
        Spacer(Modifier.height(6.dp))
        val scroll = rememberScrollState()
        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .verticalScroll(scroll),
            ) {
                Text(
                    text = content,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
