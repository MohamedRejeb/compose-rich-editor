package com.mohamedrejeb.richeditor.sample.common.listsconfig

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.paragraph.type.ListPrefixAlignment
import com.mohamedrejeb.richeditor.paragraph.type.OrderedListStyleType
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedListStyleType
import com.mohamedrejeb.richeditor.sample.common.components.SampleScaffold
import com.mohamedrejeb.richeditor.sample.common.ui.theme.SampleAccents
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

    SampleScaffold(
        title = "Lists configuration",
        navigateBack = navigateBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            HelpCard()

            ConfigCard(title = "Prefix alignment", accent = SampleAccents.Sky) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListPrefixAlignment.entries.forEach { option ->
                        FilterChip(
                            selected = alignment == option,
                            onClick = {
                                alignment = option
                                state.config.listPrefixAlignment = option
                            },
                            label = { Text(option.name) },
                            colors = chipColors(SampleAccents.Sky),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "End (default) aligns the dots of 1. and 10. vertically. Start gives every item a " +
                        "uniform left edge. Try indent = 0 to see the safety fallback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ConfigCard(
                title = "Ordered list indent — ${orderedIndent.toInt()} sp",
                accent = SampleAccents.Indigo,
            ) {
                Slider(
                    value = orderedIndent,
                    valueRange = 0f..80f,
                    steps = 79,
                    onValueChange = {
                        orderedIndent = it
                        state.config.orderedListIndent = it.toInt()
                    },
                )
            }

            ConfigCard(
                title = "Unordered list indent — ${unorderedIndent.toInt()} sp",
                accent = SampleAccents.Indigo,
            ) {
                Slider(
                    value = unorderedIndent,
                    valueRange = 0f..80f,
                    steps = 79,
                    onValueChange = {
                        unorderedIndent = it
                        state.config.unorderedListIndent = it.toInt()
                    },
                )
            }

            ConfigCard(title = "Ordered list style type", accent = SampleAccents.Coral) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetOrderedStyles.forEachIndexed { index, (label, styleType) ->
                        FilterChip(
                            selected = orderedStyleIndex == index,
                            onClick = {
                                orderedStyleIndex = index
                                state.config.orderedListStyleType = styleType
                            },
                            label = { Text(label) },
                            colors = chipColors(SampleAccents.Coral),
                        )
                    }
                }
            }

            ConfigCard(title = "Unordered list style type", accent = SampleAccents.Amber) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PresetUnorderedStyles.forEachIndexed { index, (label, styleType) ->
                        FilterChip(
                            selected = unorderedStyleIndex == index,
                            onClick = {
                                unorderedStyleIndex = index
                                state.config.unorderedListStyleType = styleType
                            },
                            label = { Text(label) },
                            colors = chipColors(SampleAccents.Amber),
                        )
                    }
                }
            }

            OutlinedRichTextEditor(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HelpCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Text(
            text = "Live tuning",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = SampleAccents.Sky,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Adjust each control and watch the editor below update without reloading. " +
                "Useful for picking sensible defaults for your own product.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ConfigCard(
    title: String,
    accent: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun chipColors(accent: Color) =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor = accent.copy(alpha = 0.18f),
        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
    )
