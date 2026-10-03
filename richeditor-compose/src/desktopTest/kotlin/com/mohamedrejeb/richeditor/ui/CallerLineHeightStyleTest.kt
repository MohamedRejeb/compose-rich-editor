package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The editor lays out the caller's `lineHeight` and `lineHeightStyle` as given, so an editor
 * and a [BasicRichText] showing the same document with the same style have the same line
 * boxes. Material3 typography sets `LineHeightStyle(Center, Trim.None)` with an explicit line
 * height; forcing `Trim.Both` on it would shrink every single-line paragraph to the font height
 * and leave the editor shorter than the read-only view of the same text.
 */
@OptIn(ExperimentalTestApi::class)
class CallerLineHeightStyleTest {

    private val material3Like = TextStyle(
        fontSize = 16.sp,
        lineHeight = 32.sp,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )

    private fun TextLayoutResult.lineBoxes(): List<Pair<Float, Float>> =
        (0 until lineCount).map { getLineTop(it) to getLineBottom(it) }

    /** Lays out the same document in an editor and a read-only view, then checks both. */
    private fun layouts(
        width: Dp,
        load: RichTextState.() -> Unit,
        block: (lineHeightPx: Float, editor: TextLayoutResult, view: TextLayoutResult) -> Unit,
    ) = runDesktopComposeUiTest(width = 500, height = 600) {
        val editorState = RichTextState().apply(load)
        val viewState = RichTextState().apply(load)
        var viewLayout: TextLayoutResult? = null
        setContent {
            Column {
                BasicRichTextEditor(
                    state = editorState,
                    textStyle = material3Like,
                    modifier = Modifier.width(width),
                )
                BasicRichText(
                    state = viewState,
                    style = material3Like,
                    onTextLayout = { viewLayout = it },
                    modifier = Modifier.width(width),
                )
            }
        }
        waitForIdle()
        val lineHeightPx = with(density) { 32.sp.toPx() }
        block(lineHeightPx, checkNotNull(editorState.textLayoutResult), checkNotNull(viewLayout))
    }

    private fun uniformBoxes(lineCount: Int, lineHeightPx: Float): List<Pair<Float, Float>> =
        List(lineCount) { it * lineHeightPx to (it + 1) * lineHeightPx }

    @Test
    fun `single line paragraphs keep the caller's line height`() =
        layouts(width = 400.dp, load = { setText("One\nTwo\nThree") }) { lineHeightPx, editor, view ->
            assertEquals(uniformBoxes(3, lineHeightPx), editor.lineBoxes())
            assertEquals(view.lineBoxes(), editor.lineBoxes())
        }

    @Test
    fun `a wrapped paragraph followed by a short one has uniform line boxes`() =
        layouts(
            width = 180.dp,
            load = { setText("One two three four five six seven eight nine ten eleven twelve\nShort") },
        ) { lineHeightPx, editor, view ->
            // How many lines the first paragraph wraps into depends on the platform font.
            assertTrue(editor.lineCount > 2, "the first paragraph must wrap")
            assertEquals(uniformBoxes(editor.lineCount, lineHeightPx), editor.lineBoxes())
            assertEquals(view.lineBoxes(), editor.lineBoxes())
        }

    @Test
    fun `a heading inside a fixed line height document lays out like the read-only view`() =
        layouts(width = 400.dp, load = { setHtml("<h1>Title</h1><p>Body</p>") }) { _, editor, view ->
            assertEquals(2, editor.lineCount)
            assertEquals(view.lineBoxes(), editor.lineBoxes())
        }
}
