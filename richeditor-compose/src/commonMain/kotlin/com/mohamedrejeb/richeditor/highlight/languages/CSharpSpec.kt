package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.BlockComment
import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

internal val CSharpSpec = LanguageSpec(
    keywords = setOf(
        "abstract", "as", "async", "await", "base", "bool", "break", "byte", "case", "catch", "char", "checked",
        "class", "const", "continue", "decimal", "default", "delegate", "do", "double", "else", "enum", "event",
        "explicit", "extern", "false", "finally", "fixed", "float", "for", "foreach", "get", "goto", "if",
        "implicit", "in", "init", "int", "interface", "internal", "is", "lock", "long", "namespace", "new", "null",
        "object", "operator", "out", "override", "params", "private", "protected", "public", "readonly", "record",
        "ref", "return", "sbyte", "sealed", "set", "short", "sizeof", "static", "string", "struct", "switch",
        "this", "throw", "true", "try", "typeof", "uint", "ulong", "unchecked", "unsafe", "ushort", "using", "var",
        "virtual", "void", "volatile", "while", "yield",
    ),
    lineComments = listOf("//"),
    blockComments = listOf(BlockComment("/*", "*/")),
    strings = listOf(
        StringRule("\"\"\"", escape = null, multiline = true),
        StringRule("\""),
        StringRule("'"),
    ),
)
