package com.mohamedrejeb.richeditor.paragraph

import androidx.compose.ui.text.font.FontFamily
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.history.deepCopy
import com.mohamedrejeb.richeditor.paragraph.type.CodeBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class CodeBlockParagraphTest {

    private fun text(value: String) = RichParagraph().also { it.children.add(RichSpan(paragraph = it, text = value)) }

    @Test
    fun `codeBlockOf makes one paragraph per line and marks the first`() {
        val block = codeBlockOf("kotlin", listOf("val a", "", "  val b"))
        assertEquals(listOf("val a", "", "  val b"), block.map { it.plainText() })
        assertEquals(listOf(true, false, false), block.map { (it.type as CodeBlock).isBlockStart })
        assertTrue(block.all { (it.type as CodeBlock).language == "kotlin" })
    }

    @Test
    fun `codeBlockOf with no lines makes one empty line`() {
        assertEquals(listOf(""), codeBlockOf(null, emptyList()).map { it.plainText() })
    }

    @Test
    fun `adjacent blocks are separate groups`() {
        val paragraphs = listOf(text("a")) + codeBlockOf("kotlin", listOf("b", "c")) + codeBlockOf(null, listOf("d")) + listOf(text("e"))
        assertEquals(listOf(1..2, 3..3), paragraphs.codeBlockGroups())
    }

    @Test
    fun `lines without a start line still form a group`() {
        val orphan = codeBlockOf("kotlin", listOf("a", "b", "c")).drop(1)
        assertEquals(listOf(0..1), orphan.codeBlockGroups())
    }

    @Test
    fun `the next paragraph type continues the block`() {
        val next = CodeBlock("kotlin", isBlockStart = true).getNextParagraphType() as CodeBlock
        assertEquals("kotlin", next.language)
        assertFalse(next.isBlockStart)
    }

    @Test
    fun `a deep copy keeps the language and the start mark`() {
        val copy = codeBlockOf("kotlin", listOf("a")).first().deepCopy()
        val type = copy.type as CodeBlock
        assertEquals("kotlin", type.language)
        assertTrue(type.isBlockStart)
        assertEquals("a", copy.plainText())
    }

    @Test
    fun `code lines are rendered in the code block font and other text is not`() {
        val state = RichTextState(initialRichParagraphList = listOf(text("p")) + codeBlockOf(null, listOf("code")))
        state.config.codeBlockFontFamily = FontFamily.Cursive
        val text = state.annotatedString
        assertEquals("p code", text.text)
        val fontAt = { offset: Int ->
            text.spanStyles.lastOrNull { it.start <= offset && offset < it.end && it.item.fontFamily != null }?.item?.fontFamily
        }
        assertNull(fontAt(0))
        assertEquals(FontFamily.Cursive, fontAt(2))
    }
}
