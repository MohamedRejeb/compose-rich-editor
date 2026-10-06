package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Issue 601: in an editor with a limited height, the backgrounds that spans draw behind their
 * text did not follow the editor's own scrolling.
 *
 * They follow it since the editor owns its scroll state. What was left: they are drawn by the
 * editor's outer node, which does not clip, so a span scrolled partly out of the text area was
 * still painted over whatever sits above or below the editor, and over the editor's own padding.
 * The drawing is now clipped to the text area on each side that has content scrolled past it.
 *
 * The layout is a 40dp spacer, the editor (60dp of text area), and a 40dp spacer, on white.
 * The code span is red, so red pixels outside the text area are the bug.
 */
@OptIn(ExperimentalTestApi::class)
class Issue601OverlayScrollTest {

    @Test
    fun `editor scroll moves the internal scroll state`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(
                state = state,
                modifier = Modifier.testTag("editor").height(60.dp),
            )
        }
        state.setHtml((1..40).joinToString("") { "<p>line $it</p>" })
        waitForIdle()
        runBlocking { state.scrollState.scrollTo(state.scrollState.maxValue) }
        waitForIdle()
        assertTrue(state.scrollState.value > 0)
    }

    @Test
    fun `a span scrolled partly past the top is not painted above the editor`() = runDesktopComposeUiTest {
        val state = editorWithCodeSpanOn(line = 1)
        setEditor(state)

        scrollTo(state, 14)

        val red = redPixelsByBand()
        assertEquals(0, red.above)
        assertTrue(red.inside > 0, "the visible part of the span is still drawn")
    }

    @Test
    fun `a span scrolled partly past the bottom is not painted below the editor`() = runDesktopComposeUiTest {
        val state = editorWithCodeSpanOn(line = LINES)
        setEditor(state)

        scrollTo(state, state.scrollState.maxValue - 14)

        val red = redPixelsByBand()
        assertEquals(0, red.below)
        assertTrue(red.inside > 0, "the visible part of the span is still drawn")
    }

    @Test
    fun `a span scrolled past the text area is not painted over the editor's padding`() = runDesktopComposeUiTest {
        val state = editorWithCodeSpanOn(line = 1)
        setEditor(state, padding = 16.dp)

        scrollTo(state, 14)

        // With the padding the text area starts 16dp lower, so that band is editor padding.
        val red = redPixelsByBand(textAreaTop = SPACER + 16, textAreaBottom = SPACER + 16 + TEXT_AREA)
        assertEquals(0, red.above)
        assertTrue(red.inside > 0, "the visible part of the span is still drawn")
    }

    @Test
    fun `a span on the first line of an unscrolled editor is drawn in full`() = runDesktopComposeUiTest {
        val clipped = editorWithCodeSpanOn(line = 1)
        setEditor(clipped)
        val unscrolled = redPixelsByBand()

        // The span's padding and stroke reach a little above the first line. Nothing is
        // scrolled past that edge, so it must not be cut there.
        assertTrue(unscrolled.above > 0)
    }

    private fun editorWithCodeSpanOn(line: Int): RichTextState {
        val state = RichTextState()
        state.config.codeSpanBackgroundColor = Color.Red
        state.config.codeSpanStrokeColor = Color.Red
        state.setHtml(
            (1..LINES).joinToString("") { index ->
                if (index == line) "<p><code>CODE SPAN HERE</code></p>" else "<p>line $index</p>"
            }
        )
        return state
    }

    private fun DesktopComposeUiTest.setEditor(state: RichTextState, padding: Dp = 0.dp) {
        setContent {
            Column(Modifier.width(300.dp).background(Color.White).testTag(ROOT_TAG)) {
                Spacer(Modifier.height(SPACER.dp))
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier.fillMaxWidth().height(TEXT_AREA.dp + padding * 2),
                    decorationBox = { innerTextField -> Box(Modifier.padding(padding)) { innerTextField() } },
                    contentPadding = PaddingValues(padding),
                )
                Spacer(Modifier.height(SPACER.dp))
            }
        }
        waitForIdle()
    }

    private fun DesktopComposeUiTest.scrollTo(state: RichTextState, value: Int) {
        runBlocking { state.scrollState.scrollTo(value) }
        waitForIdle()
        assertEquals(value, state.scrollState.value)
    }

    private data class RedPixels(val above: Int, val inside: Int, val below: Int)

    private fun DesktopComposeUiTest.redPixelsByBand(
        textAreaTop: Int = SPACER,
        textAreaBottom: Int = SPACER + TEXT_AREA,
    ): RedPixels {
        val pixels = onNodeWithTag(ROOT_TAG).captureToImage().toPixelMap()
        var above = 0
        var inside = 0
        var below = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val color = pixels[x, y]
                if (color.red > 0.8f && color.green < 0.3f && color.blue < 0.3f) {
                    when {
                        y < textAreaTop -> above++
                        y < textAreaBottom -> inside++
                        else -> below++
                    }
                }
            }
        }
        return RedPixels(above, inside, below)
    }

    private companion object {
        const val ROOT_TAG = "root"
        const val LINES = 40
        const val SPACER = 40
        const val TEXT_AREA = 60
    }
}
