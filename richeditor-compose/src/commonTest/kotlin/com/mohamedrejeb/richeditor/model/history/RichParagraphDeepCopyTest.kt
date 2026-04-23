package com.mohamedrejeb.richeditor.model.history

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class RichParagraphDeepCopyTest {

    @Test
    fun deepCopyProducesDetachedParagraphWithEqualText() {
        val original = RichParagraph()
        val child = RichSpan(
            paragraph = original,
            text = "hello",
            spanStyle = SpanStyle(fontWeight = FontWeight.Bold),
        )
        original.children.add(child)

        val copy = original.deepCopy()

        assertNotSame(original, copy)
        assertEquals(1, copy.children.size)
        assertNotSame(child, copy.children[0])
        assertEquals("hello", copy.children[0].text)
        assertSame(copy, copy.children[0].paragraph)
    }

    @Test
    fun deepCopyPreservesNestedChildren() {
        val p = RichParagraph()
        val parent = RichSpan(paragraph = p, text = "a")
        val nested = RichSpan(paragraph = p, parent = parent, text = "b")
        parent.children.add(nested)
        p.children.add(parent)

        val copy = p.deepCopy()
        val copyParent = copy.children[0]
        val copyNested = copyParent.children[0]

        assertEquals("a", copyParent.text)
        assertEquals("b", copyNested.text)
        assertSame(copyParent, copyNested.parent)
        assertSame(copy, copyNested.paragraph)
    }

    @Test
    fun mutatingCopyDoesNotAffectOriginal() {
        val p = RichParagraph()
        val child = RichSpan(paragraph = p, text = "x")
        p.children.add(child)

        val copy = p.deepCopy()
        copy.children[0].text = "MUTATED"

        assertEquals("x", p.children[0].text)
        assertEquals("MUTATED", copy.children[0].text)
    }

    @Test
    fun deepCopyPreservesRichSpanStyleInstance() {
        val p = RichParagraph()
        val link = RichSpanStyle.Link(url = "https://example.com")
        p.children.add(
            RichSpan(paragraph = p, text = "site", richSpanStyle = link)
        )

        val copy = p.deepCopy()

        assertSame(link, copy.children[0].richSpanStyle)
        assertTrue(copy.children[0].richSpanStyle is RichSpanStyle.Link)
    }

    @Test
    fun deepCopyClonesParagraphType() {
        val p = RichParagraph(type = UnorderedList())
        val copy = p.deepCopy()

        assertTrue(copy.type is UnorderedList)
        assertNotSame(p.type, copy.type)
        // The cloned type's startRichSpan must point at the cloned paragraph, not the
        // original, so it renders against the correct tree.
        assertSame(copy, copy.type.startRichSpan.paragraph)
    }
}
