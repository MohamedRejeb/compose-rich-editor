package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.BlockComment
import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

internal val CppSpec = LanguageSpec(
    keywords = setOf(
        "auto", "break", "case", "char", "const", "continue", "default", "do", "double", "else", "enum", "extern",
        "float", "for", "goto", "if", "inline", "int", "long", "register", "restrict", "return", "short", "signed",
        "sizeof", "static", "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while", "NULL",
        "alignas", "alignof", "bool", "catch", "class", "concept", "const_cast", "constexpr", "co_await",
        "co_return", "co_yield", "decltype", "delete", "dynamic_cast", "explicit", "export", "false", "friend",
        "mutable", "namespace", "new", "noexcept", "nullptr", "operator", "override", "private", "protected",
        "public", "reinterpret_cast", "requires", "static_cast", "template", "this", "throw", "true", "try",
        "typeid", "typename", "using", "virtual",
    ),
    lineComments = listOf("//"),
    blockComments = listOf(BlockComment("/*", "*/")),
    strings = listOf(
        StringRule("\""),
        StringRule("'"),
    ),
    annotationPrefix = '#',
)
