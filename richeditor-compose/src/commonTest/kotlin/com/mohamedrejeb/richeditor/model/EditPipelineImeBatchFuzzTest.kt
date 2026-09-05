package com.mohamedrejeb.richeditor.model

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.ui.text.TextRange
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Seeded fuzz over the BTF2 edit pipeline, driven the way the editor's `InputTransformation` drives
 * it: a [TextFieldBuffer] carrying the platform's already-applied edit, replayed through
 * [applyChangeList], reconciled by [reconcileBufferWithModel], then committed back to
 * [RichTextState.textFieldState] the way BTF2 commits its buffer.
 *
 * The batches are shaped like real IME traffic rather than uniform random noise: a growing
 * composing word, a shrinking one, a word committed with its trailing space, a backspace, a replace
 * over an arbitrary range, an Enter, and occasionally two disjoint replaces inside a single batch
 * (one `endBatchEdit` carrying two deltas), which is the shape that produced #716.
 *
 * ## Oracles
 *
 * 1. **Shadow model.** A plain [StringBuilder] receives the same logical edits at the same offsets
 *    and must equal [RichTextState.toText] afterwards. The two agree character for character
 *    because the shadow holds the newline the platform inserted exactly where `toText` writes its
 *    paragraph separator. The buffer, in contrast, carries a space at that offset:
 *    `annotatedString` is built with `newText.replace('\n', ' ')`, so the model text and the buffer
 *    text differ by that substitution and by nothing else. `the paragraph separator mapping is a
 *    newline for space substitution` derives that relation from one manual batch, and every fuzz
 *    step re-asserts it.
 * 2. **Convergence.** After [reconcileBufferWithModel] the buffer text equals
 *    [RichTextState.annotatedString]`.text`. This is what makes the editor's next frame render what
 *    the model holds.
 * 3. **No exception.** Every step runs inside the reporting wrapper; a throw fails with the op log.
 * 4. **Derived state.** Every tenth step, the parity oracle of `DerivedStateParityTest` restricted
 *    to `currentSpanStyle`: serialize, decode into a fresh state, park the caret at the same offset,
 *    compare. The html round trip must reproduce the document's text first; each seed asserts the
 *    check ran at least once.
 *
 * ## Scope of the shadow oracle
 *
 * Oracle 1 holds only while every character in the buffer came from the platform. List paragraphs
 * break that: the `"• "` / `"1. "` prefix is model-generated, so the model rightly adds one when a
 * new item opens, drops one when two items merge, and takes the paragraph out of the list when an
 * edit lands on the marker itself. A [StringBuilder] cannot know any of that. List documents
 * therefore run under `seeded ime batches over list documents converge without the shadow oracle`,
 * which keeps oracles 2, 3 and 4 and the separator mapping and drops only the shadow. Do not add
 * list documents to the shadow session; its divergences there are the oracle's fault, not the
 * model's.
 *
 * ## Runtime
 *
 * Four fixed seeds over 150 steps each per session, so 1200 steps in all. The budget was measured
 * on the JVM (`desktopTest`, about 0.1s for the whole class inside a full suite run, 0.26s when the
 * class runs alone and pays for warm up). This suite is in commonTest, so `allTests` also runs those
 * 1200 steps on the wasmJs browser target and the iOS simulator, where they will cost more; the step
 * counts are plain constants here, so lower them if a slower target makes them a problem, rather
 * than reaching for per-platform machinery.
 */
class EditPipelineImeBatchFuzzTest {

    private data class Edit(val start: Int, val end: Int, val text: String)

    /**
     * The relation the shadow oracle depends on, derived by hand: the model joins paragraphs with
     * `\n` and the buffer the editor lays out joins them with a space, at the same offsets.
     */
    @Test
    fun `the paragraph separator mapping is a newline for space substitution`() {
        val state = RichTextState()
        state.setText("ab")

        val buffer = state.textFieldState.toTextFieldBuffer()
        buffer.replace(2, 2, "\ncd")
        state.applyChangeList(buffer)
        state.reconcileBufferWithModel(buffer)

        assertEquals(2, state.richParagraphList.size)
        assertEquals("ab\ncd", state.toText(), "the model separates paragraphs with a newline")
        assertEquals(
            "ab cd",
            state.annotatedString.text,
            "the buffer separates the same paragraphs with a space at the same offset",
        )
        assertEquals(
            state.annotatedString.text,
            state.toText().replace('\n', ' '),
            "the two differ by that substitution and by nothing else",
        )
        assertEquals(
            state.annotatedString.text,
            buffer.asCharSequence().toString(),
            "the reconciled buffer holds the model text",
        )
    }

