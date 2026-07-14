package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Regression pin for #730/#731: a pure selection change on background-free content must
 * not invalidate the visual transformation. The #635 selection mask only changes the
 * output when a span carries a background color; rebuilding otherwise recreates the
 * visualTransformation mid-gesture, and Android's legacy BasicTextField re-filters on
 * every change, fighting the selection manager.
 */
class Issue730SelectionRegressionTest {

    @Test
    fun `pure selection change without background spans must not rebuild the visual transformation`() {
        val state = RichTextState()
        state.setText("alpha beta gamma\ndelta epsilon zeta")
        val text = state.textFieldValue.text

        val before = state.visualTransformation

        // Mimic the platform's long-press word selection: same text, new selection
        state.onTextFieldValueChange(TextFieldValue(text, TextRange(6, 10)))

        assertSame(
            before,
            state.visualTransformation,
            "A pure selection change on content without background spans rebuilt the " +
                "visual transformation. On Android this re-filter mid-gesture breaks " +
                "long-press word selection, select-all and handle dragging (#730, #731).",
        )
    }
}
