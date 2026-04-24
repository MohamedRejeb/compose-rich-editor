package com.mohamedrejeb.richeditor.parser.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Reproduces https://github.com/MohamedRejeb/compose-rich-editor/issues/550
 *
 * Bullet list items that are simultaneously bold + italic should each emit
 * `- ***Text***`. The reporter saw the first item rendered correctly but
 * later items as `-** Text**` (missing the leading space + one `*`).
 */
@OptIn(ExperimentalRichTextApi::class)
class Issue550BulletBoldItalicTest {

    private val boldItalic = SpanStyle(
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
    )

    private fun bulletParagraph(text: String): RichParagraph =
        RichParagraph(type = UnorderedList()).also { paragraph ->
            paragraph.children.add(
                RichSpan(
                    text = text,
                    paragraph = paragraph,
                    spanStyle = boldItalic,
                )
            )
        }

    @Test
    fun threeBulletsWithBoldItalic_eachLineHasBalancedDelimiters() {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                bulletParagraph("First"),
                bulletParagraph("Second"),
                bulletParagraph("Third"),
            )
        )

        val expected = """
            - ***First***
            - ***Second***
            - ***Third***
        """.trimIndent()

        assertEquals(expected, state.toMarkdown())
    }

    @Test
    fun twoBulletsWithBoldItalic_secondItemIsCorrect() {
        val state = RichTextState(
            initialRichParagraphList = listOf(
                bulletParagraph("First"),
                bulletParagraph("Second"),
            )
        )

        val expected = """
            - ***First***
            - ***Second***
        """.trimIndent()

        assertEquals(expected, state.toMarkdown())
    }

    @Test
    fun threeBulletsWithBoldOnly_eachLineHasBalancedDelimiters() {
        val bold = SpanStyle(fontWeight = FontWeight.Bold)
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(type = UnorderedList()).also { paragraph ->
                    paragraph.children.add(RichSpan(text = "First", paragraph = paragraph, spanStyle = bold))
                },
                RichParagraph(type = UnorderedList()).also { paragraph ->
                    paragraph.children.add(RichSpan(text = "Second", paragraph = paragraph, spanStyle = bold))
                },
                RichParagraph(type = UnorderedList()).also { paragraph ->
                    paragraph.children.add(RichSpan(text = "Third", paragraph = paragraph, spanStyle = bold))
                },
            )
        )

        val expected = """
            - **First**
            - **Second**
            - **Third**
        """.trimIndent()

        assertEquals(expected, state.toMarkdown())
    }

    @Test
    fun threeBulletsWithBoldItalic_typedThroughApi() {
        val state = RichTextState()
        state.toggleUnorderedList()
        state.toggleSpanStyle(boldItalic)

        typeAtEnd(state, "First")
        typeAtEnd(state, "\n")
        typeAtEnd(state, "Second")
        typeAtEnd(state, "\n")
        typeAtEnd(state, "Third")

        val expected = """
            - ***First***
            - ***Second***
            - ***Third***
        """.trimIndent()

        assertEquals(expected, state.toMarkdown())
    }

    private fun typeAtEnd(state: RichTextState, chunk: String) {
        val current = state.annotatedString.text
        val newText = current + chunk
        state.onTextFieldValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(newText.length),
            )
        )
    }

    @Test
    fun threeBulletsWithItalicOnly_eachLineHasBalancedDelimiters() {
        val italic = SpanStyle(fontStyle = FontStyle.Italic)
        val state = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(type = UnorderedList()).also { paragraph ->
                    paragraph.children.add(RichSpan(text = "First", paragraph = paragraph, spanStyle = italic))
                },
                RichParagraph(type = UnorderedList()).also { paragraph ->
                    paragraph.children.add(RichSpan(text = "Second", paragraph = paragraph, spanStyle = italic))
                },
                RichParagraph(type = UnorderedList()).also { paragraph ->
                    paragraph.children.add(RichSpan(text = "Third", paragraph = paragraph, spanStyle = italic))
                },
            )
        )

        val expected = """
            - *First*
            - *Second*
            - *Third*
        """.trimIndent()

        assertEquals(expected, state.toMarkdown())
    }
}
