package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.HeadingParagraphStyle
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import com.mohamedrejeb.richeditor.parser.utils.H1SpanStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RichTextStateHtmlParserEncodeTest {
    @Test
    fun testRemoveHtmlTextExtraSpaces() {
        val html = """
            Hello       World!      Welcome to 
             
             Compose Rich Text Editor!
        """.trimIndent()

        assertEquals(
            "Hello World! Welcome to Compose Rich Text Editor!",
            removeHtmlTextExtraSpaces(html)
        )
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testHtmlWithImage() {
        val html = """
            <!DOCTYPE html>
            <html>
            <body>

            <h1>The img element</h1>

            <img src="https://picsum.photos/200/300" alt="Girl in a jacket" width="500" height="600">

            </body>
            </html>
        """.trimIndent()

        val richTextState = RichTextStateHtmlParser.encode(html)

        val h1 = richTextState.richParagraphList[0].children.first()
        val image = richTextState.richParagraphList[1].children.first()

        assertEquals(2, richTextState.richParagraphList.size)
        assertEquals(1, richTextState.richParagraphList[0].children.size)
        assertEquals(1, richTextState.richParagraphList[1].children.size)
        assertEquals("The img element", h1.text)
        assertEquals(H1SpanStyle, h1.spanStyle)
        assertIs<RichSpanStyle.Image>(image.richSpanStyle)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testHtmlWithBrAndImage() {
        val html = """
            <!DOCTYPE html>
            <html>
            <body>

            <h1>The img element</h1>
            <br>
            <img src="https://picsum.photos/200/300" alt="Girl in a jacket" width="500" height="600">

            </body>
            </html>
        """.trimIndent()

        val richTextState = RichTextStateHtmlParser.encode(html)

        val h1 = richTextState.richParagraphList[0].children.first()
        val image = richTextState.richParagraphList[2].children.first()

        assertEquals(3, richTextState.richParagraphList.size)
        assertEquals(1, richTextState.richParagraphList[0].children.size)
        assertTrue(richTextState.richParagraphList[1].isBlank())
        // It's only 1, but we have the added rich span for each paragraph with index > 0
        assertEquals(1, richTextState.richParagraphList[2].children.size)
        assertEquals("The img element", h1.text)
        assertEquals(H1SpanStyle, h1.spanStyle)
        assertIs<RichSpanStyle.Image>(image.richSpanStyle)
    }

    @Test
    fun testHtmlWithEmptyBlockElements1() {
        val html = """
            <!DOCTYPE html>
            <html>
            <body>

            <p><p>dd  dd<span> second</span></p></p>

            </body>
            </html>
        """.trimIndent()

        val richTextState = RichTextStateHtmlParser.encode(html)

        assertEquals(1, richTextState.richParagraphList.size)
        assertEquals("dd dd second", richTextState.annotatedString.text)

        richTextState.setHtml(
            """
                <!DOCTYPE html>
                <html>
                <body>
    
                <p><p><p> second</p></p></p>
    
                </body>
                </html>
            """.trimIndent()
        )
    }

    @Test
    fun testHtmlWithEmptyBlockElements2() {
        val html =
            """
                <!DOCTYPE html>
                <html>
                <body>
    
                <p><p><p> second</p></p></p>
    
                </body>
                </html>
            """.trimIndent()

        val richTextState = RichTextStateHtmlParser.encode(html)

        assertEquals(1, richTextState.richParagraphList.size)
        assertEquals("second", richTextState.annotatedString.text)
    }

    @Test
    fun testBrEncodeDecode() {
        val html = "<p>ABC</p><br><br><br>"

        val state = RichTextStateHtmlParser.encode(html)

        assertEquals(5, state.richParagraphList.size)
        assertEquals(html, state.toHtml())
    }

    @Test
    fun testBrEncodeDecode2() {
        val html = "<br><p>ABC</p><br><br><p>ABC</p><br><br>"

        val state = RichTextStateHtmlParser.encode(html)

        assertEquals(8, state.richParagraphList.size)
        assertEquals(html, state.toHtml())
    }

    @Test
    fun testBrInMiddleOrParagraph() {
        val html = """
            <h1>Hello<br>World!</h1>
        """.trimIndent()

        val richTextState = RichTextStateHtmlParser.encode(html)

        assertEquals(2, richTextState.richParagraphList.size)
        assertEquals(1, richTextState.richParagraphList[0].children.size)
        assertEquals(1, richTextState.richParagraphList[1].children.size)

        val firstPart = richTextState.richParagraphList[0].children.first()
        val secondPart = richTextState.richParagraphList[1].children.first()

        assertEquals("Hello", firstPart.text)
        assertEquals("World!", secondPart.text)

        assertEquals(H1SpanStyle, firstPart.spanStyle)
        assertEquals(H1SpanStyle, secondPart.spanStyle)
    }

    @Test
    fun testEncodeUnorderedList() {
        val html = """
            <ul>
                <li>Item 1</li>
                <li>Item 2</li>
                <li>Item 3</li>
            </ul>
        """.trimIndent()

        val richTextState = RichTextStateHtmlParser.encode(html)

        assertEquals(3, richTextState.richParagraphList.size)

        val firstItem = richTextState.richParagraphList[0].children[0]
        val secondItem = richTextState.richParagraphList[1].children[0]
        val thirdItem = richTextState.richParagraphList[2].children[0]

        richTextState.richParagraphList.forEach { p ->
            assertIs<UnorderedList>(p.type)
        }

        assertEquals("Item 1", firstItem.text)
        assertEquals("Item 2", secondItem.text)
        assertEquals("Item 3", thirdItem.text)
    }

    @Test
    fun testEncodeUnorderedListWithNestedList() {
        val html = """
            <ul>
                <li>Item1</li>
                <li>Item2
                    <ul>
                        <li>Item2.1</li>
                        <li>Item2.2</li>
                    </ul>
                </li>
                <li>Item3</li>
            </ul>
        """
            .trimIndent()
            .replace("\n", "")
            .replace(" ", "")

        val richTextState = RichTextStateHtmlParser.encode(html)

        assertEquals(5, richTextState.richParagraphList.size)

        val firstItem = richTextState.richParagraphList[0].children[0]
        val secondItem = richTextState.richParagraphList[1].children[0]
        val thirdItem = richTextState.richParagraphList[2].children[0]
        val fourthItem = richTextState.richParagraphList[3].children[0]
        val fifthItem = richTextState.richParagraphList[4].children[0]

        richTextState.richParagraphList.forEachIndexed { i, p ->
            val type = p.type
            assertIs<UnorderedList>(type)

            if (
                i == 0 ||
                i == 1 ||
                i == 4
            )
                assertEquals(1, type.level)
            else
                assertEquals(2, type.level)
        }

        assertEquals("Item1", firstItem.text)
        assertEquals("Item2", secondItem.text)
        assertEquals("Item2.1", thirdItem.text)
        assertEquals("Item2.2", fourthItem.text)
        assertEquals("Item3", fifthItem .text)
    }

    @Test
    fun testEncodeOrderedList() {
        val html = """
            <ol>
                <li>Item 1</li>
                <li>Item 2</li>
                <li>Item 3</li>
            </ol>
        """.trimIndent()

        val richTextState = RichTextStateHtmlParser.encode(html)

        assertEquals(3, richTextState.richParagraphList.size)

        val firstItem = richTextState.richParagraphList[0].children[0]
        val secondItem = richTextState.richParagraphList[1].children[0]
        val thirdItem = richTextState.richParagraphList[2].children[0]

        richTextState.richParagraphList.forEach { p ->
            assertIs<OrderedList>(p.type)
        }

        assertEquals("Item 1", firstItem.text)
        assertEquals("Item 2", secondItem.text)
        assertEquals("Item 3", thirdItem.text)
    }

    @Test
    fun testEncodeOrderedListWithNestedList() {
        val html = """
            <ol>
                <li>Item1</li>
                <li>Item2
                    <ol>
                        <li>Item2.1</li>
                        <li>Item2.2</li>
                    </ol>
                </li>
                <li>Item3</li>
            </ol>
        """
            .trimIndent()
            .replace("\n", "")
            .replace(" ", "")

        val richTextState = RichTextStateHtmlParser.encode(html)

        assertEquals(5, richTextState.richParagraphList.size)

        val firstItem = richTextState.richParagraphList[0].children[0]
        val secondItem = richTextState.richParagraphList[1].children[0]
        val thirdItem = richTextState.richParagraphList[2].children[0]
        val fourthItem = richTextState.richParagraphList[3].children[0]
        val fifthItem = richTextState.richParagraphList[4].children[0]

        richTextState.richParagraphList.forEachIndexed { i, p ->
            val type = p.type
            assertIs<OrderedList>(type)

            if (
                i == 0 ||
                i == 1 ||
                i == 4
            )
                assertEquals(1, type.level)
            else
                assertEquals(2, type.level)
        }

        assertEquals("Item1", firstItem.text)
        assertEquals("Item2", secondItem.text)
        assertEquals("Item2.1", thirdItem.text)
        assertEquals("Item2.2", fourthItem.text)
        assertEquals("Item3", fifthItem .text)
    }

    @Test
    fun testEncodeHeadingParagraphStyles() {
        val html = """
            <h1>Heading 1</h1>
            <p>Some text</p>
            <h2>Heading 2</h2>
            <p>More text</p>
        """.trimIndent()

        val state = RichTextStateHtmlParser.encode(html)

        assertEquals(4, state.richParagraphList.size)

        // Paragraph 0: H1
        val p0 = state.richParagraphList[0]
        assertEquals(HeadingParagraphStyle.H1, p0.getHeadingParagraphStyle())
        assertEquals("Heading 1", p0.getFirstNonEmptyChild()?.text)
        assertEquals(HeadingParagraphStyle.H1.getSpanStyle(), p0.getFirstNonEmptyChild()?.spanStyle)

        // Paragraph 1: Normal
        val p1 = state.richParagraphList[1]
        assertEquals(HeadingParagraphStyle.Normal, p1.getHeadingParagraphStyle())
        assertEquals("Some text", p1.getFirstNonEmptyChild()?.text)
        assertEquals(SpanStyle(), p1.getFirstNonEmptyChild()?.spanStyle) // Default SpanStyle

        // Paragraph 2: H2
        val p2 = state.richParagraphList[2]
        assertEquals(HeadingParagraphStyle.H2, p2.getHeadingParagraphStyle())
        assertEquals("Heading 2", p2.getFirstNonEmptyChild()?.text)
        assertEquals(HeadingParagraphStyle.H2.getSpanStyle(), p2.getFirstNonEmptyChild()?.spanStyle)

        // Paragraph 3: Normal
        val p3 = state.richParagraphList[3]
        assertEquals(HeadingParagraphStyle.Normal, p3.getHeadingParagraphStyle())
        assertEquals("More text", p3.getFirstNonEmptyChild()?.text)
        assertEquals(SpanStyle(), p3.getFirstNonEmptyChild()?.spanStyle) // Default SpanStyle
    }

    @Test
    fun testEncodeHeadingParagraphStylesWithInlineStyles() {
        val html = """
            <h1 style="color: red;">Red Heading 1</h1>
            <h2 style="font-weight: bold;">Bold Heading 2</h2>
        """.trimIndent()

        val state = RichTextStateHtmlParser.encode(html)

        assertEquals(2, state.richParagraphList.size)

        // Paragraph 0: H1 with red color
        val p0 = state.richParagraphList[0]
        assertEquals(HeadingParagraphStyle.H1, p0.getHeadingParagraphStyle())
        assertEquals("Red Heading 1", p0.getFirstNonEmptyChild()?.text)
        // Check that the base H1 style is applied AND the red color
        val expectedH1Style = HeadingParagraphStyle.H1.getSpanStyle().merge(SpanStyle(color = androidx.compose.ui.graphics.Color.Red))
        assertEquals(expectedH1Style, p0.getFirstNonEmptyChild()?.spanStyle)

        // Paragraph 1: H2 with bold font weight
        val p1 = state.richParagraphList[1]
        assertEquals(HeadingParagraphStyle.H2, p1.getHeadingParagraphStyle())
        assertEquals("Bold Heading 2", p1.getFirstNonEmptyChild()?.text)
        // Check that the base H2 style is applied AND the bold font weight
        val expectedH2Style = HeadingParagraphStyle.H2.getSpanStyle().merge(SpanStyle(fontWeight = FontWeight.Bold))
        assertEquals(expectedH2Style, p1.getFirstNonEmptyChild()?.spanStyle)
    }

}
