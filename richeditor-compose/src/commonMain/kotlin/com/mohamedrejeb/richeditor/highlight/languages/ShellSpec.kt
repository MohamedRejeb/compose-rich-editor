package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

internal val ShellSpec = LanguageSpec(
    keywords = setOf(
        "case", "do", "done", "elif", "else", "esac", "export", "fi", "for", "function", "if", "in", "local",
        "readonly", "return", "select", "then", "until", "while",
    ),
    lineComments = listOf("#"),
    strings = listOf(
        StringRule("\"", multiline = true),
        StringRule("'", escape = null, multiline = true),
    ),
    commentNeedsLeadingSpace = true,
)
