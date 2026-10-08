package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.type.ConfigurableStartTextWidth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A read-only view that shows streamed Markdown gets new content on every chunk, often in a
 * state that has never been laid out. Its list items must sit at their final indent on the
 * first frame: a frame at the unmeasured indent followed by a corrected one makes every item
 * jump sideways on each chunk.
 */
@OptIn(ExperimentalTestApi::class)
class StreamedListIndentStabilityTest {

    private val style = TextStyle(fontSize = 16.sp)

    private val bulletChunks = listOf("- one", "- one\n- two", "- one\n- two\n- three and more")

    private val numberedChunks = listOf(
        "1. a",
        (1..9).joinToString("\n") { "$it. item" },
        (1..10).joinToString("\n") { "$it. item" },
    )

    /**
     * Streams [chunks] into a view and returns, for every text layout, where the text of the
     * first list item starts. [newStatePerChunk] rebuilds the state for each chunk instead of
     * calling `setMarkdown` on the same one.
     */
    private fun firstItemTextStarts(
        chunks: List<String>,
        newStatePerChunk: Boolean,
        prefixLength: Int,
    ): List<Float> {
        val starts = mutableListOf<Float>()
        runDesktopComposeUiTest(width = 500, height = 600) {
            var markdown by mutableStateOf(chunks.first())
            val sharedState = RichTextState()
            setContent {
                val state =
                    if (newStatePerChunk)
                        remember(markdown) { RichTextState().apply { setMarkdown(markdown) } }
                    else
                        remember(markdown) { sharedState.apply { setMarkdown(markdown) } }
                BasicRichText(
                    state = state,
                    style = style,
                    modifier = Modifier.width(400.dp),
                    onTextLayout = { starts += it.getHorizontalPosition(prefixLength, true) },
                )
            }
            waitForIdle()
            chunks.drop(1).forEach { chunk ->
                markdown = chunk
                waitForIdle()
            }
        }
        return starts
    }

    private fun assertStable(starts: List<Float>) {
        assertTrue(starts.isNotEmpty(), "Expected at least one text layout")
        assertEquals(1, starts.distinct().size, "List item moved between layouts: $starts")
    }

    @Test
    fun `bullet items keep their indent when every chunk builds a new state`() =
        assertStable(firstItemTextStarts(bulletChunks, newStatePerChunk = true, prefixLength = 2))

    @Test
    fun `bullet items keep their indent when chunks are set on the same state`() =
        assertStable(firstItemTextStarts(bulletChunks, newStatePerChunk = false, prefixLength = 2))

    @Test
    fun `numbered items keep their indent when every chunk builds a new state`() =
        assertStable(firstItemTextStarts(numberedChunks, newStatePerChunk = true, prefixLength = 3))

    @Test
    fun `numbered items keep their indent when chunks are set on the same state`() =
        assertStable(firstItemTextStarts(numberedChunks, newStatePerChunk = false, prefixLength = 3))

    private fun RichTextState.firstPrefixWidth() =
        (richParagraphList.first().type as ConfigurableStartTextWidth).startTextWidth

    /**
     * A view that wraps its content is only as wide as its text, without the indent, so a short
     * item wraps right after its prefix. The layout pass cannot read a prefix width off such a
     * layout and has to leave the measured one alone, or the view never settles.
     */
    @Test
    fun `a short list in a view that wraps its content settles on the measured prefix width`() =
        runDesktopComposeUiTest(width = 500, height = 600) {
            val wide = RichTextState().apply { setMarkdown("- one") }
            val wrapping = RichTextState().apply { setMarkdown("- one") }
            setContent {
                Column {
                    BasicRichText(state = wide, style = style, modifier = Modifier.width(400.dp))
                    BasicRichText(state = wrapping, style = style)
                }
            }
            waitForIdle()
            assertTrue(wide.firstPrefixWidth() > 0.sp, "Expected the prefix to be measured")
            assertEquals(wide.firstPrefixWidth(), wrapping.firstPrefixWidth())
        }
}
