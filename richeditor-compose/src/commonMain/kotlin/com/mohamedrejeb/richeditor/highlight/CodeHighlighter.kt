package com.mohamedrejeb.richeditor.highlight

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/** Finds the parts of a piece of code that are drawn in a colour of their own. */
@ExperimentalRichTextApi
public object CodeHighlighter {

    /**
     * The tokens of [code], in order, inside the text and never overlapping. It never throws:
     * a string or comment that is not closed runs to the end of its line or of the code.
     */
    public fun tokenize(code: String, language: CodeLanguage): List<CodeToken> =
        CodeScanner.scan(code, language.spec)
}
