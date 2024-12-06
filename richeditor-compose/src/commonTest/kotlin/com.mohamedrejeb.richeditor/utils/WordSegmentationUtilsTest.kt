package com.mohamedrejeb.richeditor.utils

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import kotlin.test.Test
import kotlin.test.assertEquals

class WordSegmentationUtilsTest {
    @Test
    fun testRichParagraphWordSequence() {

        val p1 = RichParagraph()
        p1.children.addAll(
            mutableListOf(
                t(p1, "Hello world"),
                t(p1, " Kotlin is great!"),
                t(
                    p1, "",
                    t(p1, " C"),
                    t(p1, "at"),
                ),
            )
        )

        val p2 = RichParagraph()
        val nestedSpan = t(p2, "", t(p2, "Nested words here"))
        p2.children.addAll(mutableListOf(nestedSpan))

        val paragraphs = listOf(p1, p2)

        verify(
            paragraphs,
            "Hello", "world", "Kotlin", "is", "great", "Cat", "Nested", "words", "here"
        )
    }

    @Test
    fun testRichParagraphWordSequenceTwo() {
        val p = RichParagraph()

        val span1 = t(
            p, "RichTextEditor",
            t(
                p, " is a ",
                t(p, "composable"),
                t(p, " that allows you to edit "),
                t(p, "rich text"),
                t(p, " content."),
            )
        )
        p.children.add(span1)

        val paragraphs = listOf(p)

        verify(
            paragraphs,
            "RichTextEditor",
            "is",
            "a",
            "composable",
            "that",
            "allows",
            "you",
            "to",
            "edit",
            "rich",
            "text",
            "content"
        )
    }

    private fun verify(paragraphs: List<RichParagraph>, vararg expectedWords: String) {
        // Get all words from the list of paragraphs
        var ii = 0
        for ((word, range) in paragraphs.getWords()) {
            //println("Word: '$word' at range: [${range.start}, ${range.end})")
            assertEquals(expectedWords[ii], word)
            ++ii
        }
    }

    @OptIn(ExperimentalRichTextApi::class)
    private fun t(p: RichParagraph, text: String, vararg children: RichSpan): RichSpan {
        return RichSpan(
            text = text,
            paragraph = p,
            children = mutableListOf(*children),
        )
    }
}