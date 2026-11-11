package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

internal val JsonSpec = LanguageSpec(
    keywords = setOf("true", "false", "null"),
    strings = listOf(StringRule("\"")),
)
