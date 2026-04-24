package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HeadingStyleTest {

    @Test
    fun levelMaps() {
        assertEquals(0, HeadingStyle.Normal.level)
        assertEquals(1, HeadingStyle.H1.level)
        assertEquals(6, HeadingStyle.H6.level)
    }

    @Test
    fun fromLevelRoundTrip() {
        HeadingStyle.entries.forEach { style ->
            assertEquals(style, HeadingStyle.fromLevel(style.level))
        }
        assertEquals(HeadingStyle.Normal, HeadingStyle.fromLevel(0))
        assertEquals(HeadingStyle.Normal, HeadingStyle.fromLevel(7))
        assertEquals(HeadingStyle.Normal, HeadingStyle.fromLevel(-1))
    }

    @Test
    fun fromHtmlTagRoundTrip() {
        assertEquals(HeadingStyle.H1, HeadingStyle.fromHtmlTag("h1"))
        assertEquals(HeadingStyle.H6, HeadingStyle.fromHtmlTag("h6"))
        assertEquals(HeadingStyle.Normal, HeadingStyle.fromHtmlTag("p"))
        assertEquals(HeadingStyle.Normal, HeadingStyle.fromHtmlTag("div"))
    }

    @Test
    fun defaultsAreFrameworkAgnostic() {
        // Defaults are em-based + Bold so the core library does not depend on Material.
        assertEquals(2.em, HeadingStyle.H1.defaultSpanStyle.fontSize)
        assertEquals(FontWeight.Bold, HeadingStyle.H1.defaultSpanStyle.fontWeight)
        assertEquals(SpanStyle(), HeadingStyle.Normal.defaultSpanStyle)
    }

    @Test
    fun setHeadingStyleAppliesAndCanBeCleared() {
        val paragraph = RichParagraph(type = DefaultParagraph()).apply {
            children.add(RichSpan(text = "Title", paragraph = this))
        }

        paragraph.applyHeadingStyle(HeadingStyle.H2)

        assertEquals(HeadingStyle.H2, paragraph.headingStyle)
        assertEquals(FontWeight.Bold, paragraph.children.first().spanStyle.fontWeight)
        assertEquals(1.5.em, paragraph.children.first().spanStyle.fontSize)

        paragraph.applyHeadingStyle(HeadingStyle.Normal)

        assertEquals(HeadingStyle.Normal, paragraph.headingStyle)
        // Visual style should be wiped back to defaults.
        assertEquals(null, paragraph.children.first().spanStyle.fontWeight)
    }

    @Test
    fun setHeadingStyleIsIdempotentAcrossLevels() {
        val paragraph = RichParagraph(type = DefaultParagraph()).apply {
            children.add(RichSpan(text = "Hello", paragraph = this))
        }

        paragraph.applyHeadingStyle(HeadingStyle.H1)
        paragraph.applyHeadingStyle(HeadingStyle.H3)

        // After H1 -> H3 we should NOT be left with H1's larger font; only H3's should remain.
        val span = paragraph.children.first().spanStyle
        assertEquals(1.17.em, span.fontSize)
        assertEquals(FontWeight.Bold, span.fontWeight)
        assertNotEquals(2.em, span.fontSize)
    }

    @Test
    fun setHeadingStylePreservesUserAddedFormatting() {
        // Underline is not part of any heading default, so it must survive H1 application
        // and removal.
        val paragraph = RichParagraph(type = DefaultParagraph()).apply {
            val span = RichSpan(text = "Hello", paragraph = this)
            span.spanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
            children.add(span)
        }

        paragraph.applyHeadingStyle(HeadingStyle.H1)
        assertEquals(TextDecoration.Underline, paragraph.children.first().spanStyle.textDecoration)

        paragraph.applyHeadingStyle(HeadingStyle.Normal)
        assertEquals(TextDecoration.Underline, paragraph.children.first().spanStyle.textDecoration)
    }

    @Test
    fun stateSetHeadingStyleAppliesAcrossSelection() {
        val state = RichTextState()
        state.setText("Paragraph 1\nParagraph 2\nParagraph 3")
        state.selection = TextRange(0, state.annotatedString.text.length)

        state.setHeadingStyle(HeadingStyle.H2)

        assertEquals(3, state.richParagraphList.size)
        assertEquals(HeadingStyle.H2, state.richParagraphList[0].headingStyle)
        assertEquals(HeadingStyle.H2, state.richParagraphList[1].headingStyle)
        assertEquals(HeadingStyle.H2, state.richParagraphList[2].headingStyle)
    }

    @Test
    fun currentHeadingStyleReflectsCaretPosition() {
        val state = RichTextState()
        state.setText("First paragraph\nSecond paragraph")
        // Apply H1 to the first paragraph only.
        state.selection = TextRange(0, "First paragraph".length)
        state.setHeadingStyle(HeadingStyle.H1)

        // Place the caret in the first paragraph -> H1.
        state.selection = TextRange(2)
        assertEquals(HeadingStyle.H1, state.currentHeadingStyle)

        // Place the caret in the second paragraph -> Normal.
        state.selection = TextRange(state.annotatedString.text.length)
        assertEquals(HeadingStyle.Normal, state.currentHeadingStyle)
    }

    @Test
    fun setHeadingStyleParticipatesInUndoRedo() {
        val state = RichTextState()
        state.setText("Heading me")
        state.selection = TextRange(0, state.annotatedString.text.length)

        state.setHeadingStyle(HeadingStyle.H3)
        assertEquals(HeadingStyle.H3, state.richParagraphList.first().headingStyle)

        state.history.undo()
        assertEquals(HeadingStyle.Normal, state.richParagraphList.first().headingStyle)

        state.history.redo()
        assertEquals(HeadingStyle.H3, state.richParagraphList.first().headingStyle)
    }
}
