package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression pin for a crash in handleRemovingStyleFromRichSpan found by the JSON
 * round-trip fuzzer: when a style toggle removes styling from a span whose previous
 * sibling is not a direct child of the containing list, indexOf returns -1 and the
 * unguarded removeAt(-1) threw IndexOutOfBoundsException.
 *
 * The crash needs a specific nesting shape (heading + background + code span + partial
 * color toggle on a list item), reproduced here by replaying the exact seeded edit
 * sequence that found it (seed 1013, ops: ul, ol, setText, h2, bg 4..9, code 3..9,
 * color 2..5).
 */
class ToggleStyleNestedSpanRemovalCrashTest {

    @Test
    fun `seeded edit sequence with heading, code span, and style toggles does not crash`() {
        val random = Random(1_013L)
        val state = RichTextState()
        repeat(20) { applyOperation(state, random) }
        assertTrue(state.annotatedString.text.isNotEmpty())
    }

    private fun applyOperation(state: RichTextState, random: Random) {
        val length = state.annotatedString.text.length
        when (random.nextInt(12)) {
            0 -> state.setText(state.annotatedString.text + " word${random.nextInt(100)}")
            1 -> if (length > 1) {
                state.selection = randomRange(random, length)
                state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            }
            2 -> if (length > 1) {
                state.selection = randomRange(random, length)
                state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
            }
            3 -> state.toggleOrderedList()
            4 -> state.toggleUnorderedList()
            5 -> if (length > 1) {
                state.selection = randomRange(random, length)
                state.addLinkToSelection("https://example.com/${random.nextInt(10)}")
            }
            6 -> if (length > 1) {
                state.selection = randomRange(random, length)
                state.toggleCodeSpan()
            }
            7 -> state.setHeadingStyle(HeadingStyle.fromLevel(random.nextInt(7)))
            8 -> if (length > 1) {
                state.selection = randomRange(random, length)
                state.toggleSpanStyle(SpanStyle(fontSize = (12 + random.nextInt(3) * 8).sp))
            }
            9 -> if (length > 1) {
                state.selection = randomRange(random, length)
                state.toggleSpanStyle(SpanStyle(color = if (random.nextBoolean()) Color.Red else Color.Blue))
            }
            10 -> if (length > 1) {
                state.selection = randomRange(random, length)
                state.toggleSpanStyle(SpanStyle(background = Color.Yellow))
            }
            11 -> if (length > 1) {
                state.selection = randomRange(random, length)
                state.toggleSpanStyle(SpanStyle(letterSpacing = 2.sp))
            }
        }
    }

    private fun randomRange(random: Random, length: Int): TextRange {
        val a = random.nextInt(length)
        val b = random.nextInt(length)
        return TextRange(minOf(a, b), maxOf(a, b) + 1)
    }
}
