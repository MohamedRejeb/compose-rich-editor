package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A tap or a click in the empty space of a line must put the caret on that line.
 *
 * Every paragraph is laid out on its own, and the platform hit test lets the caret sit after
 * the final character only on the last line of a layout. A tap past the text of a paragraph
 * therefore reports the paragraph's end offset, which is also the next paragraph's first
 * offset and renders there.
 */
@OptIn(ExperimentalTestApi::class)
class TapAtParagraphEdgeTest {

    private enum class Input { Touch, Mouse }

    private enum class Editor { Basic, RightToLeft, PaddedDecoration, MaterialWithLabel }

    @Test
    fun `a tap after the text of a paragraph puts the caret at its end`() = forEachInput { input ->
        runEditorTest { state ->
            tap(input, state, state.layout().afterTextOf(offset = HI.start))

            assertEquals(TextRange(HI.end), state.selection)
        }
    }

    @Test
    fun `a tap after the text of a centered paragraph puts the caret at its end`() =
        forEachInput { input ->
            runEditorTest { state ->
                tap(input, state, state.layout().afterTextOf(offset = CENTER.start))

                assertEquals(TextRange(CENTER.end), state.selection)
            }
        }

    @Test
    fun `a tap before the text of a centered paragraph puts the caret at its start`() =
        forEachInput { input ->
            runEditorTest { state ->
                tap(input, state, state.layout().beforeTextOf(offset = CENTER.start))

                assertEquals(TextRange(CENTER.start), state.selection)
            }
        }

    @Test
    fun `a tap before the text of a right aligned paragraph puts the caret at its start`() =
        forEachInput { input ->
            runEditorTest { state ->
                tap(input, state, state.layout().beforeTextOf(offset = RIGHT.start))

                assertEquals(TextRange(RIGHT.start), state.selection)
            }
        }

    @Test
    fun `a tap on an empty paragraph puts the caret on it`() = forEachInput { input ->
        runEditorTest { state ->
            tap(input, state, state.layout().afterTextOf(offset = EMPTY.start))

            assertEquals(TextRange(EMPTY.start), state.selection)
        }
    }

    @Test
    fun `a tap after the text of the last paragraph puts the caret at the end`() =
        forEachInput { input ->
            runEditorTest { state ->
                state.textFieldState.edit { selection = TextRange.Zero }
                waitForIdle()

                tap(input, state, state.layout().afterTextOf(offset = LAST.start))

                assertEquals(TextRange(LAST.end), state.selection)
            }
        }

    @Test
    fun `a tap on the first character of a paragraph puts the caret at its start`() =
        forEachInput { input ->
            runEditorTest { state ->
                tap(input, state, state.layout().firstCharacterOf(offset = LAST.start))

                assertEquals(TextRange(LAST.start), state.selection)
            }
        }

    @Test
    fun `a tap after the text of a right to left paragraph puts the caret at its end`() =
        forEachInput { input ->
            runEditorTest(
                html = "<p>$ARABIC_WORD</p><p>$ARABIC_WORD</p>",
                editor = Editor.RightToLeft,
            ) { state ->
                // The text starts at the right edge, so the space after it is on the left.
                tap(input, state, state.layout().beforeTextOf(offset = 0))

                assertEquals(TextRange(ARABIC_WORD.length), state.selection)
            }
        }

    @Test
    fun `a tap after the text of a list item puts the caret at its end`() = forEachInput { input ->
        runEditorTest(html = "<ul><li>One</li><li>Two</li></ul>") { state ->
            val firstItemEnd = state.annotatedString.text.indexOf("One") + "One".length

            tap(input, state, state.layout().afterTextOf(offset = firstItemEnd))

            assertEquals(TextRange(firstItemEnd), state.selection)
        }
    }

    @Test
    fun `a tap after the text of an ordered list item puts the caret at its end`() =
        forEachInput { input ->
            runEditorTest(html = "<ol><li>One</li><li>Two</li></ol>") { state ->
                val firstItemEnd = state.annotatedString.text.indexOf("One") + "One".length

                tap(input, state, state.layout().afterTextOf(offset = firstItemEnd))

                assertEquals(TextRange(firstItemEnd), state.selection)
            }
        }

