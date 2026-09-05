package com.mohamedrejeb.richeditor.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Parity between the model and what the editor actually lays out.
 *
 * A model-only suite cannot see the class of bug that shipped three Android regressions: the
 * paragraph list was right while the rendered [androidx.compose.ui.text.TextLayoutResult] was one
 * line short, or the style the model held never reached the buffer BTF2 measured. These tests drive
 * the real editor and assert against [RichTextState.textLayoutResult] only.
 *
 * The rendered text is [RichTextState.annotatedString] plus one output-only substitution: a
 * trailing empty paragraph owns no range of its own, so the pipeline turns the separator space in
 * front of it into a newline (see `substituteTrailingSeparatorWithNewline`). [expectedLayoutText]
 * reproduces that rule, so a drift in either direction fails here rather than silently changing the
 * rendered line count.
 *
 * Widths are left at the desktop test default and every document stays a few words long, so a line
 * in the layout is always a paragraph and never a soft wrap.
 */
@OptIn(ExperimentalTestApi::class)
class RenderParityTest {

    /**
     * The text the editor is expected to lay out: the model text, with the trailing paragraph
     * separator substituted for a newline when the last paragraph is empty.
     */
    private fun expectedLayoutText(state: RichTextState): String {
        val text = state.annotatedString.text
        val last = state.annotatedString.paragraphStyles.lastOrNull() ?: return text
        val trailingEmptyParagraph =
            last.start == last.end &&
                last.start == text.length &&
                text.isNotEmpty() &&
                text.last() == ' '
        return if (trailingEmptyParagraph) text.dropLast(1) + "\n" else text
    }

    /** Offset of the first character of every paragraph, marker prefix included. */
    private fun paragraphStartOffsets(state: RichTextState): List<Int> {
        var offset = 0
        return state.toText().split("\n").map { paragraph ->
            val start = offset
            offset += paragraph.length + 1
            start
        }
    }

    /**
     * The core oracle: one rendered line per model paragraph, the rendered text equal to the model
     * text under the separator mapping, and every paragraph starting on its own line.
     */
    private fun assertRenderParity(state: RichTextState, label: String) {
        assertEquals(
            state.annotatedString.text,
            state.toText().replace('\n', ' '),
            "$label: model text and buffer text must agree under the separator mapping",
        )
        val paragraphTexts = state.toText().split("\n")
        assertEquals(
            state.richParagraphList.size,
            paragraphTexts.size,
            "$label: toText must produce one line per paragraph",
        )

        val layout = assertNotNull(state.textLayoutResult, "$label: the editor produced no layout")
        assertEquals(
            expectedLayoutText(state),
            layout.layoutInput.text.text,
            "$label: the laid out text must be the model text",
        )
        assertEquals(
            state.richParagraphList.size,
            layout.lineCount,
            "$label: the layout must render one line per paragraph",
        )
        paragraphStartOffsets(state).forEachIndexed { index, start ->
            assertEquals(
                index,
                layout.getLineForOffset(start),
                "$label: paragraph $index must start on line $index",
            )
        }
    }

    /** Every character index the layout renders with [FontWeight.Bold]. */
    private fun boldIndices(state: RichTextState): Set<Int> {
        val layout = state.textLayoutResult ?: return emptySet()
        return layout.layoutInput.text.spanStyles
            .filter { it.item.fontWeight == FontWeight.Bold }
            .flatMap { (it.start until it.end).toList() }
            .toSet()
    }

    // --- (a) line count parity across the operations matrix ---

    @Test
    fun `typing renders a single line`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("hello")
        waitForIdle()

