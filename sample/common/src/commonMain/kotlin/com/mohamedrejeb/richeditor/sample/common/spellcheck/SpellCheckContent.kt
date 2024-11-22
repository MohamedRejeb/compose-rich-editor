package com.mohamedrejeb.richeditor.sample.common.spellcheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.SuggestionItem
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.components.RichTextStyleRow
import com.mohamedrejeb.richeditor.sample.common.richeditor.SpellCheck
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.mohamedrejeb.richeditor.utils.WordSegment
import com.mohamedrejeb.richeditor.utils.findWordSegmentContainingRange
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
            val spellChecker by rememberSpellChecker()
            var lastTextHash = remember { -1 }
            val misspelledWords = remember { mutableListOf<WordSegment>() }

            fun runSpellCheck() {
                val sp = spellChecker ?: return
                println("Running spell check...")

                richTextState.apply {
                    // Remove all existing spell checks
                    getAllRichSpans()
                        .filter { it.first is SpellCheck }
                        .forEach { span ->
                            removeRichSpan(SpellCheck, span.second)
                        }

                    misspelledWords.clear()
                    getWords().mapNotNullTo(misspelledWords) { segment ->
                        val suggestions = sp.lookup(segment.text)
                        if (suggestions.spellingIsCorrect(segment.text)) {
                            null
                        } else {
                            println("Misspelling found! $segment.word")
                            segment
                        }
                    }

                    misspelledWords.fastForEach { wordSegment ->
                        addRichSpan(SpellCheck, wordSegment.range)
                    }
                }
            }

            // Run SpellCheck as soon as it is ready
            LaunchedEffect(spellChecker) {
                if (spellChecker != null) {
                    runSpellCheck()
                }
            }

            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                scope.launch {
                    // This is a very naive algorithm that just removes all spell check spans and
                    // reruns the entire spell check again
                    richTextState.textChanges.debounceUntilQuiescent(1.seconds).collect { updated ->
                        val newTextHash = updated.annotatedString.hashCode()
                        if (lastTextHash != newTextHash) {
                            runSpellCheck()
                            lastTextHash = newTextHash
                        }
                    }
                }
            }

            var spellCheckWord by remember { mutableStateOf<WordSegment?>(null) }
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

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    BasicRichTextEditor(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        state = richTextState,
                        textStyle = TextStyle.Default.copy(color = Color.White),
                        cursorBrush = SolidColor(Color.White),
                        onRichSpanClick = { span, range, click ->
                            if (span is SpellCheck) {
                                spellCheckWord = findWordSegmentContainingRange(
                                    misspelledWords,
                                    range
                                )
                                menuPosition = click
                                expanded = true
                            }
                        },
                    )

                    SpellCheckDropdown(
                        spellCheckWord,
                        menuPosition,
                        spellChecker,
                        dismiss = ::clearSpellCheck,
                        correctSpelling = { segment, correction ->
                            richTextState.replaceTextRange(segment.range, correction)
                            clearSpellCheck()
                        }
                    )
                }

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

@Composable
fun SpellCheckDropdown(
    word: WordSegment?,
    position: Offset,
    spellChecker: SpellChecker?,
    dismiss: () -> Unit,
    correctSpelling: (WordSegment, String) -> Unit
) {
    var suggestionItems by remember { mutableStateOf(emptyList<SuggestionItem>()) }

    LaunchedEffect(word, spellChecker) {
        word ?: return@LaunchedEffect
        spellChecker ?: return@LaunchedEffect

        val suggestions = spellChecker.lookupCompound(word.text)
        suggestionItems = if (word.text.isSpelledCorrectly(suggestions).not()) {
            // If things are misspelled, see if it just needs to be broken up
            val composition = spellChecker.wordBreakSegmentation(word.text)
            val segmentedWord = composition.segmentedString
            if (segmentedWord != null && suggestions.find { it.term.equals(segmentedWord, ignoreCase = true) } == null) {
                // Add the segmented suggest as first item if it didn't already exist
                listOf(SuggestionItem(segmentedWord, 1.0, 0.1)) + suggestions
            } else {
                suggestions
            }
        } else {
            emptyList()
        }
    }

    Box(modifier = Modifier.offset(x = position.x.dp, y = position.y.dp)) {
        DropdownMenu(
            expanded = word != null,
            onDismissRequest = dismiss,
        ) {
            word ?: return@DropdownMenu
            if (suggestionItems.isNotEmpty()) {
                suggestionItems.forEach { item ->
                    val correctedWord = applyCapitalizationStrategy(word.text, item.term)
                    DropdownMenuItem(
                        text = { Text(correctedWord) },
                        onClick = { correctSpelling(word, correctedWord) },
                    )
                }
            } else {
                DropdownMenuItem(
                    text = { Text("No suggestions") },
                    onClick = dismiss,
                )
            }
        }
    }
}

private fun <T> Flow<T>.debounceUntilQuiescent(duration: Duration): Flow<T> = channelFlow {
    var job: Job? = null
    collect { value ->
        job?.cancel()
        job = launch {
            delay(duration)
            send(value)
            job = null
        }
    }
}

private fun applyCapitalizationStrategy(source: String, target: String): String {
    fun isAllUpperCase(str: String): Boolean = str.all { it.isUpperCase() || !it.isLetter() }
    fun isAllLowerCase(str: String): Boolean = str.all { it.isLowerCase() || !it.isLetter() }
    fun isTitleCase(str: String): Boolean {
        val words = str.split(" ")
        return words.size > 1 && words.all {
            it.isNotEmpty() && it[0].isUpperCase() && it.substring(1)
                .all { char -> char.isLowerCase() }
        }
    }

    fun isCollapsedTitleCase(str: String): Boolean {
        return str.length > 2 && str[0].isUpperCase() && str[1].isLowerCase() && str.substring(2)
            .any { char -> char.isUpperCase() }
    }

    fun isInitialCaps(str: String): Boolean =
        str.isNotEmpty() && str[0].isUpperCase() && str.substring(1)
            .all { it.isLowerCase() || !it.isLetter() }

    fun makeTitleCase(target: String): String {
        return target.split(" ").joinToString(" ") { word ->
            if (word.isNotEmpty()) word[0].uppercase() + word.substring(1).lowercase() else word
        }
    }

    return when {
        isAllUpperCase(source) -> target.uppercase()
        isAllLowerCase(source) -> target.lowercase()
        isTitleCase(source) -> makeTitleCase(target)
        isCollapsedTitleCase(source) -> makeTitleCase(target)
        isInitialCaps(source) -> target.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        else -> target
    }
}
