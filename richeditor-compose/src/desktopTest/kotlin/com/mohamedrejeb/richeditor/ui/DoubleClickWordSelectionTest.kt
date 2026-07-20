package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.mohamedrejeb.richeditor.model.RichTextState
import org.junit.Test
import kotlin.test.fail

/**
 * Reproduction: double-clicking a short word in an ordered list selects the word, then
 * the selection shifts (reported as "Three" becoming "3. Thre": pulled back over the
 * list marker with the last letter dropped).
 */
@OptIn(ExperimentalTestApi::class)
class DoubleClickWordSelectionTest {

    @Test
    fun `double click word selection in ordered list must stay on the word`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor"),
                )
            }
            state.setMarkdown("1. One\n2. Two\n3. Three\n4. Four\n5. Five")
            waitForIdle()

            runDoubleClickProbe(state, wordCharOffset = 1)
        }

    @Test
    fun `double click matrix over click positions`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor"),
                )
            }
            state.setMarkdown("1. One\n2. Two\n3. Three\n4. Four\n5. Five")
            waitForIdle()

            // Click every character position of the word, including the last one
            for (offset in 0 until "Three".length) {
                runDoubleClickProbe(state, wordCharOffset = offset)
            }
        }

    @Test
    fun `double click with background spans still on the word`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor"),
                )
            }
            // A background span makes the #635 selection mask relevant, so every
            // selection change rebuilds the annotated string mid-gesture: the one
            // legitimate case where the rc14 rebuild path still runs
            state.setHtml(
                "<ol><li>One</li><li>Two</li>" +
                    "<li><span style=\"background-color:#ffff00\">Three</span></li>" +
                    "<li>Four</li><li>Five</li></ol>",
            )
            waitForIdle()

            for (offset in 0 until "Three".length) {
                runDoubleClickProbe(state, wordCharOffset = offset)
            }
        }

    @Test
    fun `double click after building the list by typing`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor"),
                )
            }
            waitForIdle()

            // Build the list the way a user does: type "1. " (auto-detected), the
            // item text, then Enter for each following item
            fun type(textToAdd: String) {
                for (ch in textToAdd) {
                    val current = state.textFieldValue.text
                    val caret = state.selection.min
                    state.onTextFieldValueChange(
                        androidx.compose.ui.text.input.TextFieldValue(
                            text = current.substring(0, caret) + ch + current.substring(caret),
                            selection = androidx.compose.ui.text.TextRange(caret + 1),
                        ),
                    )
                }
            }
            type("1. One\nTwo\nThree\nFour\nFive")
            waitForIdle()

            for (offset in 0 until "Three".length) {
                runDoubleClickProbe(state, wordCharOffset = offset)
            }
        }

    @Test
    fun `double click in material editor with padding`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                com.mohamedrejeb.richeditor.ui.material3.RichTextEditor(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor"),
                )
            }
            state.setMarkdown("1. One\n2. Two\n3. Three\n4. Four\n5. Five")
            waitForIdle()

            for (offset in 0 until "Three".length) {
                runDoubleClickProbe(state, wordCharOffset = offset)
            }
        }

    @Test
    fun `double click with realistic timing and jitter`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor"),
                )
            }
            state.setMarkdown("1. One\n2. Two\n3. Three\n4. Four\n5. Five")
            waitForIdle()

            val text = state.textFieldValue.text
            val wordStart = text.indexOf("Three")
            val wordEnd = wordStart + "Three".length
            val layout = checkNotNull(state.textLayoutResult)
            val node = onNodeWithTag("editor").fetchSemanticsNode()
            val origin = state.textFieldWindowPosition - node.positionInRoot
            val clickAt = origin + layout.getBoundingBox(wordStart + 2).center

            val observations = mutableListOf<String>()
            fun snapshot(label: String) {
                val sel = state.selection
                observations += "$label: $sel \"${
                    state.textFieldValue.text.substring(sel.min, sel.max)
                }\""
            }

            // Real double click: press/release, ~180ms, press, 1px jitter, release
            onNodeWithTag("editor").performMouseInput {
                moveTo(clickAt)
                press()
                advanceEventTime(40)
                release()
                advanceEventTime(180)
                press()
                advanceEventTime(20)
                moveBy(androidx.compose.ui.geometry.Offset(1f, 0f))
                advanceEventTime(30)
                release()
            }
            waitForIdle()
            snapshot("after gesture")
            repeat(8) {
                mainClock.advanceTimeBy(50)
                waitForIdle()
                snapshot("after +${(it + 1) * 50}ms")
            }

            val sel = state.selection
            if (sel.min != wordStart || sel.max != wordEnd) {
                fail(
                    buildString {
                        appendLine("Selection drifted with realistic double click.")
                        appendLine("expected ($wordStart, $wordEnd), text=\"$text\"")
                        observations.forEach { appendLine("  $it") }
                    },
                )
            }
        }

    private fun ComposeUiTest.runDoubleClickProbe(
        state: RichTextState,
        wordCharOffset: Int,
    ) {
        val text = state.textFieldValue.text
        val wordStart = text.indexOf("Three")
        val wordEnd = wordStart + "Three".length

        val layout = checkNotNull(state.textLayoutResult) { "layout not ready" }
        // Translate from text-layout space to node space (the Material decoration
        // offsets the inner text field by its content padding)
        val node = onNodeWithTag("editor").fetchSemanticsNode()
        val textOriginInNode = state.textFieldWindowPosition - node.positionInRoot
        val clickAt = textOriginInNode + layout.getBoundingBox(wordStart + wordCharOffset).center

        val observations = mutableListOf<String>()

        fun snapshot(label: String) {
            val sel = state.selection
            observations += "$label: $sel \"${
                state.textFieldValue.text.substring(sel.min, sel.max)
            }\""
        }

        onNodeWithTag("editor").performMouseInput {
            doubleClick(clickAt)
        }
        waitForIdle()
        snapshot("right after double click")

        repeat(8) {
            mainClock.advanceTimeBy(50)
            waitForIdle()
            snapshot("after +${(it + 1) * 50}ms")
        }

        val finalSel = state.selection
        if (finalSel.min != wordStart || finalSel.max != wordEnd) {
            fail(
                buildString {
                    appendLine("Selection drifted off the double-clicked word (clicked char $wordCharOffset).")
                    appendLine("expected ($wordStart, $wordEnd), text=\"$text\"")
                    observations.forEach { appendLine("  $it") }
                },
            )
        }
    }
}
