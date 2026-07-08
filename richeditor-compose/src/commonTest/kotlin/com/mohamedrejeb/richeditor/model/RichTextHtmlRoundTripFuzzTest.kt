package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.fail

/**
 * Fuzz round 3: HTML export round-trip stability. Builds documents through random edits
 * and style toggles, then checks: toHtml() must not crash, setHtml(toHtml()) must
 * preserve the visible text, and toHtml -> setHtml -> toHtml must be idempotent (one
 * normalization pass allowed). Distinct failure classes get deterministic pins in
 * RichTextStateCorruptionCollectionTest.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextHtmlRoundTripFuzzTest {

    @Test
    fun `html export round trip must preserve text and be idempotent`() {
        val scenarios = 400
        val stepsPerScenario = 12

        for (seed in 0 until scenarios) {
            val random = Random(seed)
            val opLog = mutableListOf<String>()
            val state = RichTextState()

            fun letters(count: Int): String =
                buildString { repeat(count) { append('a' + random.nextInt(26)) } }

            fun finding(message: String): Nothing =
                fail(
                    buildString {
                        appendLine("Round-trip finding, seed=$seed")
                        appendLine(message)
                        appendLine("Op log (${opLog.size} ops):")
                        opLog.forEach { appendLine("  $it") }
                    },
                )

            try {
                when (random.nextInt(7)) {
                    0 -> state.setText("plain text content here")
                    1 -> state.setMarkdown("- item one\n- item two")
                    2 -> state.setMarkdown("1. first\n2. second\n3. third")
                    3 -> state.setHtml("<p>before <b>bold</b> after</p><p>second paragraph</p>")
                    4 -> Unit
                    // #734 shape: ordered items with continuation text and blank lines
                    5 -> state.setMarkdown(
                        "intro line\n\n1. first title\ncontinuation text here\n\n" +
                            "2. second title\n\n3. third title\nmore continuation",
                    )
                    // #736 shape: nested list levels (built via the level API below)
                    6 -> {
                        state.setMarkdown("1. aa\n2. bb\n3. cc\n4. dd\n5. ee")
                        val t = state.textFieldValue.text
                        state.onTextFieldValueChange(TextFieldValue(t, TextRange(t.indexOf("cc") + 1)))
                        state.increaseListLevel()
                        val t2 = state.textFieldValue.text
                        state.onTextFieldValueChange(TextFieldValue(t2, TextRange(t2.indexOf("dd") + 1)))
                        state.increaseListLevel()
                        state.increaseListLevel()
                    }
                }
                opLog += "init text=${state.textFieldValue.text.replace('\n', '|')}"

                repeat(stepsPerScenario) {
                    val text = state.textFieldValue.text
                    val caret = state.selection.min.coerceIn(0, text.length)

                    when (random.nextInt(10)) {
                        in 0..3 -> {
                            val ch = 'a' + random.nextInt(26)
                            opLog += "type '$ch'@$caret"
                            state.onTextFieldValueChange(
                                TextFieldValue(
                                    text.substring(0, caret) + ch + text.substring(caret),
                                    TextRange(caret + 1),
                                ),
                            )
                        }

                        4 -> {
                            opLog += "space@$caret"
                            state.onTextFieldValueChange(
                                TextFieldValue(
                                    text.substring(0, caret) + ' ' + text.substring(caret),
                                    TextRange(caret + 1),
                                ),
                            )
                        }

                        5 -> {
                            opLog += "newline@$caret"
                            state.onTextFieldValueChange(
                                TextFieldValue(
                                    text.substring(0, caret) + '\n' + text.substring(caret),
                                    TextRange(caret + 1),
                                ),
                            )
                        }

                        in 6..7 -> {
                            if (text.isNotEmpty()) {
                                val a = random.nextInt(text.length)
                                val b = (a + 1 + random.nextInt(text.length - a)).coerceAtMost(text.length)
                                opLog += "style[$a,$b]"
                                state.onTextFieldValueChange(TextFieldValue(text, TextRange(a, b)))
                                when (random.nextInt(4)) {
                                    0 -> state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                    1 -> state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
                                    2 -> state.toggleCodeSpan()
                                    3 -> state.addLinkToSelection("https://example.com/${letters(4)}")
                                }
                            }
                        }

                        8 -> {
                            when (random.nextInt(4)) {
                                0 -> {
                                    opLog += "toggleUnorderedList"
                                    state.toggleUnorderedList()
                                }
                                1 -> {
                                    opLog += "toggleOrderedList"
                                    state.toggleOrderedList()
                                }
                                2 -> {
                                    opLog += "increaseListLevel"
                                    state.increaseListLevel()
                                }
                                else -> {
                                    opLog += "decreaseListLevel"
                                    state.decreaseListLevel()
                                }
                            }
                        }

                        else -> {
                            if (text.length >= 2) {
                                val a = random.nextInt(text.length)
                                val b = (a + 1 + random.nextInt((text.length - a).coerceAtMost(5)))
                                    .coerceAtMost(text.length)
                                opLog += "delete[$a,$b)"
                                state.onTextFieldValueChange(
                                    TextFieldValue(text.removeRange(a, b), TextRange(a)),
                                )
                            }
                        }
                    }
                }

                val text0 = state.textFieldValue.text
                val html1 = state.toHtml()

                val state2 = RichTextState()
                state2.setHtml(html1)
                val text1 = state2.textFieldValue.text
                // Known pre-existing ambiguity (not a pinned finding): the number of
                // trailing empty lines is not bijective through the <br>/implicit
                // paragraph mapping at document end. Content must match exactly;
                // trailing paragraph separators are tolerated.
                if (text1.trimEnd(' ') != text0.trimEnd(' ')) {
                    finding(
                        "round-trip changed the visible text:\n" +
                            "  before=\"${text0.replace("\n", "\\n")}\"\n" +
                            "  after =\"${text1.replace("\n", "\\n")}\"\n" +
                            "  html  =$html1",
                    )
                }

                val html2 = state2.toHtml()
                val state3 = RichTextState()
                state3.setHtml(html2)
                val html3 = state3.toHtml()
                if (html3 != html2) {
                    finding(
                        "html round-trip is not idempotent:\n  html2=$html2\n  html3=$html3",
                    )
                }
            } catch (t: Throwable) {
                if (t is AssertionError) throw t
                // Edit-pipeline crashes (tree/text desync families) are already pinned
                // by Issue716StringIndexFuzzTest and RichTextEditCorruptionFuzzTest;
                // this harness hunts the export surface, so skip the scenario when the
                // crash came from onTextFieldValueChange.
                val isKnownEditPipelineCrash = t is IndexOutOfBoundsException &&
                    t.stackTraceToString().contains("onTextFieldValueChange")
                if (!isKnownEditPipelineCrash) {
                    finding("CRASH: ${t.stackTraceToString()}")
                }
            }
        }
    }
}
