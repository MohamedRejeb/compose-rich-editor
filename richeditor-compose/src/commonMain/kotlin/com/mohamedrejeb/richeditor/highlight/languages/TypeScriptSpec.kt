package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.BlockComment
import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

internal val TypeScriptSpec = LanguageSpec(
    keywords = setOf(
        "async", "await", "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete",
        "do", "else", "export", "extends", "false", "finally", "for", "from", "function", "get", "if", "import",
        "in", "instanceof", "let", "new", "null", "of", "return", "set", "static", "super", "switch", "this",
        "throw", "true", "try", "typeof", "undefined", "var", "void", "while", "with", "yield",
        "abstract", "any", "as", "boolean", "declare", "enum", "implements", "interface", "is", "keyof",
        "namespace", "never", "number", "object", "private", "protected", "public", "readonly", "string",
        "symbol", "type", "unknown",
    ),
    lineComments = listOf("//"),
    blockComments = listOf(BlockComment("/*", "*/")),
    strings = listOf(
        StringRule("`", multiline = true),
        StringRule("\""),
        StringRule("'"),
    ),
    annotationPrefix = '@',
)
