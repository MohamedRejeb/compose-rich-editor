package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pins that a style mutation never replaces the editor's OutputTransformation instance.
 *
 * BasicTextField remembers its TransformedTextFieldState on that instance, and a new one makes
 * the decorator node restart the input session and recreates the selection and layout state.
 * The transformation reads `annotatedString`, which is snapshot state, inside the framework's
 * derived state, so a style-only change re-runs it with the instance left in place.
 *
 * Desktop tests cannot observe an input session restart, so instance identity stands in for it.
 * Every case also asserts that the mutation changed the model and that the text layout carries
 * exactly the model's styles, because a style change must still reach the layout.
 */
@OptIn(ExperimentalTestApi::class)
class OutputTransformationIdentityTest {

    @Test
    fun `toggling a span style over a selection keeps the instance`() = runEditorTest { state ->
        select(state, TextRange(0, 5))

        assertStyleMutationKeepsInstance(state) {
            state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        }
    }

    @Test
    fun `toggling a code span over a selection keeps the instance`() = runEditorTest { state ->
        select(state, TextRange(0, 5))

        assertStyleMutationKeepsInstance(state) {
            state.toggleCodeSpan()
        }
    }

    @Test
    fun `adding updating and removing a link keeps the instance`() = runEditorTest { state ->
        select(state, TextRange(0, 5))

        assertStyleMutationKeepsInstance(state) {
            state.addLinkToSelection(url = "https://example.com")
        }
        // The url is not part of the rendered style, so the model's styles stay equal here.
        assertStyleMutationKeepsInstance(state, changesStyles = false) {
            state.updateLink(url = "https://example.org")
        }
        assertEquals("https://example.org", state.selectedLinkUrl)
        assertStyleMutationKeepsInstance(state) {
            state.removeLink()
        }
    }

    @Test
    fun `setting a heading style keeps the instance`() = runEditorTest { state ->
        assertStyleMutationKeepsInstance(state) {
            state.setHeadingStyle(HeadingStyle.H1)
        }
    }

    @Test
    fun `adding and removing a paragraph style keeps the instance`() = runEditorTest { state ->
        val centered = ParagraphStyle(textAlign = TextAlign.Center)

        assertStyleMutationKeepsInstance(state) {
            state.addParagraphStyle(centered)
        }
        assertStyleMutationKeepsInstance(state) {
            state.removeParagraphStyle(centered)
        }
    }

    @Test
    fun `changing the config keeps the instance`() = runEditorTest { state ->
        select(state, TextRange(0, 5))
        state.addLinkToSelection(url = "https://example.com")
        waitForIdle()

        assertStyleMutationKeepsInstance(state) {
            @Suppress("DEPRECATION")
            state.setConfig(linkColor = Color.Red)
        }
    }

    @Test
    fun `measuring a list prefix for the first time keeps the instance`() = runEditorTest { state ->
        assertTrue(state.startTextWidthCache.isEmpty(), "Expected no prefix measured yet")

        assertStyleMutationKeepsInstance(state) {
            state.toggleUnorderedList()
        }

        assertTrue(state.startTextWidthCache.isNotEmpty(), "Expected the list prefix to be measured")
    }

    private fun runEditorTest(
        block: DesktopComposeUiTest.(state: RichTextState) -> Unit,
    ) = runDesktopComposeUiTest {
        val state = RichTextState()

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

        onNodeWithTag(EDITOR_TAG).performTextInput("hello world")
        waitForIdle()

        block(state)
    }

    /**
     * A programmatic write, which leaves the text and the transformation untouched.
     */
    private fun DesktopComposeUiTest.select(state: RichTextState, range: TextRange) {
        state.textFieldState.edit { selection = range }
        waitForIdle()
    }

    private fun DesktopComposeUiTest.assertStyleMutationKeepsInstance(
        state: RichTextState,
        changesStyles: Boolean = true,
        mutate: () -> Unit,
    ) {
        val before = state.outputTransformation
        val stylesBefore = state.annotatedString

        mutate()
        waitForIdle()

        if (changesStyles) {
            assertNotEquals(stylesBefore, state.annotatedString, "Expected the mutation to change the styles")
        }
        assertStylesReachedLayout(state)
        assertSame(
            before,
            state.outputTransformation,
            "A style mutation must not replace the OutputTransformation instance",
        )
    }

    private fun assertStylesReachedLayout(state: RichTextState) {
        val layoutResult = assertNotNull(state.textLayoutResult, "Expected a text layout result")
        val rendered = layoutResult.layoutInput.text

        assertEquals(
            state.annotatedString.spanStyles.toStyleSet(),
            rendered.spanStyles.toStyleSet(),
            "Expected the text layout input to carry the model's span styles",
        )
        assertEquals(
            state.annotatedString.paragraphStyles.toStyleSet(),
            rendered.paragraphStyles.toStyleSet(),
            "Expected the text layout input to carry the model's paragraph styles",
        )
    }

    /**
     * Collapsed ranges are left out: the transformation drops them on purpose.
     */
    private fun <T> List<AnnotatedString.Range<T>>.toStyleSet(): Set<Triple<T, Int, Int>> =
        filter { it.start != it.end }
            .map { Triple(it.item, it.start, it.end) }
            .toSet()

    private companion object {
        const val EDITOR_TAG = "editor"
    }
}