    @Test
    fun `a tap on an empty list item puts the caret on it`() = forEachInput { input ->
        runEditorTest(html = "<ul><li>One</li><li><br></li><li>Two</li></ul>") { state ->
            val text = state.annotatedString.text
            val emptyItemEnd = text.indexOf("Two") - "• ".length - 1
            assertEquals(' ', text[emptyItemEnd], "Expected the empty item's separator")

            tap(input, state, state.layout().afterTextOf(offset = emptyItemEnd))

            assertEquals(TextRange(emptyItemEnd), state.selection)
        }
    }

    @Test
    fun `a tap after a wrapped line keeps the caret on that line`() = forEachInput { input ->
        runEditorTest(
            html = "<p>" + "word ".repeat(WRAPPING_WORD_COUNT).trim() + "</p><p>Next</p>",
        ) { state ->
            val layout = state.layout()
            assertTrue(layout.getLineForOffset(layout.getLineEnd(0) + 1) > 0, "Expected a wrapped paragraph")

            tap(input, state, layout.afterTextOf(offset = 0))

            assertEquals(0, state.layout().getLineForOffset(state.selection.start))
            assertTrue(state.selection.collapsed)
        }
    }

    @Test
    fun `a tap after the text works inside a padded decoration`() = forEachInput { input ->
        runEditorTest(editor = Editor.PaddedDecoration) { state ->
            tap(input, state, state.layout().lowerEdgeAfterTextOf(offset = HI.start))

            assertEquals(TextRange(HI.end), state.selection)
        }
    }

    @Test
    fun `a tap after the text works in a material editor with a label`() = forEachInput { input ->
        runEditorTest(editor = Editor.MaterialWithLabel) { state ->
            tap(input, state, state.layout().lowerEdgeAfterTextOf(offset = HI.start))

            assertEquals(TextRange(HI.end), state.selection)
        }
    }

    @Test
    fun `a click held for a while still puts the caret at the end of the paragraph`() =
        runEditorTest { state ->
            val position = nodePositionOf(state, state.layout().afterTextOf(offset = HI.start))

            onNodeWithTag(EDITOR_TAG).performMouseInput {
                moveTo(position)
                press()
            }
            waitForIdle()
            // Real time: the press position is kept by a monotonic clock, not the test clock.
            Thread.sleep(HELD_CLICK_MILLIS)
            onNodeWithTag(EDITOR_TAG).performMouseInput { release() }
            waitForIdle()

            assertEquals(TextRange(HI.end), state.selection)
        }

    /**
     * The release of a held press arrives after the press position has stopped protecting the
     * caret step from the IME heuristic, and the typing makes that step look like the one a
     * suggestion pick ends with.
     */
    @Test
    fun `a tap onto an empty paragraph right after typing does not insert a space`() =
        runEditorTest(html = "<p>Thi</p><p><br></p><p>Signature</p>") { state ->
            state.textFieldState.edit { selection = TextRange("Thi".length) }
            waitForIdle()
            // Pressed before the typing, on the layout of the text as it is then.
            val press = nodePositionOf(state, state.layout().afterTextOf(offset = "Thi".length + 1))
            val emptyParagraph = "This".length + 1

            onNodeWithTag(EDITOR_TAG).performTouchInput { down(press) }
            waitForIdle()
            mainClock.advanceTimeBy(PRESS_POSITION_LIFETIME_MILLIS)
            onNodeWithTag(EDITOR_TAG).performTextInput("s")
            onNodeWithTag(EDITOR_TAG).performTouchInput { up() }
            waitForIdle()

            assertEquals("This\n\nSignature", state.toText())
            assertEquals(TextRange(emptyParagraph), state.selection)
        }

