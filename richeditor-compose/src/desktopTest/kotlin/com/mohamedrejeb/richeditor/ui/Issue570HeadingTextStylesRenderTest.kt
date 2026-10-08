package com.mohamedrejeb.richeditor.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Issue 570: `RichTextConfig.headingTextStyles` is applied when the text is rendered, so
 * changing it while the content is on screen has to reach the laid out text of both the
 * editor and the read-only `BasicRichText`, with no edit or selection change in between.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRichTextApi::class)
class Issue570HeadingTextStylesRenderTest {

    @Test
    fun `changing the heading styles re-lays out the editor`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setHtml("<h1>Title</h1><p>Body</p>") }
        var layout: TextLayoutResult? = null

        setContent {
            BasicRichTextEditor(state = state, onTextLayout = { layout = it })
        }
        waitForIdle()
        val heightBefore = assertNotNull(layout).size.height

        state.config.headingTextStyles = mapOf(HeadingStyle.H1 to TextStyle(fontSize = 80.sp))
        waitForIdle()

        assertHeadingLaidOutAt80Sp(assertNotNull(layout), heightBefore)
    }

    @Test
    fun `changing the heading styles re-lays out the read-only text`() = runDesktopComposeUiTest {
        val state = RichTextState().apply { setHtml("<h1>Title</h1><p>Body</p>") }
        var layout: TextLayoutResult? = null

        setContent {
            BasicRichText(state = state, onTextLayout = { layout = it })
        }
        waitForIdle()
        val heightBefore = assertNotNull(layout).size.height

        state.config.headingTextStyles = mapOf(HeadingStyle.H1 to TextStyle(fontSize = 80.sp))
        waitForIdle()

        assertHeadingLaidOutAt80Sp(assertNotNull(layout), heightBefore)
    }

    private fun assertHeadingLaidOutAt80Sp(layout: TextLayoutResult, heightBefore: Int) {
        assertTrue(
            layout.layoutInput.text.spanStyles.any { it.item.fontSize == 80.sp && it.start == 0 && it.end >= 5 },
            "the laid out text carries the heading style",
        )
        assertTrue(layout.size.height > heightBefore, "the heading takes more room at the larger size")
    }
}
