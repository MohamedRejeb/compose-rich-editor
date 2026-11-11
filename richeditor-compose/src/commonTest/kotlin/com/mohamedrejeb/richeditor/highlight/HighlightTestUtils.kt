package com.mohamedrejeb.richeditor.highlight

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

@OptIn(ExperimentalRichTextApi::class)
internal fun tokenTexts(code: String, language: CodeLanguage): List<Pair<String, CodeTokenKind>> =
    CodeHighlighter.tokenize(code, language).map { code.substring(it.start, it.end) to it.kind }
