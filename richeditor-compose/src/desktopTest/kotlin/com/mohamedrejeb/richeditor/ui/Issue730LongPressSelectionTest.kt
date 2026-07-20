package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Gesture-level mimics for #730 (long press selection and select-all) and #731
 * (selection handle drag). Every press registers the press position for 300ms, during
 * which pure selection changes route through adjustSelection; the platform delivers
 * word selection, handle drags and select-all as plain onTextFieldValueChange calls,
 * and these tests mimic that sequence.
 */
@OptIn(ExperimentalTestApi::class)
class Issue730LongPressSelectionTest {

    /**
     * The press is registered but no layout exists yet (first frames, or editor
     * recycled into a lazy list): the platform's word selection must still be applied.
     */
    @Test
    fun `word selection arriving while press is registered without layout must not be dropped`() =
        runBlocking {
            val state = RichTextState()
            state.setText("alpha beta gamma")
            val text = state.textFieldValue.text

            // Mimic the press registration the editor performs on PressInteraction.Press
            val pressJob = launch(Dispatchers.Default) {
                state.adjustSelectionAndRegisterPressPosition(Offset(12f, 8f))
            }
            delay(80) // inside the 300ms press window

            // Platform long-press: select the word "beta"
            state.onTextFieldValueChange(TextFieldValue(text, TextRange(6, 10)))
            val applied = state.selection

            pressJob.cancel()

            assertEquals(
                TextRange(6, 10),
                applied,
                "The word selection was dropped because a press was registered and " +
                    "textLayoutResult was null (#730)",
            )
        }

    /**
     * Full gesture mimic with a real composed editor (layout available):
     * press, long-press word selection inside the 300ms window, drag-handle extensions,
     * a second press, then select-all. Every selection the platform sets must be honored.
     */
    @Test
    fun `mimicked long press word selection then drag then select all with real layout`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            mainClock.autoAdvance = false
            val state = RichTextState()
            lateinit var scope: CoroutineScope
            setContent {
                scope = rememberCoroutineScope()
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            mainClock.advanceTimeBy(64)
            state.setText("alpha beta gamma\ndelta epsilon zeta\nlast line words")
            mainClock.advanceTimeBy(64)

            val text = state.textFieldValue.text
            val issues = mutableListOf<String>()

            fun expectSelection(label: String, expected: TextRange) {
                if (state.selection != expected) {
                    issues += "$label: expected $expected, actual ${state.selection}"
                }
            }

            // Press at the start of the field, then the platform's long-press word
            // selection arrives while the press is still registered (within 300ms)
            scope.launch { state.adjustSelectionAndRegisterPressPosition(Offset(8f, 8f)) }
            mainClock.advanceTimeBy(48)
            state.onTextFieldValueChange(TextFieldValue(text, TextRange(0, 5)))
            mainClock.advanceTimeBy(16)
            expectSelection("word selection within press window", TextRange(0, 5))

            // Drag the end handle: successive extensions while the window expires
            listOf(10, 16, 22).forEach { end ->
                state.onTextFieldValueChange(TextFieldValue(state.textFieldValue.text, TextRange(0, end)))
                mainClock.advanceTimeBy(96)
                expectSelection("drag extension to $end", TextRange(0, end))
            }

            // Let the press window fully expire, then a long-press on the second line
            // arrives after the window (real Android long-press timeout > 300ms)
            mainClock.advanceTimeBy(400)
            scope.launch { state.adjustSelectionAndRegisterPressPosition(Offset(60f, 30f)) }
            mainClock.advanceTimeBy(350)
            state.onTextFieldValueChange(TextFieldValue(state.textFieldValue.text, TextRange(17, 22)))
            mainClock.advanceTimeBy(16)
            expectSelection("word selection after press window expired", TextRange(17, 22))

            // Select all from the toolbar (no press on the field)
            val len = state.textFieldValue.text.length
            state.onTextFieldValueChange(TextFieldValue(state.textFieldValue.text, TextRange(0, len)))
            mainClock.advanceTimeBy(16)
            expectSelection("select all", TextRange(0, len))

            if (issues.isNotEmpty()) {
                fail("Selection gestures were not honored:\n" + issues.joinToString("\n"))
            }
        }

