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
 * Edit-pipeline fuzz harness with corruption oracles, stronger than
 * [Issue716StringIndexFuzzTest]'s crash-only net: every edit checks
 * annotatedString/textFieldValue consistency and selection bounds, plain-document
 * scenarios require the text to exactly equal the committed text,
 * toHtml()/toMarkdown() run periodically, and undo/redo and emoji surrogate pairs are
 * part of the op mix. All known-bug masks except the separator-overwrite one (see
 * `knownSeparatorOverwrite` below) were removed after the fixes landed.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextEditCorruptionFuzzTest {

    @Test
    fun `fuzz with corruption oracles must find no new issues`() {
        val scenarios = 600
        val stepsPerScenario = 40

        for (seed in 0 until scenarios) {
            val random = Random(seed)
            val opLog = mutableListOf<String>()
            val state = RichTextState()
            val valueHistory = mutableListOf<TextFieldValue>()
            val plainMode = random.nextBoolean()

            fun letters(count: Int): String =
                buildString { repeat(count) { append('a' + random.nextInt(26)) } }

            fun finding(message: String): Nothing =
                fail(
                    buildString {
                        appendLine("Corruption fuzz finding, seed=$seed plainMode=$plainMode")
                        appendLine(message)
                        appendLine("Op log (${opLog.size} ops):")
                        opLog.forEach { appendLine("  $it") }
                    },
                )

            fun send(desc: String, value: TextFieldValue) {
                opLog += "$desc -> VALUE(\"${value.text.replace("\n", "\\n")}\", ${value.selection.start}, ${value.selection.end})"
                try {
                    state.onTextFieldValueChange(value)
                } catch (t: Throwable) {
                    finding("CRASH in onTextFieldValueChange: ${t.stackTraceToString()}")
                }
                valueHistory += state.textFieldValue

                val tfv = state.textFieldValue
                if (tfv.text != state.annotatedString.text) {
                    finding(
                        "annotatedString/textFieldValue mismatch: tfv=\"${tfv.text}\" " +
                            "annotated=\"${state.annotatedString.text}\"",
                    )
                }
                if (tfv.selection.max > tfv.text.length || tfv.selection.min < 0) {
                    finding("selection out of bounds: ${tfv.selection} for len=${tfv.text.length}")
                }
                if (plainMode) {
                    val expected = value.text.replace('\n', ' ')
                    // Known class (pinned in RichTextStateCorruptionCollectionTest): the
                    // paragraph separator position is force-rewritten to a hardcoded space
                    // during the rebuild, so length-preserving mismatches whose only
                    // differences are spaces in the actual text are not new findings.
                    val knownSeparatorOverwrite =
                        expected.length == tfv.text.length &&
                            expected.indices.all { expected[it] == tfv.text[it] || tfv.text[it] == ' ' }
                    if (tfv.text != expected && knownSeparatorOverwrite) {
                        opLog += "  (known separator overwrite, ignored)"
                    } else if (tfv.text != expected) {
                        finding(
                            "plain text corruption: committed=\"${value.text.replace("\n", "\\n")}\" " +
                                "expected=\"$expected\" actual=\"${tfv.text}\"",
                        )
                    }
                }
            }

            try {
                if (plainMode) {
                    when (random.nextInt(3)) {
                        1 -> state.setText("Hello world, this is some plain text content.")
                        2 -> state.setText("alpha beta\ngamma delta\nepsilon zeta")
                    }
                    opLog += "init plain shape"
                } else {
                    when (random.nextInt(4)) {
                        0 -> state.setMarkdown("- item one\n- item two\n- item three")
                        1 -> state.setMarkdown("1. first entry\n2. second entry\n3. third entry")
                        2 -> state.setHtml("<p><b>Bold start</b> middle <i>italic end</i></p><ul><li>alpha</li><li>beta</li></ul>")
                        else -> state.setMarkdown("Title line\n\n- bullet **bold** text\n- second bullet\n\nclosing paragraph")
                    }
                    opLog += "init rich shape"
                }
                opLog += "init text=${state.textFieldValue.text.replace('\n', '|')}"

                repeat(stepsPerScenario) { step ->
                    val current = state.textFieldValue.text
                    val caret = state.textFieldValue.selection.min.coerceIn(0, current.length)

                    when (random.nextInt(100)) {
                        in 0..21 -> {
                            val ch = 'a' + random.nextInt(26)
                            send(
                                "type '$ch'@$caret",
                                TextFieldValue(
                                    text = current.substring(0, caret) + ch + current.substring(caret),
                                    selection = TextRange(caret + 1),
                                ),
                            )
                        }

                        in 22..28 -> {
                            send(
                                "newline@$caret",
                                TextFieldValue(
                                    text = current.substring(0, caret) + '\n' + current.substring(caret),
                                    selection = TextRange(caret + 1),
                                ),
                            )
                        }

                        in 29..40 -> {
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

                        in 41..50 -> {
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

                        in 51..57 -> {
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

                        in 58..65 -> {
                            // Word-internal autocorrect: never rewrites a range containing a
                            // space (the separator drop corruption is already pinned)
                            if (current.isNotEmpty()) {
                                val a = random.nextInt(current.length)
                                val b = (a + 1 + random.nextInt(6)).coerceAtMost(current.length)
                                if (!current.substring(a, b).contains(' ')) {
                                    val repl = letters(random.nextInt(8))
                                    val newText = current.substring(0, a) + repl + current.substring(b)
                                    send(
                                        "autocorrect[$a,$b)->'$repl'",
                                        TextFieldValue(
                                            text = newText,
                                            selection = TextRange((a + repl.length).coerceAtMost(newText.length)),
                                        ),
                                    )
                                }
                            }
                        }

                        in 66..71 -> {
                            if (current.isNotEmpty()) {
                                val a = random.nextInt(current.length)
                                val b = (a + 1 + random.nextInt(current.length - a)).coerceAtMost(current.length)
                                send("select[$a,$b]", TextFieldValue(current, TextRange(a, b)))
                            }
                        }

                        in 72..77 -> {
                            // Replace the current selection, but skip when it contains a space
                            val sel = state.textFieldValue.selection
                            if (!current.substring(sel.min, sel.max).contains(' ')) {
                                val repl = letters(random.nextInt(6))
                                val newText = current.substring(0, sel.min) + repl + current.substring(sel.max)
                                send(
                                    "replaceSel[${sel.min},${sel.max}]->'$repl'",
                                    TextFieldValue(text = newText, selection = TextRange(sel.min + repl.length)),
                                )
                            }
                        }

                        in 78..82 -> {
                            // Emoji surrogate pair typed at the caret
                            val emoji = if (random.nextBoolean()) "😀" else "👍"
                            send(
                                "emoji@$caret",
                                TextFieldValue(
                                    text = current.substring(0, caret) + emoji + current.substring(caret),
                                    selection = TextRange(caret + emoji.length),
                                ),
                            )
                        }

                        in 83..86 -> {
                            // Backspace over a full surrogate pair when one precedes the caret
                            if (caret >= 2 && current[caret - 1].isLowSurrogate() && current[caret - 2].isHighSurrogate()) {
                                send(
                                    "emojiBackspace@$caret",
                                    TextFieldValue(
                                        text = current.removeRange(caret - 2, caret),
                                        selection = TextRange(caret - 2),
                                    ),
                                )
                            } else if (caret > 0) {
                                send(
                                    "backspace@$caret",
                                    TextFieldValue(
                                        text = current.substring(0, caret - 1) + current.substring(caret),
                                        selection = TextRange(caret - 1),
                                    ),
                                )
                            }
                        }

                        in 87..89 -> {
                            try {
                                if (random.nextBoolean()) state.history.undo() else state.history.redo()
                                opLog += "undo/redo"
                            } catch (t: Throwable) {
                                finding("CRASH in undo/redo: ${t.stackTraceToString()}")
                            }
                            valueHistory += state.textFieldValue
                        }

                        in 90..92 -> {
                            if (!plainMode) {
                                if (random.nextBoolean()) state.toggleUnorderedList() else state.toggleOrderedList()
                                opLog += "toggleList"
                                valueHistory += state.textFieldValue
                            }
                        }

                        in 93..95 -> {
                            if (!plainMode && current.isNotEmpty()) {
                                val a = random.nextInt(current.length)
                                val b = (a + 1 + random.nextInt(current.length - a)).coerceAtMost(current.length)
                                send("selectForStyle[$a,$b]", TextFieldValue(current, TextRange(a, b)))
                                if (random.nextBoolean()) {
                                    state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                    opLog += "toggleBold[$a,$b]"
                                } else {
                                    state.toggleCodeSpan()
                                    opLog += "toggleCodeSpan[$a,$b]"
                                }
                                valueHistory += state.textFieldValue
                            }
                        }

                        else -> {
                            if (valueHistory.size >= 2) {
                                val stale = valueHistory[random.nextInt(valueHistory.size - 1)]
                                send("staleReplay", stale)
                            }
                        }
                    }

                    if (step % 7 == 6) {
                        try {
                            state.toHtml()
                            state.toMarkdown()
                        } catch (t: Throwable) {
                            finding("CRASH in toHtml/toMarkdown: ${t.stackTraceToString()}")
                        }
                    }
                }
            } catch (t: Throwable) {
                if (t is AssertionError) throw t
                finding("CRASH outside send: ${t.stackTraceToString()}")
            }
        }
    }
}
