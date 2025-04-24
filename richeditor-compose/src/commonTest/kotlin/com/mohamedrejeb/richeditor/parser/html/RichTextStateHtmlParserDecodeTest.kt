package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.HeadingParagraphStyle
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

    @Test
    fun testDecodeSpanWithOnlySpace() {
        val html = "<meta charset='utf-8'><span style=\"box-sizing: border-box; color: rgb(240, 246, 252); font-family: -apple-system, &quot;system-ui&quot;, &quot;Segoe UI&quot;, &quot;Noto Sans&quot;, Helvetica, Arial, sans-serif, &quot;Apple Color Emoji&quot;, &quot;Segoe UI Emoji&quot;; font-size: 14px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(1, 4, 9); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">results in the</span><span style=\"color: rgb(240, 246, 252); font-family: -apple-system, &quot;system-ui&quot;, &quot;Segoe UI&quot;, &quot;Noto Sans&quot;, Helvetica, Arial, sans-serif, &quot;Apple Color Emoji&quot;, &quot;Segoe UI Emoji&quot;; font-size: 14px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(1, 4, 9); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial; display: inline !important; float: none;\"><span> </span></span><b style=\"box-sizing: border-box; font-weight: var(--base-text-weight-semibold, 600); color: rgb(240, 246, 252); font-family: -apple-system, &quot;system-ui&quot;, &quot;Segoe UI&quot;, &quot;Noto Sans&quot;, Helvetica, Arial, sans-serif, &quot;Apple Color Emoji&quot;, &quot;Segoe UI Emoji&quot;; font-size: 14px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: start; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(1, 4, 9); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">Horizon-School</b>"
        val richTextState = RichTextStateHtmlParser.encode(html)

        assertEquals(
            "results in the Horizon-School",
            richTextState.annotatedString.text
        )
    }

    @Test
    fun testDecodeHeadingParagraphStyles() {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(type = DefaultParagraph()).also {
                    it.children.add(RichSpan(text = "Normal Paragraph", paragraph = it))
                },
                RichParagraph(type = DefaultParagraph()).also {
                    it.children.add(RichSpan(text = "Heading 1", paragraph = it))
                    it.setHeadingStyle(HeadingParagraphStyle.H1)
                },
                RichParagraph(type = DefaultParagraph()).also {
                    it.children.add(RichSpan(text = "Heading 2", paragraph = it))
                    it.setHeadingStyle(HeadingParagraphStyle.H2)
                },
                RichParagraph(type = DefaultParagraph()).also {
                    it.children.add(RichSpan(text = "Heading 3", paragraph = it))
                    it.setHeadingStyle(HeadingParagraphStyle.H3)
                },
                RichParagraph(type = DefaultParagraph()).also {
                    it.children.add(RichSpan(text = "Heading 4", paragraph = it))
                    it.setHeadingStyle(HeadingParagraphStyle.H4)
                },
                RichParagraph(type = DefaultParagraph()).also {
                    it.children.add(RichSpan(text = "Heading 5", paragraph = it))
                    it.setHeadingStyle(HeadingParagraphStyle.H5)
                },
                RichParagraph(type = DefaultParagraph()).also {
                    it.children.add(RichSpan(text = "Heading 6", paragraph = it))
                    it.setHeadingStyle(HeadingParagraphStyle.H6)
                },
                RichParagraph(type = DefaultParagraph()).also {
                    it.children.add(RichSpan(text = "Another Normal Paragraph", paragraph = it))
                }
            )
        )

        val html = RichTextStateHtmlParser.decode(state)

        val expectedHtml = """
            <p>Normal Paragraph</p>
            <h1>Heading 1</h1>
            <h2>Heading 2</h2>
            <h3>Heading 3</h3>
            <h4>Heading 4</h4>
            <h5>Heading 5</h5>
            <h6>Heading 6</h6>
            <p>Another Normal Paragraph</p>
        """.trimIndent().replace("\n", "") // Remove newlines for comparison

        assertEquals(expectedHtml, html.replace("\n", ""))
    }

    @Test
    fun testDecodeHeadingParagraphStylesWithAdditionalSpanStyle() {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(type = DefaultParagraph()).also {
                    val span = RichSpan(text = "Bold Heading 1", paragraph = it)
                    span.spanStyle = span.spanStyle.merge(SpanStyle(fontWeight = FontWeight.Bold))
                    it.children.add(span)
                    it.setHeadingStyle(HeadingParagraphStyle.H1)
                },
                RichParagraph(type = DefaultParagraph()).also {
                    val span = RichSpan(text = "Italic Heading 2", paragraph = it)
                    span.spanStyle = span.spanStyle.merge(SpanStyle(fontStyle = FontStyle.Italic))
                    it.children.add(span)
                    it.setHeadingStyle(HeadingParagraphStyle.H2)
                }
            )
        )

        val html = RichTextStateHtmlParser.decode(state)

        // Expected HTML should have the heading tag and the additional style in the span's style attribute
        val expectedHtml = """
            <h1><b>Bold Heading 1</b></h1>
            <h2><i>Italic Heading 2</i></h2>
        """.trimIndent().replace("\n", "")

        assertEquals(expectedHtml, html.replace("\n", ""))
    }

    @Test
    fun testSetHeadingParagraphStyleWithSelection() {
        val state = RichTextState()
        val initialText = "Paragraph 1\nParagraph 2\nParagraph 3"
        state.setText(initialText)

        // Select "Paragraph 2"
        val paragraph2StartIndex = initialText.indexOf("Paragraph 2")
        val paragraph2EndIndex = paragraph2StartIndex + "Paragraph 2".length
        state.selection = TextRange(paragraph2StartIndex, paragraph2EndIndex)

        // Apply H2 heading style
        state.setHeadingStyle(HeadingParagraphStyle.H2)

        // Verify the second paragraph is now H2
        assertEquals(3, state.richParagraphList.size)
        assertEquals(HeadingParagraphStyle.Normal, state.richParagraphList[0].getHeadingParagraphStyle())
        assertEquals(HeadingParagraphStyle.H2, state.richParagraphList[1].getHeadingParagraphStyle())
        assertEquals(HeadingParagraphStyle.Normal, state.richParagraphList[2].getHeadingParagraphStyle())

        // Verify the text content is unchanged
        assertEquals(initialText.replace("\n", " "), state.annotatedString.text)

        // Decode to HTML and verify the tag
        val html = state.toHtml()
        val expectedHtmlPart = "<h2>Paragraph 2</h2>"
        assertTrue(html.contains(expectedHtmlPart), "Generated HTML should contain $expectedHtmlPart")
    }
}
