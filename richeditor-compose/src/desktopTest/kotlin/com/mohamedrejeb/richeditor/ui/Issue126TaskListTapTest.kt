package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.type.TaskList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.runBlocking

/**
 * Issue 126: a tap or a click on the marker of a task list item checks or unchecks it.
 *
 * The marker is text, so the editor finds it by the position of the press in the text
 * layout: the line of the marker and the horizontal span of its characters. The toggle runs
 * when the pointer comes up close to where it went down, before the long press timeout. A
 * read-only or disabled editor does not toggle, and neither does a drag, a long press, or a
 * tap on the text of the item.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class Issue126TaskListTapTest {

    private enum class Input { Touch, Mouse }

    @Test
    fun `a tap on the marker checks the item`() = forEachInput { input ->
        runEditorTest { state ->
            tap(input, state, state.markerCenter(item = 0))

            assertEquals(listOf(true, true, false), state.checkedStates())
            assertEquals("☑ todo ☑ done ☐ nested", state.annotatedString.text)
        }
    }

    @Test
    fun `a tap on the marker of a checked item unchecks it`() = forEachInput { input ->
        runEditorTest { state ->
            tap(input, state, state.markerCenter(item = 1))

            assertEquals(listOf(false, false, false), state.checkedStates())
        }
    }

    @Test
    fun `a tap on the marker of a nested item toggles that item`() = forEachInput { input ->
        runEditorTest { state ->
            tap(input, state, state.markerCenter(item = 2))

            assertEquals(listOf(false, true, true), state.checkedStates())
        }
    }

    @Test
    fun `two taps on the marker restore the state`() = forEachInput { input ->
        runEditorTest { state ->
            tap(input, state, state.markerCenter(item = 0))
            tap(input, state, state.markerCenter(item = 0))

            assertEquals(listOf(false, true, false), state.checkedStates())
        }
    }

    @Test
    fun `a tap on the marker leaves a collapsed caret on the tapped item`() = forEachInput { input ->
        runEditorTest { state ->
            tap(input, state, state.markerCenter(item = 1))

            val item = state.richParagraphList[1]
            val itemStart = item.type.startRichSpan.textRange.min
            val itemEnd = itemStart + "☑ done".length
            assertTrue(state.selection.collapsed, "selection ${state.selection}")
            assertTrue(state.selection.start in itemStart..itemEnd, "selection ${state.selection}")
        }
    }

    @Test
    fun `a tap on the marker is one undo step`() = forEachInput { input ->
        runEditorTest { state ->
            state.history.clear()

            tap(input, state, state.markerCenter(item = 0))
            assertTrue(state.history.canUndo)
            state.history.undo()
            waitForIdle()

            assertEquals(listOf(false, true, false), state.checkedStates())
            assertFalse(state.history.canUndo)
        }
    }

    @Test
    fun `a tap on the text of an item does not toggle it`() = forEachInput { input ->
        runEditorTest { state ->
            val textStart = state.richParagraphList[0].type.startRichSpan.textRange.max

            tap(input, state, state.layout().getBoundingBox(textStart + 1).center)

            assertEquals(listOf(false, true, false), state.checkedStates())
        }
    }

    @Test
    fun `a tap level with a nested marker but before it does not toggle`() = forEachInput { input ->
        runEditorTest { state ->
            val marker = state.markerCenter(item = 2)
            val markerStart = state.layout().getHorizontalPosition(
                state.richParagraphList[2].type.startRichSpan.textRange.min,
                usePrimaryDirection = true,
            )
            assertTrue(markerStart > 4f, "Expected the nested marker to be indented, was at $markerStart")

            tap(input, state, Offset(x = markerStart / 2f, y = marker.y))

            assertEquals(listOf(false, true, false), state.checkedStates())
        }
    }

    @Test
    fun `a tap on the marker in a read-only editor does not toggle`() = forEachInput { input ->
        runEditorTest(readOnly = true) { state ->
            tap(input, state, state.markerCenter(item = 0))

            assertEquals(listOf(false, true, false), state.checkedStates())
        }
    }

    @Test
    fun `a tap on the marker in a disabled editor does not toggle`() = forEachInput { input ->
        runEditorTest(enabled = false) { state ->
            tap(input, state, state.markerCenter(item = 0))

            assertEquals(listOf(false, true, false), state.checkedStates())
        }
    }

    @Test
    fun `a click with the secondary mouse button on the marker does not toggle`() = runEditorTest { state ->
        val position = nodePositionOf(state, state.markerCenter(item = 0))

        onNodeWithTag(EDITOR_TAG).performMouseInput { rightClick(position) }
        waitForIdle()

        assertEquals(listOf(false, true, false), state.checkedStates())
    }

    @Test
    fun `a drag that starts on the marker does not toggle`() = runEditorTest { state ->
        val press = nodePositionOf(state, state.markerCenter(item = 0))

        onNodeWithTag(EDITOR_TAG).performTouchInput {
            down(press)
            moveBy(Offset(x = 0f, y = DRAG_DISTANCE))
            up()
        }
        waitForIdle()

        assertEquals(listOf(false, true, false), state.checkedStates())
    }

    @Test
    fun `a long press on the marker does not toggle`() = runEditorTest { state ->
        val press = nodePositionOf(state, state.markerCenter(item = 0))

        onNodeWithTag(EDITOR_TAG).performTouchInput {
            down(press)
            advanceEventTime(LONG_PRESS_MILLIS)
            up()
        }
        waitForIdle()

        assertEquals(listOf(false, true, false), state.checkedStates())
    }

    @Test
    fun `a tap on the marker toggles inside a padded decoration`() = forEachInput { input ->
        runEditorTest(padded = true) { state ->
            tap(input, state, state.markerCenter(item = 2))

            assertEquals(listOf(false, true, true), state.checkedStates())
        }
    }

    // The decoration does not fill the width, so the editor is as narrow as its minimum
    // width and the indented marker of the nested item is alone on its line.
    @Test
    fun `a tap on a marker that wraps onto its own line toggles the item`() = forEachInput { input ->
        runEditorTest(padded = true, fillDecoration = false) { state ->
            val marker = state.richParagraphList[2].type.startRichSpan.textRange
            val layout = state.layout()
            assertTrue(
                layout.getLineForOffset(marker.min) < layout.getLineForOffset(marker.max),
                "Expected the marker to be alone on its line",
            )

            tap(input, state, state.markerCenter(item = 2))

            assertEquals(listOf(false, true, true), state.checkedStates())
        }
    }

    @Test
    fun `a tap on the marker toggles in a scrolled editor`() = forEachInput { input ->
        val markdown = (1..SCROLLED_ITEM_COUNT).joinToString(separator = "\n") { "- [ ] item $it" }
        runEditorTest(markdown = markdown, height = SCROLLED_EDITOR_HEIGHT) { state ->
            runBlocking { state.scrollState.scrollTo(state.scrollState.maxValue) }
            waitForIdle()
            assertTrue(state.scrollState.value > 0, "Expected the editor to scroll")
            val last = SCROLLED_ITEM_COUNT - 1

            tap(input, state, state.markerCenter(item = last))

            assertEquals(
                List(SCROLLED_ITEM_COUNT) { it == last },
                state.checkedStates(),
            )
        }
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

    private fun runEditorTest(
        markdown: String = DOCUMENT_MARKDOWN,
        readOnly: Boolean = false,
        enabled: Boolean = true,
        padded: Boolean = false,
        fillDecoration: Boolean = true,
        height: Dp? = null,
        block: DesktopComposeUiTest.(state: RichTextState) -> Unit,
    ) = runDesktopComposeUiTest(width = 480, height = 480) {
        val state = RichTextState()
        setContent {
            BasicRichTextEditor(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(height?.let { Modifier.height(it) } ?: Modifier)
                    .testTag(EDITOR_TAG),
                enabled = enabled,
                readOnly = readOnly,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .then(if (fillDecoration) Modifier.fillMaxWidth() else Modifier)
                            .then(if (padded) Modifier.padding(DECORATION_PADDING) else Modifier),
                        propagateMinConstraints = fillDecoration,
                    ) {
                        innerTextField()
                    }
                },
            )
        }
        state.setMarkdown(markdown)
        waitForIdle()

        block(state)
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
        val node = onNodeWithTag(EDITOR_TAG).fetchSemanticsNode().positionInWindow
        val textOrigin = state.textFieldWindowPosition - node
        return Offset(
            x = inText.x + textOrigin.x,
            y = inText.y + textOrigin.y - state.scrollState.value,
        )
    }

    private fun RichTextState.layout() = checkNotNull(textLayoutResult)

    private fun RichTextState.checkedStates(): List<Boolean> =
        richParagraphList.map { (it.type as TaskList).checked }

    /** The center of the ballot box of the item, in the text layout. */
    private fun RichTextState.markerCenter(item: Int): Offset =
        layout().getBoundingBox(richParagraphList[item].type.startRichSpan.textRange.min).center

    private companion object {
        const val EDITOR_TAG = "editor"
        const val DOCUMENT_MARKDOWN = "- [ ] todo\n- [x] done\n  - [ ] nested"
        const val DRAG_DISTANCE = 120f
        const val LONG_PRESS_MILLIS = 1_000L
        const val SCROLLED_ITEM_COUNT = 40
        val SCROLLED_EDITOR_HEIGHT = 120.dp
        val DECORATION_PADDING = 24.dp
    }
}
