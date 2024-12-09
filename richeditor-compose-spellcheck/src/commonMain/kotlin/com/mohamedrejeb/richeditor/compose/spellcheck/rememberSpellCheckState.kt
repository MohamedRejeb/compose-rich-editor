package com.mohamedrejeb.richeditor.compose.spellcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.mohamedrejeb.richeditor.model.rememberRichTextState

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

    return state
}