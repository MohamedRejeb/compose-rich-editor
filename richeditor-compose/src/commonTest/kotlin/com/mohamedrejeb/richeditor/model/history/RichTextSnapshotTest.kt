package com.mohamedrejeb.richeditor.model.history

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

@OptIn(ExperimentalRichTextApi::class)
class RichTextSnapshotTest {

    @Test
    fun captureDeepCopiesTheParagraphList() {
        val p = RichParagraph()
        p.children.add(RichSpan(paragraph = p, text = "hi"))
        val snap = RichTextSnapshot.capture(
            paragraphs = listOf(p),
            selection = TextRange(0),
            composition = null,
            toAddSpanStyle = SpanStyle(),
            toAddRichSpanStyle = RichSpanStyle.Default,
            timestampMs = 1_000L,
        )
        assertNotSame(p, snap.paragraphs[0])
        assertEquals("hi", snap.paragraphs[0].children[0].text)

        // Mutate the live tree; snapshot must remain intact.
        p.children[0].text = "CHANGED"
        assertEquals("hi", snap.paragraphs[0].children[0].text)
    }

    @Test
    fun selectionAndTimestampArePreserved() {
        val snap = RichTextSnapshot.capture(
            paragraphs = listOf(RichParagraph()),
            selection = TextRange(2, 5),
            composition = TextRange(3, 4),
            toAddSpanStyle = SpanStyle(),
            toAddRichSpanStyle = RichSpanStyle.Default,
            timestampMs = 12_345L,
        )
        assertEquals(TextRange(2, 5), snap.selection)
        assertEquals(TextRange(3, 4), snap.composition)
        assertEquals(12_345L, snap.timestampMs)
    }
}
