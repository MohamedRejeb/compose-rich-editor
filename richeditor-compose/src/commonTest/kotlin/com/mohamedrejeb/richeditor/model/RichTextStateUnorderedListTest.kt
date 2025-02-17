package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.DefaultParagraph
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedList
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedListStyleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun testLevelsWithDifferentStyleType() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = UnorderedList(
                        initialLevel = 1
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
                        initialLevel = 2
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
                        initialLevel = 3
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
                        initialLevel = 1
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
                        initialLevel = 2
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
                        initialLevel = 3
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
                        initialLevel = 4 // Beyond the default prefix list length
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
                initialLevel = 1
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

    @Test
    fun testExitEmptyListItem() {
        // Test with exitListOnEmptyItem = true (default)
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = UnorderedList(),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "",
                            paragraph = it,
                        ),
                    )
                }
            )
        )

        // Simulate pressing Enter on empty list item
        richTextState.selection = TextRange(richTextState.annotatedString.length)
        richTextState.addTextAfterSelection("\n")

        // Verify that list formatting is removed
        assertEquals(1, richTextState.richParagraphList.size)
        assertIs<DefaultParagraph>(richTextState.richParagraphList[0].type)

        // Test with exitListOnEmptyItem = false
        val richTextState2 = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = UnorderedList(),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "",
                            paragraph = it,
                        ),
                    )
                }
            )
        )
        richTextState2.config.exitListOnEmptyItem = false

        // Simulate pressing Enter on empty list item
        richTextState2.selection = TextRange(richTextState2.annotatedString.length)
        richTextState2.addTextAfterSelection("\n")

        // Verify that list formatting is preserved
        assertEquals(2, richTextState2.richParagraphList.size)
        assertIs<UnorderedList>(richTextState2.richParagraphList[0].type)
        assertIs<UnorderedList>(richTextState2.richParagraphList[1].type)
    }
}