    @Test
    fun `seeded ime batches keep the shadow model and the buffer converged`() {
        val startingDocuments = listOf(
            "",
            "<p>Hello <b>bold</b> world</p>",
            "<p>one</p><p><b>two</b> three</p>",
            "<h1>title</h1><p>body <i>text</i></p>",
        )
        startingDocuments.forEachIndexed { seed, html ->
            runSession(seed = seed, startingHtml = html, steps = 150, useShadow = true)
        }
    }

    /**
     * The same batches over list documents, which is where both pipeline crashes this harness found
     * actually lived. The shadow oracle is off here for the reason given in the class doc; every
     * other oracle still runs.
     */
    @Test
    fun `seeded ime batches over list documents converge without the shadow oracle`() {
        val startingDocuments = listOf(
            "<ul><li>alpha</li><li>beta</li></ul>",
            "<ol><li>alpha</li><li><b>beta</b> gamma</li></ol>",
            "<ul><li>a<b>b</b>c</li></ul><p>after</p>",
            "<h2>heading</h2><ul><li>item</li></ul>",
        )
        startingDocuments.forEachIndexed { seed, html ->
            runSession(seed = seed, startingHtml = html, steps = 150, useShadow = false)
        }
    }

    private fun runSession(seed: Int, startingHtml: String, steps: Int, useShadow: Boolean) {
        val random = Random(seed)
        val state = RichTextState()
        if (startingHtml.isNotEmpty()) state.setHtml(startingHtml)
        val shadow = StringBuilder(state.toText())
        val log = mutableListOf<String>("seed=$seed start=[$startingHtml]")
        var derivedChecks = 0
        var derivedSkips = 0

        fun report(step: Int, cause: Throwable?): Nothing = fail(
            buildString {
                appendLine("IME batch fuzz failed at seed=$seed step=$step")
                appendLine("model  : [${state.toText().replace("\n", "\\n")}]")
                appendLine("buffer : [${state.annotatedString.text}]")
                appendLine("shadow : [${shadow.toString().replace("\n", "\\n")}]")
                appendLine("ops:")
                log.forEach { appendLine("  $it") }
                if (cause != null) appendLine(cause.stackTraceToString())
            }
        )

        for (step in 0 until steps) {
            val text = state.textFieldState.text.toString()
            val edits = nextBatch(random, text) ?: continue
            log += "step $step: " + edits.joinToString(" + ") {
                "replace(${it.start} ${it.end} \"${it.text.replace("\n", "\\n")}\")"
            }

            try {
                applyBatch(state, edits)
                assertEquals(
                    state.annotatedString.text,
                    state.textFieldState.text.toString(),
                    "the committed buffer must hold the model text",
                )

                if (useShadow) {
                    // setRange, not replace: the three argument replace is a JVM only API.
                    edits.forEach { shadow.setRange(it.start, it.end, it.text) }
                    assertEquals(
                        shadow.toString(),
                        state.toText(),
                        "the shadow model and the rich text must agree",
                    )
                }
                assertEquals(
                    state.annotatedString.text,
                    state.toText().replace('\n', ' '),
                    "the separator mapping must still hold",
                )

                if (step % 10 == 9) {
                    if (checkDerivedSpanStyle(state)) derivedChecks++ else derivedSkips++
                }
            } catch (assertion: AssertionError) {
                report(step, assertion)
            } catch (other: Throwable) {
                report(step, other)
            }
        }

        assertTrue(
            derivedChecks > 0,
            "seed=$seed ran no derived state check at all ($derivedSkips skipped)",
        )
    }

