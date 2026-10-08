package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.BlockComment
import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

internal val SwiftSpec = LanguageSpec(
    keywords = setOf(
        "actor", "any", "as", "associatedtype", "async", "await", "break", "case", "catch", "class", "continue",
        "default", "defer", "deinit", "do", "else", "enum", "extension", "fallthrough", "false", "fileprivate",
        "final", "for", "func", "guard", "if", "import", "in", "indirect", "init", "inout", "internal", "is",
        "lazy", "let", "mutating", "nil", "nonisolated", "open", "operator", "override", "private", "protocol",
        "public", "repeat", "rethrows", "return", "self", "Self", "some", "static", "struct", "subscript",
        "super", "switch", "throw", "throws", "true", "try", "typealias", "var", "weak", "where", "while",
    ),
    lineComments = listOf("//"),
    blockComments = listOf(BlockComment("/*", "*/", nests = true)),
    strings = listOf(
        StringRule("\"\"\"", multiline = true),
        StringRule("\""),
    ),
    annotationPrefix = '@',
)