        assertRenderParity(state, "typing")
    }

    @Test
    fun `enter at the end renders two lines`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("hello")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()

        assertEquals(2, state.richParagraphList.size)
        assertRenderParity(state, "enter at end")
    }

    @Test
    fun `enter in the middle renders two lines`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("abcdef")
        waitForIdle()
        state.selection = TextRange(3)
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()

        assertEquals(2, state.richParagraphList.size)
        assertEquals("abc\ndef", state.toText())
        assertRenderParity(state, "enter in the middle")
    }

    @Test
    fun `enter on an empty document renders two lines`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()

        assertEquals(2, state.richParagraphList.size)
        assertRenderParity(state, "enter on an empty document")
    }

    @Test
    fun `two enters in a row render three lines`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("hello")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()

        assertEquals(3, state.richParagraphList.size)
        assertRenderParity(state, "two enters")
    }

    @Test
    fun `backspace merging two paragraphs renders one line`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("abc")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("def")
        waitForIdle()
        assertRenderParity(state, "before the merge")

        state.selection = TextRange(4)
        waitForIdle()
        onNodeWithTag("editor").performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()

        assertEquals(1, state.richParagraphList.size)
        assertEquals("abcdef", state.toText())
        assertRenderParity(state, "after the merge")
    }

    @Test
    fun `an unordered list keeps every item on its own line`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        state.toggleUnorderedList()
        waitForIdle()
        onNodeWithTag("editor").performTextInput("one")
        waitForIdle()
        assertRenderParity(state, "first item")

        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        assertEquals(2, state.richParagraphList.size)
        assertRenderParity(state, "second item opened")

        onNodeWithTag("editor").performTextInput("two")
        waitForIdle()
        assertRenderParity(state, "second item typed")

        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        assertEquals(3, state.richParagraphList.size)
        assertRenderParity(state, "third item opened")
    }

    @Test
    fun `enter at the start of a paragraph renders an empty line above`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("hello")
        waitForIdle()
        state.selection = TextRange(0)
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()

        assertEquals(2, state.richParagraphList.size)
        assertEquals("\nhello", state.toText())
        assertRenderParity(state, "enter at the start")
    }

    @Test
    fun `enter inside a list item splits it into two rendered lines`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("abcdef")
        waitForIdle()
        state.toggleUnorderedList()
        waitForIdle()
        // Past the "• " marker, in the middle of the item's own text.
        state.selection = TextRange(5)
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()

        assertEquals(2, state.richParagraphList.size)
        assertRenderParity(state, "enter inside a list item")
    }

    @Test
    fun `backspace at the start of the second list item merges the rendered lines`() =
        runDesktopComposeUiTest {
            lateinit var state: RichTextState
            setContent {
                state = rememberRichTextState()
                BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
            }
            state.toggleUnorderedList()
            waitForIdle()
            onNodeWithTag("editor").performTextInput("one")
            waitForIdle()
            onNodeWithTag("editor").performTextInput("\n")
            waitForIdle()
            onNodeWithTag("editor").performTextInput("two")
            waitForIdle()
            assertEquals(2, state.richParagraphList.size)
            assertRenderParity(state, "two list items")

            state.selection = TextRange(paragraphStartOffsets(state)[1] + 2)
            waitForIdle()
            onNodeWithTag("editor").performKeyInput { pressKey(Key.Backspace) }
            waitForIdle()

            assertRenderParity(state, "after the list backspace")
        }

    @Test
    fun `emptying the second paragraph keeps two rendered lines`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("abc")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("d")
        waitForIdle()
        assertRenderParity(state, "before emptying")

        onNodeWithTag("editor").performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()

        assertEquals(2, state.richParagraphList.size, "the empty paragraph should survive")
        assertRenderParity(state, "after emptying the second paragraph")
    }

    @Test
    fun `setHtml with a trailing empty paragraph renders the empty line`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        state.setHtml("<p>one</p><p></p>")
        waitForIdle()

        assertEquals(2, state.richParagraphList.size)
        assertRenderParity(state, "setHtml with a trailing empty paragraph")
    }

    /**
     * A trailing `<p><br></p>` imports as one empty paragraph (`LineBreakHtmlSemanticsTest` pins
     * the count). The count is the HTML parser's business; what this asserts is that whatever the
     * parser produced is what the editor renders.
     */
    @Test
    fun `setHtml with a trailing br paragraph renders one line per paragraph`() =
        runDesktopComposeUiTest {
            lateinit var state: RichTextState
            setContent {
                state = rememberRichTextState()
                BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
            }
            state.setHtml("<p>one</p><p><br></p>")
            waitForIdle()

            assertRenderParity(state, "setHtml with a trailing br paragraph")
        }

    @Test
    fun `typing after setHtml keeps the rendered lines in step`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        state.setHtml("<p>one</p><p>two</p>")
        waitForIdle()
        state.selection = TextRange(3)
        waitForIdle()
        onNodeWithTag("editor").performTextInput("X")
        waitForIdle()

        assertEquals("oneX\ntwo", state.toText())
        assertRenderParity(state, "typing after setHtml")
    }

    @Test
    fun `setHtml with three paragraphs renders three lines`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        state.setHtml("<p>one</p><p>two</p><p>three</p>")
        waitForIdle()

        assertEquals(3, state.richParagraphList.size)
        assertRenderParity(state, "setHtml with three paragraphs")
    }

    @Test
    fun `undo and redo of an enter keep the rendered line count in step`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("abcdef")
        waitForIdle()
        state.selection = TextRange(3)
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        assertRenderParity(state, "after the enter")

        state.history.undo()
        waitForIdle()
        assertEquals(1, state.richParagraphList.size, "undo should restore one paragraph")
        assertRenderParity(state, "after undo")

        state.history.redo()
        waitForIdle()
        assertEquals(2, state.richParagraphList.size, "redo should restore two paragraphs")
        assertRenderParity(state, "after redo")
    }

    @Test
    fun `undo of a list toggle removes the rendered marker`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("item")
        waitForIdle()
        state.toggleUnorderedList()
        waitForIdle()
        assertRenderParity(state, "list toggled on")

        state.history.undo()
        waitForIdle()

        assertEquals("item", state.toText(), "undo should drop the marker from the model")
        assertRenderParity(state, "list toggle undone")
    }

    // --- (b) span presence in the rendered layout ---

    @Test
    fun `bold on a range survives in the rendered spans through three keystrokes`() =
        runDesktopComposeUiTest {
            lateinit var state: RichTextState
            setContent {
                state = rememberRichTextState()
                BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
            }
            onNodeWithTag("editor").performTextInput("hello world")
            waitForIdle()
            state.selection = TextRange(0, 5)
            waitForIdle()
            state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            waitForIdle()
            assertEquals(
                (0 until 5).toSet(),
                boldIndices(state),
                "the toggled range should render bold right away",
            )

            state.selection = TextRange(11)
            waitForIdle()
            repeat(3) { index ->
                onNodeWithTag("editor").performTextInput("x")
                waitForIdle()
                assertEquals(
                    (0 until 5).toSet(),
                    boldIndices(state),
                    "the bold range should be unchanged after keystroke ${index + 1}",
                )
            }
        }

    @Test
    fun `a staged bold renders on every char typed under it`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("abc")
        waitForIdle()
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        waitForIdle()

        repeat(3) { index ->
            onNodeWithTag("editor").performTextInput("z")
            waitForIdle()
            assertEquals(
                (3..3 + index).toSet(),
                boldIndices(state),
                "every char typed under the staged bold should render bold (keystroke ${index + 1})",
            )
        }
    }

    @Test
    fun `enter inside bold text keeps bold on both rendered lines`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("abcdef")
        waitForIdle()
        state.selection = TextRange(0, 6)
        waitForIdle()
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        waitForIdle()
        state.selection = TextRange(3)
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()

        assertEquals(2, state.richParagraphList.size)
        // "abc" on line one and "def" on line two, with the separator itself unstyled.
        assertEquals(
            setOf(0, 1, 2, 4, 5, 6),
            boldIndices(state),
            "both halves of the split should still render bold",
        )
        assertRenderParity(state, "enter inside bold text")
    }

    // --- (c) paragraph level styling in the rendered layout ---

    @Test
    fun `a heading renders its font size in the layout`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("title")
        waitForIdle()
        state.setHeadingStyle(HeadingStyle.H1)
        waitForIdle()

        val layout = assertNotNull(state.textLayoutResult)
        assertTrue(
            layout.layoutInput.text.spanStyles.any {
                it.start == 0 &&
                    it.end == 5 &&
                    it.item.fontSize == HeadingStyle.H1.defaultSpanStyle.fontSize
            },
            "the heading font size should reach the laid out spans, got " +
                layout.layoutInput.text.spanStyles.joinToString {
                    "${it.start}-${it.end}:${it.item.fontSize}"
                },
        )
        assertRenderParity(state, "heading")
    }

    @Test
    fun `a centered paragraph renders its alignment in the layout`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        state.setHtml("<p style=\"text-align: center;\">centered</p><p>plain</p>")
        waitForIdle()

        val layout = assertNotNull(state.textLayoutResult)
        assertTrue(
            layout.layoutInput.text.paragraphStyles.any {
                it.start == 0 && it.item.textAlign == TextAlign.Center
            },
            "the centered paragraph should reach the laid out paragraph styles, got " +
                layout.layoutInput.text.paragraphStyles.joinToString {
                    "${it.start}-${it.end}:${it.item.textAlign}"
                },
        )
        assertRenderParity(state, "centered paragraph")
    }

    @Test
    fun `backspacing a paragraph into a heading renders one whole heading line`() =
        runDesktopComposeUiTest {
            lateinit var state: RichTextState
            val focusRequester = FocusRequester()
            setContent {
                state = rememberRichTextState()
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier.focusRequester(focusRequester).testTag("editor"),
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
            waitForIdle()
            state.setHtml("<h1>title</h1><p>body</p>")
            waitForIdle()
            assertRenderParity(state, "heading above a paragraph")

            state.selection = TextRange(6)
            waitForIdle()
            onNodeWithTag("editor").performKeyInput { pressKey(Key.Backspace) }
            waitForIdle()

            assertEquals(1, state.richParagraphList.size)
            assertEquals("titlebody", state.toText())
            // The heading's weight is carried on the spans, so the merged text has to pick it up
            // or the line renders half sized.
            assertEquals(
                (0 until 9).toSet(),
                boldIndices(state),
                "the merged line should render as one heading",
            )
            assertRenderParity(state, "paragraph merged into a heading")
        }

    // --- (d) list markers in the rendered text ---

    @Test
    fun `a list paragraph renders its marker prefix in the layout text`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("item")
        waitForIdle()
        state.toggleUnorderedList()
        waitForIdle()

        val layout = assertNotNull(state.textLayoutResult)
        assertTrue(
            layout.layoutInput.text.text.startsWith("• "),
            "the bullet prefix should be part of the laid out text, got " +
                "[${layout.layoutInput.text.text}]",
        )
        assertRenderParity(state, "unordered list marker")
    }

    @Test
    fun `an ordered list renders the next number prefix on the new line`() = runDesktopComposeUiTest {
        lateinit var state: RichTextState
        setContent {
            state = rememberRichTextState()
            BasicRichTextEditor(state = state, modifier = Modifier.testTag("editor"))
        }
        onNodeWithTag("editor").performTextInput("one")
        waitForIdle()
        state.toggleOrderedList()
        waitForIdle()
        onNodeWithTag("editor").performTextInput("\n")
        waitForIdle()
        onNodeWithTag("editor").performTextInput("two")
        waitForIdle()

        val layout = assertNotNull(state.textLayoutResult)
        val secondItemStart = paragraphStartOffsets(state)[1]
        assertEquals(
            1,
            layout.getLineForOffset(secondItemStart),
            "the second item must start on the second rendered line",
        )
        assertEquals(
            "2. ",
            layout.layoutInput.text.text.substring(secondItemStart, secondItemStart + 3),
            "the second item's number prefix should be laid out at the start of its line",
        )
        assertRenderParity(state, "ordered list marker")
    }
}