    /**
     * A suggestion pick at a paragraph end finishes with a caret step over the separator,
     * which the editor answers by materializing the space (#779). An earlier corrected tap
     * on the same offset must not make that step look press driven.
     */
    @Test
    fun `a suggestion pick at a paragraph end still works after a corrected tap`() =
        runEditorTest(html = "<p>This</p><p><br></p><p>Signature</p>") { state ->
            // The caret is already where the tap is corrected to, so the tap changes nothing
            // and no selection pass follows it.
            state.textFieldState.edit { selection = TextRange("This".length) }
            waitForIdle()
            tap(Input.Touch, state, state.layout().afterTextOf(offset = 0))
            assertEquals(TextRange("This".length), state.selection)
            mainClock.advanceTimeBy(PRESS_POSITION_LIFETIME_MILLIS)

            onNodeWithTag(EDITOR_TAG).performKeyInput {
                pressKey(Key.Backspace)
                pressKey(Key.Backspace)
            }
            waitForIdle()
            // Real time: a recent hardware key press turns the recognition off.
            Thread.sleep(HARDWARE_KEY_WINDOW_MILLIS)
            onNodeWithTag(EDITOR_TAG).performTextInput("e")
            waitForIdle()
            assertEquals(TextRange("The".length), state.selection)

            state.textFieldState.edit { selection = TextRange("The".length + 1) }
            waitForIdle()

            assertEquals("The \n\nSignature", state.toText())
            assertEquals(TextRange("The ".length), state.selection)
        }