    /**
     * The editor's `InputTransformation` tail, minus the editor: replay the buffer's changes into
     * the model, push anything the model injected back into the buffer, then commit the buffer the
     * way BTF2 does once the transformation returns.
     */
    private fun applyBatch(state: RichTextState, edits: List<Edit>) {
        val buffer = state.textFieldState.toTextFieldBuffer()
        edits.forEach { buffer.replace(it.start, it.end, it.text) }
        val caret = (edits.last().start + edits.last().text.length).coerceIn(0, buffer.length)
        buffer.selection = TextRange(caret)

        state.applyChangeList(buffer)
        state.reconcileBufferWithModel(buffer)

        assertEquals(
            state.annotatedString.text,
            buffer.asCharSequence().toString(),
            "the reconciled buffer must converge on the model text",
        )

        val committedText = buffer.asCharSequence().toString()
        val committedSelection = buffer.selection
        state.pendingSelectionDuringSync = null
        state.setTextFieldStateFromValue(committedText, committedSelection)
        // The editor's selection observer, which fires on every buffer commit. It dedupes
        // against the selection the replay already handled, so most batches see it do nothing;
        // leaving it out would let the harness report a staleness the real editor never has.
        state.handleSelectionChanged(state.textFieldState.selection, fromGestureObserver = true)
    }

    /**
     * Returns true when the comparison ran, false when the html round trip could not reproduce the
     * document and there was nothing to compare against.
     */
    private fun checkDerivedSpanStyle(state: RichTextState): Boolean {
        val fresh = RichTextState()
        fresh.setHtml(state.toHtml())
        assertEquals(
            state.annotatedString.text,
            fresh.annotatedString.text,
            "the html round trip must reproduce the document: ${state.toHtml()}",
        )

        val selection = state.selection
        fresh.selection = TextRange(0)
        fresh.selection = selection
        if (fresh.selection != selection) return false

        assertEquals(
            fresh.currentSpanStyle,
            state.currentSpanStyle,
            "currentSpanStyle at $selection must match a fresh decode of the same document",
        )
        return true
    }

    private fun word(random: Random, length: Int): String =
        buildString { repeat(length) { append('a' + random.nextInt(26)) } }

    /** One IME batch, or null when the current text cannot host the drawn shape. */
    private fun nextBatch(random: Random, text: String): List<Edit>? {
        val n = text.length
        // Weighted so the multi-delta batch stays the rare shape it is in real traffic.
        return when (random.nextInt(10)) {
            0 -> listOf(Edit(n, n, word(random, 1)))
            1 -> {
                val k = random.nextInt(1, 5).coerceAtMost(n)
                if (k < 1) null else listOf(Edit(n - k, n, word(random, k + 1)))
            }
            2 -> {
                val k = random.nextInt(2, 5).coerceAtMost(n)
                if (k < 2) null else listOf(Edit(n - k, n, word(random, k - 1)))
            }
            3 -> listOf(Edit(n, n, word(random, random.nextInt(1, 5)) + " "))
            4 -> if (n < 1) null else listOf(Edit(n - 1, n, ""))
            5 -> {
                val start = random.nextInt(n + 1)
                val end = (start + random.nextInt(5)).coerceAtMost(n)
                val replacement = word(random, random.nextInt(4))
                if (start == end && replacement.isEmpty()) null
                else listOf(Edit(start, end, replacement))
            }
            6 -> listOf(random.nextInt(n + 1).let { Edit(it, it, "\n") })
            7 -> listOf(Edit(n, n, word(random, random.nextInt(1, 4))))
            8 -> if (n < 1) null else listOf(Edit(n - 1, n, ""))
            else -> {
                // Two disjoint non-adjacent replaces inside one batch. Applied high offset first
                // so the second edit's offsets are still the ones drawn against `text`.
                if (n < 6) return null
                val low = random.nextInt(0, n / 2)
                val high = random.nextInt(n / 2 + 1, n)
                if (high <= low + 1) return null
                listOf(
                    Edit(high, (high + 1).coerceAtMost(n), word(random, 1)),
                    Edit(low, low + 1, word(random, 1)),
                )
            }
        }
    }
}
