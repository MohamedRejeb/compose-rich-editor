package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.EmptyLineAnchor
import com.mohamedrejeb.richeditor.model.RichTextHighlight
import com.mohamedrejeb.richeditor.model.RichTextState
import java.awt.datatransfer.DataFlavor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Issue #564: there was no way to mark ranges of the text, such as the matches of a find-in-text
 * feature, without changing the document.
 *
 * [RichTextState.highlights] is projected over the text when it is rendered: by the editor's
 * OutputTransformation and by the read-only [BasicRichText]. These tests drive the real
 * composables and assert on the laid out text, which must carry each highlight after the
 * document's own styles so the highlight wins, while the document, the clipboard and the undo
 * history stay free of it.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class Issue564HighlightsRenderTest {

    private val match = SpanStyle(background = Color.Yellow)
    private val focusedMatch = SpanStyle(background = Color.Red, color = Color.White)

    @Test
    fun `the editor lays out the highlights`() = runEditorTest { state, _ ->
        state.highlights = listOf(
            RichTextHighlight(TextRange(0, 5), match),
            RichTextHighlight(TextRange(12, 17), focusedMatch),
        )
        waitForIdle()

        val layout = editorLayout(state)
        assertEquals(Color.Yellow, layout.backgroundAt(0))
        assertEquals(Color.Yellow, layout.backgroundAt(4))
        assertEquals(Color.Unspecified, layout.backgroundAt(5))
        assertEquals(Color.Red, layout.backgroundAt(12))
        assertEquals(Color.White, layout.colorAt(16))
        assertEquals(Color.Unspecified, layout.backgroundAt(17))
    }

    @Test
    fun `setting and clearing highlights keeps the output transformation instance`() =
        runEditorTest { state, _ ->
            val transformation = state.outputTransformation

            state.highlights = listOf(RichTextHighlight(TextRange(0, 5), match))
            waitForIdle()
            assertEquals(Color.Yellow, editorLayout(state).backgroundAt(2))

            state.highlights = emptyList()
            waitForIdle()
            assertEquals(Color.Unspecified, editorLayout(state).backgroundAt(2))

            assertSame(transformation, state.outputTransformation)
        }

    @Test
    fun `a highlight background wins over a span background`() = runEditorTest(
        html = "<p><span style=\"background: blue\">Hello</span> World</p>",
    ) { state, _ ->
        assertEquals(Color.Blue, editorLayout(state).backgroundAt(2))

        state.highlights = listOf(RichTextHighlight(TextRange(1, 4), match))
        waitForIdle()

        val layout = editorLayout(state)
        assertEquals(Color.Blue, layout.backgroundAt(0))
        assertEquals(Color.Yellow, layout.backgroundAt(2))
        assertEquals(Color.Blue, layout.backgroundAt(4))
    }

    @Test
    fun `a highlight background is masked under the selection`() = runEditorTest { state, _ ->
        state.highlights = listOf(RichTextHighlight(TextRange(0, 5), focusedMatch))
        select(state, TextRange(2, 4))

        val selected = editorLayout(state)
        assertEquals(Color.Red, selected.backgroundAt(1))
        assertEquals(Color.Transparent, selected.backgroundAt(2))
        assertEquals(Color.White, selected.colorAt(2))
        assertEquals(Color.Red, selected.backgroundAt(4))

        select(state, TextRange(4))

        assertEquals(Color.Red, editorLayout(state).backgroundAt(2))
    }

    @Test
    fun `typing keeps the highlights at their offsets`() = runEditorTest(html = "<p>Hello</p>") { state, _ ->
        state.highlights = listOf(RichTextHighlight(TextRange(0, 5), match))
        select(state, TextRange(5))

        onNodeWithTag(EDITOR_TAG).performTextInput(" World")
        waitForIdle()

        assertEquals("<p>Hello World</p>", state.toHtml())
        val layout = editorLayout(state)
        assertEquals(Color.Yellow, layout.backgroundAt(4))
        assertEquals(Color.Unspecified, layout.backgroundAt(5))
    }

    @Test
    fun `deleting text under and before a highlight clamps it`() = runEditorTest(html = "<p>Hello World</p>") { state, _ ->
        state.highlights = listOf(
            RichTextHighlight(TextRange(6, 11), match),
            RichTextHighlight(TextRange(9, 30), focusedMatch),
        )
        select(state, TextRange(11))

        repeat(4) {
            onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.Backspace) }
            waitForIdle()
        }

        assertEquals("Hello W", state.annotatedString.text)
        val layout = editorLayout(state)
        assertEquals(Color.Yellow, layout.backgroundAt(6))
        assertTrue(layout.layoutInput.text.spanStyles.all { it.end <= 7 })

        select(state, TextRange(0, 7))
        onNodeWithTag(EDITOR_TAG).performKeyInput { pressKey(Key.Backspace) }
        waitForIdle()

        assertEquals("", state.annotatedString.text)
        onNodeWithTag(EDITOR_TAG).performTextInput("ab")
        waitForIdle()
        assertEquals("ab", state.annotatedString.text)
    }

    @Test
    fun `a highlight over a trailing empty paragraph stops at the model text`() =
        runEditorTest(html = "<p>Hello</p>") { state, _ ->
            select(state, TextRange(5))
            onNodeWithTag(EDITOR_TAG).performTextInput("\n")
            waitForIdle()
            assertEquals("Hello ", state.annotatedString.text)

            state.highlights = listOf(RichTextHighlight(TextRange(0, 100), match))
            waitForIdle()

            val layout = editorLayout(state)
            assertEquals("Hello $EmptyLineAnchor", layout.layoutInput.text.text)
            assertEquals(Color.Yellow, layout.backgroundAt(5))
            assertEquals(Color.Unspecified, layout.backgroundAt(6))
        }

    @Test
    fun `undo after typing restores the text and keeps the highlights`() =
        runEditorTest(html = "<p>Hello</p>") { state, _ ->
            val highlights = listOf(RichTextHighlight(TextRange(0, 5), match))
            state.highlights = highlights
            select(state, TextRange(5))
            assertFalse(state.history.canUndo)

            onNodeWithTag(EDITOR_TAG).performTextInput("!")
            waitForIdle()
            state.history.undo()
            waitForIdle()

            assertEquals("Hello", state.annotatedString.text)
            assertEquals(highlights, state.highlights)
            assertEquals(Color.Yellow, editorLayout(state).backgroundAt(2))
        }

    @Test
    fun `copying highlighted text copies it without the highlight`() = runEditorTest { state, clipboard ->
        val range = TextRange(0, 11)
        val expectedHtml = state.toHtml(range)
        state.highlights = listOf(RichTextHighlight(TextRange(0, 5), match))
        select(state, range)

        onNodeWithTag(EDITOR_TAG).performKeyInput {
            keyDown(shortcutModifierKey)
            pressKey(Key.C)
            keyUp(shortcutModifierKey)
        }
        waitForIdle()

        val contents = assertNotNull(clipboard.awt.getContents(null), "Expected clipboard contents")
        assertEquals("Hello World", contents.getTransferData(DataFlavor.stringFlavor))
        val html = contents.getTransferData(DataFlavor.fragmentHtmlFlavor) as String
        assertEquals(expectedHtml, html)
        assertFalse(html.contains("background"), "Expected no highlight in the copied html: $html")
    }

    @Test
    fun `the read-only text lays out the highlights`() = runDesktopComposeUiTest {
        val state = RichTextState()
        state.setHtml("<p><span style=\"background: blue\">Hello</span> World</p>")
        var layout: TextLayoutResult? = null

        setContent {
            BasicRichText(state = state, onTextLayout = { layout = it })
        }
        waitForIdle()
        assertEquals(Color.Blue, assertNotNull(layout).backgroundAt(2))

        state.highlights = listOf(
            RichTextHighlight(TextRange(1, 4), match),
            RichTextHighlight(TextRange(6, 100), focusedMatch),
        )
        waitForIdle()

        val highlighted = assertNotNull(layout)
        assertEquals("Hello World", highlighted.layoutInput.text.text)
        assertEquals(Color.Blue, highlighted.backgroundAt(0))
        assertEquals(Color.Yellow, highlighted.backgroundAt(2))
        assertEquals(Color.Red, highlighted.backgroundAt(10))

        state.highlights = emptyList()
        waitForIdle()

        assertEquals(Color.Blue, assertNotNull(layout).backgroundAt(2))
        assertEquals(Color.Unspecified, assertNotNull(layout).backgroundAt(10))
    }

    @Test
    fun `the expandable read-only text renders the highlights`() = runDesktopComposeUiTest {
        val state = RichTextState()
        state.setHtml("<p>Hello World</p>")
        state.highlights = listOf(RichTextHighlight(TextRange(6, 11), match))

        setContent {
            ExpandableBasicRichText(
                state = state,
                expanded = false,
                onExpandedChange = {},
                modifier = Modifier.testTag(EDITOR_TAG),
            )
        }
        waitForIdle()

        val rendered = onNodeWithTag(EDITOR_TAG).fetchSemanticsNode().config[SemanticsProperties.Text].single()
        assertEquals("Hello World", rendered.text)
        assertEquals(listOf(AnnotatedString.Range(match, 6, 11)), rendered.spanStyles.filter { it.item == match })
    }

    private class RecordingClipboard : Clipboard {
        val awt = java.awt.datatransfer.Clipboard("test")

        override val nativeClipboard: NativeClipboard get() = awt

        override suspend fun getClipEntry(): ClipEntry? = null

        override suspend fun setClipEntry(clipEntry: ClipEntry?) = Unit
    }

    private fun runEditorTest(
        html: String = "<p><b>Hello</b> World</p><p>Hello again</p>",
        block: DesktopComposeUiTest.(state: RichTextState, clipboard: RecordingClipboard) -> Unit,
    ) = runDesktopComposeUiTest {
        val state = RichTextState()
        state.setHtml(html)
        val clipboard = RecordingClipboard()

        setContent {
            val focusRequester = remember { FocusRequester() }
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                Box {
                    BasicRichTextEditor(
                        state = state,
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .testTag(EDITOR_TAG),
                    )
                }
            }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
        waitForIdle()

        block(state, clipboard)
    }

    private fun DesktopComposeUiTest.select(state: RichTextState, range: TextRange) {
        state.textFieldState.edit { selection = range }
        waitForIdle()
    }

    private fun editorLayout(state: RichTextState): TextLayoutResult =
        assertNotNull(state.textLayoutResult, "Expected a text layout result")

    /** The last range covering an offset is the one the text is drawn with. */
    private fun TextLayoutResult.backgroundAt(offset: Int): Color =
        stylesAt(offset).lastOrNull { it.background != Color.Unspecified }?.background ?: Color.Unspecified

    private fun TextLayoutResult.colorAt(offset: Int): Color =
        stylesAt(offset).lastOrNull { it.color != Color.Unspecified }?.color ?: Color.Unspecified

    private fun TextLayoutResult.stylesAt(offset: Int): List<SpanStyle> =
        layoutInput.text.spanStyles
            .filter { range: AnnotatedString.Range<SpanStyle> -> offset in range.start until range.end }
            .map { it.item }

    private companion object {
        const val EDITOR_TAG = "editor"

        // The framework maps clipboard shortcuts to Meta on macOS and to Ctrl elsewhere.
        val shortcutModifierKey: Key =
            if (System.getProperty("os.name") == "Mac OS X") Key.MetaLeft else Key.CtrlLeft
    }
}
