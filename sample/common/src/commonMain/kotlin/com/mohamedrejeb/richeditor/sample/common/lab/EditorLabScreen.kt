package com.mohamedrejeb.richeditor.sample.common.lab

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.RichTextStyleButton
import com.mohamedrejeb.richeditor.sample.common.components.SampleScaffold
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import kotlin.time.TimeSource

/**
 * A workbench for manual device checks: fixed documents, a live readout of the state the
 * checks ask about, a timestamped log of its changes, and a plain field to paste into.
 */
@Composable
fun EditorLabScreen(navigateBack: () -> Unit) {
    val state = rememberRichTextState()
    val pageScroll = rememberScrollState()
    val log = remember(state) { LabLog(start = TimeSource.Monotonic.markNow()) }
    var scenarioName by rememberSaveable { mutableStateOf(LabScenario.Paragraphs.name) }
    // Saved with the state, so a restored document is not replaced by its scenario again.
    var loadedScenarioName by rememberSaveable { mutableStateOf<String?>(null) }
    var readOnly by rememberSaveable { mutableStateOf(false) }
    val scenario = LabScenario.valueOf(scenarioName)

    LaunchedEffect(state, scenario) {
        if (loadedScenarioName != scenario.name) {
            state.setHtml(scenario.html)
            loadedScenarioName = scenario.name
        }
        log.clear()
        log.record(state = state, pageScroll = pageScroll)
    }

    SampleScaffold(
        title = "Editor lab",
        navigateBack = navigateBack,
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.ime)
                // The app's root clears the focus on any tap nothing consumed, which would
                // end the selection and the composition under observation.
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 20.dp)
                .verticalScroll(pageScroll),
        ) {
            Spacer(Modifier.height(4.dp))
            ScenarioPicker(
                selected = scenario,
                onSelect = { scenarioName = it.name },
            )
            LabToolbar(
                state = state,
                readOnly = readOnly,
                onReadOnlyChange = { readOnly = it },
            )
            LabEditor(state = state, scenario = scenario, readOnly = readOnly)
            StateReadout(state = state, pageScroll = pageScroll, readOnly = readOnly)
            LabLogPanel(log = log)
            PasteTarget()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScenarioPicker(
    selected: LabScenario,
    onSelect: (LabScenario) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LabScenario.entries.forEach { scenario ->
            FilterChip(
                selected = scenario == selected,
                onClick = { onSelect(scenario) },
                label = { Text(scenario.label) },
                modifier = Modifier.focusProperties { canFocus = false },
            )
        }
    }
}

@Composable
private fun LabToolbar(
    state: RichTextState,
    readOnly: Boolean,
    onReadOnlyChange: (Boolean) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RichTextStyleButton(
            onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
            isSelected = state.isBold(),
            icon = Icons.Outlined.FormatBold,
        )
        RichTextStyleButton(
            onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
            isSelected = state.currentSpanStyle.fontStyle == FontStyle.Italic,
            icon = Icons.Outlined.FormatItalic,
        )
        RichTextStyleButton(
            onClick = { state.toggleUnorderedList() },
            isSelected = state.isUnorderedList,
            icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
        )
        RichTextStyleButton(
            onClick = { state.toggleOrderedList() },
            isSelected = state.isOrderedList,
            icon = Icons.Outlined.FormatListNumbered,
        )
        FilterChip(
            selected = readOnly,
            onClick = { onReadOnlyChange(!readOnly) },
            label = { Text("Read only") },
            modifier = Modifier.focusProperties { canFocus = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabEditor(
    state: RichTextState,
    scenario: LabScenario,
    readOnly: Boolean,
) {
    OutlinedRichTextEditor(
        state = state,
        readOnly = readOnly,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                scenario.editorHeight
                    ?.let { Modifier.height(it) }
                    ?: Modifier
            ),
    )
}

@Composable
private fun StateReadout(
    state: RichTextState,
    pageScroll: ScrollState,
    readOnly: Boolean,
) {
    val lines = listOf(
        "read only      $readOnly",
        "selection      ${state.selection.describe()}",
        "composition    ${state.composition?.describe() ?: "none"}",
        "bold           ${state.isBold()}",
        "list           ${state.isList}",
        "editor scroll  ${state.scrollState.value} of ${state.scrollState.maxValue}",
        "page scroll    ${pageScroll.value} of ${pageScroll.maxValue}",
        "length         ${state.annotatedString.text.length}",
    )

    LabPanel(contentPadding = PaddingValues(16.dp)) {
        lines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

/**
 * A plain text field, so a cut or a copy can be checked without leaving the app.
 */
@Composable
private fun PasteTarget() {
    var pasted by rememberSaveable { mutableStateOf("") }

    OutlinedTextField(
        value = pasted,
        onValueChange = { pasted = it },
        label = { Text("Paste here to check the clipboard") },
        modifier = Modifier.fillMaxWidth(),
    )
}
