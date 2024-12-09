package com.mohamedrejeb.richeditor.compose.spellcheck

import androidx.compose.foundation.ContextMenuData
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.LocalContextMenuData
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
public actual fun SpellCheckTextContextMenuProvider(
    modifier: Modifier,
    spellCheckMenuState: SpellCheckMenuState,
    content: @Composable () -> Unit
) {
    val currentContextMenuData = LocalContextMenuData.current
    val contextMenuData = remember(spellCheckMenuState.missSpelling.value) {
        ContextMenuData(
            items = {
                val wordSegment = spellCheckMenuState.missSpelling.value?.wordSegment
                    ?: return@ContextMenuData emptyList()

                val suggestionItems = spellCheckMenuState.spellCheckState.getSuggestions(wordSegment.text)
                if (suggestionItems.isNotEmpty()) {
                    suggestionItems.map { suggestion ->
                        ContextMenuItem(
                            suggestion.term,
                            onClick = {
                                spellCheckMenuState.performCorrection(
                                    wordSegment,
                                    suggestion.term
                                )
                            }
                        )
                    }
                } else {
                    emptyList()
                }
            },
            next = currentContextMenuData
        )
    }

    LocalContextMenuDataProvider(contextMenuData) {
        Box(modifier = modifier) {
            content()
        }
    }
}

/**
 * In order to be able to recompose the menu items, I need this overload.
 */
@Composable
private fun LocalContextMenuDataProvider(
    data: ContextMenuData,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContextMenuData provides data
    ) {
        content()
    }
}