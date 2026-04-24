package com.mohamedrejeb.richeditor.sample.common.listsconfig

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.paragraph.type.ListPrefixAlignment
import com.mohamedrejeb.richeditor.paragraph.type.OrderedListStyleType
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedListStyleType
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor

private val PresetOrderedStyles: List<Pair<String, OrderedListStyleType>> = listOf(
    "Decimal (1, 2, ...)" to OrderedListStyleType.Decimal,
    "Lower alpha (a, b, ...)" to OrderedListStyleType.LowerAlpha,
    "Upper alpha (A, B, ...)" to OrderedListStyleType.UpperAlpha,
    "Lower roman (i, ii, ...)" to OrderedListStyleType.LowerRoman,
    "Upper roman (I, II, ...)" to OrderedListStyleType.UpperRoman,
    "Multiple (default)" to OrderedListStyleType.Multiple(
        OrderedListStyleType.Decimal,
        OrderedListStyleType.LowerRoman,
        OrderedListStyleType.LowerAlpha,
    ),
)

private val PresetUnorderedStyles: List<Pair<String, UnorderedListStyleType>> = listOf(
    "Default (• ◦ ▪)" to UnorderedListStyleType.from("•", "◦", "▪"),
    "Dashes (− ∙ ·)" to UnorderedListStyleType.from("−", "∙", "·"),
    "Arrows (▸ ▹ ›)" to UnorderedListStyleType.from("▸", "▹", "›"),
    "Stars (★ ☆ ✦)" to UnorderedListStyleType.from("★", "☆", "✦"),
)

private const val SampleHtml: String = """
<p>Try the toggles above — watch how the pre-filled lists react.</p>
<ol>
    <li>First item</li>
    <li>Second item</li>
    <li>Third item</li>
    <li>Fourth item</li>
    <li>Fifth item</li>
    <li>Sixth item</li>
    <li>Seventh item</li>
    <li>Eighth item</li>
    <li>Ninth item</li>
    <li>Tenth item — watch the jump from single to double digits</li>
    <li>Eleventh item</li>
    <li>Twelfth item</li>
</ol>
<ul>
    <li>Top-level bullet
        <ul>
            <li>Second-level bullet
                <ul>
                    <li>Third-level bullet</li>
                    <li>Another third-level bullet</li>
                </ul>
            </li>
            <li>Another second-level bullet</li>
        </ul>
    </li>
    <li>Another top-level bullet</li>
</ul>
"""

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalRichTextApi::class)
@Composable
fun ListsConfigSampleScreen(navigateBack: () -> Unit) {
    val state = rememberRichTextState()

    LaunchedEffect(Unit) {
        state.setHtml(SampleHtml)
    }

    var orderedIndent by remember { mutableStateOf(state.config.orderedListIndent.toFloat()) }
    var unorderedIndent by remember { mutableStateOf(state.config.unorderedListIndent.toFloat()) }
    var alignment by remember { mutableStateOf(state.config.listPrefixAlignment) }
    var orderedStyleIndex by remember { mutableStateOf(PresetOrderedStyles.lastIndex) }
    var unorderedStyleIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lists configuration") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize(),
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
                text = "Adjust the controls to see how each config affects ordered and unordered lists.",
                style = MaterialTheme.typography.bodyMedium,
            )

            SectionLabel("Prefix alignment")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ListPrefixAlignment.entries.forEach { option ->
                    FilterChip(
                        selected = alignment == option,
                        onClick = {
                            alignment = option
                            state.config.listPrefixAlignment = option
                        },
                        label = { Text(option.name) },
                    )
                }
            }
            Text(
                text = "End (default) aligns the dots of 1. and 10. vertically. Start gives every item a " +
                        "uniform left edge. Try indent = 0 to see the safety fallback.",
                style = MaterialTheme.typography.bodySmall,
            )

            SectionLabel("Ordered list indent — ${orderedIndent.toInt()} sp")
            Slider(
                value = orderedIndent,
                valueRange = 0f..80f,
                steps = 79,
                onValueChange = {
                    orderedIndent = it
                    state.config.orderedListIndent = it.toInt()
                },
            )

            SectionLabel("Unordered list indent — ${unorderedIndent.toInt()} sp")
            Slider(
                value = unorderedIndent,
                valueRange = 0f..80f,
                steps = 79,
                onValueChange = {
                    unorderedIndent = it
                    state.config.unorderedListIndent = it.toInt()
                },
            )

            SectionLabel("Ordered list style type")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetOrderedStyles.forEachIndexed { index, (label, styleType) ->
                    FilterChip(
                        selected = orderedStyleIndex == index,
                        onClick = {
                            orderedStyleIndex = index
                            state.config.orderedListStyleType = styleType
                        },
                        label = { Text(label) },
                    )
                }
            }

            SectionLabel("Unordered list style type")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PresetUnorderedStyles.forEachIndexed { index, (label, styleType) ->
                    FilterChip(
                        selected = unorderedStyleIndex == index,
                        onClick = {
                            unorderedStyleIndex = index
                            state.config.unorderedListStyleType = styleType
                        },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))

            OutlinedRichTextEditor(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
    )
}
