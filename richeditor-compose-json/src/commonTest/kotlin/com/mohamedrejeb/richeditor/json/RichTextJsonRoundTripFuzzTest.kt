package com.mohamedrejeb.richeditor.json

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Fuzz oracles:
 * 1. toJson never throws on any reachable state.
 * 2. setJson(toJson()) preserves visible text.
 * 3. toJson composed with setJson is idempotent (canonical form).
 */
class RichTextJsonRoundTripFuzzTest {

    @Test
    fun `random edit sequences round-trip through json`() {
        repeat(50) { iteration ->
            val seed = 1_000L + iteration
            val random = Random(seed)
            val state = RichTextState()
            repeat(20) { applyRandomOperation(state, random) }

            val json = state.toJson()
            val restored = RichTextState().setJson(json)
            assertEquals(state.toText(), restored.toText(), "Text lost (seed $seed)")
            assertEquals(json, restored.toJson(), "Not idempotent (seed $seed)")
        }
    }

    private fun applyRandomOperation(state: RichTextState, random: Random) {
        val length = state.annotatedString.text.length
        when (random.nextInt(7)) {
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
        }
    }

    private fun randomRange(random: Random, length: Int): TextRange {
        val a = random.nextInt(length)
        val b = random.nextInt(length)
        return TextRange(minOf(a, b), maxOf(a, b) + 1)
    }
}
