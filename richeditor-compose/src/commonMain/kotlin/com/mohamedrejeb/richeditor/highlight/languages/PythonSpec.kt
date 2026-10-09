package com.mohamedrejeb.richeditor.highlight.languages

import com.mohamedrejeb.richeditor.highlight.LanguageSpec
import com.mohamedrejeb.richeditor.highlight.StringRule

internal val PythonSpec = LanguageSpec(
    keywords = setOf(
        "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
        "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda",
        "match", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield",
    ),
    lineComments = listOf("#"),
    strings = listOf(
        StringRule("\"\"\"", multiline = true),
        StringRule("'''", multiline = true),
        StringRule("\""),
        StringRule("'"),
    ),
    annotationPrefix = '@',
)
