package com.mohamedrejeb.richeditor.model

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.annotation.InternalRichTextApi
import com.mohamedrejeb.richeditor.model.trigger.Trigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalRichTextApi::class, InternalRichTextApi::class)
class TokenClickLookupTest {

    @Test
    fun getTokenByIndex_returnsToken_whenIndexInsideTokenSpan() {
        val state = RichTextState()
        state.registerTrigger(Trigger(id = "mention", char = '@'))
        state.setHtml("hi <span data-trigger=\"mention\" data-id=\"u1\">@bob</span>!")

        val token = state.getTokenByTextIndex(textIndex = 4)

        assertEquals("mention", token?.triggerId)
        assertEquals("u1", token?.id)
        assertEquals("@bob", token?.label)
    }

    @Test
    fun getTokenByIndex_returnsNull_whenIndexOutsideTokenSpan() {
        val state = RichTextState()
        state.registerTrigger(Trigger(id = "mention", char = '@'))
        state.setHtml("hi <span data-trigger=\"mention\" data-id=\"u1\">@bob</span>!")

        assertNull(state.getTokenByTextIndex(textIndex = 0))
        assertNull(state.getTokenByTextIndex(textIndex = 100))
    }

    @Test
    fun getTokenByIndex_returnsNull_forLinkSpan() {
        val state = RichTextState()
        state.addLink(text = "link", url = "https://example.com")
        assertNull(state.getTokenByTextIndex(textIndex = 1))
    }
}
