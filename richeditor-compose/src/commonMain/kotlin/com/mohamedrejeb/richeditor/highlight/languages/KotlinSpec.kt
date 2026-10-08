package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.BlockComment
import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

internal val KotlinSpec = LanguageSpec(
    keywords = setOf(
        "abstract", "actual", "annotation", "as", "break", "by", "catch", "class", "companion", "const",
        "constructor", "continue", "crossinline", "data", "do", "else", "enum", "expect", "external", "false",
        "final", "finally", "for", "fun", "get", "if", "import", "in", "infix", "init", "inline", "inner",
        "interface", "internal", "is", "lateinit", "noinline", "null", "object", "open", "operator", "out",
        "override", "package", "private", "protected", "public", "reified", "return", "sealed", "set", "super",
        "suspend", "tailrec", "this", "throw", "true", "try", "typealias", "val", "value", "var", "vararg",
        "when", "where", "while",
    ),
    lineComments = listOf("//"),
    blockComments = listOf(BlockComment("/*", "*/", nests = true)),
    strings = listOf(
        StringRule("\"\"\"", escape = null, multiline = true),
        StringRule("\""),
        StringRule("'"),
    ),
    annotationPrefix = '@',
)
