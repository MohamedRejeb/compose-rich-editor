package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression test for issue #635.
 *
 * By design, a span's background color is masked with [Color.Transparent] under
 * the live selection so the system selection highlight stays visible. Once the
 * selection clears the background must become visible again - including when
 * the user only clicks elsewhere to collapse the selection, without typing.
 *
 * Before the fix, [RichTextState.updateTextFieldValue] took a fast path on
 * pure selection changes that skipped [RichTextState.updateAnnotatedString],
 * leaving the stale mask baked into the annotated string. The background only
 * reappeared after the next edit (which is what #635 observed).
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue635BackgroundOnSelectionTest {

    private fun stateWithText(text: String): RichTextState =
        RichTextState(
            listOf(
                RichParagraph(key = 1).also { paragraph ->
                    paragraph.children.add(
                        RichSpan(text = text, paragraph = paragraph),
                    )
                }
            )
        )

    private fun backgroundAt(state: RichTextState, offset: Int): Color {
        var background: Color = Color.Unspecified
        state.annotatedString.spanStyles
            .filter { offset in it.start until it.end }
            .forEach { range ->
                if (range.item.background != Color.Unspecified) {
                    background = range.item.background
                }
            }
        return background
    }

    @Test
    fun backgroundIsMaskedWhileSelectionCoversIt() {
        val state = stateWithText("Hello World")

        state.selection = TextRange(0, 5)
        state.addSpanStyle(SpanStyle(background = Color.Yellow))

        // Selection still active over the painted range - mask should render
        // the background as transparent so the selection highlight is visible.
        assertEquals(
            Color.Transparent,
            backgroundAt(state, offset = 2),
            "background must be masked with Transparent under the live selection",
        )
    }

    @Test
    fun backgroundReappearsWhenSelectionCollapsesWithoutTyping() {
        val state = stateWithText("Hello World")

        state.selection = TextRange(0, 5)
        state.addSpanStyle(SpanStyle(background = Color.Yellow))

        // Collapse the selection without editing the text (simulates a click
        // elsewhere / caret move). The mask must be recomputed so the real
        // yellow background is now visible.
        state.onTextFieldValueChange(
            TextFieldValue(
                text = state.annotatedString.text,
                selection = TextRange(2),
            )
        )

        assertEquals(
            Color.Yellow,
            backgroundAt(state, offset = 2),
            "background must become visible once the selection collapses",
        )
    }

    @Test
    fun backgroundReappearsWhenSelectionMovesAwayFromPaintedRange() {
        val state = stateWithText("Hello World")

        state.selection = TextRange(0, 5)
        state.addSpanStyle(SpanStyle(background = Color.Yellow))

        // Move the selection entirely off the painted range - still non-collapsed
        // but no longer intersecting "Hello", so the mask should no longer hide it.
        state.onTextFieldValueChange(
            TextFieldValue(
                text = state.annotatedString.text,
                selection = TextRange(6, 11),
            )
        )

        assertEquals(
            Color.Yellow,
            backgroundAt(state, offset = 2),
            "background must be visible on ranges outside the new selection",
        )
    }
}
