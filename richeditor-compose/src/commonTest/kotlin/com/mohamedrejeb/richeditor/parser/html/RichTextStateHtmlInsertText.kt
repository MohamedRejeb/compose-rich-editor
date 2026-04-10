package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextStateHtmlInsertText {

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlAtStart() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        richTextState.insertHtml("<b>Inserted</b>", 0)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(2, paragraph.children.size)

        val firstSpan = paragraph.children[0]
        assertEquals("Inserted", firstSpan.text)
        assertEquals(FontWeight.Bold, firstSpan.spanStyle.fontWeight)

        val secondSpan = paragraph.children[1]
        assertEquals("Initial content", secondSpan.text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlInMiddle() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Before content After</p>")

        richTextState.insertHtml("<i>Inserted</i>", 7)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(3, paragraph.children.size)

        assertEquals("Before ", paragraph.children[0].text)

        val insertedSpan = paragraph.children[1]
        assertEquals("Inserted", insertedSpan.text)
        assertEquals(FontStyle.Italic, insertedSpan.spanStyle.fontStyle)

        assertEquals("content After", paragraph.children[2].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlAtEnd() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        richTextState.insertHtml("<u>Inserted</u>", 15)

        richTextState.printParagraphs()

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(2, paragraph.children.size)

        assertEquals("Initial content", paragraph.children[0].text)

        val insertedSpan = paragraph.children[1]
        assertEquals("Inserted", insertedSpan.text)
        assertEquals(TextDecoration.Underline, insertedSpan.spanStyle.textDecoration)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlWithMultipleParagraphsAtStart() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>First</p><p>Last</p>")

        richTextState.insertHtml("<p>New1</p><p>New2</p>", 6)
        richTextState.printParagraphs()

        assertEquals(3, richTextState.richParagraphList.size)
        assertEquals("First", richTextState.richParagraphList[0].children[0].text)
        assertEquals("New1", richTextState.richParagraphList[1].children[0].text)
        assertEquals("New2Last", richTextState.richParagraphList[2].children[0].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlWithMultipleParagraphsInMiddle() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>FirstLast</p>")

        richTextState.insertHtml("<p>New1</p><p>New2</p>", 5)

        assertEquals(2, richTextState.richParagraphList.size)
        assertEquals("FirstNew1", richTextState.richParagraphList[0].children[0].text)
        assertEquals("New2Last", richTextState.richParagraphList[1].children[0].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlWithMultipleParagraphsAtEnd() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>First</p><p>Last</p>")

        richTextState.insertHtml("<p>New1</p><p>New2</p>", 5)

        assertEquals(3, richTextState.richParagraphList.size)
        assertEquals("FirstNew1", richTextState.richParagraphList[0].children[0].text)
        assertEquals("New2", richTextState.richParagraphList[1].children[0].text)
        assertEquals("Last", richTextState.richParagraphList[2].children[0].text)
    }


    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertHtmlWithMultipleParagraphsWithBr() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>First</p><p>Last</p>")

        richTextState.insertHtml("<br><p>New1</p><p>New2</p>", 5)

        assertEquals(4, richTextState.richParagraphList.size)
        assertEquals("First", richTextState.richParagraphList[0].children[0].text)
        assertEquals("New1", richTextState.richParagraphList[1].children[0].text)
        assertEquals("New2", richTextState.richParagraphList[2].children[0].text)
        assertEquals("Last", richTextState.richParagraphList[3].children[0].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertEmptyHtml() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Content</p>")

        richTextState.insertHtml("", 3)

        assertEquals(1, richTextState.richParagraphList.size)
        assertEquals("Content", richTextState.richParagraphList[0].children[0].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertMarkdownAtStart() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        richTextState.insertMarkdown("**Inserted**", 0)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(2, paragraph.children.size)

        val firstSpan = paragraph.children[0]
        assertEquals("Inserted", firstSpan.text)
        assertEquals(FontWeight.Bold, firstSpan.spanStyle.fontWeight)

        val secondSpan = paragraph.children[1]
        assertEquals("Initial content", secondSpan.text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertMarkdownInMiddle() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Before content After</p>")

        richTextState.insertMarkdown("*Inserted*", 7)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(3, paragraph.children.size)

        assertEquals("Before ", paragraph.children[0].text)

        val insertedSpan = paragraph.children[1]
        assertEquals("Inserted", insertedSpan.text)
        assertEquals(FontStyle.Italic, insertedSpan.spanStyle.fontStyle)

        assertEquals("content After", paragraph.children[2].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertMarkdownAtEnd() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        richTextState.insertMarkdown("__Inserted__", 15)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(2, paragraph.children.size)

        assertEquals("Initial content", paragraph.children[0].text)

        val insertedSpan = paragraph.children[1]
        assertEquals("Inserted", insertedSpan.text)
        assertEquals(FontWeight.Bold, insertedSpan.spanStyle.fontWeight)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertEmptyMarkdown() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        richTextState.insertMarkdown("", 7)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(1, paragraph.children.size)

        val span = paragraph.children[0]
        assertEquals("Initial content", span.text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertComplexMarkdown() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        richTextState.insertMarkdown("**Bold** and *italic*\nNew paragraph with __bold__", 15)

        assertEquals(2, richTextState.richParagraphList.size)

        // First paragraph
        val firstParagraph = richTextState.richParagraphList[0]
        assertEquals(4, firstParagraph.children.size)

        assertEquals("Initial content", firstParagraph.children[0].text)

        val boldSpan = firstParagraph.children[1]
        assertEquals("Bold", boldSpan.text)
        assertEquals(FontWeight.Bold, boldSpan.spanStyle.fontWeight)

        assertEquals(" and ", firstParagraph.children[2].text)

        val italicSpan = firstParagraph.children[3]
        assertEquals("italic", italicSpan.text)
        assertEquals(FontStyle.Italic, italicSpan.spanStyle.fontStyle)

        // Second paragraph
        val secondParagraph = richTextState.richParagraphList[1]
        assertEquals(2, secondParagraph.children.size)

        assertEquals("New paragraph with ", secondParagraph.children[0].text)

        val boldSpan2 = secondParagraph.children[1]
        assertEquals("bold", boldSpan2.text)
        assertEquals(FontWeight.Bold, boldSpan2.spanStyle.fontWeight)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertSingleParagraph() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Initial content</p>")

        val newParagraph = RichParagraph().also { paragraph ->
            paragraph.children.add(
                RichSpan(
                    text = "Inserted",
                    paragraph = paragraph,
                    spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
                )
            )
        }

        richTextState.insertParagraphs(listOf(newParagraph), 15)

        assertEquals(1, richTextState.richParagraphList.size)
        val paragraph = richTextState.richParagraphList[0]
        assertEquals(2, paragraph.children.size)

        assertEquals("Initial content", paragraph.children[0].text)

        val insertedSpan = paragraph.children[1]
        assertEquals("Inserted", insertedSpan.text)
        assertEquals(FontWeight.Bold, insertedSpan.spanStyle.fontWeight)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertMultipleParagraphs() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Before Middle After</p>")

        val paragraph1 = RichParagraph().also { paragraph ->
            paragraph.children.add(
                RichSpan(
                    text = "First",
                    paragraph = paragraph,
                    spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
                )
            )
        }

        val paragraph2 = RichParagraph().also { paragraph ->
            paragraph.children.add(
                RichSpan(
                    text = "Second",
                    paragraph = paragraph,
                    spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
                )
            )
        }

        richTextState.insertParagraphs(listOf(paragraph1, paragraph2), 7)

        assertEquals(2, richTextState.richParagraphList.size)

        // First paragraph
        val firstParagraph = richTextState.richParagraphList[0]
        assertEquals(2, firstParagraph.children.size)
        assertEquals("Before ", firstParagraph.children[0].text)

        val firstInserted = firstParagraph.children[1]
        assertEquals("First", firstInserted.text)
        assertEquals(FontWeight.Bold, firstInserted.spanStyle.fontWeight)

        // Second paragraph
        val secondParagraph = richTextState.richParagraphList[1]
        assertEquals(2, secondParagraph.children.size)

        val secondInserted = secondParagraph.children[0]
        assertEquals("Second", secondInserted.text)
        assertEquals(FontStyle.Italic, secondInserted.spanStyle.fontStyle)

        assertEquals("Middle After", secondParagraph.children[1].text)
    }

    @OptIn(ExperimentalRichTextApi::class)
    @Test
    fun testInsertParagraphsEdgeCases() {
        val richTextState = RichTextState()
        richTextState.setHtml("<p>Original</p>")

        // Create test paragraphs
        val paragraph1 = RichParagraph().also { paragraph ->
            paragraph.children.add(
                RichSpan(
                    text = "Start",
                    paragraph = paragraph,
                    spanStyle = SpanStyle(fontWeight = FontWeight.Bold)
                )
            )
        }

        val paragraph2 = RichParagraph().also { paragraph ->
            paragraph.children.add(
                RichSpan(
                    text = "End",
                    paragraph = paragraph,
                    spanStyle = SpanStyle(fontStyle = FontStyle.Italic)
                )
            )
        }

        // Test inserting at position 0
        richTextState.insertParagraphs(listOf(paragraph1), 0)
        assertEquals(1, richTextState.richParagraphList.size)
        assertEquals(2, richTextState.richParagraphList[0].children.size)
        assertEquals("Start", richTextState.richParagraphList[0].children[0].text)
        assertEquals(FontWeight.Bold, richTextState.richParagraphList[0].children[0].spanStyle.fontWeight)
        assertEquals("Original", richTextState.richParagraphList[0].children[1].text)

        // Test inserting at the end
        richTextState.insertParagraphs(listOf(paragraph2), richTextState.annotatedString.text.length)
        assertEquals(1, richTextState.richParagraphList.size)
        assertEquals(3, richTextState.richParagraphList[0].children.size)
        assertEquals("End", richTextState.richParagraphList[0].children[2].text)
        assertEquals(FontStyle.Italic, richTextState.richParagraphList[0].children[2].spanStyle.fontStyle)

        // Test inserting empty paragraph list
        val textBefore = richTextState.annotatedString.text
        richTextState.insertParagraphs(emptyList(), 5)
        assertEquals(textBefore, richTextState.annotatedString.text)
    }

}