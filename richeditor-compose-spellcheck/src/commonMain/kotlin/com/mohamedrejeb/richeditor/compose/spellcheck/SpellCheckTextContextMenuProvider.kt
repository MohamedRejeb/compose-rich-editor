package com.mohamedrejeb.richeditor.compose.spellcheck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.mohamedrejeb.richeditor.utils.WordSegment

@Composable
public expect fun SpellCheckTextContextMenuProvider(
    modifier: Modifier,
    spellCheckMenuState: SpellCheckMenuState,
    content: @Composable () -> Unit
)

public data class SpellCheckMenuState(
    val spellCheckState: SpellCheckState,
) {
    val missSpelling: MutableState<MissSpelling?> = mutableStateOf(null)

    public fun clearSpellCheck() {
        missSpelling.value = null
    }

    public fun performCorrection(toReplace: WordSegment, correction: String) {
        spellCheckState.correctSpelling(toReplace, correction)
        clearSpellCheck()
    }

    public data class MissSpelling(val wordSegment: WordSegment, val menuPosition: Offset)
}