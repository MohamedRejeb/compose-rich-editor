package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.toTextFieldBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [reconcileBufferWithModel] used to rewrite the whole buffer, which the Android IME reads as a
 * text reset and answers by restarting input: the keyboard blinks the moment "- " turns into a
 * list. These pin the replace down to the region that actually differs.
 *
 * Suppressing the state sync reproduces the replay shape exactly: the model text moves ahead
 * while [RichTextState.textFieldState] still holds the pre-injection text.
 */
@OptIn(ExperimentalFoundationApi::class)
class EditPipelineReconcileBufferTest {

    private fun staleBufferFor(state: RichTextState): TextFieldBuffer =
        state.textFieldState.toTextFieldBuffer()

    @Test
    fun `injecting a list prefix changes only the injected region`() {
        val state = RichTextState()
        state.setText("item")
        state.skipTextFieldStateSync = true
        state.toggleUnorderedList()

        val bufferText = state.textFieldState.text.toString()
        val targetText = state.annotatedString.text
        val markerLength = targetText.length - bufferText.length
        assertTrue(markerLength > 0, "the toggle should have injected a marker")

        val buffer = staleBufferFor(state)
        state.reconcileBufferWithModel(buffer)

        assertEquals(targetText, buffer.asCharSequence().toString())
        assertEquals(1, buffer.changes.changeCount)
        assertEquals(0, buffer.changes.getOriginalRange(0).min)
        assertEquals(
            0,
            buffer.changes.getOriginalRange(0).max,
            "a pure insertion must not claim any of the original text",
        )
        assertEquals(markerLength, buffer.changes.getRange(0).max)
    }

    @Test
    fun `removing a list prefix changes only the removed region`() {
        val state = RichTextState()
        state.setText("item")
        state.toggleUnorderedList()
        state.skipTextFieldStateSync = true
        state.toggleUnorderedList()

        val bufferText = state.textFieldState.text.toString()
        val targetText = state.annotatedString.text
        val markerLength = bufferText.length - targetText.length
        assertTrue(markerLength > 0, "the second toggle should have removed the marker")

        val buffer = staleBufferFor(state)
        state.reconcileBufferWithModel(buffer)

        assertEquals(targetText, buffer.asCharSequence().toString())
        assertEquals(1, buffer.changes.changeCount)
        assertEquals(0, buffer.changes.getOriginalRange(0).min)
        assertEquals(markerLength, buffer.changes.getOriginalRange(0).max)
        assertEquals(
            0,
            buffer.changes.getRange(0).max,
            "the removal must not rewrite the surviving text",
        )
    }

    @Test
    fun `a buffer that already matches the model is left untouched`() {
        val state = RichTextState()
        state.setText("item")

        val buffer = staleBufferFor(state)
        state.reconcileBufferWithModel(buffer)

        assertEquals(state.annotatedString.text, buffer.asCharSequence().toString())
        assertEquals(0, buffer.changes.changeCount)
    }
}
