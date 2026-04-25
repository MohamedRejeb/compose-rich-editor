package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType.Companion.startText
import kotlin.test.*
import kotlin.test.Ignore

/**
 * Reproduction tests for HTML parser bugs from GitHub issues.
 * These tests document the expected behavior - failing tests indicate bugs to fix.
 *
 * Issues covered:
 * - #391: setHtml removes spaces between separately-bolded words
 * - #610: <br> inside <strong> breaks bold on next line
 * - #586: <br> breaks <a> tag
 * - #583: \n rendered as empty <p></p> instead of <br>
 * - #569: Ordered lists break beyond item 10
 * - #574: Custom ordered list start values always reset to 1
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextStateHtmlParserBugTest {

    // ========================================================================
    // #391: setHtml removes spaces between separately-bolded words
    // ========================================================================

    @Test
    fun testIssue391_spaceBetweenBoldWordsPreserved() {
        // Input: <p><b>hello</b> <b>world</b></p>
        // Expected: "hello world" with space preserved
        // Bug: space between the two bold spans is lost → "helloworld"
        val state = RichTextState()
        state.setHtml("<p><b>hello</b> <b>world</b></p>")

        val text = state.annotatedString.text
        assertTrue(
            text.contains("hello world"),
            "#391: Space between bold words should be preserved. Got: '$text'"
        )
    }

    @Test
    fun testIssue391_spaceBetweenDifferentStyleSpans() {
        // Variant: space between bold and italic
        val state = RichTextState()
        state.setHtml("<p><b>hello</b> <i>world</i></p>")

        val text = state.annotatedString.text
        assertTrue(
            text.contains("hello world"),
            "#391 variant: Space between bold and italic should be preserved. Got: '$text'"
        )
    }

    @Test
    fun testIssue391_multipleSpacesBetweenStyledSpans() {
        // Multiple styled spans with spaces between them
        val state = RichTextState()
        state.setHtml("<p><b>one</b> <b>two</b> <b>three</b></p>")

        val text = state.annotatedString.text
        assertTrue(
            text.contains("one two three"),
            "#391 variant: Spaces between multiple bold spans should be preserved. Got: '$text'"
        )
    }

    @Test
    fun testIssue391_htmlRoundTrip() {
        // setHtml → toHtml → setHtml should preserve spaces
        val originalHtml = "<p><b>hello</b> <b>world</b></p>"
        val state = RichTextState()
        state.setHtml(originalHtml)

        val exportedHtml = state.toHtml()

        val state2 = RichTextState()
        state2.setHtml(exportedHtml)

        val text = state2.annotatedString.text
        assertTrue(
            text.contains("hello world"),
            "#391 round-trip: Space should survive HTML round-trip. Got: '$text'"
        )
    }

    // ========================================================================
    // #610: <br> inside <strong> breaks bold on next line
    // ========================================================================

    @Test
    fun testIssue610_brInsideStrongPreservesBold() {
        // Input: <strong>first line<br>second line</strong>
        // Expected: both "first line" and "second line" should be bold
        // Bug: "second line" loses bold formatting after <br>
        val state = RichTextState()
        state.setHtml("<p><strong>first line<br>second line</strong></p>")

        val text = state.annotatedString.text
        assertTrue(
            text.contains("first line"),
            "#610: Should contain 'first line'. Got: '$text'"
        )
        assertTrue(
            text.contains("second line"),
            "#610: Should contain 'second line'. Got: '$text'"
        )

        // Both lines should be bold - check that the second line's span has bold
        val secondLineStart = text.indexOf("second line")
        assertTrue(secondLineStart >= 0, "#610: 'second line' not found in text")

        // Find the RichSpan containing "second line" and verify it has bold styling
        val richSpan = state.getRichSpanByTextIndex(secondLineStart)
        assertNotNull(richSpan, "#610: Should find RichSpan at 'second line' position")

        val fullStyle = richSpan.fullSpanStyle
        assertEquals(
            FontWeight.Bold,
            fullStyle.fontWeight,
            "#610: 'second line' after <br> inside <strong> should still be bold"
        )
    }

    @Test
    fun testIssue610_brInsideEmPreservesItalic() {
        // Same issue but with <em>/<i>
        val state = RichTextState()
        state.setHtml("<p><em>first line<br>second line</em></p>")

        val text = state.annotatedString.text
        val secondLineStart = text.indexOf("second line")
        assertTrue(secondLineStart >= 0, "#610 variant: 'second line' not found")

        val richSpan = state.getRichSpanByTextIndex(secondLineStart)
        assertNotNull(richSpan, "#610 variant: Should find RichSpan")

        val fullStyle = richSpan.fullSpanStyle
        assertEquals(
            FontStyle.Italic,
            fullStyle.fontStyle,
            "#610 variant: 'second line' after <br> inside <em> should still be italic"
        )
    }

    @Test
    fun testIssue610_realWorldExample() {
        // From the actual issue report
        val html = "<strong>first</strong> <strong>staff</strong> Deleted Time Clock entry of " +
            "<strong>second staff</strong> for Visit at " +
            "<strong>7895 Clyde Park Avenue Southwest<br>Byron Center, Michigan 49315</strong>"

        val state = RichTextState()
        state.setHtml(html)

        val text = state.annotatedString.text
        assertTrue(
            text.contains("Byron Center"),
            "#610 real-world: Should contain 'Byron Center'. Got: '$text'"
        )

        // "Byron Center" should be bold (it's inside the <strong> after <br>)
        val byronStart = text.indexOf("Byron Center")
        if (byronStart >= 0) {
            val richSpan = state.getRichSpanByTextIndex(byronStart)
            assertNotNull(richSpan, "#610 real-world: Should find RichSpan for 'Byron Center'")
            assertEquals(
                FontWeight.Bold,
                richSpan.fullSpanStyle.fontWeight,
                "#610 real-world: 'Byron Center' should be bold (inside <strong> after <br>)"
            )
        }
    }

    // ========================================================================
    // #586: <br> breaks <a> tag
    // ========================================================================

    @Test
    fun testIssue586_brInsideLinkPreservesLink() {
        // Input: <a href="https://example.com"><br>Text<br></a>
        // Expected: "Text" should still be a link
        // Bug: <br> inside <a> breaks the link
        val state = RichTextState()
        state.setHtml("""<p><a href="https://example.com">Link text<br>more link text</a></p>""")

        val text = state.annotatedString.text
        assertTrue(
            text.contains("more link text"),
            "#586: Should contain 'more link text'. Got: '$text'"
        )

        // "more link text" should still be a link
        val moreLinkStart = text.indexOf("more link text")
        if (moreLinkStart >= 0) {
            val richSpan = state.getRichSpanByTextIndex(moreLinkStart)
            assertNotNull(richSpan, "#586: Should find RichSpan for 'more link text'")

            // Walk up parent chain to find link style
            var span = richSpan
            var foundLink = false
            while (span != null) {
                if (span.richSpanStyle is RichSpanStyle.Link) {
                    foundLink = true
                    break
                }
                span = span.parent
            }
            assertTrue(
                foundLink,
                "#586: 'more link text' after <br> inside <a> should still be a link"
            )
        }
    }

    // ========================================================================
    // #583: \n rendered as empty <p></p> instead of <br> in HTML output
    // ========================================================================

    @Test
    fun testIssue583_newlinesWithinParagraphExportAsBr() {
        // When a user types newlines within a paragraph (not Enter to create new paragraph),
        // the HTML output should use <br> not empty <p></p>
        val state = RichTextState()
        state.setHtml("<p>Line one<br>Line two<br>Line three</p>")

        val html = state.toHtml()

        // The output should represent the line breaks, not as separate empty paragraphs
        // At minimum, all three lines should be present
        assertTrue(
            html.contains("Line one"),
            "#583: HTML output should contain 'Line one'. Got: '$html'"
        )
        assertTrue(
            html.contains("Line two"),
            "#583: HTML output should contain 'Line two'. Got: '$html'"
        )
        assertTrue(
            html.contains("Line three"),
            "#583: HTML output should contain 'Line three'. Got: '$html'"
        )

        // Ideally they should be in the same paragraph with <br>, not separate <p> tags
        // Count <p> tags - if there are 3 <p> tags for what was 1 paragraph, that's the bug
        val pCount = "<p>".toRegex().findAll(html).count()
        assertEquals(
            1,
            pCount,
            "#583: Single paragraph with <br> should export as 1 <p>, not $pCount. HTML: '$html'"
        )
    }

    @Test
    fun testIssue583_brRoundTrip() {
        // <br> inside a paragraph should survive round-trip
        val input = "<p>Line one<br>Line two<br>Line three</p>"
        val state = RichTextState()
        state.setHtml(input)

        val html = state.toHtml()
        assertEquals(input, html, "#583: <br> round-trip should be exact")
    }

    @Test
    fun testIssue583_lineBreakFlagClearedOnParagraphStyleChange() {
        // Changing paragraph style (e.g. alignment) on a <br> paragraph
        // should clear isFromLineBreak so it exports as its own <p>
        val state = RichTextState()
        state.setHtml("<p>Line one<br>Line two</p>")

        // Verify initial state: line two should be isFromLineBreak
        assertTrue(state.richParagraphList[1].isFromLineBreak, "Line two should be from linebreak initially")

        // Select "Line two" and change alignment
        val lineTwoStart = state.annotatedString.text.indexOf("Line two")
        state.selection = TextRange(lineTwoStart, lineTwoStart + "Line two".length)
        state.addParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))

        // isFromLineBreak should now be cleared
        assertFalse(state.richParagraphList[1].isFromLineBreak, "isFromLineBreak should be cleared after paragraph style change")

        // HTML should now have two <p> tags
        val html = state.toHtml()
        val pCount = "<p".toRegex().findAll(html).count()
        assertEquals(2, pCount, "Should have 2 <p> tags after style change. HTML: '$html'")
    }

    @Test
    fun testIssue583_parentStyleChangeClearsTrailingContinuations() {
        // Changing the FIRST paragraph's style should clear isFromLineBreak
        // on all trailing continuation paragraphs
        val state = RichTextState()
        state.setHtml("<p>Line one<br>Line two<br>Line three</p>")

        assertEquals(3, state.richParagraphList.size)
        assertFalse(state.richParagraphList[0].isFromLineBreak)
        assertTrue(state.richParagraphList[1].isFromLineBreak)
        assertTrue(state.richParagraphList[2].isFromLineBreak)

        // Select "Line one" (the parent paragraph) and change alignment
        val lineOneStart = state.annotatedString.text.indexOf("Line one")
        state.selection = TextRange(lineOneStart, lineOneStart + "Line one".length)
        state.addParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))

        // All trailing continuations should be cleared
        assertFalse(state.richParagraphList[1].isFromLineBreak, "Line two should no longer be from linebreak")
        assertFalse(state.richParagraphList[2].isFromLineBreak, "Line three should no longer be from linebreak")

        // HTML should have 3 separate <p> tags
        val html = state.toHtml()
        val pCount = "<p".toRegex().findAll(html).count()
        assertEquals(3, pCount, "Should have 3 <p> tags after parent style change. HTML: '$html'")
    }

    @Test
    fun testIssue583_lineBreakFlagClearedOnListToggle() {
        // Converting a <br> paragraph to a list should clear isFromLineBreak
        val state = RichTextState()
        state.setHtml("<p>Line one<br>Line two</p>")

        assertTrue(state.richParagraphList[1].isFromLineBreak)

        // Select "Line two" and toggle ordered list
        val lineTwoStart = state.annotatedString.text.indexOf("Line two")
        state.selection = TextRange(lineTwoStart, lineTwoStart + "Line two".length)
        state.toggleOrderedList()

        assertFalse(state.richParagraphList[1].isFromLineBreak, "isFromLineBreak should be cleared after list toggle")
    }

    @Test
    fun testIssue583_lineBreakFlagPreservedOnSpanStyleChange() {
        // Changing SPAN style (bold, italic) should NOT clear isFromLineBreak
        // because span styles don't change the paragraph structure
        val state = RichTextState()
        state.setHtml("<p>Line one<br>Line two</p>")

        assertTrue(state.richParagraphList[1].isFromLineBreak)

        // Select "Line two" and make it bold
        val lineTwoStart = state.annotatedString.text.indexOf("Line two")
        state.selection = TextRange(lineTwoStart, lineTwoStart + "Line two".length)
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

        // isFromLineBreak should still be true - bold doesn't change paragraph structure
        assertTrue(state.richParagraphList[1].isFromLineBreak, "isFromLineBreak should survive span style changes")

        // HTML should still use <br>
        val html = state.toHtml()
        assertTrue(html.contains("<br>"), "Should still use <br>. HTML: '$html'")
    }

    // ========================================================================
    // #569: Ordered lists break beyond item 10
    // ========================================================================

    @Test
    fun testIssue569_orderedListBeyond10ItemsRendersCorrectly() {
        // Build a 20-item ordered list
        val html = "<ol>" +
            (1..20).joinToString("") { "<li>Next element</li>" } +
            "</ol>"

        val state = RichTextState()
        state.setHtml(html)

        // Should have 20 paragraphs, all ordered lists
        assertEquals(
            20,
            state.richParagraphList.size,
            "#569: Should have 20 paragraphs. Got: ${state.richParagraphList.size}"
        )

        // All should be OrderedList type
        state.richParagraphList.forEachIndexed { i, paragraph ->
            assertIs<OrderedList>(
                paragraph.type,
                "#569: Paragraph $i should be OrderedList, got ${paragraph.type::class.simpleName}"
            )
        }

        // Verify numbers are sequential 1..20
        state.richParagraphList.forEachIndexed { i, paragraph ->
            val type = paragraph.type as OrderedList
            assertEquals(
                i + 1,
                type.number,
                "#569: Item $i should have number ${i + 1}, got ${type.number}"
            )
        }

        // Verify the text contains all items (none lost or corrupted)
        val text = state.annotatedString.text
        // Should contain 20 instances of "Next element"
        val count = "Next element".toRegex().findAll(text).count()
        assertEquals(
            20,
            count,
            "#569: Should have 20 'Next element' instances in text. Got: $count"
        )
    }

    @Test
    fun testIssue569_orderedListItemsHaveCorrectPrefixLength() {
        // The bug manifests as misalignment - items 1-9 have prefix "N. " (3 chars)
        // but items 10+ have prefix "NN. " (4 chars). Verify prefixes are correct.
        val html = "<ol>" +
            (1..15).joinToString("") { "<li>Item</li>" } +
            "</ol>"

        val state = RichTextState()
        state.setHtml(html)

        state.richParagraphList.forEachIndexed { i, paragraph ->
            val type = paragraph.type
            assertIs<OrderedList>(type)
            val prefix = type.startText
            val expectedNumber = i + 1
            assertTrue(
                prefix.contains("$expectedNumber"),
                "#569: Prefix for item $expectedNumber should contain '$expectedNumber'. Got: '$prefix'"
            )
        }
    }

    @Test
    fun testIssue569_htmlRoundTripPreservesAllItems() {
        val html = "<ol>" +
            (1..20).joinToString("") { "<li>Item $it</li>" } +
            "</ol>"

        val state = RichTextState()
        state.setHtml(html)
        val exportedHtml = state.toHtml()

        // Re-parse
        val state2 = RichTextState()
        state2.setHtml(exportedHtml)

        assertEquals(
            state.richParagraphList.size,
            state2.richParagraphList.size,
            "#569 round-trip: Paragraph count should be preserved"
        )

        // All items should still be present
        for (i in 1..20) {
            assertTrue(
                state2.annotatedString.text.contains("Item $i"),
                "#569 round-trip: 'Item $i' should survive round-trip"
            )
        }
    }

    // ========================================================================
    // #574: Custom ordered list start values always reset to 1
    // ========================================================================

    @Test
    fun testIssue574_olStartAttributeRespected() {
        // <ol start="10"> should start numbering at 10
        val state = RichTextState()
        state.setHtml("<ol start=\"10\"><li>Item A</li><li>Item B</li><li>Item C</li></ol>")

        assertEquals(
            3,
            state.richParagraphList.size,
            "#574: Should have 3 paragraphs"
        )

        // First item should be numbered 10, not 1
        val firstType = state.richParagraphList[0].type
        assertIs<OrderedList>(firstType)
        assertEquals(
            10,
            firstType.number,
            "#574: First item with start=10 should have number 10, got ${firstType.number}"
        )

        val secondType = state.richParagraphList[1].type
        assertIs<OrderedList>(secondType)
        assertEquals(
            11,
            secondType.number,
            "#574: Second item should have number 11, got ${secondType.number}"
        )

        val thirdType = state.richParagraphList[2].type
        assertIs<OrderedList>(thirdType)
        assertEquals(
            12,
            thirdType.number,
            "#574: Third item should have number 12, got ${thirdType.number}"
        )
    }

    @Test
    fun testIssue574_olStartAttributeInHtmlOutput() {
        // If we set start=5, the HTML output should include start="5"
        val state = RichTextState()
        state.setHtml("<ol start=\"5\"><li>A</li><li>B</li></ol>")

        val html = state.toHtml()
        assertTrue(
            html.contains("start") || html.contains("5"),
            "#574: HTML output should preserve start attribute. Got: '$html'"
        )
    }

    // TODO: <li value="N"> is parsed correctly but checkParagraphsType()
    //  renumbers items sequentially, overriding the per-item value.
    //  Fixing this requires preserving explicit values through normalization.
    @Ignore
    @Test
    fun testIssue574_liValueAttributeRespected() {
        // <li value="5"> should set that item's number
        val state = RichTextState()
        state.setHtml("<ol><li>Normal</li><li value=\"5\">Custom</li><li>After</li></ol>")

        val secondType = state.richParagraphList[1].type
        assertIs<OrderedList>(secondType)
        assertEquals(
            5,
            secondType.number,
            "#574: <li value=5> should set number to 5, got ${secondType.number}"
        )

        // The item after should be 6
        val thirdType = state.richParagraphList[2].type
        assertIs<OrderedList>(thirdType)
        assertEquals(
            6,
            thirdType.number,
            "#574: Item after <li value=5> should be 6, got ${thirdType.number}"
        )
    }
}
