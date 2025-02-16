package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.text.TextRange
import kotlin.test.Test

class RichTextStateOrderedListTest {

    @Test
    fun testDeletingTenOrderedListItems() {
        // This was causing a crash
        val richTextState = RichTextState()

        richTextState.setMarkdown(
            """
                1. a
                2. a
                3. a
                4. a
                5. a
                6. a
                7. a
                8. a
                9. a
                10. a
            """.trimIndent()
        )

        richTextState.selection = TextRange(0, richTextState.annotatedString.length)

        richTextState.removeSelectedText()
    }

}