package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

internal val YamlSpec = LanguageSpec(
    keywords = setOf("true", "false", "null", "yes", "no", "on", "off"),
    lineComments = listOf("#"),
    strings = listOf(
        StringRule("\""),
        StringRule("'"),
    ),
    commentNeedsLeadingSpace = true,
)
