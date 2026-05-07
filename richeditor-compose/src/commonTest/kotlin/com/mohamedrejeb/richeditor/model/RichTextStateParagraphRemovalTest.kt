package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import kotlin.test.Test

/**
 * Regression tests for two related crashes in [RichTextState.updateAnnotatedString].
 *
 * Bug 1 — IndexOutOfBoundsException (index: 12, size: 12 at SnapshotStateList.get):
 *   The loop iterated `richParagraphList` via `fastForEachIndexed` (which captures
 *   the list size once) and called `richParagraphList.removeAt(i)` inside the body.
 *   Once the first paragraph was removed the loop continued past the new end of the
 *   list and threw.  Fix: collect indices to remove, apply after the loop.
 *
 * Bug 2 — StringIndexOutOfBoundsException (begin N, end M, length L where M > L):
 *   `text.substring(index, index + richSpan.text.length)` assumed the span's stored
 *   length never exceeds the incoming text.  Samsung soft keyboards on budget devices
 *   (e.g. A04, A57) route some input through `TextFieldKeyInput` (dispatchKeyEvent)
 *   rather than the IME protocol, delivering a new `TextFieldValue` atomically before
 *   span reconciliation runs.  Physical keyboards trigger the same code path.
 *   Fix: clamp `safeStart`/`safeEnd` to `text.length` before the substring call.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextStateParagraphRemovalTest {

    @Test
    fun `shrinking text below paragraph range does not throw IndexOutOfBoundsException`() {
        val paragraphCount = 13
        val paragraphs = List(paragraphCount) { i ->
            RichParagraph(key = i + 1).also { p ->
                p.children.add(RichSpan(text = "line$i", paragraph = p))
            }
        }
        val state = RichTextState(initialRichParagraphList = paragraphs)

        // Force updateAnnotatedString down the `index > newText.length` removal branch
        // on many paragraphs at once.  Before the fix this crashed with IOOB because
        // removeAt(i) was called during fastForEachIndexed.
        state.onTextFieldValueChange(
            TextFieldValue(text = "", selection = TextRange.Zero)
        )
    }

    @Test
    fun `repeated shrink-grow cycles do not throw`() {
        val state = RichTextState(
            initialRichParagraphList = List(20) { i ->
                RichParagraph(key = i + 1).also { p ->
                    p.children.add(RichSpan(text = "paragraph$i", paragraph = p))
                }
            }
        )

        repeat(5) {
            state.onTextFieldValueChange(
                TextFieldValue(text = "a", selection = TextRange(1))
            )
            state.onTextFieldValueChange(
                TextFieldValue(text = "aaaa aaaa aaaa", selection = TextRange(14))
            )
        }
    }

    @Test
    fun `drastic text shrink does not throw StringIndexOutOfBoundsException`() {
        // Exercises the AnnotatedStringExt bounds-clamp fix.  Build a state with a
        // moderately long paragraph, then deliver a TextFieldValue whose text is much
        // shorter than the spans currently represent — simulating the atomic update
        // that Samsung's soft keyboard sends via TextFieldKeyInput.
        val longText = "Hello world this is a longer paragraph with multiple words"
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(key = 1).also { p ->
                    p.children.add(RichSpan(text = longText, paragraph = p))
                }
            )
        )

        // Deliver a text value that is far shorter than the span's stored length.
        state.onTextFieldValueChange(
            TextFieldValue(text = "Hi", selection = TextRange(2))
        )
    }
}
