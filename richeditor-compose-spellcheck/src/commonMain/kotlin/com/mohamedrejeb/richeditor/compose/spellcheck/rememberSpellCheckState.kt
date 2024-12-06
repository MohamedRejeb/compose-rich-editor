package com.mohamedrejeb.richeditor.compose.spellcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.mohamedrejeb.richeditor.compose.spellcheck.utils.debounceUntilQuiescent
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
public fun rememberSpellCheckState(spellChecker: SpellChecker?): SpellCheckState {
    val richTextState = rememberRichTextState()
    val state = remember { SpellCheckState(richTextState, spellChecker) }

    // Run SpellCheck as soon as it is ready
    LaunchedEffect(spellChecker) {
        if (spellChecker != null) {
            state.spellChecker = spellChecker
            state.runSpellCheck()
        }
    }

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            richTextState.textChanges.debounceUntilQuiescent(1.seconds).collect { richTextState ->
                state.onTextChange(richTextState)
            }
        }
    }

    return state
}