    @Test
    fun `an arrow key while the mouse button is held is not pulled back`() =
        runEditorTest { state ->
            val position = nodePositionOf(state, state.layout().afterTextOf(offset = HI.start))
            onNodeWithTag(EDITOR_TAG).performMouseInput {
                moveTo(position)
                press()
            }
            waitForIdle()
            assertEquals(TextRange(HI.end), state.selection)

            onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.DirectionRight) }
            waitForIdle()

            assertEquals(TextRange(RIGHT.start), state.selection)
            onNodeWithTag(EDITOR_TAG).performMouseInput { release() }
        }

    /**
     * The tap lands on the caret's own position, so its release places nothing. The key press
     * that follows must not be treated as something the tap caused.
     */
    @Test
    fun `an arrow key after a tap that moved nothing is not pulled back`() = forEachInput { input ->
        runEditorTest { state ->
            state.textFieldState.edit { selection = TextRange(HI.end) }
            waitForIdle()
            val layout = state.layout()
            val onTheCaret = Offset(x = layout.getLineRight(0) - 1f, y = layout.centerOf(line = 0))

            tap(input, state, onTheCaret)
            assertEquals(TextRange(HI.end), state.selection)

            onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.DirectionRight) }
            waitForIdle()

            assertEquals(TextRange(RIGHT.start), state.selection)
        }
    }

    @Test
    fun `a drag that starts on the line above the caret leaves the caret alone`() =
        runEditorTest { state ->
            tap(Input.Touch, state, state.layout().beforeTextOf(offset = RIGHT.start))
            assertEquals(TextRange(RIGHT.start), state.selection)
            val press = nodePositionOf(state, state.layout().afterTextOf(offset = HI.start))

            onNodeWithTag(EDITOR_TAG).performTouchInput {
                down(press)
                moveBy(Offset(x = 0f, y = DRAG_DISTANCE))
                up()
            }
            waitForIdle()

            assertEquals(TextRange(RIGHT.start), state.selection)
        }

    @Test
    fun `a tap after the text in a scrolled editor puts the caret at the end of that line`() =
        forEachInput { input ->
            runScrolledEditorTest { state ->
                val tappedStart = state.annotatedString.text.indexOf(SCROLLED_LINE)

                tap(input, state, state.layout().afterTextOf(offset = tappedStart))

                assertEquals(TextRange(tappedStart + SCROLLED_LINE.length), state.selection)
            }
        }

    @Test
    fun `a tap on the first character of a paragraph in a scrolled editor puts the caret at its start`() =
        forEachInput { input ->
            runScrolledEditorTest { state ->
                val tappedStart = state.annotatedString.text.indexOf(SCROLLED_LINE)

                tap(input, state, state.layout().firstCharacterOf(offset = tappedStart))

                assertEquals(TextRange(tappedStart), state.selection)
            }
        }

    @Test
    fun `a long press drag into the empty space of a scrolled line stays on that line`() =
        runScrolledEditorTest { state ->
            state.treatSelectionChangesAsGesture = true
            val lineStart = state.annotatedString.text.indexOf(SCROLLED_LINE)
            val layout = state.layout()
            val word = nodePositionOf(state, layout.getBoundingBox(lineStart + 1).center)
            val empty = nodePositionOf(state, layout.afterTextOf(offset = lineStart))

            onNodeWithTag(EDITOR_TAG).performTouchInput {
                down(word)
                advanceEventTime(LONG_PRESS_MILLIS)
                moveTo(empty)
                up()
            }
            waitForIdle()

            assertEquals(TextRange(lineStart, lineStart + SCROLLED_LINE.length), state.selection)
        }

    /**
     * Runs every input even when one fails, so a failure names all the inputs it affects.
     */
    private fun forEachInput(block: (Input) -> Unit) {
        val failures = Input.entries.mapNotNull { input ->
            runCatching { block(input) }
                .exceptionOrNull()
                ?.let { cause -> input to cause }
        }
        if (failures.isEmpty()) return

        fail(
            message = failures.joinToString(separator = "\n") { (input, cause) -> "$input: ${cause.message}" },
            cause = failures.first().second,
        )
    }

    private fun runScrolledEditorTest(
        block: DesktopComposeUiTest.(state: RichTextState) -> Unit,
    ) = runEditorTest(
        html = (1..SCROLLED_LINE_COUNT).joinToString(separator = "") { "<p>Line $it</p>" },
        height = SCROLLED_EDITOR_HEIGHT,
    ) { state, scope ->
        scope.launch { state.scrollState.scrollTo(state.scrollState.maxValue) }
        waitForIdle()
        assertTrue(state.scrollState.value > 0, "Expected the editor to scroll")

        block(state)
    }

    private fun runEditorTest(
        html: String = DOCUMENT_HTML,
        editor: Editor = Editor.Basic,
        block: DesktopComposeUiTest.(state: RichTextState) -> Unit,
    ) = runEditorTest(html = html, editor = editor, height = null) { state, _ -> block(state) }

    private fun runEditorTest(
        html: String,
        editor: Editor = Editor.Basic,
        height: Dp?,
        block: DesktopComposeUiTest.(state: RichTextState, scope: CoroutineScope) -> Unit,
    ) = runDesktopComposeUiTest(width = 480, height = 480) {
        val state = RichTextState()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            TestedEditor(
                editor = editor,
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(height?.let { Modifier.height(it) } ?: Modifier)
                    .testTag(EDITOR_TAG),
            )
        }
        state.setHtml(html)
        if (html == DOCUMENT_HTML) {
            state.selection = TextRange(RIGHT.start, RIGHT.end)
            state.addParagraphStyle(ParagraphStyle(textAlign = TextAlign.End))
            state.selection = TextRange(CENTER.start, CENTER.end)
            state.addParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))
            state.selection = TextRange(LAST.end)
        }
        waitForIdle()

        block(state, scope)
    }

    @Composable
    private fun TestedEditor(
        editor: Editor,
        state: RichTextState,
        modifier: Modifier,
    ) {
        when (editor) {
            Editor.Basic -> BasicRichTextEditor(state = state, modifier = modifier)

            Editor.RightToLeft -> CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicRichTextEditor(state = state, modifier = modifier)
            }

            Editor.PaddedDecoration -> BasicRichTextEditor(
                state = state,
                modifier = modifier,
                decorationBox = { innerTextField ->
                    Box(Modifier.padding(DECORATION_PADDING)) { innerTextField() }
                },
            )

            Editor.MaterialWithLabel -> RichTextEditor(
                state = state,
                modifier = modifier,
                label = { Text("Label") },
            )
        }
    }

    private fun DesktopComposeUiTest.tap(input: Input, state: RichTextState, inText: Offset) {
        val position = nodePositionOf(state, inText)
        when (input) {
            Input.Touch -> onNodeWithTag(EDITOR_TAG).performTouchInput { click(position) }
            Input.Mouse -> onNodeWithTag(EDITOR_TAG).performMouseInput { click(position) }
        }
        waitForIdle()
    }

    /**
     * Converts a position in the text layout to one in the editor node: the decoration moves
     * the text inside the node, and scrolling moves it up.
     */
    private fun DesktopComposeUiTest.nodePositionOf(state: RichTextState, inText: Offset): Offset {
        val node = onNodeWithTag(EDITOR_TAG).fetchSemanticsNode()
        val textOrigin = state.textFieldWindowPosition - node.positionInWindow
        // Compensate for scroll so clicks target the correct text-space line.
        // Subtract the scroll offset to map text coords into the visible viewport area.
        val yLocal = (inText.y + textOrigin.y - state.scrollState.value).coerceAtLeast(0f)
        return Offset(
            x = inText.x + textOrigin.x,
            y = yLocal,
        )
    }

    private fun RichTextState.layout(): TextLayoutResult = checkNotNull(textLayoutResult)

    private fun TextLayoutResult.centerOf(line: Int): Float =
        (getLineTop(line) + getLineBottom(line)) / 2f

    /**
     * The middle of the empty space after the text, on the line that holds [offset].
     */
    private fun TextLayoutResult.afterTextOf(offset: Int): Offset {
        val line = getLineForOffset(offset)
        return Offset(x = emptySpaceAfter(line), y = centerOf(line))
    }

    /**
     * The same space, near the bottom of the line: where a text origin that is off by the
     * decoration's offset resolves the next line.
     */
    private fun TextLayoutResult.lowerEdgeAfterTextOf(offset: Int): Offset {
        val line = getLineForOffset(offset)
        return Offset(x = emptySpaceAfter(line), y = getLineBottom(line) - 1f)
    }

    private fun TextLayoutResult.emptySpaceAfter(line: Int): Float {
        val textEnd = getLineRight(line)
        assertTrue(size.width - textEnd > MIN_EMPTY_SPACE, "Expected empty space after the text")
        return (textEnd + size.width) / 2f
    }

    /**
     * The middle of the empty space before the text, on the line that holds [offset].
     */
    private fun TextLayoutResult.beforeTextOf(offset: Int): Offset {
        val line = getLineForOffset(offset)
        val textStart = getLineLeft(line)
        assertTrue(textStart > MIN_EMPTY_SPACE, "Expected empty space before the text")
        return Offset(x = textStart / 2f, y = centerOf(line))
    }

    private fun TextLayoutResult.firstCharacterOf(offset: Int): Offset =
        Offset(x = getLineLeft(getLineForOffset(offset)) + 1f, y = centerOf(getLineForOffset(offset)))

    private data class Paragraph(val start: Int, val end: Int)

    private companion object {
        const val EDITOR_TAG = "editor"
        const val MIN_EMPTY_SPACE = 8f
        const val DRAG_DISTANCE = 60f
        const val LONG_PRESS_MILLIS = 800L
        const val HELD_CLICK_MILLIS = 700L
        const val HARDWARE_KEY_WINDOW_MILLIS = 400L
        const val PRESS_POSITION_LIFETIME_MILLIS = 400L
        const val WRAPPING_WORD_COUNT = 40
        const val SCROLLED_LINE_COUNT = 12
        const val SCROLLED_LINE = "Line 11"
        const val ARABIC_WORD = "مرحبا"
        val SCROLLED_EDITOR_HEIGHT = 60.dp
        val DECORATION_PADDING = 24.dp

        // Rendered as "Hi Right Center  Last line": one separator after each paragraph.
        const val DOCUMENT_HTML = "<p>Hi</p><p>Right</p><p>Center</p><p><br></p><p>Last line</p>"
        val HI = Paragraph(start = 0, end = 2)
        val RIGHT = Paragraph(start = 3, end = 8)
        val CENTER = Paragraph(start = 9, end = 15)
        val EMPTY = Paragraph(start = 16, end = 16)
        val LAST = Paragraph(start = 17, end = 26)
    }
}
