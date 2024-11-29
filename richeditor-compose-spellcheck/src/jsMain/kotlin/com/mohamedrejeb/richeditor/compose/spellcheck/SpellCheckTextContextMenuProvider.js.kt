package com.mohamedrejeb.richeditor.compose.spellcheck

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mohamedrejeb.richeditor.compose.spellcheck.ui.SpellCheckDropdown

@Composable
public actual fun SpellCheckTextContextMenuProvider(
    modifier: Modifier,
    spellCheckMenuState: SpellCheckMenuState,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()

        spellCheckMenuState.missSpelling.value?.apply {
            SpellCheckDropdown(
                wordSegment,
                menuPosition,
                spellCheckMenuState.spellCheckState,
                dismiss = spellCheckMenuState::clearSpellCheck,
                correctSpelling = spellCheckMenuState::performCorrection
            )
        }
    }
}