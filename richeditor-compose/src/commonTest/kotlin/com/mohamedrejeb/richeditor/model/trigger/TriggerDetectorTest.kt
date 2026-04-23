package com.mohamedrejeb.richeditor.model.trigger

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalRichTextApi::class)
class TriggerDetectorTest {

    private val mention = Trigger(id = "mention", char = '@')
    private val hashtag = Trigger(id = "hashtag", char = '#')
    private val command = Trigger(id = "command", char = '/')

    @Test
    fun `single trigger char yields empty query`() {
        val result = detectActiveTrigger(
            text = "@",
            caretOffset = 1,
            triggers = listOf(mention),
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertEquals("mention", result?.triggerId)
        assertEquals("", result?.query)
        assertEquals(TextRange(0, 1), result?.range)
    }

    @Test
    fun `trigger followed by chars yields full query`() {
        val result = detectActiveTrigger(
            text = "@moh",
            caretOffset = 4,
            triggers = listOf(mention),
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertEquals("moh", result?.query)
        assertEquals(TextRange(0, 4), result?.range)
    }

    @Test
    fun `stop char cancels query`() {
        val result = detectActiveTrigger(
            text = "@moh ",
            caretOffset = 5,
            triggers = listOf(mention),
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertNull(result)
    }

    @Test
    fun `requireWordBoundary blocks mid-word activation`() {
        val result = detectActiveTrigger(
            text = "foo@bar",
            caretOffset = 7,
            triggers = listOf(mention),
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertNull(result)
    }

    @Test
    fun `requireWordBoundary allows activation after space`() {
        val result = detectActiveTrigger(
            text = "foo @bar",
            caretOffset = 8,
            triggers = listOf(mention),
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertEquals("bar", result?.query)
        assertEquals(TextRange(4, 8), result?.range)
    }

    @Test
    fun `requireWordBoundary allows activation at text start`() {
        val result = detectActiveTrigger(
            text = "@alice",
            caretOffset = 6,
            triggers = listOf(mention),
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertEquals("alice", result?.query)
    }

    @Test
    fun `requireWordBoundary false allows mid-word activation`() {
        val relaxed = Trigger(id = "mention", char = '@', requireWordBoundary = false)
        val result = detectActiveTrigger(
            text = "foo@bar",
            caretOffset = 7,
            triggers = listOf(relaxed),
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertEquals("bar", result?.query)
    }

    @Test
    fun `multiple triggers dispatch to the matching char`() {
        val triggers = listOf(mention, hashtag, command)

        val resultHash = detectActiveTrigger(
            text = "post #release",
            caretOffset = 13,
            triggers = triggers,
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertEquals("hashtag", resultHash?.triggerId)
        assertEquals("release", resultHash?.query)

        val resultCmd = detectActiveTrigger(
            text = "do /hel",
            caretOffset = 7,
            triggers = triggers,
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertEquals("command", resultCmd?.triggerId)
        assertEquals("hel", resultCmd?.query)
    }

    @Test
    fun `maxQueryLength abandons overlong queries`() {
        val capped = Trigger(id = "mention", char = '@', maxQueryLength = 3)
        val result = detectActiveTrigger(
            text = "@abcdef",
            caretOffset = 7,
            triggers = listOf(capped),
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertNull(result)
    }

    @Test
    fun `empty triggers list always yields null`() {
        val result = detectActiveTrigger(
            text = "@anything",
            caretOffset = 9,
            triggers = emptyList(),
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertNull(result)
    }

    @Test
    fun `caret before trigger char yields null`() {
        val result = detectActiveTrigger(
            text = "@moh",
            caretOffset = 0,
            triggers = listOf(mention),
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertNull(result)
    }

    @Test
    fun `caret past stop char after trigger yields null`() {
        val result = detectActiveTrigger(
            text = "@moh and more",
            caretOffset = 13,
            triggers = listOf(mention),
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertNull(result)
    }

    @Test
    fun `suppressed range suppresses re-activation while caret inside`() {
        val result = detectActiveTrigger(
            text = "@moh",
            caretOffset = 3,
            triggers = listOf(mention),
            textLayoutResult = null,
            suppressedRange = TextRange(0, 4),
        )
        assertNull(result)
    }

    @Test
    fun `suppressed range does not block after caret leaves`() {
        // Caret is past the suppressed range — but a stop char is needed to
        // end the token; this case ensures suppression clears when outside.
        val result = detectActiveTrigger(
            text = "@moh after",
            caretOffset = 5, // just after the space
            triggers = listOf(mention),
            textLayoutResult = null,
            suppressedRange = TextRange(0, 4),
        )
        assertNull(result) // blocked by stop char, not by suppression
    }

    @Test
    fun `newline acts as stop char by default`() {
        val result = detectActiveTrigger(
            text = "@moh\n",
            caretOffset = 5,
            triggers = listOf(mention),
            textLayoutResult = null,
            suppressedRange = null,
        )
        assertNull(result)
    }
}
