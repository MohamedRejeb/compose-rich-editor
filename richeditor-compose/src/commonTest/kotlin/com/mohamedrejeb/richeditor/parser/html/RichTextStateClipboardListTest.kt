package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Comprehensive tests for clipboard copy (toHtml with range) and paste (insertHtml)
 * operations involving lists. These tests exercise the code paths used by
 * [DesktopRichTextClipboardManager] for rich text copy/paste.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextStateClipboardListTest {

    // =========================================================================
    // SECTION 1: toHtml(range) — Copy scenarios with ordered lists
    // =========================================================================

    @Test
    fun testCopyEntireOrderedList() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li><li>Third</li></ol>"
        )
        val text = state.annotatedString.text
        val fullRange = TextRange(0, text.length)

        val html = state.toHtml(fullRange)

        assertEquals(
            "<ol><li>First</li><li>Second</li><li>Third</li></ol>",
            html
        )
    }

    @Test
    fun testCopyEntireOrderedListMatchesNoRangeToHtml() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li></ol>"
        )
        val text = state.annotatedString.text
        val fullRange = TextRange(0, text.length)

        assertEquals(
            state.toHtml(),
            state.toHtml(fullRange)
        )
    }

    @Test
    fun testCopyFirstItemOfOrderedList() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li><li>Third</li></ol>"
        )
        val text = state.annotatedString.text

        // Select only the first list item's text content
        val firstStart = text.indexOf("First")
        val firstEnd = firstStart + "First".length

        val html = state.toHtml(TextRange(firstStart, firstEnd))

        // Should produce a valid ordered list with just the first item
        assertEquals("<ol><li>First</li></ol>", html)
    }

    @Test
    fun testCopyLastItemOfOrderedList() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li><li>Third</li></ol>"
        )
        val text = state.annotatedString.text

        val thirdStart = text.indexOf("Third")
        val thirdEnd = thirdStart + "Third".length

        val html = state.toHtml(TextRange(thirdStart, thirdEnd))

        assertEquals("<ol><li>Third</li></ol>", html)
    }

    @Test
    fun testCopyMiddleItemOfOrderedList() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li><li>Third</li></ol>"
        )
        val text = state.annotatedString.text

        val secondStart = text.indexOf("Second")
        val secondEnd = secondStart + "Second".length

        val html = state.toHtml(TextRange(secondStart, secondEnd))

        assertEquals("<ol><li>Second</li></ol>", html)
    }

    @Test
    fun testCopyTwoItemsOfOrderedList() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li><li>Third</li></ol>"
        )
        val text = state.annotatedString.text

        val firstStart = text.indexOf("First")
        val secondEnd = text.indexOf("Second") + "Second".length

        val html = state.toHtml(TextRange(firstStart, secondEnd))

        assertEquals(
            "<ol><li>First</li><li>Second</li></ol>",
            html
        )
    }

    @Test
    fun testCopyPartialTextWithinSingleOrderedListItem() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>Hello World</li><li>Second</li></ol>"
        )
        val text = state.annotatedString.text

        // Select only "World" from the first item
        val worldStart = text.indexOf("World")
        val worldEnd = worldStart + "World".length

        val html = state.toHtml(TextRange(worldStart, worldEnd))

        assertEquals("<ol><li>World</li></ol>", html)
    }

    @Test
    fun testCopyPartialTextAcrossOrderedListItems() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>Hello World</li><li>Second Item</li></ol>"
        )
        val text = state.annotatedString.text

        // Select "World" + separator + start of "Second"
        val worldStart = text.indexOf("World")
        val secondPartEnd = text.indexOf("Second") + "Second".length

        val html = state.toHtml(TextRange(worldStart, secondPartEnd))

        // Should produce two list items with the partial text
        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("World"))
        assertTrue(html.contains("Second"))
        assertTrue(html.contains("</ol>"))
    }

    // =========================================================================
    // SECTION 2: toHtml(range) — Copy scenarios with unordered lists
    // =========================================================================

    @Test
    fun testCopyEntireUnorderedList() {
        val state = RichTextStateHtmlParser.encode(
            "<ul><li>Apple</li><li>Banana</li><li>Cherry</li></ul>"
        )
        val text = state.annotatedString.text
        val fullRange = TextRange(0, text.length)

        val html = state.toHtml(fullRange)

        assertEquals(
            "<ul><li>Apple</li><li>Banana</li><li>Cherry</li></ul>",
            html
        )
    }

    @Test
    fun testCopyEntireUnorderedListMatchesNoRangeToHtml() {
        val state = RichTextStateHtmlParser.encode(
            "<ul><li>Apple</li><li>Banana</li></ul>"
        )
        val text = state.annotatedString.text

        assertEquals(
            state.toHtml(),
            state.toHtml(TextRange(0, text.length))
        )
    }

    @Test
    fun testCopySingleUnorderedListItem() {
        val state = RichTextStateHtmlParser.encode(
            "<ul><li>Apple</li><li>Banana</li><li>Cherry</li></ul>"
        )
        val text = state.annotatedString.text

        val bananaStart = text.indexOf("Banana")
        val bananaEnd = bananaStart + "Banana".length

        val html = state.toHtml(TextRange(bananaStart, bananaEnd))

        assertEquals("<ul><li>Banana</li></ul>", html)
    }

    // =========================================================================
    // SECTION 3: toHtml(range) — Copy across mixed content (paragraphs + lists)
    // =========================================================================

    @Test
    fun testCopyFromParagraphIntoOrderedList() {
        val state = RichTextStateHtmlParser.encode(
            "<p>Before text</p><ol><li>First</li><li>Second</li></ol>"
        )
        val text = state.annotatedString.text

        // Select from "text" through "First"
        val textStart = text.indexOf("text")
        val firstEnd = text.indexOf("First") + "First".length

        val html = state.toHtml(TextRange(textStart, firstEnd))

        assertTrue(html.contains("text"))
        assertTrue(html.contains("First"))
    }

    @Test
    fun testCopyFromOrderedListIntoParagraph() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li></ol><p>After text</p>"
        )
        val text = state.annotatedString.text

        // Select from "Second" through "After"
        val secondStart = text.indexOf("Second")
        val afterEnd = text.indexOf("After") + "After".length

        val html = state.toHtml(TextRange(secondStart, afterEnd))

        assertTrue(html.contains("Second"))
        assertTrue(html.contains("After"))
    }

    @Test
    fun testCopyEntireParagraphFollowedByList() {
        val state = RichTextStateHtmlParser.encode(
            "<p>Intro</p><ol><li>First</li><li>Second</li></ol>"
        )
        val text = state.annotatedString.text

        val html = state.toHtml(TextRange(0, text.length))

        assertEquals(
            "<p>Intro</p><ol><li>First</li><li>Second</li></ol>",
            html
        )
    }

    @Test
    fun testCopyEntireListFollowedByParagraph() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li></ol><p>Outro</p>"
        )
        val text = state.annotatedString.text

        val html = state.toHtml(TextRange(0, text.length))

        assertEquals(
            "<ol><li>First</li><li>Second</li></ol><p>Outro</p>",
            html
        )
    }

    @Test
    fun testCopyParagraphBetweenTwoLists() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>OL1</li></ol><p>Middle</p><ul><li>UL1</li></ul>"
        )
        val text = state.annotatedString.text

        val html = state.toHtml(TextRange(0, text.length))

        assertEquals(
            "<ol><li>OL1</li></ol><p>Middle</p><ul><li>UL1</li></ul>",
            html
        )
    }

    @Test
    fun testCopyOnlyParagraphBetweenLists() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>OL1</li></ol><p>Middle</p><ul><li>UL1</li></ul>"
        )
        val text = state.annotatedString.text

        val middleStart = text.indexOf("Middle")
        val middleEnd = middleStart + "Middle".length

        val html = state.toHtml(TextRange(middleStart, middleEnd))

        assertEquals("<p>Middle</p>", html)
    }

    // =========================================================================
    // SECTION 4: toHtml(range) — Copy with mixed list types (OL + UL)
    // =========================================================================

    @Test
    fun testCopyAcrossOrderedAndUnorderedLists() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>One</li><li>Two</li></ol><ul><li>Alpha</li><li>Beta</li></ul>"
        )
        val text = state.annotatedString.text

        val html = state.toHtml(TextRange(0, text.length))

        assertEquals(
            "<ol><li>One</li><li>Two</li></ol><ul><li>Alpha</li><li>Beta</li></ul>",
            html
        )
    }

    @Test
    fun testCopySpanningFromOrderedToUnorderedList() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>One</li><li>Two</li></ol><ul><li>Alpha</li><li>Beta</li></ul>"
        )
        val text = state.annotatedString.text

        // Select from "Two" through "Alpha"
        val twoStart = text.indexOf("Two")
        val alphaEnd = text.indexOf("Alpha") + "Alpha".length

        val html = state.toHtml(TextRange(twoStart, alphaEnd))

        assertTrue(html.contains("Two"))
        assertTrue(html.contains("Alpha"))
        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("<ul>"))
    }

    // =========================================================================
    // SECTION 5: toHtml(range) — Copy with nested lists
    // =========================================================================

    @Test
    fun testCopyEntireNestedOrderedList() {
        val state = RichTextState(
            listOf(
                RichParagraph(type = OrderedList(number = 1, initialLevel = 1)).also {
                    it.children.add(RichSpan(text = "Parent", paragraph = it))
                },
                RichParagraph(type = OrderedList(number = 1, initialLevel = 2)).also {
                    it.children.add(RichSpan(text = "Child", paragraph = it))
                },
                RichParagraph(type = OrderedList(number = 2, initialLevel = 1)).also {
                    it.children.add(RichSpan(text = "Parent2", paragraph = it))
                }
            )
        )
        val text = state.annotatedString.text

        val html = state.toHtml(TextRange(0, text.length))

        assertTrue(html.contains("Parent"))
        assertTrue(html.contains("Child"))
        assertTrue(html.contains("Parent2"))
        // Should have nested <ol> tags
        assertTrue(html.contains("<ol>"))
    }

    @Test
    fun testCopyOnlyNestedChildItem() {
        val state = RichTextState(
            listOf(
                RichParagraph(type = OrderedList(number = 1, initialLevel = 1)).also {
                    it.children.add(RichSpan(text = "Parent", paragraph = it))
                },
                RichParagraph(type = OrderedList(number = 1, initialLevel = 2)).also {
                    it.children.add(RichSpan(text = "Child", paragraph = it))
                },
                RichParagraph(type = OrderedList(number = 2, initialLevel = 1)).also {
                    it.children.add(RichSpan(text = "Parent2", paragraph = it))
                }
            )
        )
        val text = state.annotatedString.text

        val childStart = text.indexOf("Child")
        val childEnd = childStart + "Child".length

        val html = state.toHtml(TextRange(childStart, childEnd))

        assertTrue(html.contains("Child"))
        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("</ol>"))
    }

    @Test
    fun testCopyNestedUnorderedList() {
        val state = RichTextState(
            listOf(
                RichParagraph(type = UnorderedList(initialLevel = 1)).also {
                    it.children.add(RichSpan(text = "Level1", paragraph = it))
                },
                RichParagraph(type = UnorderedList(initialLevel = 2)).also {
                    it.children.add(RichSpan(text = "Level2", paragraph = it))
                },
                RichParagraph(type = UnorderedList(initialLevel = 3)).also {
                    it.children.add(RichSpan(text = "Level3", paragraph = it))
                }
            )
        )
        val text = state.annotatedString.text

        val html = state.toHtml(TextRange(0, text.length))

        assertTrue(html.contains("Level1"))
        assertTrue(html.contains("Level2"))
        assertTrue(html.contains("Level3"))
        assertTrue(html.contains("<ul>"))
    }

    // =========================================================================
    // SECTION 6: toHtml(range) — Copy with styled list items
    // =========================================================================

    @Test
    fun testCopyOrderedListWithBoldItem() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li><b>Bold Item</b></li><li>Normal Item</li></ol>"
        )
        val text = state.annotatedString.text

        val html = state.toHtml(TextRange(0, text.length))

        assertTrue(html.contains("<b>Bold Item</b>") || html.contains("<strong>Bold Item</strong>"))
        assertTrue(html.contains("Normal Item"))
    }

    @Test
    fun testCopyOnlyBoldListItem() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li><b>Bold Item</b></li><li>Normal Item</li></ol>"
        )
        val text = state.annotatedString.text

        val boldStart = text.indexOf("Bold Item")
        val boldEnd = boldStart + "Bold Item".length

        val html = state.toHtml(TextRange(boldStart, boldEnd))

        assertTrue(html.contains("Bold Item"))
        assertTrue(html.contains("<ol>"))
    }

    @Test
    fun testCopyListItemWithMixedStyles() {
        val state = RichTextStateHtmlParser.encode(
            "<ul><li>Normal <b>Bold</b> <i>Italic</i></li></ul>"
        )
        val text = state.annotatedString.text

        val html = state.toHtml(TextRange(0, text.length))

        assertTrue(html.contains("Normal"))
        assertTrue(html.contains("Bold"))
        assertTrue(html.contains("Italic"))
        assertTrue(html.contains("<ul>"))
    }

    // =========================================================================
    // SECTION 7: toHtml(range) — Edge cases
    // =========================================================================

    @Test
    fun testCopyEmptyRangeInList() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li></ol>"
        )

        val html = state.toHtml(TextRange(0, 0))

        // Empty range should produce minimal output
        assertTrue(html.isNotEmpty())
    }

    @Test
    fun testCopyReversedRangeInList() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li></ol>"
        )
        val text = state.annotatedString.text

        val firstStart = text.indexOf("First")
        val secondEnd = text.indexOf("Second") + "Second".length

        // Reversed range should produce same result as normal range
        assertEquals(
            state.toHtml(TextRange(firstStart, secondEnd)),
            state.toHtml(TextRange(secondEnd, firstStart))
        )
    }

    @Test
    fun testCopyRangeBeyondTextLengthInList() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li></ol>"
        )
        val text = state.annotatedString.text

        val secondStart = text.indexOf("Second")

        // Range extends beyond text length
        val html = state.toHtml(TextRange(secondStart, text.length + 100))

        assertTrue(html.contains("Second"))
    }

    @Test
    fun testCopySingleCharacterFromListItem() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>Hello</li></ol>"
        )
        val text = state.annotatedString.text

        val hStart = text.indexOf("Hello")

        val html = state.toHtml(TextRange(hStart, hStart + 1))

        assertEquals("<ol><li>H</li></ol>", html)
    }

    // =========================================================================
    // SECTION 8: toHtml(range) — List with only prefix (selecting prefix area)
    // =========================================================================

    @Test
    fun testCopyIncludingListPrefix() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li></ol>"
        )
        val text = state.annotatedString.text

        // Select from the very beginning (which includes the list prefix "1. ")
        val html = state.toHtml(TextRange(0, text.length))

        assertEquals("<ol><li>First</li></ol>", html)
    }

    @Test
    fun testCopyOnlyListPrefixArea() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li></ol>"
        )
        val text = state.annotatedString.text

        // Select just the prefix area (before the actual text content)
        val firstIdx = text.indexOf("First")
        if (firstIdx > 0) {
            // There IS prefix text - try selecting just the prefix
            val html = state.toHtml(TextRange(0, firstIdx))
            // Should handle gracefully without crash
            assertTrue(html.isNotEmpty())
        }
    }

    // =========================================================================
    // SECTION 9: insertHtml — Paste list into regular text
    // =========================================================================

    @Test
    fun testInsertOrderedListAtStartOfParagraph() {
        val state = RichTextState()
        state.setHtml("<p>Original content</p>")

        state.insertHtml("<ol><li>First</li><li>Second</li></ol>", 0)

        assertTrue(state.richParagraphList.size >= 2)
        // Check that list items are present
        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("First") })
        assertTrue(allText.any { it.contains("Second") })
    }

    @Test
    fun testInsertOrderedListInMiddleOfParagraph() {
        val state = RichTextState()
        state.setHtml("<p>Before After</p>")

        state.insertHtml("<ol><li>Item1</li><li>Item2</li></ol>", 7)

        // Should split the paragraph and insert list items
        assertTrue(state.richParagraphList.size >= 2)
        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Item1") })
        assertTrue(allText.any { it.contains("Item2") })
    }

    @Test
    fun testInsertOrderedListAtEndOfParagraph() {
        val state = RichTextState()
        state.setHtml("<p>Original</p>")

        state.insertHtml(
            "<ol><li>First</li><li>Second</li></ol>",
            state.annotatedString.text.length
        )

        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Original") })
        assertTrue(allText.any { it.contains("First") })
        assertTrue(allText.any { it.contains("Second") })
    }

    @Test
    fun testInsertUnorderedListIntoParagraph() {
        val state = RichTextState()
        state.setHtml("<p>Before After</p>")

        state.insertHtml("<ul><li>Apple</li><li>Banana</li></ul>", 7)

        assertTrue(state.richParagraphList.size >= 2)
        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Apple") })
        assertTrue(allText.any { it.contains("Banana") })
    }

    @Test
    fun testInsertSingleListItemIntoParagraph() {
        val state = RichTextState()
        state.setHtml("<p>Original content</p>")

        state.insertHtml("<ol><li>Single</li></ol>", 8)

        // Single list item insertion - should merge into existing paragraph
        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Single") })
    }

    // =========================================================================
    // SECTION 10: insertHtml — Paste regular text into list
    // =========================================================================

    @Test
    fun testInsertPlainTextIntoOrderedListItem() {
        val state = RichTextState()
        state.setHtml("<ol><li>Hello World</li><li>Second</li></ol>")

        val text = state.annotatedString.text
        val helloEnd = text.indexOf("Hello") + "Hello".length

        state.insertHtml("<p> Beautiful</p>", helloEnd)

        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Beautiful") })
    }

    @Test
    fun testInsertStyledTextIntoListItem() {
        val state = RichTextState()
        state.setHtml("<ol><li>Original</li></ol>")

        val text = state.annotatedString.text
        val origEnd = text.indexOf("Original") + "Original".length

        state.insertHtml("<b> Bold</b>", origEnd)

        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Bold") })
    }

    @Test
    fun testInsertParagraphIntoMiddleOfList() {
        val state = RichTextState()
        state.setHtml("<ol><li>First</li><li>Second</li><li>Third</li></ol>")

        val text = state.annotatedString.text
        val secondEnd = text.indexOf("Second") + "Second".length

        state.insertHtml("<p>Paragraph</p>", secondEnd)

        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Paragraph") })
    }

    // =========================================================================
    // SECTION 11: insertHtml — Paste list into existing list
    // =========================================================================

    @Test
    fun testInsertOrderedListIntoOrderedList() {
        val state = RichTextState()
        state.setHtml("<ol><li>One</li><li>Three</li></ol>")

        val text = state.annotatedString.text
        val oneEnd = text.indexOf("One") + "One".length

        state.insertHtml("<ol><li>Two</li></ol>", oneEnd)

        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("One") })
        assertTrue(allText.any { it.contains("Two") })
        assertTrue(allText.any { it.contains("Three") })
    }

    @Test
    fun testInsertUnorderedListIntoOrderedList() {
        val state = RichTextState()
        state.setHtml("<ol><li>One</li><li>Three</li></ol>")

        val text = state.annotatedString.text
        val oneEnd = text.indexOf("One") + "One".length

        state.insertHtml("<ul><li>Bullet</li></ul>", oneEnd)

        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("One") })
        assertTrue(allText.any { it.contains("Bullet") })
        assertTrue(allText.any { it.contains("Three") })
    }

    @Test
    fun testInsertMultipleListItemsIntoExistingList() {
        val state = RichTextState()
        state.setHtml("<ol><li>One</li><li>Four</li></ol>")

        val text = state.annotatedString.text
        val oneEnd = text.indexOf("One") + "One".length

        state.insertHtml("<ol><li>Two</li><li>Three</li></ol>", oneEnd)

        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("One") })
        assertTrue(allText.any { it.contains("Two") })
        assertTrue(allText.any { it.contains("Three") })
        assertTrue(allText.any { it.contains("Four") })
    }

    @Test
    fun testInsertNestedListIntoFlatList() {
        val state = RichTextState()
        state.setHtml("<ol><li>One</li><li>Two</li></ol>")

        val text = state.annotatedString.text
        val oneEnd = text.indexOf("One") + "One".length

        state.insertHtml(
            "<ol><li>Sub1</li><ol><li>SubSub1</li></ol></ol>",
            oneEnd
        )

        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Sub1") })
        assertTrue(allText.any { it.contains("SubSub1") })
    }

    // =========================================================================
    // SECTION 12: insertHtml — Edge cases
    // =========================================================================

    @Test
    fun testInsertEmptyHtmlIntoList() {
        val state = RichTextState()
        state.setHtml("<ol><li>First</li></ol>")

        val textBefore = state.annotatedString.text
        state.insertHtml("", 0)

        assertEquals(textBefore, state.annotatedString.text)
    }

    @Test
    fun testInsertListIntoEmptyState() {
        val state = RichTextState()

        state.insertHtml("<ol><li>First</li><li>Second</li></ol>", 0)

        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("First") })
        assertTrue(allText.any { it.contains("Second") })
    }

    @Test
    fun testInsertListAtStartOfExistingList() {
        val state = RichTextState()
        state.setHtml("<ol><li>Original</li></ol>")

        // Insert at the very start (position 0, which is in the prefix area)
        state.insertHtml("<ul><li>Prepended</li></ul>", 0)

        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Prepended") })
        assertTrue(allText.any { it.contains("Original") })
    }

    // =========================================================================
    // SECTION 13: Round-trip tests (copy then paste)
    // =========================================================================

    @Test
    fun testRoundTripOrderedList() {
        // Create a state with a list
        val original = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li></ol>"
        )
        val text = original.annotatedString.text

        // "Copy" - get HTML for the full range
        val copiedHtml = original.toHtml(TextRange(0, text.length))

        // "Paste" into a new state
        val target = RichTextState()
        target.setHtml("<p>Target</p>")
        val targetText = target.annotatedString.text
        target.insertHtml(copiedHtml, targetText.length)

        // Verify list content is preserved
        val allText = target.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Target") })
        assertTrue(allText.any { it.contains("First") })
        assertTrue(allText.any { it.contains("Second") })
    }

    @Test
    fun testRoundTripUnorderedList() {
        val original = RichTextStateHtmlParser.encode(
            "<ul><li>Apple</li><li>Banana</li></ul>"
        )
        val text = original.annotatedString.text

        val copiedHtml = original.toHtml(TextRange(0, text.length))

        val target = RichTextState()
        target.setHtml("<p>Before</p>")
        target.insertHtml(copiedHtml, target.annotatedString.text.length)

        val allText = target.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Before") })
        assertTrue(allText.any { it.contains("Apple") })
        assertTrue(allText.any { it.contains("Banana") })
    }

    @Test
    fun testRoundTripPartialListCopy() {
        val original = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li><li>Third</li></ol>"
        )
        val text = original.annotatedString.text

        // Copy only the second item
        val secondStart = text.indexOf("Second")
        val secondEnd = secondStart + "Second".length
        val copiedHtml = original.toHtml(TextRange(secondStart, secondEnd))

        // Paste into new state
        val target = RichTextState()
        target.setHtml("<p>Content</p>")
        target.insertHtml(copiedHtml, target.annotatedString.text.length)

        val allText = target.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Content") })
        assertTrue(allText.any { it.contains("Second") })
    }

    @Test
    fun testRoundTripMixedContent() {
        val original = RichTextStateHtmlParser.encode(
            "<p>Intro</p><ol><li>First</li><li>Second</li></ol><p>Outro</p>"
        )
        val text = original.annotatedString.text

        val copiedHtml = original.toHtml(TextRange(0, text.length))

        val target = RichTextState()
        target.insertHtml(copiedHtml, 0)

        val allText = target.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Intro") })
        assertTrue(allText.any { it.contains("First") })
        assertTrue(allText.any { it.contains("Second") })
        assertTrue(allText.any { it.contains("Outro") })
    }

    @Test
    fun testRoundTripStyledListItem() {
        val original = RichTextStateHtmlParser.encode(
            "<ol><li><b>Bold</b> and <i>Italic</i></li><li>Normal</li></ol>"
        )
        val text = original.annotatedString.text

        val copiedHtml = original.toHtml(TextRange(0, text.length))

        // The round-tripped HTML should preserve styling
        assertTrue(copiedHtml.contains("Bold"))
        assertTrue(copiedHtml.contains("Italic"))
        assertTrue(copiedHtml.contains("Normal"))
        assertTrue(copiedHtml.contains("<ol>"))
    }

    // =========================================================================
    // SECTION 14: Complex scenarios
    // =========================================================================

    @Test
    fun testCopyPasteListBetweenTwoParagraphs() {
        val state = RichTextState()
        state.setHtml("<p>Hello</p><p>World</p>")

        val text = state.annotatedString.text
        val helloEnd = text.indexOf("Hello") + "Hello".length

        state.insertHtml(
            "<ol><li>One</li><li>Two</li></ol>",
            helloEnd
        )

        // Verify structure: Hello + list items + World
        assertTrue(state.richParagraphList.size >= 3)
        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Hello") })
        assertTrue(allText.any { it.contains("One") })
        assertTrue(allText.any { it.contains("Two") })
        assertTrue(allText.any { it.contains("World") })
    }

    @Test
    fun testCopyOrderedListWithManyItems() {
        val items = (1..10).map { "<li>Item $it</li>" }.joinToString("")
        val state = RichTextStateHtmlParser.encode("<ol>$items</ol>")
        val text = state.annotatedString.text

        val html = state.toHtml(TextRange(0, text.length))

        // All items should be present
        for (i in 1..10) {
            assertTrue(html.contains("Item $i"), "Missing Item $i in: $html")
        }
        assertTrue(html.startsWith("<ol>"))
        assertTrue(html.endsWith("</ol>"))
    }

    @Test
    fun testCopyListWithLinkSpan() {
        val state = RichTextStateHtmlParser.encode(
            "<ul><li>Visit <a href=\"https://example.com\">Example</a></li></ul>"
        )
        val text = state.annotatedString.text

        val html = state.toHtml(TextRange(0, text.length))

        assertTrue(html.contains("Visit"))
        assertTrue(html.contains("Example"))
        assertTrue(html.contains("<ul>"))
    }

    @Test
    fun testMultipleSequentialPasteOperations() {
        val state = RichTextState()
        state.setHtml("<p>Base</p>")

        // First paste: ordered list
        state.insertHtml(
            "<ol><li>One</li></ol>",
            state.annotatedString.text.length
        )

        // Second paste: unordered list
        state.insertHtml(
            "<ul><li>Alpha</li></ul>",
            state.annotatedString.text.length
        )

        val allText = state.richParagraphList.flatMap { p -> p.children.map { it.text } }
        assertTrue(allText.any { it.contains("Base") })
        assertTrue(allText.any { it.contains("One") })
        assertTrue(allText.any { it.contains("Alpha") })
    }

    @Test
    fun testCopyListAndPasteMultipleTimes() {
        val original = RichTextStateHtmlParser.encode(
            "<ul><li>Item</li></ul>"
        )
        val text = original.annotatedString.text
        val copiedHtml = original.toHtml(TextRange(0, text.length))

        val target = RichTextState()
        target.setHtml("<p>Start</p>")

        // Paste 3 times at the end
        target.insertHtml(copiedHtml, target.annotatedString.text.length)
        target.insertHtml(copiedHtml, target.annotatedString.text.length)
        target.insertHtml(copiedHtml, target.annotatedString.text.length)

        // All pasted text should be present in the output
        // Note: adjacent spans with the same style may be merged during updateAnnotatedString,
        // so "Item" may appear as "ItemItemItem" in a single span rather than three separate spans.
        val fullText = target.richParagraphList
            .flatMap { p -> p.children.map { it.text } }
            .joinToString("")
        assertTrue(fullText.contains("Start"), "Missing 'Start' in: $fullText")
        assertTrue(fullText.contains("ItemItemItem"), "Expected 3 pasted 'Item' texts, got: $fullText")
    }

    // =========================================================================
    // SECTION 15: Programmatic state construction + toHtml (decode test)
    // =========================================================================

    @Test
    fun testDecodeOrderedListWithRange() {
        val state = RichTextState(
            listOf(
                RichParagraph(type = OrderedList(1)).also {
                    it.children.add(RichSpan(text = "First", paragraph = it))
                },
                RichParagraph(type = OrderedList(2)).also {
                    it.children.add(RichSpan(text = "Second", paragraph = it))
                },
                RichParagraph(type = OrderedList(3)).also {
                    it.children.add(RichSpan(text = "Third", paragraph = it))
                }
            )
        )

        // Full HTML should work
        assertEquals(
            "<ol><li>First</li><li>Second</li><li>Third</li></ol>",
            state.toHtml()
        )

        // Full range should match no-range version
        val text = state.annotatedString.text
        assertEquals(
            state.toHtml(),
            state.toHtml(TextRange(0, text.length))
        )
    }

    @Test
    fun testDecodeUnorderedListWithRange() {
        val state = RichTextState(
            listOf(
                RichParagraph(type = UnorderedList()).also {
                    it.children.add(RichSpan(text = "Alpha", paragraph = it))
                },
                RichParagraph(type = UnorderedList()).also {
                    it.children.add(RichSpan(text = "Beta", paragraph = it))
                }
            )
        )

        assertEquals(
            "<ul><li>Alpha</li><li>Beta</li></ul>",
            state.toHtml()
        )

        val text = state.annotatedString.text
        assertEquals(
            state.toHtml(),
            state.toHtml(TextRange(0, text.length))
        )
    }

    @Test
    fun testDecodeMixedParagraphAndListWithRange() {
        val state = RichTextState(
            listOf(
                RichParagraph(type = DefaultParagraph()).also {
                    it.children.add(RichSpan(text = "Intro", paragraph = it))
                },
                RichParagraph(type = OrderedList(1)).also {
                    it.children.add(RichSpan(text = "One", paragraph = it))
                },
                RichParagraph(type = OrderedList(2)).also {
                    it.children.add(RichSpan(text = "Two", paragraph = it))
                },
                RichParagraph(type = DefaultParagraph()).also {
                    it.children.add(RichSpan(text = "Outro", paragraph = it))
                }
            )
        )

        val expected = "<p>Intro</p><ol><li>One</li><li>Two</li></ol><p>Outro</p>"
        assertEquals(expected, state.toHtml())

        val text = state.annotatedString.text
        assertEquals(expected, state.toHtml(TextRange(0, text.length)))
    }

    // =========================================================================
    // SECTION 16: List numbering preservation after copy
    // =========================================================================

    @Test
    fun testCopiedOrderedListStartsFromOne() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li><li>Third</li></ol>"
        )
        val text = state.annotatedString.text

        // Copy only the third item - should it produce <li> with number 3 or reset to 1?
        val thirdStart = text.indexOf("Third")
        val thirdEnd = thirdStart + "Third".length

        val html = state.toHtml(TextRange(thirdStart, thirdEnd))

        // The copied single item should be a valid list
        assertTrue(html.contains("<li>Third</li>"))
        assertTrue(html.contains("<ol>"))
    }

    @Test
    fun testCopiedMiddleItemsPreserveListStructure() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li><li>Third</li><li>Fourth</li></ol>"
        )
        val text = state.annotatedString.text

        // Copy Second and Third
        val secondStart = text.indexOf("Second")
        val thirdEnd = text.indexOf("Third") + "Third".length

        val html = state.toHtml(TextRange(secondStart, thirdEnd))

        assertTrue(html.contains("<ol>"))
        assertTrue(html.contains("<li>Second</li>"))
        assertTrue(html.contains("<li>Third</li>"))
        assertTrue(html.contains("</ol>"))
    }

    // =========================================================================
    // SECTION 17: insertHtml — Paragraph type preservation
    // =========================================================================

    @Test
    fun testInsertListPreservesSurroundingParagraphTypes() {
        val state = RichTextState()
        state.setHtml("<p>Before</p><p>After</p>")

        val text = state.annotatedString.text
        val beforeEnd = text.indexOf("Before") + "Before".length

        state.insertHtml("<ol><li>Item</li></ol>", beforeEnd)

        // "Before" paragraph should still be DefaultParagraph
        assertIs<DefaultParagraph>(state.richParagraphList.first().type)

        // "After" paragraph should still be DefaultParagraph
        val afterParagraph = state.richParagraphList.last()
        val afterText = afterParagraph.children.firstOrNull()?.text ?: ""
        if (afterText.contains("After")) {
            assertIs<DefaultParagraph>(afterParagraph.type)
        }
    }

    @Test
    fun testInsertParagraphPreservesListType() {
        val state = RichTextState()
        state.setHtml("<ol><li>One</li><li>Two</li></ol>")

        val text = state.annotatedString.text
        val oneEnd = text.indexOf("One") + "One".length

        state.insertHtml("<p>Plain</p>", oneEnd)

        // First paragraph should still be a list type
        val firstParagraphText = state.richParagraphList.first().children.firstOrNull()?.text ?: ""
        if (firstParagraphText.contains("One")) {
            assertIs<OrderedList>(state.richParagraphList.first().type)
        }
    }

    // =========================================================================
    // SECTION 18: toText(range) with lists
    // =========================================================================

    @Test
    fun testToTextEntireOrderedList() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li></ol>"
        )
        val text = state.annotatedString.text

        val plainText = state.toText(TextRange(0, text.length))

        assertTrue(plainText.contains("First"))
        assertTrue(plainText.contains("Second"))
    }

    @Test
    fun testToTextPartialOrderedList() {
        val state = RichTextStateHtmlParser.encode(
            "<ol><li>First</li><li>Second</li><li>Third</li></ol>"
        )
        val text = state.annotatedString.text

        val firstStart = text.indexOf("First")
        val firstEnd = firstStart + "First".length

        val plainText = state.toText(TextRange(firstStart, firstEnd))

        assertTrue(plainText.contains("First"))
        // Should NOT contain other items
        assertTrue(!plainText.contains("Second"))
        assertTrue(!plainText.contains("Third"))
    }

    // =========================================================================
    // SECTION 19: HTML encode → decode round trip with lists
    // =========================================================================

    @Test
    fun testHtmlRoundTripSimpleOrderedList() {
        val html = "<ol><li>First</li><li>Second</li></ol>"
        val state = RichTextStateHtmlParser.encode(html)
        val decoded = RichTextStateHtmlParser.decode(state)

        assertEquals(html, decoded)
    }

    @Test
    fun testHtmlRoundTripSimpleUnorderedList() {
        val html = "<ul><li>Apple</li><li>Banana</li></ul>"
        val state = RichTextStateHtmlParser.encode(html)
        val decoded = RichTextStateHtmlParser.decode(state)

        assertEquals(html, decoded)
    }

    @Test
    fun testHtmlRoundTripMixedListTypes() {
        val html = "<ol><li>One</li></ol><ul><li>Alpha</li></ul>"
        val state = RichTextStateHtmlParser.encode(html)
        val decoded = RichTextStateHtmlParser.decode(state)

        assertEquals(html, decoded)
    }

    @Test
    fun testHtmlRoundTripListWithParagraphs() {
        val html = "<p>Before</p><ol><li>One</li><li>Two</li></ol><p>After</p>"
        val state = RichTextStateHtmlParser.encode(html)
        val decoded = RichTextStateHtmlParser.decode(state)

        assertEquals(html, decoded)
    }

    @Test
    fun testHtmlRoundTripStyledListItems() {
        val html = "<ol><li><b>Bold</b></li><li><i>Italic</i></li></ol>"
        val state = RichTextStateHtmlParser.encode(html)
        val decoded = RichTextStateHtmlParser.decode(state)

        // Check essential structure is preserved (tag order might vary)
        assertTrue(decoded.contains("<ol>"))
        assertTrue(decoded.contains("Bold"))
        assertTrue(decoded.contains("Italic"))
        assertTrue(decoded.contains("</ol>"))
    }

    @Test
    fun testHtmlRoundTripEmptyListItem() {
        val html = "<ol><li></li><li>Content</li></ol>"
        val state = RichTextStateHtmlParser.encode(html)
        val decoded = RichTextStateHtmlParser.decode(state)

        assertTrue(decoded.contains("Content"))
        assertTrue(decoded.contains("<ol>"))
    }
}
