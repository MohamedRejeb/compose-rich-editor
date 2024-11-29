package com.mohamedrejeb.richeditor.compose.spellcheck.utils

import com.darkrockstudios.symspellkt.common.SuggestionItem

public fun List<SuggestionItem>.spellingIsCorrect(word: String): Boolean =
    (size == 1 && get(0).term.equals(word, ignoreCase = true))

public fun String.isSpelledCorrectly(suggestions: List<SuggestionItem>): Boolean =
    (suggestions.size == 1 && suggestions[0].term.equals(this, ignoreCase = true))