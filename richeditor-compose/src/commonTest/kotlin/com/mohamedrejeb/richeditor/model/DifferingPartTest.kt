package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Unit coverage for [differingPart], the edit a whole text rewrite is cut down to. */
class DifferingPartTest {

    @Test
    fun `equal texts differ nowhere`() {
        assertNull(differingPart(original = "abc", rewritten = "abc", caret = 3))
    }

    @Test
    fun `an appended character is an insertion at the end`() {
        assertEquals(
            InputDelta(TextRange(3), "d"),
            differingPart(original = "abc", rewritten = "abcd", caret = 4),
        )
    }

    @Test
    fun `an inserted character is an insertion at its place`() {
        assertEquals(
            InputDelta(TextRange(1), "x"),
            differingPart(original = "abc", rewritten = "axbc", caret = 2),
        )
    }

    @Test
    fun `a removed character is a deletion of it`() {
        assertEquals(
            InputDelta(TextRange(1, 2), ""),
            differingPart(original = "abc", rewritten = "ac", caret = 1),
        )
    }

    @Test
    fun `a replaced word keeps what surrounds it`() {
        assertEquals(
            InputDelta(TextRange(4, 7), "new"),
            differingPart(original = "one old two", rewritten = "one new two", caret = 7),
        )
    }

    @Test
    fun `a character typed into a run of the same one goes in at the caret`() {
        assertEquals(
            InputDelta(TextRange(1), "a"),
            differingPart(original = "aaa", rewritten = "aaaa", caret = 2),
        )
        assertEquals(
            InputDelta(TextRange(0), "a"),
            differingPart(original = "aaa", rewritten = "aaaa", caret = 1),
        )
        assertEquals(
            InputDelta(TextRange(3), "a"),
            differingPart(original = "aaa", rewritten = "aaaa", caret = 4),
        )
    }

    @Test
    fun `a character deleted from a run of the same one comes out at the caret`() {
        assertEquals(
            InputDelta(TextRange(1, 2), ""),
            differingPart(original = "aaa", rewritten = "aa", caret = 1),
        )
    }

    @Test
    fun `a caret before every placement takes the earliest one`() {
        assertEquals(
            InputDelta(TextRange(0), "a"),
            differingPart(original = "aaa", rewritten = "aaaa", caret = 0),
        )
    }

    @Test
    fun `texts with nothing in common are replaced whole`() {
        assertEquals(
            InputDelta(TextRange(0, 3), "xyz"),
            differingPart(original = "abc", rewritten = "xyz", caret = 3),
        )
    }

    @Test
    fun `an emptied text is a deletion of everything`() {
        assertEquals(
            InputDelta(TextRange(0, 3), ""),
            differingPart(original = "abc", rewritten = "", caret = 0),
        )
    }
}
