package com.mohamedrejeb.richeditor.model

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedListStyleType
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalRichTextApi::class)
class RichTextStateUnorderedListTest {

    @Test
    fun testDefaultUnorderedListStyleType() {
        val richTextState = RichTextState()

        // Default style type should be "•", "◦", "▪"
        assertEquals(
            UnorderedListStyleType.from("•", "◦", "▪"),
            richTextState.config.unorderedListStyleType
        )
    }

    @Test
    fun testCustomUnorderedListStyleType() {
        val richTextState = RichTextState()
        val customStyleType = UnorderedListStyleType.from("-", "+", "*")

        richTextState.config.unorderedListStyleType = customStyleType

        assertEquals(
            customStyleType,
            richTextState.config.unorderedListStyleType
        )
    }

    @Test
    fun testNestedLevelsWithDifferentStyleType() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialNestedLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First level",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialNestedLevel = 2
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Second level",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialNestedLevel = 3
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Third level",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Verify that each level uses the correct prefix
        val firstParagraph = richTextState.richParagraphList[0]
        val secondParagraph = richTextState.richParagraphList[1]
        val thirdParagraph = richTextState.richParagraphList[2]

        assertEquals("• ", firstParagraph.type.startRichSpan.text)
        assertEquals("◦ ", secondParagraph.type.startRichSpan.text)
        assertEquals("▪ ", thirdParagraph.type.startRichSpan.text)
    }

    @Test
    fun testPrefixIndexBoundsHandling() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialNestedLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First level",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialNestedLevel = 2
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Second level",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialNestedLevel = 3
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Third level",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = UnorderedList(
                        initialNestedLevel = 4 // Beyond the default prefix list length
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Deep nested level",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Should use the last available prefix when nesting level exceeds prefix list length
        val paragraph = richTextState.richParagraphList[3]
        assertEquals("▪ ", paragraph.type.startRichSpan.text)
    }

    @Test
    fun testEmptyPrefixList() {
        val richTextState = RichTextState()
        richTextState.config.unorderedListStyleType = UnorderedListStyleType.from()

        val paragraph = RichParagraph(
            type = UnorderedList(
                initialNestedLevel = 1
            ),
        ).also {
            it.children.add(
                RichSpan(
                    text = "Test",
                    paragraph = it,
                ),
            )
        }
        richTextState.richParagraphList.clear()
        richTextState.richParagraphList.add(paragraph)

        // Should fallback to bullet point when the prefix list is empty
        assertEquals("• ", paragraph.type.startRichSpan.text)
    }
}
