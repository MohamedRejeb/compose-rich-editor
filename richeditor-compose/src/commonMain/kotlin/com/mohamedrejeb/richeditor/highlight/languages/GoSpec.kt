package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.BlockComment
import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

internal val GoSpec = LanguageSpec(
    keywords = setOf(
        "break", "case", "chan", "const", "continue", "default", "defer", "else", "fallthrough", "false", "for",
        "func", "go", "goto", "if", "import", "interface", "iota", "map", "nil", "package", "range", "return",
        "select", "struct", "switch", "true", "type", "var",
    ),
    lineComments = listOf("//"),
    blockComments = listOf(BlockComment("/*", "*/")),
    strings = listOf(
        StringRule("`", escape = null, multiline = true),
        StringRule("\""),
        StringRule("'"),
    ),
)
