package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.BlockComment
import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

internal val JavaSpec = LanguageSpec(
    keywords = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
        "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto",
        "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package",
        "private", "protected", "public", "record", "return", "sealed", "short", "static", "strictfp", "super",
        "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "var", "void", "volatile",
        "while", "yield",
    ),
    lineComments = listOf("//"),
    blockComments = listOf(BlockComment("/*", "*/")),
    strings = listOf(
        StringRule("\"\"\"", multiline = true),
        StringRule("\""),
        StringRule("'"),
    ),
    annotationPrefix = '@',
)
