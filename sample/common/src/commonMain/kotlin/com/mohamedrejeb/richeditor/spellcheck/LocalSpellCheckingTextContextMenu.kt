package com.mohamedrejeb.richeditor.spellcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.mohamedrejeb.richeditor.sample.common.spellcheck.SpellCheckState
import com.mohamedrejeb.richeditor.utils.WordSegment

@Composable
expect fun SpellCheckTextContextMenuProvider(
    modifier: Modifier,
    spellCheckMenuState: SpellCheckMenuState,
    content: @Composable () -> Unit
)

data class SpellCheckMenuState(
    val spellCheckState: SpellCheckState,
) {
    val missSpelling: MutableState<MissSpelling?> = mutableStateOf(null)

    fun clearSpellCheck() {
        missSpelling.value = null
    }

    fun performCorrection(toReplace: WordSegment, correction: String) {
        spellCheckState.correctSpelling(toReplace, correction)
        clearSpellCheck()
    }

    data class MissSpelling(val wordSegment: WordSegment, val menuPosition: Offset)
}