    /**
     * Seeded fuzz over the gesture surface with a real layout: random presses (with the
     * 300ms window sometimes active, sometimes expired), random word/range selections,
     * drag extensions and occasional typing. Oracles: no exception, the selection the
     * platform set is honored for non-collapsed selections, and the annotated string
     * stays in sync with the text field value.
     */
    @Test
    fun `selection gesture fuzz with real layout`() =
        runDesktopComposeUiTest(width = 480, height = 360) {
            mainClock.autoAdvance = false
            val state = RichTextState()
            lateinit var scope: CoroutineScope
            setContent {
                scope = rememberCoroutineScope()
                BasicRichTextEditor(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            mainClock.advanceTimeBy(64)

            for (seed in 0 until 30) {
                val random = Random(seed)
                val opLog = mutableListOf<String>()
                state.setMarkdown(
                    when (seed % 3) {
                        0 -> "alpha beta gamma\n\ndelta epsilon zeta\n\nlast line words"
                        1 -> "- item one\n- item two\n- item three"
                        else -> "1. first entry\n2. second entry\n3. third entry"
                    },
                )
                mainClock.advanceTimeBy(64)

                fun finding(message: String): Nothing =
                    fail(
                        buildString {
                            appendLine("Selection gesture fuzz finding, seed=$seed")
                            appendLine(message)
                            opLog.forEach { appendLine("  $it") }
                        },
                    )

                repeat(12) {
                    val text = state.textFieldValue.text
                    try {
                        when (random.nextInt(10)) {
                            in 0..2 -> {
                                val x = random.nextInt(200).toFloat()
                                val y = random.nextInt(80).toFloat()
                                opLog += "press($x,$y)"
                                scope.launch {
                                    state.adjustSelectionAndRegisterPressPosition(Offset(x, y))
                                }
                                mainClock.advanceTimeBy(random.nextInt(16, 64).toLong())
                            }

                            in 3..5 -> {
                                if (text.isNotEmpty()) {
                                    val a = random.nextInt(text.length)
                                    val b = (a + 1 + random.nextInt(text.length - a))
                                        .coerceAtMost(text.length)
                                    opLog += "select[$a,$b]"
                                    state.onTextFieldValueChange(TextFieldValue(text, TextRange(a, b)))
                                    mainClock.advanceTimeBy(16)
                                    if (state.selection != TextRange(a, b)) {
                                        finding(
                                            "non-collapsed selection not honored: " +
                                                "requested [$a,$b], actual ${state.selection}",
                                        )
                                    }
                                }
                            }

                            in 6..7 -> {
                                val pos = random.nextInt(text.length + 1)
                                opLog += "caret@$pos"
                                state.onTextFieldValueChange(TextFieldValue(text, TextRange(pos)))
                                mainClock.advanceTimeBy(16)
                            }

                            8 -> {
                                val caret = state.selection.min.coerceIn(0, text.length)
                                val ch = 'a' + random.nextInt(26)
                                opLog += "type '$ch'@$caret"
                                state.onTextFieldValueChange(
                                    TextFieldValue(
                                        text.substring(0, caret) + ch + text.substring(caret),
                                        TextRange(caret + 1),
                                    ),
                                )
                                mainClock.advanceTimeBy(16)
                            }

                            else -> {
                                opLog += "expire press window"
                                mainClock.advanceTimeBy(400)
                            }
                        }
                    } catch (t: Throwable) {
                        if (t is AssertionError) throw t
                        finding("CRASH: ${t.stackTraceToString()}")
                    }

                    val tfv = state.textFieldValue
                    if (tfv.text != state.annotatedString.text) {
                        finding(
                            "annotatedString/textFieldValue mismatch: " +
                                "tfv=\"${tfv.text}\" annotated=\"${state.annotatedString.text}\"",
                        )
                    }
                    if (tfv.selection.max > tfv.text.length || tfv.selection.min < 0) {
                        finding("selection out of bounds: ${tfv.selection} len=${tfv.text.length}")
                    }
                }

                // Let pending press windows expire between scenarios
                mainClock.advanceTimeBy(400)
            }
        }
}
