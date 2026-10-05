package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The background a code span draws behind its text must be painted whatever the shape of the
 * rest of the document.
 *
 * The drawing is skipped while the text layout belongs to an older text, which was checked by
 * comparing lengths. An empty last paragraph adds an output-only character to the laid out
 * text, so every span background disappeared as soon as the document ended with an empty line.
 */
@OptIn(ExperimentalTestApi::class)
class CustomSpanDrawingTest {

    @Test
    fun `a code span is drawn in a document that ends with text`() = runDesktopComposeUiTest {
        setEditor("<p><code>CODE SPAN</code> text</p>")

        assertTrue(codeSpanPixels() > 0)
    }

    @Test
    fun `a code span is drawn in a document that ends with an empty paragraph`() = runDesktopComposeUiTest {
        setEditor("<p><code>CODE SPAN</code> text</p><p><br></p>")

        assertTrue(codeSpanPixels() > 0)
    }

    private fun DesktopComposeUiTest.setEditor(html: String) {
        val state = RichTextState()
        state.config.codeSpanBackgroundColor = Color.Red
        state.config.codeSpanStrokeColor = Color.Red
        state.setHtml(html)
        setContent {
            Box(Modifier.width(300.dp).background(Color.White).testTag(ROOT_TAG)) {
                BasicRichTextEditor(state = state, modifier = Modifier.fillMaxWidth())
            }
        }
        waitForIdle()
    }

    private fun DesktopComposeUiTest.codeSpanPixels(): Int {
        val pixels = onNodeWithTag(ROOT_TAG).captureToImage().toPixelMap()
        var count = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val color = pixels[x, y]
                if (color.red > 0.8f && color.green < 0.3f && color.blue < 0.3f) count++
            }
        }
        return count
    }

    private companion object {
        const val ROOT_TAG = "root"
    }
}
