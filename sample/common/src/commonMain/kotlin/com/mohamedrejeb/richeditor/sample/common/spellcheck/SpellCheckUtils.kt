package com.mohamedrejeb.richeditor.sample.common.spellcheck

import com.darkrockstudios.symspellkt.common.SuggestionItem

fun List<SuggestionItem>.spellingIsCorrect(word: String): Boolean =
    (size == 1 && get(0).term == word)

fun String.isSpelledCorrectly(suggestions: List<SuggestionItem>): Boolean =
    (suggestions.size == 1 && suggestions[0].term == this)