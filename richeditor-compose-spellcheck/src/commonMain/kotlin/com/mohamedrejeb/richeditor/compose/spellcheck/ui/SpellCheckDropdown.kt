package com.mohamedrejeb.richeditor.compose.spellcheck.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.darkrockstudios.symspellkt.common.SuggestionItem
import com.mohamedrejeb.richeditor.compose.spellcheck.SpellCheckState
import com.mohamedrejeb.richeditor.utils.WordSegment
import kotlin.math.roundToInt

@Composable
internal fun SpellCheckDropdown(
    word: WordSegment?,
    position: Offset,
    spellCheckState: SpellCheckState,
    dismiss: () -> Unit,
    correctSpelling: (WordSegment, String) -> Unit
) {
    var suggestionItems by remember { mutableStateOf(emptyList<SuggestionItem>()) }

    LaunchedEffect(word, spellCheckState) {
        word ?: return@LaunchedEffect
        suggestionItems = spellCheckState.getSuggestions(word.text)
    }

    Box(modifier = Modifier.offset { IntOffset(position.x.roundToInt(), position.y.roundToInt())}) {
        DropdownMenu(
            expanded = word != null,
            onDismissRequest = dismiss,
        ) {
            word ?: return@DropdownMenu
            if (suggestionItems.isNotEmpty()) {
                suggestionItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.term) },
                        onClick = { correctSpelling(word, item.term) },
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
