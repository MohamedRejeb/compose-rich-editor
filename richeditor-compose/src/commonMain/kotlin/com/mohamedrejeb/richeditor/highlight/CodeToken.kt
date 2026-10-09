package com.mohamedrejeb.richeditor.highlight

import androidx.compose.runtime.Immutable
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/** What a piece of code is, for colouring. */
@ExperimentalRichTextApi
public enum class CodeTokenKind { Keyword, String, Number, Comment, Annotation }

/** A coloured piece of code: the characters from [start] up to, not including, [end]. */
@ExperimentalRichTextApi
@Immutable
public class CodeToken(
    public val start: Int,
    public val end: Int,
    public val kind: CodeTokenKind,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is CodeToken && start == other.start && end == other.end && kind == other.kind)

    override fun hashCode(): Int = 31 * (31 * start + end) + kind.hashCode()

    override fun toString(): String = "CodeToken($start, $end, $kind)"
}
