package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `RichTextConfig.paragraphSpacing` puts vertical space between the paragraphs of a
 * [BasicRichText] (issue 543).
 * Compose has no paragraph spacing, so each gap is a one-line paragraph carved out of the
 * separator character: the oracle is the line boxes of the same document laid out with and
 * without spacing, which may differ only by the gap lines.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class RichTextParagraphSpacingTest {

    private val style = TextStyle(fontSize = 14.sp)
    private val spacing = 20.sp

    private class Layouts(val plain: TextLayoutResult, val spaced: TextLayoutResult, val gapPx: Float) {
        val gapLines: List<Int>
            get() = (0 until spaced.lineCount).filter { spaced.lineHeight(it) == gapPx }
    }

    private fun layouts(
        width: Dp = 400.dp,
        paragraphSpacing: TextUnit = spacing,
        load: RichTextState.() -> Unit,
        block: Layouts.() -> Unit,
    ) = runDesktopComposeUiTest(width = 500, height = 800) {
        val plainState = RichTextState().apply(load)
        val spacedState = RichTextState().apply(load).apply { config.paragraphSpacing = paragraphSpacing }
        var plain: TextLayoutResult? = null
        var spaced: TextLayoutResult? = null
        setContent {
            BasicRichText(
                state = plainState,
                style = style,
                onTextLayout = { plain = it },
                modifier = Modifier.width(width),
            )
            BasicRichText(
                state = spacedState,
                style = style,
                onTextLayout = { spaced = it },
                modifier = Modifier.width(width),
            )
        }
        waitForIdle()
        val gapPx = if (paragraphSpacing.isSpecified) with(density) { paragraphSpacing.toPx() } else 0f
        Layouts(checkNotNull(plain), checkNotNull(spaced), gapPx).block()
    }

    @Test
    fun `two paragraphs are separated by exactly the spacing`() =
        layouts(load = { setText("First\nSecond") }) {
            assertEquals(2, plain.lineCount)
            assertEquals(listOf(1), gapLines)
            assertEquals(3, spaced.lineCount)
            assertEquals(plain.lineHeight(0), spaced.lineHeight(0))
            assertEquals(plain.lineHeight(1), spaced.lineHeight(2))
            assertEquals(plain.size.height + gapPx.toInt(), spaced.size.height)
        }

    @Test
    fun `the laid out text is unchanged`() =
        layouts(load = { setText("First\nSecond\nThird") }) {
            assertEquals(plain.layoutInput.text.text, spaced.layoutInput.text.text)
        }

    @Test
    fun `a wrapped paragraph gets one gap after its last line`() =
        layouts(
            width = 120.dp,
            load = { setText("One two three four five six seven eight nine ten\nShort") },
        ) {
            assertTrue(plain.lineCount > 2, "the first paragraph must wrap")
            assertEquals(listOf(plain.lineCount - 1), gapLines)
            assertEquals(plain.lineCount + 1, spaced.lineCount)
            assertEquals(plain.size.height + gapPx.toInt(), spaced.size.height)
        }

    @Test
    fun `there is no gap after the last paragraph`() =
        layouts(load = { setText("First\nSecond\nLast") }) {
            assertEquals(listOf(1, 3), gapLines)
            assertEquals(5, spaced.lineCount)
            assertEquals(plain.lineHeight(2), spaced.lineHeight(4))
        }

    /** A collapsed range cannot carry the gap style, so the trailing empty line keeps its height. */
    @Test
    fun `a trailing empty paragraph gets no gap before it`() =
        layouts(load = { setText("First\n") }) {
            assertEquals(2, plain.lineCount)
            assertEquals(2, spaced.lineCount)
            assertEquals(plain.size.height, spaced.size.height)
        }

    /** Single paragraph mode has no separator characters, so there is nothing to turn into a gap. */
    @Test
    fun `single paragraph mode is left untouched`() =
        layouts(
            load = {
                singleParagraphMode = true
                setHtml("<p>AB</p><p></p><p>CD</p><p>E</p>")
            },
        ) {
            assertEquals(plain.lineCount, spaced.lineCount)
            assertEquals(plain.size.height, spaced.size.height)
        }

    @Test
    fun `items of a list stay together and the list is spaced from the text around it`() =
        layouts(load = { setHtml("<p>Before</p><ul><li>One</li><li>Two</li><li>Three</li></ul><p>After</p>") }) {
            assertEquals(5, plain.lineCount)
            // Before, gap, One, Two, Three, gap, After
            assertEquals(listOf(1, 5), gapLines)
            assertEquals(7, spaced.lineCount)
        }

    @Test
    fun `an empty paragraph is the gap instead of adding a line to it`() =
        layouts(load = { setHtml("<p>First</p><p></p><p>Third</p>") }) {
            assertEquals(3, plain.lineCount)
            assertEquals(3, spaced.lineCount)
            assertEquals(listOf(1), gapLines)
        }

    /** A blank line between Markdown blocks is an empty paragraph, a tight boundary is not. */
    @Test
    fun `markdown blocks get the same gap with or without a blank line between them`() =
        layouts(load = { setMarkdown("# Title\nRight under the title.\n\nSecond paragraph.") }) {
            // Title, Right under the title., empty, Second paragraph.
            assertEquals(4, plain.lineCount)
            // Title, gap, Right under the title., gap, Second paragraph.
            assertEquals(5, spaced.lineCount)
            assertEquals(listOf(1, 3), gapLines)
        }

    /**
     * Skia never lays out a paragraph shorter than one line of the text style's font when its
     * line height is at or below the font size, so a small spacing comes out as a full line.
     */
    @Test
    fun `a spacing below the font size still separates the paragraphs`() =
        layouts(paragraphSpacing = 4.sp, load = { setText("First\nSecond") }) {
            assertEquals(plain.lineCount + 1, spaced.lineCount)
            assertTrue(spaced.size.height - plain.size.height >= gapPx.toInt())
        }

    @Test
    fun `changing the spacing on a displayed text lays it out again`() =
        runDesktopComposeUiTest(width = 500, height = 800) {
            val state = RichTextState().apply { setText("First\nSecond") }
            var layout: TextLayoutResult? = null
            setContent { BasicRichText(state = state, style = style, onTextLayout = { layout = it }) }
            waitForIdle()
            assertEquals(2, checkNotNull(layout).lineCount)

            state.config.paragraphSpacing = spacing
            waitForIdle()
            assertEquals(3, checkNotNull(layout).lineCount)
        }

    @Test
    fun `an unspecified spacing changes nothing`() =
        layouts(paragraphSpacing = TextUnit.Unspecified, load = { setText("First\nSecond") }) {
            assertEquals(plain.lineCount, spaced.lineCount)
            assertEquals(plain.size.height, spaced.size.height)
        }
}

private fun TextLayoutResult.lineHeight(line: Int): Float = getLineBottom(line) - getLineTop(line)
