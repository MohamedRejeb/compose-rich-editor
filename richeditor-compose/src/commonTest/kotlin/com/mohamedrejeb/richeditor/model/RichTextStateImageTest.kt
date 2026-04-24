package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for issue #466 - typing on a text field renders
 * broken characters when an image exists.
 *
 * Root cause hypothesis: an `<img>` span inserts an
 * [androidx.compose.foundation.text.appendInlineContent] placeholder
 * (a `\uFFFC` object-replacement char) into the rendered
 * [RichTextState.annotatedString], but the underlying
 * [RichTextState.textFieldValue] text gets no corresponding
 * character. This desyncs their lengths, and because the
 * [OffsetMapping] is [OffsetMapping.Identity], Compose uses the
 * annotated length to compute offsets while the raw text is shorter -
 * so subsequent edits land at the wrong positions and typed
 * characters appear garbled.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextStateImageTest {

    @Test
    fun testImageDoesNotDesyncAnnotatedAndRawLengths() {
        val state = RichTextState()
        state.setHtml("""<p>Hello <img src="test.png" width="10" height="10" alt="img"/> World</p>""")

        assertEquals(
            state.annotatedString.text.length,
            state.textFieldValue.text.length,
            "annotatedString and textFieldValue must have the same length (image span must " +
                "reserve a char in raw text to match the inline-content placeholder). " +
                "annotated=<${state.annotatedString.text}>, raw=<${state.textFieldValue.text}>",
        )
    }

    @Test
    fun testTypingAfterImagePreservesTypedChars() {
        val state = RichTextState()
        state.setHtml("""<p>Hello <img src="test.png" width="10" height="10" alt="img"/></p>""")

        val before = state.textFieldValue.text
        val pos = before.length
        state.selection = TextRange(pos)

        val typed = "XYZ"
        val newText = before.substring(0, pos) + typed + before.substring(pos)
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(pos + typed.length))
        )

        assertEquals(
            newText,
            state.textFieldValue.text,
            "Typed characters after an image should be preserved in the underlying text.",
        )
    }

    @Test
    fun testTypingBeforeImagePreservesTypedChars() {
        val state = RichTextState()
        state.setHtml("""<p><img src="test.png" width="10" height="10" alt="img"/></p>""")

        val before = state.textFieldValue.text
        state.selection = TextRange(0)

        val typed = "ABC"
        val newText = typed + before
        state.onTextFieldValueChange(
            TextFieldValue(text = newText, selection = TextRange(typed.length))
        )

        assertEquals(newText, state.textFieldValue.text)
    }

    @Test
    fun testMultipleImagesKeepLengthsInSync() {
        val state = RichTextState()
        state.setHtml(
            """<p>A <img src="a.png" width="10" height="10"/> """ +
                """B <img src="b.png" width="10" height="10"/> """ +
                """C</p>"""
        )

        assertEquals(
            state.annotatedString.text.length,
            state.textFieldValue.text.length,
            "Multiple images must not desync lengths.",
        )
    }
}
