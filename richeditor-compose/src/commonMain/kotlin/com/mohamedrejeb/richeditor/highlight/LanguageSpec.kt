package com.mohamedrejeb.richeditor.highlight

internal class BlockComment(val open: String, val close: String, val nests: Boolean = false)

/** A string literal form. [escape] is null for raw strings. A string that is not [multiline] ends at the line end. */
internal class StringRule(
    val open: String,
    val close: String = open,
    val escape: Char? = '\\',
    val multiline: Boolean = false,
)

/**
 * What the scanner needs to know about a language. [strings] and [blockComments] are tried in
 * order, so a longer opener (triple quotes) must come before a shorter one that it starts with.
 */
internal class LanguageSpec(
    val keywords: Set<String>,
    val lineComments: List<String> = emptyList(),
    val blockComments: List<BlockComment> = emptyList(),
    val strings: List<StringRule> = emptyList(),
    val annotationPrefix: Char? = null,
    val ignoreKeywordCase: Boolean = false,
    /** True where a comment marker only counts at the start or after whitespace (shell, YAML). */
    val commentNeedsLeadingSpace: Boolean = false,
)
