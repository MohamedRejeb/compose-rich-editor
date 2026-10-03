package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * Regression pin for #730/#731 on the legacy value bridge: a pure selection change on
 * background-free content must not rebuild [RichTextState.annotatedString]. The #635
 * selection mask only changes the output when a span carries a background color; the
 * editor's OutputTransformation reads the annotated string, so a rebuild per selection
 * change would re-style the output on every gesture tick for an unchanged rendering.
 */
class Issue730SelectionRegressionTest {

    @Test
    fun `pure selection change without background spans must not rebuild the annotated string`() {
        val state = RichTextState()
        state.setText("alpha beta gamma\ndelta epsilon zeta")
        val text = state.textFieldValue.text

        val before = state.annotatedString

        // Mimic the platform's long-press word selection: same text, new selection
        state.onTextFieldValueChange(TextFieldValue(text, TextRange(6, 10)))

        assertSame(
            before,
            state.annotatedString,
            "A pure selection change on content without background spans rebuilt the " +
                "annotated string, re-styling the output mid-gesture (#730, #731).",
        )
    }
}
