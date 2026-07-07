package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.fail

/**
 * Pins issue #716 (StringIndexOutOfBoundsException in updateAnnotatedString): a batched
 * IME edit shaped like deleteSurroundingText(n, 0) + commitText("\n") in one
 * endBatchEdit used to be misclassified as an IME revert, leaving the span tree longer
 * than the text. Deterministic repros first, then the seeded fuzz harness that
 * surfaced the crash, kept as a wide regression net over IME-like edit sequences.
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue716StringIndexFuzzTest {

    @Test
    fun `ime batch edit with deletion plus newline must not crash updateAnnotatedString`() {
        // Part 1: minimal deterministic repro. Three plain paragraphs, caret inside the
        // middle one, then a single batched IME edit that deletes 5 characters behind the
        // caret and inserts a newline: the new value is shorter and has one more newline,
        // the shape the old pipeline misclassified as an IME revert.
        run {
            val state = RichTextState()
            state.setText("hello world\nsecond line here\nthird line")
            val old = state.textFieldValue.text

            // Collapsed caret at 20, like a user typing inside "second line here"
            state.onTextFieldValueChange(TextFieldValue(old, TextRange(20)))

            // One endBatchEdit: deleteSurroundingText(5, 0) + commitText("\n")
            val batched = old.removeRange(15, 20).let { it.substring(0, 15) + "\n" + it.substring(15) }
            try {
                state.onTextFieldValueChange(TextFieldValue(batched, TextRange(16)))
            } catch (t: Throwable) {
                fail(
                    buildString {
                        appendLine("Issue #716 reproduced with a single batched IME edit.")
                        appendLine("Old text: \"${old.replace("\n", "\\n")}\" (len=${old.length}), caret=20")
                        appendLine("New value: \"${batched.replace("\n", "\\n")}\" (len=${batched.length}), caret=16")
                        appendLine("The value is shorter with one more newline, the shape the old")
                        appendLine("pipeline misclassified as an IME revert, leaving the span tree with")
                        appendLine("more characters than the text given to updateAnnotatedString.")
                        appendLine()
                        appendLine(t.stackTraceToString())
                    },
                )
            }
        }

        // Part 2: seeded fuzz harness over IME-like edit sequences. This is what found
        // the crash (seed=1). Kept as a regression net: it must stay green after the fix.
        val scenarios = 1000
        val stepsPerScenario = 30

        for (seed in 0 until scenarios) {
            val random = Random(seed)
            val opLog = mutableListOf<String>()
            val state = RichTextState()
            val valueHistory = mutableListOf<TextFieldValue>()

            fun letters(count: Int): String =
                buildString { repeat(count) { append('a' + random.nextInt(26)) } }

            fun send(desc: String, value: TextFieldValue) {
                opLog += "$desc -> VALUE(\"${value.text.replace("\n", "\\n")}\", ${value.selection.start}, ${value.selection.end})"
                state.onTextFieldValueChange(value)
                valueHistory += state.textFieldValue
            }

            try {
                val shape = random.nextInt(6)
                when (shape) {
                    0 -> Unit
                    1 -> state.setText("Hello world, this is some plain text content.")
                    2 -> state.setMarkdown("- item one\n- item two\n- item three")
                    3 -> state.setMarkdown("1. first entry\n2. second entry\n3. third entry")
                    4 -> state.setHtml("<p><b>Bold start</b> middle <i>italic end</i></p><ul><li>alpha</li><li>beta</li></ul>")
                    5 -> state.setMarkdown("Title line\n\n- bullet **bold** text\n- second bullet\n\nclosing paragraph")
                }
                opLog += "init shape=$shape text=${state.textFieldValue.text.replace('\n', '|')}"

                repeat(stepsPerScenario) {
                    val current = state.textFieldValue.text
                    val caret = state.textFieldValue.selection.min.coerceIn(0, current.length)

                    when (random.nextInt(100)) {
                        in 0..24 -> {
                            // Plain typing at the caret
                            val ch = 'a' + random.nextInt(26)
                            send(
                                "type '$ch'@$caret",
                                TextFieldValue(
                                    text = current.substring(0, caret) + ch + current.substring(caret),
                                    selection = TextRange(caret + 1),
                                ),
                            )
                        }

                        in 25..32 -> {
                            // Newline at the caret
                            send(
                                "newline@$caret",
                                TextFieldValue(
                                    text = current.substring(0, caret) + '\n' + current.substring(caret),
                                    selection = TextRange(caret + 1),
                                ),
                            )
                        }

                        in 33..46 -> {
                            // Backspace
                            if (caret > 0) {
                                send(
                                    "backspace@$caret",
                                    TextFieldValue(
                                        text = current.substring(0, caret - 1) + current.substring(caret),
                                        selection = TextRange(caret - 1),
                                    ),
                                )
                            }
                        }

                        in 47..56 -> {
                            // Range deletion with the selection left at the cut point
                            if (current.length >= 2) {
                                val a = random.nextInt(current.length)
                                val b = (a + 1 + random.nextInt((current.length - a).coerceAtMost(8)))
                                    .coerceAtMost(current.length)
                                send(
                                    "deleteRange[$a,$b)",
                                    TextFieldValue(
                                        text = current.removeRange(a, b),
                                        selection = TextRange(a),
                                    ),
                                )
                            }
                        }

                        in 57..64 -> {
                            // Range deletion but the IME reports an unrelated selection
                            if (current.length >= 2) {
                                val a = random.nextInt(current.length)
                                val b = (a + 1 + random.nextInt((current.length - a).coerceAtMost(8)))
                                    .coerceAtMost(current.length)
                                val newText = current.removeRange(a, b)
                                val sel = random.nextInt(newText.length + 1)
                                send(
                                    "deleteRangeWeirdSel[$a,$b) sel=$sel",
                                    TextFieldValue(text = newText, selection = TextRange(sel)),
                                )
                            }
                        }

                        in 65..72 -> {
                            // Autocorrect behind the cursor: replace an earlier region with a
                            // different-length word while the caret stays at the end
                            if (current.isNotEmpty()) {
                                val a = random.nextInt(current.length)
                                val b = (a + 1 + random.nextInt(6)).coerceAtMost(current.length)
                                val repl = letters(random.nextInt(8))
                                val newText = current.substring(0, a) + repl + current.substring(b)
                                send(
                                    "autocorrect[$a,$b)->'$repl'",
                                    TextFieldValue(text = newText, selection = TextRange(newText.length)),
                                )
                            }
                        }

                        in 73..78 -> {
                            // Pure selection change to a non-collapsed range
                            if (current.isNotEmpty()) {
                                val a = random.nextInt(current.length)
                                val b = (a + 1 + random.nextInt(current.length - a)).coerceAtMost(current.length)
                                send("select[$a,$b]", TextFieldValue(current, TextRange(a, b)))
                            }
                        }

                        in 79..84 -> {
                            // Replace whatever is currently selected (insert when collapsed)
                            val sel = state.textFieldValue.selection
                            val repl = letters(random.nextInt(6))
                            val newText = current.substring(0, sel.min) + repl + current.substring(sel.max)
                            send(
                                "replaceSel[${sel.min},${sel.max}]->'$repl'",
                                TextFieldValue(text = newText, selection = TextRange(sel.min + repl.length)),
                            )
                        }

                        in 85..90 -> {
                            // IME batch edit: delete a chunk somewhere and insert a newline at
                            // the caret in the same value (shorter text, more newlines)
                            if (current.length >= 3) {
                                val a = random.nextInt(current.length - 2)
                                val b = a + 2 + random.nextInt((current.length - a - 2).coerceAtMost(6) + 1)
                                var t = current.removeRange(a, b)
                                val pos = random.nextInt(t.length + 1)
                                t = t.substring(0, pos) + '\n' + t.substring(pos)
                                send(
                                    "imeBatch del[$a,$b)+newline@$pos",
                                    TextFieldValue(text = t, selection = TextRange((pos + 1).coerceAtMost(t.length))),
                                )
                            }
                        }

                        in 91..93 -> {
                            // Toolbar: toggle list type at the current caret
                            if (random.nextBoolean()) state.toggleUnorderedList() else state.toggleOrderedList()
                            opLog += "toggleList"
                            valueHistory += state.textFieldValue
                        }

                        in 94..96 -> {
                            // Toolbar: bold a random range
                            if (current.isNotEmpty()) {
                                val a = random.nextInt(current.length)
                                val b = (a + 1 + random.nextInt(current.length - a)).coerceAtMost(current.length)
                                send("selectForBold[$a,$b]", TextFieldValue(current, TextRange(a, b)))
                                state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                opLog += "toggleBold[$a,$b]"
                                valueHistory += state.textFieldValue
                            }
                        }

                        else -> {
                            // Stale replay: resend a value the editor produced a few edits ago,
                            // as a restarted input session would after replaying batched commands
                            if (valueHistory.size >= 2) {
                                val stale = valueHistory[random.nextInt(valueHistory.size - 1)]
                                send("staleReplay", stale)
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                fail(
                    buildString {
                        appendLine("Fuzz crash, seed=$seed")
                        appendLine("Op log (${opLog.size} ops):")
                        opLog.forEach { appendLine("  $it") }
                        appendLine("Exception: ${t.stackTraceToString()}")
                    },
                )
            }
        }
    }
}
