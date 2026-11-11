package com.mohamedrejeb.richeditor.document

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.codeBlockGroups
import com.mohamedrejeb.richeditor.paragraph.codeBlockOf
import com.mohamedrejeb.richeditor.paragraph.plainText
import com.mohamedrejeb.richeditor.paragraph.type.CodeBlock
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalRichTextApi::class)
class CodeBlockDocumentTest {

    private val paragraphs = codeBlockOf("kotlin", listOf("val a", "", "  val b")) + codeBlockOf(null, listOf("c"))

    @Test
    fun `code lines encode as code block blocks`() {
        val document = RichTextDocumentEncoder.encode(paragraphs)
        assertEquals(
            listOf(
                RichTextBlock(text = "val a", type = RichTextBlockType.CodeBlock("kotlin", isBlockStart = true)),
                RichTextBlock(text = "", type = RichTextBlockType.CodeBlock("kotlin", isBlockStart = false)),
                RichTextBlock(text = "  val b", type = RichTextBlockType.CodeBlock("kotlin", isBlockStart = false)),
                RichTextBlock(text = "c", type = RichTextBlockType.CodeBlock(null, isBlockStart = true)),
            ),
            document.blocks,
        )
    }

    @Test
    fun `a document round trip keeps lines languages and block boundaries`() {
        val decoded = RichTextDocumentDecoder.decode(RichTextDocumentEncoder.encode(paragraphs))
        assertEquals(listOf("val a", "", "  val b", "c"), decoded.map { it.plainText() })
        assertEquals(listOf("kotlin", "kotlin", "kotlin", null), decoded.map { (it.type as CodeBlock).language })
        assertEquals(listOf(0..2, 3..3), decoded.codeBlockGroups())
    }
}
