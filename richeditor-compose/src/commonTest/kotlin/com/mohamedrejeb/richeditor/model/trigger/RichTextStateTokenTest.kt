package com.mohamedrejeb.richeditor.model.trigger

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class RichTextStateTokenTest {

    private fun newState() = RichTextState().apply {
        registerTrigger(Trigger(id = "mention", char = '@'))
        registerTrigger(Trigger(id = "hashtag", char = '#'))
    }

    @Test
    fun `registerTrigger rejects duplicate char`() {
        val state = RichTextState()
        state.registerTrigger(Trigger(id = "mention", char = '@'))
        assertFailsWith<IllegalArgumentException> {
            state.registerTrigger(Trigger(id = "other", char = '@'))
        }
    }

    @Test
    fun `registerTrigger replaces existing id`() {
        val state = RichTextState()
        state.registerTrigger(Trigger(id = "mention", char = '@'))
        state.registerTrigger(Trigger(id = "mention", char = '@', maxQueryLength = 10))
        assertEquals(1, state.triggers.size)
        assertEquals(10, state.triggers.first().maxQueryLength)
    }

    @Test
    fun `unregisterTrigger clears active query for that id`() {
        val state = newState()
        state.addTextAtIndex(0, "@moh")
        assertNotNull(state.activeTriggerQuery)
        state.unregisterTrigger("mention")
        assertNull(state.activeTriggerQuery)
    }

    @Test
    fun `typing trigger produces active query`() {
        val state = newState()
        state.addTextAtIndex(0, "@")
        val q = state.activeTriggerQuery
        assertNotNull(q)
        assertEquals("mention", q.triggerId)
        assertEquals("", q.query)
    }

    @Test
    fun `typing trigger then chars updates query`() {
        val state = newState()
        state.addTextAtIndex(0, "@moh")
        assertEquals("moh", state.activeTriggerQuery?.query)
    }

    @Test
    fun `typing stop char cancels query`() {
        val state = newState()
        state.addTextAtIndex(0, "@moh ")
        assertNull(state.activeTriggerQuery)
    }

    @Test
    fun `cancelActiveTrigger dismisses without changing text`() {
        val state = newState()
        state.addTextAtIndex(0, "@moh")
        assertNotNull(state.activeTriggerQuery)
        state.cancelActiveTrigger()
        assertNull(state.activeTriggerQuery)
        assertEquals("@moh", state.toText())
    }

    @Test
    fun `insertToken replaces query with token plus trailing space`() {
        val state = newState()
        state.addTextAtIndex(0, "Hi @moh")
        state.insertToken(triggerId = "mention", id = "u1", label = "@mohamed")

        assertEquals("Hi @mohamed ", state.toText())
        assertNull(state.activeTriggerQuery)
        // Caret after the trailing space
        assertEquals(TextRange("Hi @mohamed ".length), state.selection)
    }

    @Test
    fun `insertToken creates atomic Token span in tree`() {
        val state = newState()
        state.addTextAtIndex(0, "Hi @moh")
        state.insertToken(triggerId = "mention", id = "u1", label = "@mohamed")

        // The span covering "@mohamed" must be Token with atomic=true.
        val span = state.getSpanAtOffset(3) // position of '@'
        assertNotNull(span)
        val style = span.richSpanStyle
        assertTrue(style is RichSpanStyle.Token, "Expected Token span, got $style")
        assertEquals("mention", style.triggerId)
        assertEquals("u1", style.id)
        assertEquals("@mohamed", style.label)
        assertTrue(style.isAtomic)
    }

    @Test
    fun `insertToken without active query throws`() {
        val state = newState()
        state.addTextAtIndex(0, "Hi ")
        assertFailsWith<IllegalStateException> {
            state.insertToken("mention", "u1", "@mohamed")
        }
    }

    @Test
    fun `insertToken with wrong triggerId throws`() {
        val state = newState()
        state.addTextAtIndex(0, "@moh")
        assertFailsWith<IllegalStateException> {
            state.insertToken("hashtag", "t1", "@mohamed")
        }
    }

    @Test
    fun `insertToken with label missing trigger char throws`() {
        val state = newState()
        state.addTextAtIndex(0, "@moh")
        assertFailsWith<IllegalArgumentException> {
            state.insertToken("mention", "u1", "mohamed")
        }
    }

    @Test
    fun `hashtag trigger works alongside mention`() {
        val state = newState()
        state.addTextAtIndex(0, "See #rel")
        val q = state.activeTriggerQuery
        assertNotNull(q)
        assertEquals("hashtag", q.triggerId)
        assertEquals("rel", q.query)

        state.insertToken("hashtag", "release-1", "#release")
        assertEquals("See #release ", state.toText())
    }

    @Test
    fun `insertToken in middle of multi-paragraph document keeps trailing space in the right paragraph`() {
        // Regression for the bug where the trailing space ended up at the start of paragraph 2
        // when the token was inserted at the end of paragraph 1.
        val state = newState()
        // Two paragraphs; the @ query is at the end of paragraph 1 before the paragraph break.
        state.setText("Hi @moh\nSecond paragraph")
        // Move caret to the end of "@moh" so activeTriggerQuery is set up for commit.
        state.selection = TextRange("Hi @moh".length)
        assertNotNull(state.activeTriggerQuery, "expected active query at end of first paragraph")

        state.insertToken("mention", "u1", "@mohamed")

        // Raw text must have the token + space inside paragraph 1, then \n, then paragraph 2.
        val expected = "Hi @mohamed \nSecond paragraph"
        assertEquals(expected, state.toText())
    }

    @Test
    fun `token renders with trigger custom style not hardcoded link color`() {
        // Regression: previously Token.spanStyle returned linkColor regardless of trigger.
        // Now the render path consults the trigger's style(config) lambda.
        val state = RichTextState()
        val customColor = androidx.compose.ui.graphics.Color(0xFF8E24AA)
        state.registerTrigger(
            Trigger(
                id = "mention",
                char = '@',
                style = { androidx.compose.ui.text.SpanStyle(color = customColor) },
            )
        )
        state.addTextAtIndex(0, "@")
        state.insertToken("mention", "u1", "@alice")

        val span = state.getRichSpanByTextIndex(0)
        require(span != null)
        // The RichSpan's explicit spanStyle should carry the trigger's custom color.
        assertEquals(customColor, span.spanStyle.color)
    }
}

/** Helper to look up a span by raw-text offset in tests. Uses the module-internal lookup. */
private fun RichTextState.getSpanAtOffset(offset: Int): com.mohamedrejeb.richeditor.model.RichSpan? =
    getRichSpanByTextIndex(offset)
