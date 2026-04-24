package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.type.ConfigurableStartTextWidth
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType.Companion.startText
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for the list-prefix flicker.
 *
 * Newly-created [UnorderedList] / [OrderedList] paragraph types start with
 * `startTextWidth = 0.sp`. Without a cache, every render path that creates
 * fresh paragraph types (setMarkdown / setHtml / toggle list / paste) renders
 * one frame with the wrong `TextIndent` before `onTextLayout` measures the
 * prefix and corrects it. The cache stores measured widths keyed by the
 * prefix string so the next paragraph with the same prefix renders correctly
 * on the first frame.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextStateListPrefixWidthCacheTest {

    @Test
    fun setMarkdown_appliesCachedPrefixWidthToNewParagraphs() {
        val state = RichTextState()

        state.setMarkdown("- one")
        val firstPara = state.richParagraphList[0].type
        require(firstPara is ConfigurableStartTextWidth)

        // Simulate the result of `adjustRichParagraphLayout` having measured
        // the bullet width on first render.
        firstPara.startTextWidth = 12.sp
        state.startTextWidthCache[firstPara.startText] = 12.sp

        // Streaming-style replacement — same prefix, more items.
        state.setMarkdown("- one\n- two\n- three")

        state.richParagraphList.forEachIndexed { index, paragraph ->
            val type = paragraph.type
            assertTrue(type is ConfigurableStartTextWidth, "Paragraph $index should be a list")
            assertEquals(
                expected = 12.sp,
                actual = type.startTextWidth,
                message = "Paragraph $index should have inherited the cached prefix width",
            )
        }
    }

    @Test
    fun toggleList_appliesCachedPrefixWidthToNewParagraph() {
        val state = RichTextState()
        state.setText("first")

        // First toggle — measure as if onTextLayout had run.
        state.toggleUnorderedList()
        val firstList = state.richParagraphList[0].type
        require(firstList is ConfigurableStartTextWidth)
        firstList.startTextWidth = 12.sp
        state.startTextWidthCache[firstList.startText] = 12.sp

        // Toggle off, type a second paragraph, toggle on again.
        state.toggleUnorderedList()
        state.addTextAfterSelection("\nsecond")
        state.toggleUnorderedList()

        val secondList = state.richParagraphList.last().type
        require(secondList is ConfigurableStartTextWidth)
        assertEquals(
            expected = 12.sp,
            actual = secondList.startTextWidth,
            message = "Toggling another list with the same prefix should pre-fill from cache",
        )
    }

    @Test
    fun setMarkdown_skipsCacheForChangedPrefix() {
        val state = RichTextState()

        state.setMarkdown("1. one")
        val firstOl = state.richParagraphList[0].type
        require(firstOl is OrderedList)
        firstOl.startTextWidth = 14.sp
        state.startTextWidthCache[firstOl.startText] = 14.sp

        // Switch to an unordered list — different prefix string, cache miss expected.
        state.setMarkdown("- one")
        val unordered = state.richParagraphList[0].type
        require(unordered is UnorderedList)
        assertEquals(
            expected = 0f,
            actual = unordered.startTextWidth.value,
            message = "Different prefix string should not pull from a stale ordered-list cache entry",
        )
    }

    @Test
    fun adjustLayout_writesPrefixWidthToCache() {
        val state = RichTextState()

        // Manually pretend the adjust pass ran and stored a width.
        state.recordMeasuredPrefixWidthForTest("• ", 11.sp)

        // Cache value should be queryable.
        assertEquals(11.sp, state.startTextWidthCache["• "])

        // Subsequent setMarkdown picks it up automatically.
        state.setMarkdown("- one")
        val type = state.richParagraphList[0].type
        require(type is ConfigurableStartTextWidth)
        assertEquals(11.sp, type.startTextWidth)
    }
}
