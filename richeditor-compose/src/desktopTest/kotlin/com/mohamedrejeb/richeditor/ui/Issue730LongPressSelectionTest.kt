package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextState
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Regression pins for #730 (long press word selection and select-all) and #731 (selection
 * handle drag) on the legacy value bridge: a selection the platform sets with no gesture
 * live is applied as is, with or without a layout.
 */
@OptIn(ExperimentalTestApi::class)
class Issue730LongPressSelectionTest {

    private fun RichTextState.platformSelection(selection: TextRange) {
        onTextFieldValueChange(TextFieldValue(textFieldValue.text, selection))
    }

    /** First frames, or an editor recycled into a lazy list: no layout exists yet. */
    @Test
    fun `a word selection arriving before any layout exists is applied`() {
        val state = RichTextState()
        state.setText("alpha beta gamma")

        state.platformSelection(TextRange(6, 10))

        assertEquals(TextRange(6, 10), state.selection)
    }

    @Test
    fun `word selection then handle extensions then select all are applied with a real layout`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            val state = RichTextState()
            setContent {
                BasicRichTextEditor(state = state, modifier = Modifier.fillMaxWidth())
            }
            state.setText("alpha beta gamma\ndelta epsilon zeta\nlast line words")
            waitForIdle()

            state.platformSelection(TextRange(0, 5))
            assertEquals(TextRange(0, 5), state.selection)

            listOf(10, 16, 22).forEach { end ->
                state.platformSelection(TextRange(0, end))
                assertEquals(TextRange(0, end), state.selection)
            }

            state.platformSelection(TextRange(17, 22))
            assertEquals(TextRange(17, 22), state.selection)

            val length = state.textFieldValue.text.length
            state.platformSelection(TextRange(0, length))
            assertEquals(TextRange(0, length), state.selection)
        }
}
