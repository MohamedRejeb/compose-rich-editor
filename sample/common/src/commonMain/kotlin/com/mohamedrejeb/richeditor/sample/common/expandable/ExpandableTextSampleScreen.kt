package com.mohamedrejeb.richeditor.sample.common.expandable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.SampleScaffold
import com.mohamedrejeb.richeditor.ui.material3.ExpandableRichText

private val longPlainHtml = """
<p>Composable functions are the building blocks of Jetpack Compose. They describe a piece of UI by
emitting nodes into the layout tree, and they recompose whenever the state they read changes. Once
you internalize that mental model, the rest of the framework - modifiers, slots, effects - falls
into place. The trick is to keep your composables small, side-effect free, and oriented around
state hoisting so the surrounding code can drive their behaviour.</p>
""".trimIndent()

private val shortHtml = """
<p>This paragraph fits comfortably inside three lines, so the affordance never appears.</p>
""".trimIndent()

private val longRichHtml = """
<p><b>Compose Rich Editor</b> ships <i>span-level styling</i>, hyperlinks like
<a href="https://github.com/MohamedRejeb/compose-rich-editor">the project repo</a>, and inline
content. The <b><i>ExpandableRichText</i></b> composable preserves all of those when truncating, but
it does <i>not</i> render code-span pills, list bullets, or paragraph backgrounds in v1 - so save it
for posts, comments, and bios rather than full markdown viewers.</p>
""".trimIndent()

@OptIn(ExperimentalRichTextApi::class)
@Composable
fun ExpandableTextSampleScreen(navigateBack: () -> Unit) {
    SampleScaffold(
        title = "Expandable",
        navigateBack = navigateBack,
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            ExpandableCard(
                title = "Long plain text",
                description = "Three-line collapsed view with an inline “… See more” toggle. Tap to expand, " +
                    "then tap “See less” to collapse.",
                html = longPlainHtml,
            )

            ExpandableCard(
                title = "Short text",
                description = "Content that fits within the three-line limit shows no toggle at all.",
                html = shortHtml,
            )

            ExpandableCard(
                title = "Span-styled content",
                description = "Bold, italic, and link styles carry through truncation. Code-span pills, " +
                    "list bullets, and paragraph backgrounds are not rendered in v1.",
                html = longRichHtml,
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalRichTextApi::class)
@Composable
private fun ExpandableCard(
    title: String,
    description: String,
    html: String,
) {
    val state = rememberRichTextState()
    var expanded by rememberSaveable(html) { mutableStateOf(false) }

    LaunchedEffect(html) {
        state.setHtml(html)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ExpandableRichText(
            state = state,
            expanded = expanded,
            onExpandedChange = { expanded = it },
            collapsedMaxLines = 3,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
