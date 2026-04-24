package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.ListMarkerStyleBehavior
import com.mohamedrejeb.richeditor.paragraph.type.ParagraphType.Companion.startText
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Regression tests for issue #668 - list markers should not inherit every
 * inline style from the first span of the list item.
 *
 * Default ([ListMarkerStyleBehavior.InheritFromText]):
 *   - Keep: color, fontSize, fontWeight, fontStyle, fontFamily, letterSpacing
 *   - Strip: textDecoration, background, baselineShift, shadow, textGeometricTransform
 *
 * [ListMarkerStyleBehavior.AlwaysDefault] strips everything - useful for editors
 * that want every bullet identical regardless of content.
 */
@OptIn(ExperimentalRichTextApi::class)
class ListMarkerStyleBehaviorTest {

    private fun listState(spanStyle: SpanStyle): RichTextState =
        RichTextState(
            listOf(
                RichParagraph(type = UnorderedList()).also { paragraph ->
                    paragraph.children.add(
                        RichSpan(
                            text = "Item",
                            paragraph = paragraph,
                            spanStyle = spanStyle,
                        )
                    )
                }
            )
        )

    /**
     * Return the style actually applied to the paragraph's marker ("• ")
     * in the rendered annotated string. The marker is at offset 0 with
     * length equal to startText.length.
     */
    private fun markerStyleOf(state: RichTextState): SpanStyle {
        val markerLength = state.richParagraphList.first().type.startText.length
        assertTrue(markerLength > 0, "expected non-empty marker")
        val markerRange = TextRange(0, markerLength)
        val matching = state.annotatedString.spanStyles
            .filter { it.start <= markerRange.min && it.end >= markerRange.max }
            .fold(SpanStyle()) { acc, range -> acc.merge(range.item) }
        return matching
    }

    // --- InheritFromText defaults ---

    @Test
    fun markerInheritsBoldByDefault() {
        val state = listState(SpanStyle(fontWeight = FontWeight.Bold))
        assertEquals(FontWeight.Bold, markerStyleOf(state).fontWeight)
    }

    @Test
    fun markerInheritsItalicByDefault() {
        val state = listState(SpanStyle(fontStyle = FontStyle.Italic))
        assertEquals(FontStyle.Italic, markerStyleOf(state).fontStyle)
    }

    @Test
    fun markerInheritsColorByDefault() {
        val state = listState(SpanStyle(color = Color.Red))
        assertEquals(Color.Red, markerStyleOf(state).color)
    }

    @Test
    fun markerInheritsFontSizeByDefault() {
        val state = listState(SpanStyle(fontSize = 24.sp))
        assertEquals(24.sp, markerStyleOf(state).fontSize)
    }

    @Test
    fun markerDoesNotInheritUnderline() {
        val state = listState(SpanStyle(textDecoration = TextDecoration.Underline))
        assertNull(markerStyleOf(state).textDecoration)
    }

    @Test
    fun markerDoesNotInheritStrikethrough() {
        val state = listState(SpanStyle(textDecoration = TextDecoration.LineThrough))
        assertNull(markerStyleOf(state).textDecoration)
    }

    @Test
    fun markerDoesNotInheritBackground() {
        val state = listState(SpanStyle(background = Color.Yellow))
        assertEquals(Color.Unspecified, markerStyleOf(state).background)
    }

    @Test
    fun markerDoesNotInheritBaselineShift() {
        val state = listState(SpanStyle(baselineShift = BaselineShift.Superscript))
        assertNull(markerStyleOf(state).baselineShift)
    }

    @Test
    fun markerKeepsBoldButNotUnderlineWhenBothApplied() {
        val state = listState(
            SpanStyle(
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
            )
        )
        val style = markerStyleOf(state)
        assertEquals(FontWeight.Bold, style.fontWeight)
        assertNull(style.textDecoration)
    }

    // --- AlwaysDefault strips everything ---

    @Test
    fun alwaysDefaultDropsBold() {
        val state = listState(SpanStyle(fontWeight = FontWeight.Bold))
        state.config.listMarkerStyleBehavior = ListMarkerStyleBehavior.AlwaysDefault
        assertNull(markerStyleOf(state).fontWeight)
    }

    @Test
    fun alwaysDefaultDropsColorAndFontSize() {
        val state = listState(SpanStyle(color = Color.Red, fontSize = 30.sp))
        state.config.listMarkerStyleBehavior = ListMarkerStyleBehavior.AlwaysDefault
        val style = markerStyleOf(state)
        assertEquals(Color.Unspecified, style.color)
        assertEquals(androidx.compose.ui.unit.TextUnit.Unspecified, style.fontSize)
    }

    @Test
    fun switchingBehaviorUpdatesMarker() {
        val state = listState(SpanStyle(fontWeight = FontWeight.Bold))

        // Default: marker is bold.
        assertEquals(FontWeight.Bold, markerStyleOf(state).fontWeight)

        // Switch to AlwaysDefault: marker loses bold.
        state.config.listMarkerStyleBehavior = ListMarkerStyleBehavior.AlwaysDefault
        assertNull(markerStyleOf(state).fontWeight)

        // Switch back: marker regains bold.
        state.config.listMarkerStyleBehavior = ListMarkerStyleBehavior.InheritFromText
        assertEquals(FontWeight.Bold, markerStyleOf(state).fontWeight)
    }
}
