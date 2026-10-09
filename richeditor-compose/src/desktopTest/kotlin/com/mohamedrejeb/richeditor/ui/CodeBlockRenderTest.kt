package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.highlight.CodeBlockColors
import com.mohamedrejeb.richeditor.model.EmptyLineAnchor
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.codeBlockGroups
import com.mohamedrejeb.richeditor.paragraph.type.CodeBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Code blocks as the real composables lay them out: token colours and the code font reach the
 * read-only text and the editor, typing keeps a block a block, and the background is painted.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class CodeBlockRenderTest {

    private val markdown = "a\n\n```kotlin\nval b = 1\n```"
    private val keyword = CodeBlockColors.Default.keyword
    private val number = CodeBlockColors.Default.number

    @Test
    fun `the read only text lays out token colours and the code font`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setMarkdown(markdown) }
        var layout: TextLayoutResult? = null
        setContent { BasicRichText(state = state, onTextLayout = { layout = it }) }
        waitForIdle()

        val text = assertNotNull(layout).layoutInput.text
        assertEquals("a val b = 1", text.text)
        assertTrue(AnnotatedString.Range(keyword, 2, 5) in text.spanStyles)
        assertTrue(AnnotatedString.Range(number, 10, 11) in text.spanStyles)
        assertTrue(text.spanStyles.any { 2 in it.start until it.end && it.item.fontFamily == FontFamily.Monospace })
        assertTrue(text.spanStyles.none { 0 in it.start until it.end && it.item.fontFamily == FontFamily.Monospace })
    }

    @Test
    fun `the editor lays out token colours`() = runEditorTest { state ->
        val layout = editorLayout(state)
        assertEquals(keyword.color, layout.colorAt(2))
        assertEquals(number.color, layout.colorAt(10))
        assertNotEquals(keyword.color, layout.colorAt(0))
    }

    @Test
    fun `typing in a code block keeps the block and recolours it`() = runEditorTest { state ->
        state.textFieldState.edit { selection = TextRange(length) }
        waitForIdle()

        onNodeWithTag(EDITOR_TAG).performTextInput(" val")
        waitForIdle()

        assertTrue(state.richParagraphList.last().type is CodeBlock)
        assertEquals("a\n\n```kotlin\nval b = 1 val\n```", state.toMarkdown())
        val layout = editorLayout(state)
        assertEquals("a val b = 1 val", layout.layoutInput.text.text.removeSuffix(EmptyLineAnchor))
        assertEquals(keyword.color, layout.colorAt(12))
        assertEquals(keyword.color, layout.colorAt(14))
    }

    @Test
    fun `enter in a code block adds a line to the same block`() = runEditorTest { state ->
        state.textFieldState.edit { selection = TextRange(length) }
        waitForIdle()

        onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        onNodeWithTag(EDITOR_TAG).performTextInput("x")
        waitForIdle()

        assertEquals(listOf(1..2), state.richParagraphList.toList().codeBlockGroups())
        assertEquals("a\n\n```kotlin\nval b = 1\nx\n```", state.toMarkdown())
    }

    @Test
    fun `the block background is drawn behind the code and not behind other text`() = runDesktopComposeUiTest {
        val state = RichTextState().apply {
            config.codeBlockBackgroundColor = Color.Red
            setMarkdown("plain\n\n```\ncode\n```")
        }
        var layout: TextLayoutResult? = null
        setContent {
            Box(Modifier.width(200.dp)) {
                BasicRichText(
                    state = state,
                    modifier = Modifier.testTag(EDITOR_TAG).fillMaxWidth(),
                    onTextLayout = { layout = it },
                )
            }
        }
        waitForIdle()

        val lines = assertNotNull(layout)
        val pixels = onNodeWithTag(EDITOR_TAG).captureToImage().toPixelMap()
        // Sampled near the right edge, where no glyph is drawn.
        val x = pixels.width - 12
        fun isRedAtLine(line: Int): Boolean {
            val y = ((lines.getLineTop(line) + lines.getLineBottom(line)) / 2).toInt()
            val color = pixels[x, y]
            return color.red > 0.8f && color.green < 0.3f && color.blue < 0.3f
        }
        assertTrue(isRedAtLine(1), "the code line has the block background")
        assertTrue(!isRedAtLine(0), "the plain line has no block background")
    }

    @Test
    fun `a block cut off by maxLines is drawn without crashing`() = runDesktopComposeUiTest {
        val state = RichTextState().apply {
            config.codeBlockBackgroundColor = Color.Red
            setMarkdown("```\none\ntwo\nthree\n```")
        }
        var layout: TextLayoutResult? = null
        setContent {
            Box(Modifier.width(200.dp)) {
                BasicRichText(
                    state = state,
                    modifier = Modifier.testTag(EDITOR_TAG).fillMaxWidth(),
                    maxLines = 2,
                    onTextLayout = { layout = it },
                )
            }
        }
        waitForIdle()

        val lines = assertNotNull(layout)
        assertEquals(2, lines.lineCount)
        val pixels = onNodeWithTag(EDITOR_TAG).captureToImage().toPixelMap()
        assertTrue(pixels.isRedAt(pixels.width - 12, lines.lineMiddle(0)), "the first visible line has the background")
        assertTrue(pixels.isRedAt(pixels.width - 12, lines.lineMiddle(1)), "the last visible line has the background")
    }

    @Test
    fun `a block that lies past maxLines is skipped without crashing`() = runDesktopComposeUiTest {
        val state = RichTextState().apply {
            config.codeBlockBackgroundColor = Color.Red
            setMarkdown("text\n\n```\ncode\n```")
        }
        var layout: TextLayoutResult? = null
        setContent {
            Box(Modifier.width(200.dp)) {
                BasicRichText(
                    state = state,
                    modifier = Modifier.testTag(EDITOR_TAG).fillMaxWidth(),
                    maxLines = 1,
                    onTextLayout = { layout = it },
                )
            }
        }
        waitForIdle()

        val lines = assertNotNull(layout)
        assertEquals(1, lines.lineCount)
        val pixels = onNodeWithTag(EDITOR_TAG).captureToImage().toPixelMap()
        assertTrue(!pixels.isRedAt(pixels.width - 12, lines.lineMiddle(0)), "the plain line has no background")
    }

    @Test
    fun `an empty last line of a block has the background`() = runDesktopComposeUiTest {
        val state = RichTextState().apply {
            config.codeBlockBackgroundColor = Color.Red
            setMarkdown("```\ncode\n```")
        }
        setContent {
            val focusRequester = remember { FocusRequester() }
            Box(Modifier.width(200.dp)) {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier.focusRequester(focusRequester).testTag(EDITOR_TAG).fillMaxWidth(),
                )
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
        waitForIdle()
        state.textFieldState.edit { selection = TextRange(length) }
        waitForIdle()

        onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.Enter) }
        waitForIdle()

        val lines = editorLayout(state)
        assertEquals(2, lines.lineCount)
        val pixels = onNodeWithTag(EDITOR_TAG).captureToImage().toPixelMap()
        assertTrue(pixels.isRedAt(pixels.width - 12, lines.lineMiddle(0)), "the code line has the background")
        assertTrue(pixels.isRedAt(pixels.width - 12, lines.lineMiddle(1)), "the new empty line has the background")
    }

    @Test
    fun `a block has room above its first line and below its last`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setMarkdown("```\none\ntwo\nthree\n```") }
        var layout: TextLayoutResult? = null
        setContent { BasicRichText(state = state, onTextLayout = { layout = it }) }
        waitForIdle()

        val lines = assertNotNull(layout)
        fun height(line: Int) = lines.getLineBottom(line) - lines.getLineTop(line)
        assertTrue(height(0) > height(1), "the first line is taller than a middle line")
        assertTrue(height(2) > height(1), "the last line is taller than a middle line")
        // The room is above the first line's text and below the last line's.
        assertTrue(lines.getLineBaseline(0) - lines.getLineTop(0) > lines.getLineBaseline(1) - lines.getLineTop(1))
        assertTrue(lines.getLineBottom(2) - lines.getLineBaseline(2) > lines.getLineBottom(1) - lines.getLineBaseline(1))
    }

    @Test
    fun `two blocks in a row do not touch`() = runDesktopComposeUiTest {
        val state = RichTextState().apply {
            config.codeBlockBackgroundColor = Color.Red
            setMarkdown("```\none\n```\n```\ntwo\n```")
        }
        var layout: TextLayoutResult? = null
        setContent {
            Box(Modifier.width(200.dp)) {
                BasicRichText(
                    state = state,
                    modifier = Modifier.testTag(EDITOR_TAG).fillMaxWidth(),
                    onTextLayout = { layout = it },
                )
            }
        }
        waitForIdle()

        val lines = assertNotNull(layout)
        val pixels = onNodeWithTag(EDITOR_TAG).captureToImage().toPixelMap()
        val x = pixels.width / 2
        assertTrue(pixels.isRedAt(x, lines.lineMiddle(0)), "the first block is drawn")
        assertTrue(pixels.isRedAt(x, lines.lineMiddle(1)), "the second block is drawn")
        assertTrue(!pixels.isRedAt(x, lines.getLineBottom(0).toInt()), "there is a gap where the two blocks meet")
    }

    @Test
    fun `the background stays inside the content padding of an editor`() = runDesktopComposeUiTest {
        val state = RichTextState().apply {
            config.codeBlockBackgroundColor = Color.Red
            setMarkdown("```\ncode\n```")
        }
        val horizontalPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 24.dp, bottom = 12.dp)
        setContent {
            BasicRichTextEditor(
                state = state,
                modifier = Modifier.width(200.dp).testTag(EDITOR_TAG),
                decorationBox = { innerTextField -> Box(Modifier.padding(horizontalPadding)) { innerTextField() } },
                contentPadding = horizontalPadding,
            )
        }
        waitForIdle()

        val red = onNodeWithTag(EDITOR_TAG).captureToImage().toPixelMap().redBounds()
        assertEquals(16, red.left, "the background starts after the start padding")
        assertEquals(200 - 24, red.right, "the background ends before the end padding")
    }

    @Test
    fun `a scrolled block is not drawn over the padding of an editor`() = runDesktopComposeUiTest {
        val state = RichTextState().apply {
            config.codeBlockBackgroundColor = Color.Red
            setMarkdown("```\n" + (1..30).joinToString("\n") { "line " + it } + "\n```")
        }
        setContent {
            val focusRequester = remember { FocusRequester() }
            BasicRichTextEditor(
                state = state,
                modifier = Modifier.width(200.dp).height(120.dp).focusRequester(focusRequester).testTag(EDITOR_TAG),
                decorationBox = { innerTextField -> Box(Modifier.padding(12.dp)) { innerTextField() } },
                contentPadding = PaddingValues(12.dp),
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
        waitForIdle()
        state.textFieldState.edit { selection = TextRange(length / 2) }
        waitForIdle()
        onNodeWithTag(EDITOR_TAG).performTextInput("x")
        waitForIdle()

        assertTrue(state.scrollState.value > 0, "the editor is scrolled")
        val red = onNodeWithTag(EDITOR_TAG).captureToImage().toPixelMap().redBounds()
        assertEquals(12, red.top, "nothing is drawn over the top padding")
        assertEquals(120 - 12, red.bottom, "nothing is drawn over the bottom padding")
    }

    /** The smallest rectangle holding every red pixel, right and bottom exclusive. */
    private fun PixelMap.redBounds(): IntRect {
        var left = width
        var top = height
        var right = 0
        var bottom = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (isRedAt(x, y)) {
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x + 1)
                    bottom = maxOf(bottom, y + 1)
                }
            }
        }
        return IntRect(left, top, right, bottom)
    }

    private fun TextLayoutResult.lineMiddle(line: Int): Int = ((getLineTop(line) + getLineBottom(line)) / 2).toInt()

    private fun PixelMap.isRedAt(x: Int, y: Int): Boolean {
        val color = this[x, y]
        return color.red > 0.8f && color.green < 0.3f && color.blue < 0.3f
    }

    private fun runEditorTest(block: DesktopComposeUiTest.(state: RichTextState) -> Unit) = runDesktopComposeUiTest {
        val state = RichTextState().apply { setMarkdown(markdown) }
        setContent {
            val focusRequester = remember { FocusRequester() }
            Box {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .testTag(EDITOR_TAG),
                )
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
        waitForIdle()
        block(state)
    }

    private fun editorLayout(state: RichTextState): TextLayoutResult =
        assertNotNull(state.textLayoutResult, "Expected a text layout result")

    /** The last range covering an offset is the one the text is drawn with. */
    private fun TextLayoutResult.colorAt(offset: Int): Color =
        layoutInput.text.spanStyles
            .filter { range: AnnotatedString.Range<SpanStyle> -> offset in range.start until range.end }
            .lastOrNull { it.item.color != Color.Unspecified }?.item?.color ?: Color.Unspecified

    private companion object {
        const val EDITOR_TAG = "editor"
    }
}
