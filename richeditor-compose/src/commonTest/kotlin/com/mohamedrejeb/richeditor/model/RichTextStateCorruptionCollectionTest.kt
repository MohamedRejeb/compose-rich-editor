package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Deterministic pins for crashes and content corruptions found by fuzzing the edit
 * pipeline (#716 family). One test per finding.
 */
class RichTextStateCorruptionCollectionTest {

    private fun orderedNumbers(text: String): List<Int> =
        Regex("(\\d+)\\. ").findAll(text).map { it.groupValues[1].toInt() }.toList()

    /**
     * A batched IME edit (delete + commit "\n" in one endBatchEdit) inside an ordered
     * list item must go through the regular split, which renumbers.
     */
    @Test
    fun `batched delete plus newline inside an ordered list item must keep numbering sequential`() {
        val state = RichTextState()
        state.setMarkdown("1. first entry\n2. second entry\n3. third entry")
        val old = state.textFieldValue.text // "1. first entry 2. second entry 3. third entry"

        // Collapsed caret after "second" (index 24), like a user typing inside item 2
        state.onTextFieldValueChange(TextFieldValue(old, TextRange(24)))

        // One endBatchEdit: deleteSurroundingText(5, 0) + commitText("\n")
        val batched = old.removeRange(19, 24).let { it.substring(0, 19) + "\n" + it.substring(19) }
        state.onTextFieldValueChange(TextFieldValue(batched, TextRange(20)))

        val text = state.textFieldValue.text
        val numbers = orderedNumbers(text)
        assertEquals(
            (1..numbers.size).toList(),
            numbers,
            "Ordered list numbers must stay sequential after the split, text=\"$text\"",
        )
    }

    @Test
    fun `deleting a middle ordered list item must renumber the following items`() {
        val state = RichTextState()
        state.setMarkdown("1. aa\n2. bb\n3. cc")
        val old = state.textFieldValue.text // "1. aa 2. bb 3. cc"

        state.onTextFieldValueChange(TextFieldValue(old, TextRange(6)))
        // Delete "2. bb " -> range [6,12)
        state.onTextFieldValueChange(TextFieldValue(old.removeRange(6, 12), TextRange(6)))

        val text = state.textFieldValue.text
        assertEquals(
            listOf(1, 2),
            orderedNumbers(text),
            "Remaining items must be renumbered 1, 2, text=\"$text\"",
        )
    }

    /**
     * A same-length replacement spanning the virtual paragraph separator (the space
     * between paragraphs in textFieldValue.text) must not drop the committed character.
     */
    @Test
    fun `same length replacement across the paragraph separator must not drop characters`() {
        val state = RichTextState()
        state.setText("abcdef\nghijkl")
        val old = state.textFieldValue.text // "abcdef ghijkl"

        state.onTextFieldValueChange(TextFieldValue(old, TextRange(old.length)))

        // Autocorrect-style same-length replacement of "f g" (range [5,8)) with "xyz"
        val incoming = old.substring(0, 5) + "xyz" + old.substring(8)
        state.onTextFieldValueChange(TextFieldValue(incoming, TextRange(8)))

        assertEquals(
            incoming,
            state.textFieldValue.text,
            "The committed text must be preserved",
        )
    }

    /**
     * List prefixes are atomic: a selection covering part of the first prefix through
     * the second item's first content char collapses both items into one, and the typed
     * character lands next to the kept "b" ("1. xb"). Must never crash or desync.
     */
    @Test
    fun `typing over a selection that starts inside a list prefix must not crash`() {
        val state = RichTextState()
        state.setMarkdown("1. aa\n2. bb")
        val old = state.textFieldValue.text // "1. aa 2. bb", prefixes [0,3) and [6,9)

        // Selection from inside the first prefix into the second item's content
        state.onTextFieldValueChange(TextFieldValue(old, TextRange(2, 10)))

        // Typing replaces the selection
        val incoming = old.substring(0, 2) + "x" + old.substring(10)
        state.onTextFieldValueChange(TextFieldValue(incoming, TextRange(3)))

        assertEquals(
            state.annotatedString.text,
            state.textFieldValue.text,
            "Tree and text field value must stay in sync",
        )
        assertEquals("1. xb", state.textFieldValue.text)
    }

    @Test
    fun `replacing a selection spanning a list prefix must preserve the committed text`() {
        val state = RichTextState()
        state.setMarkdown("1. aa\n2. bb")
        val old = state.textFieldValue.text // "1. aa 2. bb"

        state.onTextFieldValueChange(TextFieldValue(old, TextRange(3, 10)))

        val incoming = old.substring(0, 3) + "xyzxyzxyz" + old.substring(10)
        state.onTextFieldValueChange(TextFieldValue(incoming, TextRange(12)))

        assertEquals(
            incoming,
            state.textFieldValue.text,
            "The committed replacement must be preserved",
        )
    }

    /**
     * A deletion clipping the inside of a list prefix removes the whole prefix and
     * demotes the item (prefixes are atomic): "1aa 2. bb" becomes "aa 1. bb".
     */
    @Test
    fun `deletion clipping inside a list prefix must demote consistently`() {
        val state = RichTextState()
        state.setMarkdown("1. aa\n2. bb")
        val old = state.textFieldValue.text // "1. aa 2. bb"

        val committed = old.removeRange(1, 3)
        state.onTextFieldValueChange(TextFieldValue(committed, TextRange(1)))

        assertEquals(
            state.annotatedString.text,
            state.textFieldValue.text,
            "Tree and text field value must stay in sync",
        )
        assertEquals("aa 1. bb", state.textFieldValue.text)
    }

    /**
     * The values look arbitrary because they were delta-debugged out of a longer fuzz
     * session, but each one is a legal IME callback; the rebuild must not crash.
     */
    @Test
    fun `repeated ime values clipping list prefixes must not crash the rebuild`() {
        val state = RichTextState()
        state.setMarkdown("1. first entry\n2. second entry\n3. third entry")

        state.onTextFieldValueChange(
            TextFieldValue("1rst entry 2. second entry 3. third entryq", TextRange(1)),
        )
        state.onTextFieldValueChange(
            TextFieldValue("👍rst entry 1. sen 2. third entryq", TextRange(6, 8)),
        )
        state.onTextFieldValueChange(
            TextFieldValue("👍rst \nentry 1. sen 2. third entryq", TextRange(7)),
        )
        state.onTextFieldValueChange(
            TextFieldValue("👍rst  . sird entryq", TextRange(7)),
        )
    }

    /**
     * A newline split followed by an insertion at the new paragraph's start must keep
     * the span tree consistent; a later small deletion must not no-op and crash.
     */
    @Test
    fun `deletion after a newline split and an insertion must not desync the span tree`() {
        val state = RichTextState()
        state.setHtml("<p><b>Bold start</b> middle <i>italic end</i></p><ul><li>alpha</li><li>beta</li></ul>")

        // Large deletion, "u\n" typed at index 1, "qw" typed at the new paragraph's
        // start, then a 2-char deletion inside it
        state.onTextFieldValueChange(
            TextFieldValue("ostartmiddlic end • alpha • beta", TextRange(1)),
        )
        state.onTextFieldValueChange(
            TextFieldValue("ou\nstartmiddlic end • alpha • beta", TextRange(3)),
        )
        state.onTextFieldValueChange(
            TextFieldValue("ou qwstartmiddlic end • alpha • beta", TextRange(5)),
        )
        state.onTextFieldValueChange(
            TextFieldValue("ou qwstartmilic end • alpha • beta", TextRange(12)),
        )
    }

    // #726
    @Test
    fun `html round trip must preserve consecutive spaces`() {
        val state = RichTextState()
        state.setText("a  b")

        val html = state.toHtml()
        val reloaded = RichTextState()
        reloaded.setHtml(html)

        assertEquals(
            state.textFieldValue.text,
            reloaded.textFieldValue.text,
            "Round trip collapsed consecutive spaces, html=$html",
        )
    }

    /**
     * The document is built through edits (not setHtml) because setHtml used to drop
     * empty items on the way in as well.
     */
    @Test
    fun `empty list items must survive an html round trip`() {
        val state = RichTextState()
        state.setMarkdown("- item one")
        val text = state.textFieldValue.text // "• item one"
        state.onTextFieldValueChange(TextFieldValue(text, TextRange(text.length)))
        // Enter at the end of the item creates a new, empty list item
        state.onTextFieldValueChange(TextFieldValue("$text\n", TextRange(text.length + 1)))
        val originalText = state.textFieldValue.text

        val html = state.toHtml()
        val reloaded = RichTextState()
        reloaded.setHtml(html)

        assertEquals(
            originalText,
            reloaded.textFieldValue.text,
            "Round trip dropped the empty list item, html=$html",
        )
    }

    // #735: the full named-entities table encodes "fj" as "&fjlig;"
    @Test
    fun `text containing the letter pair fj must survive an html round trip`() {
        val state = RichTextState()
        state.setText("fjord")

        val html = state.toHtml()
        val reloaded = RichTextState()
        reloaded.setHtml(html)

        assertEquals(
            "fjord",
            reloaded.textFieldValue.text,
            "Round trip mangled 'fj', html=$html",
        )
    }

    /**
     * Asserts encode idempotence: reimporting "<li>a<br>b</li>" used to derail list
     * reconstruction and the next encode emitted malformed HTML.
     */
    @Test
    fun `line break inside a list item must survive an html round trip`() {
        val state = RichTextState()
        state.setHtml("<ul><li>a<br>b</li></ul>")

        val html2 = state.toHtml()
        val reloaded = RichTextState()
        reloaded.setHtml(html2)
        val html3 = reloaded.toHtml()

        assertEquals(
            html2,
            html3,
            "Re-encoding the round-tripped list item changed (and malformed) the HTML",
        )
    }

    // #734
    @Test
    fun `markdown ordered list with continuation text must keep its numbering`() {
        val state = RichTextState()
        state.setMarkdown(
            """
            Hello! This is a notice.

            1. First item title
            This is the first item description.

            2. Second item title

            3. Third item title
            This is the third item description.
            """.trimIndent(),
        )

        val text = state.textFieldValue.text
        assertEquals(
            true,
            text.contains("2. Second item title") && text.contains("3. Third item title"),
            "Ordered list numbering was reset, text=\"${text.replace("\n", "\\n")}\"",
        )
    }

    /**
     * #736: asserts HTML structure rather than a round trip because the library's own
     * parser reimports the malformed sibling-nested shape losslessly; only external
     * consumers (browsers) see the breakage.
     */
    @Test
    fun `nested list levels must export well formed html`() {
        val state = RichTextState()
        state.setMarkdown("1. a\n2. b\n3. c\n4. d\n5. e")

        fun caretOn(content: Char) {
            val text = state.textFieldValue.text
            val index = text.indexOf(content)
            state.onTextFieldValueChange(
                TextFieldValue(text, TextRange(index + 1)),
            )
        }

        caretOn('c')
        state.increaseListLevel() // c -> level 2
        caretOn('d')
        state.increaseListLevel()
        state.increaseListLevel() // d -> level 3

        val html = state.toHtml()

        assertEquals(
            false,
            html.contains("</li><ol") || html.contains("</ol><ol"),
            "Nested lists must nest inside their parent <li> and must not split the " +
                "top-level list, html=$html",
        )
    }

    /**
     * Unordered flavor of the #736 defect; same structure-based assertion.
     */
    @Test
    fun `unordered list nesting must export well formed html`() {
        val state = RichTextState()
        state.setMarkdown("- a\n- b")
        val text = state.textFieldValue.text
        state.onTextFieldValueChange(
            TextFieldValue(text, TextRange(text.indexOf('b') + 1)),
        )
        state.increaseListLevel() // b -> level 2

        val html = state.toHtml()

        assertEquals(
            false,
            html.contains("</li><ul") || html.contains("</ul><ul"),
            "Nested unordered lists must nest inside their parent <li>, html=$html",
        )
    }

    @Test
    fun `undo after typing into a loaded document must revert only the typing`() {
        val state = RichTextState()
        state.setText("base text")
        val loaded = state.textFieldValue.text

        state.onTextFieldValueChange(TextFieldValue("$loaded!", TextRange(loaded.length + 1)))
        state.history.undo()

        assertEquals(
            loaded,
            state.textFieldValue.text,
            "Undo must revert the typed character, not the whole loaded document",
        )
    }
}
