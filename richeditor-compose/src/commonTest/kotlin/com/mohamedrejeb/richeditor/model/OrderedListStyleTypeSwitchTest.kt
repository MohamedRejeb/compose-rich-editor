package com.mohamedrejeb.richeditor.model

import com.mohamedrejeb.richeditor.paragraph.type.OrderedListStyleType
import kotlin.test.Test

/**
 * Regression for a crash when switching [RichTextConfig.orderedListStyleType] at
 * runtime while the editor already contains an ordered list whose prefix widths
 * change between the two styles (e.g. "10. " → "viii. "). The raw text still
 * held the old prefixes and `updateAnnotatedString` read children's text via
 * substring using the *new* prefix length, causing a StringIndexOutOfBounds.
 */
class OrderedListStyleTypeSwitchTest {

    private val sampleHtml = buildString {
        append("<ol>")
        (1..12).forEach { i -> append("<li>Item number $i</li>") }
        append("</ol>")
    }

    @Test
    fun switchingToLowerRomanDoesNotCrash() {
        val state = RichTextState()
        state.setHtml(sampleHtml)

        state.config.orderedListStyleType = OrderedListStyleType.LowerRoman
    }

    @Test
    fun switchingToUpperRomanDoesNotCrash() {
        val state = RichTextState()
        state.setHtml(sampleHtml)

        state.config.orderedListStyleType = OrderedListStyleType.UpperRoman
    }

    @Test
    fun switchingBackAndForthDoesNotCrash() {
        val state = RichTextState()
        state.setHtml(sampleHtml)

        state.config.orderedListStyleType = OrderedListStyleType.LowerRoman
        state.config.orderedListStyleType = OrderedListStyleType.UpperAlpha
        state.config.orderedListStyleType = OrderedListStyleType.Decimal
    }
}
