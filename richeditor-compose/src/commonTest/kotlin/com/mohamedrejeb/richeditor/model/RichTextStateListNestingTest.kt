package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.paragraph.RichParagraph
import com.mohamedrejeb.richeditor.paragraph.type.OrderedList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class RichTextStateListNestingTest {

    @Test
    fun testCanIncreaseListNestedLevel() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialNestedLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Second",
                            paragraph = it,
                        ),
                    )
                }
            )
        )
        richTextState.selection = TextRange(10)

        assertTrue(richTextState.canIncreaseListNestedLevel())
    }

    @Test
    fun testCannotIncreaseListNestedLevelWithoutPreviousList() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First",
                            paragraph = it,
                        ),
                    )
                }
            )
        )
        richTextState.selection = TextRange(5)

        assertFalse(richTextState.canIncreaseListNestedLevel())
    }

    @Test
    fun testCannotIncreaseListNestedLevelWhenHigherThanPrevious() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialNestedLevel = 2
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Second",
                            paragraph = it,
                        ),
                    )
                }
            )
        )
        richTextState.selection = TextRange(11)

        assertFalse(richTextState.canIncreaseListNestedLevel())
    }

    @Test
    fun testCanDecreaseListNestedLevel() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 2
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First",
                            paragraph = it,
                        ),
                    )
                }
            )
        )
        richTextState.selection = TextRange(5)

        assertTrue(richTextState.canDecreaseListNestedLevel())
    }

    @Test
    fun testCannotDecreaseListNestedLevelAtLevel1() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First",
                            paragraph = it,
                        ),
                    )
                }
            )
        )
        richTextState.selection = TextRange(5)

        assertFalse(richTextState.canDecreaseListNestedLevel())
    }

    @Test
    fun testIncreaseListNestedLevel() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First",
                            paragraph = it,
                        ),
                    )
                },
                RichParagraph(
                    type = OrderedList(
                        number = 2,
                        initialNestedLevel = 1
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "Second",
                            paragraph = it,
                        ),
                    )
                }
            )
        )
        richTextState.selection = TextRange(11)

        richTextState.increaseListNestedLevel()

        val secondParagraphType = richTextState.richParagraphList[1].type as OrderedList
        assertEquals(2, secondParagraphType.nestedLevel)
    }

    @Test
    fun testDecreaseListNestedLevel() {
        val richTextState = RichTextState(
            initialRichParagraphList = listOf(
                RichParagraph(
                    type = OrderedList(
                        number = 1,
                        initialNestedLevel = 2
                    ),
                ).also {
                    it.children.add(
                        RichSpan(
                            text = "First",
                            paragraph = it,
                        ),
                    )
                }
            )
        )
        richTextState.selection = TextRange(5)

        richTextState.decreaseListNestedLevel()

        val paragraphType = richTextState.richParagraphList[0].type as OrderedList
        assertEquals(1, paragraphType.nestedLevel)
    }

}
