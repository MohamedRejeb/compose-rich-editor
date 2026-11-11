package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.BlockComment
import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

internal val DartSpec = LanguageSpec(
    keywords = setOf(
        "abstract", "as", "assert", "async", "await", "base", "break", "case", "catch", "class", "const",
        "continue", "covariant", "default", "deferred", "do", "dynamic", "else", "enum", "export", "extends",
        "extension", "external", "factory", "false", "final", "finally", "for", "get", "if", "implements",
        "import", "in", "interface", "is", "late", "library", "mixin", "new", "null", "on", "operator", "part",
        "required", "rethrow", "return", "sealed", "set", "static", "super", "switch", "this", "throw", "true",
        "try", "typedef", "var", "void", "when", "while", "with", "yield",
    ),
    lineComments = listOf("//"),
    blockComments = listOf(BlockComment("/*", "*/")),
    strings = listOf(
        StringRule("\"\"\"", multiline = true),
        StringRule("'''", multiline = true),
        StringRule("\""),
        StringRule("'"),
    ),
    annotationPrefix = '@',
)
