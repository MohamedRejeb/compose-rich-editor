package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class RichTextStateHtmlParserDecodeTest {

    @Test
    fun testParsingSimpleHtmlWithBrBackAndForth() {
        val html = "<br><p>Hello World&excl;</p>"

        val richTextState = RichTextStateHtmlParser.encode(html)

        assertEquals(2, richTextState.richParagraphList.size)
        assertTrue(richTextState.richParagraphList[0].isBlank())
        assertEquals(1, richTextState.richParagraphList[1].children.size)

        val parsedHtml = RichTextStateHtmlParser.decode(richTextState)

        assertEquals(html, parsedHtml)
    }

    @Test
    fun testDecodeSingleLineBreak() {
        val expectedHtml = "<p>First</p><br><p>Second</p>"

        val richTextState = RichTextState(
            listOf(
                RichParagraph(
                    type = DefaultParagraph()
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = DefaultParagraph()
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = DefaultParagraph()
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Second",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        assertEquals(expectedHtml, richTextState.toHtml())
    }

    @Test
    fun testDecodeMultipleLineBreaks() {
        val expectedHtml = "<br><p>First</p><br><br><p>Second</p><br>"

        val richTextState = RichTextState(
            listOf(
                RichParagraph(
                    type = DefaultParagraph()
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = DefaultParagraph()
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = DefaultParagraph()
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = DefaultParagraph()
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = DefaultParagraph()
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Second",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = DefaultParagraph()
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "",
                            paragraph = it,
                        )
                    )
                },
            )
        )

        assertEquals(expectedHtml, richTextState.toHtml())
    }

    @Test
    fun testDecodeOrderedList() {
        val expectedHtml = "<ol><li>First</li><li>Second</li></ol>"

        val richTextState = RichTextState(
            listOf(
                RichParagraph(
                    key = 0,
                    type = OrderedList(1)
                ).also {
                    it.children.add(
                        RichSpan(
                            key = 0,
                            text = "First",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    key = 1,
                    type = OrderedList(2)
                ).also {
                    it.children.add(
                        RichSpan(
                            key = 0,
                            text = "Second",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        assertEquals(expectedHtml, richTextState.toHtml())
    }

    @Test
    fun testDecodeUnorderedList() {
        val expectedHtml = "<ul><li>First</li><li>Second</li></ul>"

        val richTextState = RichTextState(
            listOf(
                RichParagraph(
                    key = 0,
                    type = UnorderedList()
                ).also {
                    it.children.add(
                        RichSpan(
                            key = 0,
                            text = "First",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    key = 1,
                    type = UnorderedList()
                ).also {
                    it.children.add(
                        RichSpan(
                            key = 0,
                            text = "Second",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        assertEquals(expectedHtml, richTextState.toHtml())
    }

    @Test
    fun testDecodeOrderedListAndUnorderedList() {
        val expectedHtml = "<ol><li>First</li><li>Second</li></ol><ul><li>Third</li><li>Fourth</li></ul>"

        val richTextState = RichTextState(
            listOf(
                RichParagraph(
                    key = 0,
                    type = OrderedList(1)
                ).also {
                    it.children.add(
                        RichSpan(
                            key = 0,
                            text = "First",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    key = 1,
                    type = OrderedList(2)
                ).also {
                    it.children.add(
                        RichSpan(
                            key = 0,
                            text = "Second",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    key = 2,
                    type = UnorderedList()
                ).also {
                    it.children.add(
                        RichSpan(
                            key = 0,
                            text = "Third",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    key = 3,
                    type = UnorderedList()
                ).also {
                    it.children.add(
                        RichSpan(
                            key = 0,
                            text = "Fourth",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        assertEquals(expectedHtml, richTextState.toHtml())
    }

    @Test
    fun testDecodeOrderedListAndUnorderedListAndParagraph() {
        val expectedHtml = "<ol><li>First</li><li>Second</li></ol><p>Paragraph</p><ul><li>Third</li><li>Fourth</li></ul>"

        val richTextState = RichTextState(
            listOf(
                RichParagraph(
                    key = 0,
                    type = OrderedList(1)
                ).also {
                    it.children.add(
                        RichSpan(
                            key = 0,
                            text = "First",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    key = 1,
                    type = OrderedList(2)
                ).also {
                    it.children.add(
                        RichSpan(
                            key = 0,
                            text = "Second",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    key = 2,
                    type = DefaultParagraph()
                ).also {
                    it.children.add(
                        RichSpan(
                            key = 0,
                            text = "Paragraph",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    key = 3,
                    type = UnorderedList()
                ).also {
                    it.children.add(
                        RichSpan(
                            key = 0,
                            text = "Third",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    key = 4,
                    type = UnorderedList()
                ).also {
                    it.children.add(
                        RichSpan(
                            key = 0,
                            text = "Fourth",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        assertEquals(expectedHtml, richTextState.toHtml())
    }

    @Test
    fun testDecodeListsWithDifferentLevels() {
        val expectedHtml = """
            <ol>
                <li>F</li>
                <ol><li>FFO</li><li>FSO</li></ol>
                <ul>
                    <li>FFU</li><li>FSU</li>
                    <ul><li>FSU3</li></ul>
                </ul>
            </ol>
            <ul>
                <li>FFU</li>
                <ol><li>FFO</li></ol>
            </ul>
            <p>Last</p>
        """
            .trimIndent()
            .replace("\n", "")
            .replace(" ", "")

        val richTextState = RichTextState(
            listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 1,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "F",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "FFO",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "FSO",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "FFU",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "FSU",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 3,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "FSU3",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 1
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "FFU",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialLevel = 2,
                    )
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "FFO",
                            paragraph = it,
                        )
                    )
                },
                RichParagraph(
                    type = DefaultParagraph()
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Last",
                            paragraph = it,
                        )
                    )
                }
            )
        )

        assertEquals(expectedHtml, richTextState.toHtml())
    }



    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testToHtmlWithRange() {
        // Test basic text conversion with range
        val richTextState = RichTextStateHtmlParser.encode("<p>Hello World!</p>")

        // Test middle range
        assertEquals(
            "<p>World</p>",
            richTextState.toHtml(TextRange(6, 11))
        )

        // Test start range
        assertEquals(
            "<p>Hello</p>",
            richTextState.toHtml(TextRange(0, 5))
        )

        // Test end range
        assertEquals(
            "<p>World&excl;</p>",
            richTextState.toHtml(TextRange(6, 12))
        )

        // Test full range
        assertEquals(
            "<p>Hello World&excl;</p>",
            richTextState.toHtml(TextRange(0, 12))
        )

        // Test empty range
        assertEquals(
            "<p></p>",
            richTextState.toHtml(TextRange(0, 0))
        )
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testToHtmlWithInvalidRange() {
        val richTextState = RichTextStateHtmlParser.encode("<p>Hello World</p>")

        // Test range with start > end
        assertEquals(
            "<p>llo</p>",
            richTextState.toHtml(TextRange(5, 2))
        )

        // Test range beyond text length
        assertEquals(
            "<p>World</p>",
            richTextState.toHtml(TextRange(6, 19))
        )
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testToHtmlWithNestedStyles() {
        val richTextState = RichTextStateHtmlParser.encode(
            "<p>Start <i>italic with <b>bold</b> text</i> end</p>"
        )

        // Test selecting only nested bold text
        assertEquals(
            "<p><b><i>bold</i></b></p>", // order of tags may change here because of optimizations
            richTextState.toHtml(TextRange(18, 22))
        )

        // Test selecting partial nested text
        assertEquals(
            "<p><i>with <b>bold</b></i></p>",
            richTextState.toHtml(TextRange(13, 22))
        )

        // Test selecting entire styled section
        assertEquals(
            "<p><i>italic with <b>bold</b> text</i></p>",
            richTextState.toHtml(TextRange(6, 27))
        )
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testToHtmlWithRangeAndStyles() {
        // Test text with mixed styles
        val richTextState = RichTextStateHtmlParser.encode(
            "<p>Hello <b>Bold</b> and <i>Italic</i> text!</p>"
        )

        // Test selecting only bold text
        assertEquals(
            "<p><b>Bold</b></p>",
            richTextState.toHtml(TextRange(6, 10))
        )

        // Test selecting only italic text
        assertEquals(
            "<p><i>Italic</i></p>",
            richTextState.toHtml(TextRange(15, 21))
        )

        // Test selecting text with multiple styles
        assertEquals(
            "<p><b>Bold</b> and <i>Italic</i></p>",
            richTextState.toHtml(TextRange(6, 21))
        )

        // Test selecting partial styled text
        assertEquals(
            "<p>o <b>Bold</b> and <i>Ital</i></p>",
            richTextState.toHtml(TextRange(4, 19))
        )
    }

    @Test
    fun testToHtmlWithRangeReversed() {
        val richTextState = RichTextStateHtmlParser.encode(
            "<p>Hey all <b>bruh </b><i>emnn <u>fdf fdf</u> fdf</i></p><p><i>so yes </i></p>"
        )

        assertEquals(
            richTextState.toHtml(TextRange(16, 28)),
            richTextState.toHtml(TextRange(28, 16))
        )
    }

}