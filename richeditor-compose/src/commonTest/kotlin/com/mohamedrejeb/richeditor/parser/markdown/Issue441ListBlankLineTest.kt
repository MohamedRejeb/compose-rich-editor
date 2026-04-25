package com.mohamedrejeb.richeditor.parser.markdown

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

/**
 * Reproduces https://github.com/MohamedRejeb/compose-rich-editor/issues/441
 *
 * When a paragraph of plain text is followed by a list, the markdown emitted
 * by [RichTextState.toMarkdown] glues the list directly under the text:
 *
 *     Text
 *     -
 *     - item
 *
 * CommonMark interprets "Text\n-" as a setext H2 (the `-` underline rule),
 * turning the paragraph into a heading and dropping the list. The fix is to
 * insert a blank line between the text paragraph and the list.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue441ListBlankLineTest {

    @Test
    fun textFollowedByEmptyBulletThenItem_producesBlankSeparator() {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(type = DefaultParagraph()).also { paragraph ->
                    paragraph.children.add(
                        RichSpan(text = "Text", paragraph = paragraph)
                    )
                },
                RichParagraph(type = UnorderedList()),
                RichParagraph(type = UnorderedList()).also { paragraph ->
                    paragraph.children.add(
                        RichSpan(text = "item", paragraph = paragraph)
                    )
                },
            )
        )

        val markdown = state.toMarkdown()

        // CommonMark setext-heading test: any `-` directly under non-empty text
        // converts it into a heading. The fix should insert a blank line.
        val lines = markdown.split('\n')
        val textIndex = lines.indexOfFirst { it.trimEnd() == "Text" }
        assertTrue(textIndex >= 0, "Text line not found in:\n$markdown")
        val nextLine = lines.getOrNull(textIndex + 1)
        assertEquals(
            "",
            nextLine?.trimEnd(),
            "Expected blank line after `Text` before list, got `$nextLine`. Full markdown:\n$markdown"
        )
    }

    @Test
    fun textFollowedByListItem_producesBlankSeparator() {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(type = DefaultParagraph()).also { paragraph ->
                    paragraph.children.add(
                        RichSpan(text = "Text", paragraph = paragraph)
                    )
                },
                RichParagraph(type = UnorderedList()).also { paragraph ->
                    paragraph.children.add(
                        RichSpan(text = "item", paragraph = paragraph)
                    )
                },
            )
        )

        val markdown = state.toMarkdown()

        val lines = markdown.split('\n')
        val textIndex = lines.indexOfFirst { it.trimEnd() == "Text" }
        assertTrue(textIndex >= 0, "Text line not found in:\n$markdown")
        val nextLine = lines.getOrNull(textIndex + 1)
        assertEquals(
            "",
            nextLine?.trimEnd(),
            "Expected blank line after `Text` before list, got `$nextLine`. Full markdown:\n$markdown"
        )
    }

    @Test
    fun textFollowedByOrderedList_producesBlankSeparator() {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(type = DefaultParagraph()).also { paragraph ->
                    paragraph.children.add(
                        RichSpan(text = "Text", paragraph = paragraph)
                    )
                },
                RichParagraph(type = OrderedList(number = 1)).also { paragraph ->
                    paragraph.children.add(
                        RichSpan(text = "item", paragraph = paragraph)
                    )
                },
            )
        )

        val markdown = state.toMarkdown()

        val lines = markdown.split('\n')
        val textIndex = lines.indexOfFirst { it.trimEnd() == "Text" }
        assertTrue(textIndex >= 0, "Text line not found in:\n$markdown")
        val nextLine = lines.getOrNull(textIndex + 1)
        assertEquals(
            "",
            nextLine?.trimEnd(),
            "Expected blank line after `Text` before list, got `$nextLine`. Full markdown:\n$markdown"
        )
    }
}
