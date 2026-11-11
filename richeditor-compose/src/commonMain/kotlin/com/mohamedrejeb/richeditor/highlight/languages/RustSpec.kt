package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.BlockComment
import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

internal val RustSpec = LanguageSpec(
    keywords = setOf(
        "as", "async", "await", "bool", "break", "char", "const", "continue", "crate", "dyn", "else", "enum",
        "extern", "f32", "f64", "false", "fn", "for", "i8", "i16", "i32", "i64", "i128", "if", "impl", "in",
        "isize", "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return", "self", "Self", "static",
        "str", "struct", "super", "trait", "true", "type", "u8", "u16", "u32", "u64", "u128", "unsafe", "use",
        "usize", "where", "while",
    ),
    lineComments = listOf("//"),
    blockComments = listOf(BlockComment("/*", "*/", nests = true)),
    // A single quote also starts a lifetime, so it is not a string opener.
    strings = listOf(StringRule("\"", multiline = true)),
)
