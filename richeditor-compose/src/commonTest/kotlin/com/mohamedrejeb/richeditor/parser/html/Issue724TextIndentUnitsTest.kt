package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.unit.TextUnitType
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression pin for #724: text-indent values in em/rem/% used to reach
 * [androidx.compose.ui.text.style.TextIndent] as Em units, which crashes Compose's
 * layout pass with "Only Sp can convert to Px". Relative units are resolved against
 * the 16px default font size on import.
 */
class Issue724TextIndentUnitsTest {

    @Test
    fun `text-indent in em rem or percent must not produce Em text indent`() {
        val offenders = mutableListOf<String>()
        for (css in listOf("2em", "1.5rem", "50%", "24px", "12pt")) {
            val state = RichTextState()
            state.setHtml("""<p style="text-indent: $css">Hello</p>""")
            val indent = state.richParagraphList.first().paragraphStyle.textIndent
            if (indent != null &&
                (indent.firstLine.type == TextUnitType.Em || indent.restLine.type == TextUnitType.Em)
            ) {
                offenders += "$css -> $indent"
            }
        }
        assertTrue(offenders.isEmpty(), "Em TextIndent reached the paragraph style:\n" + offenders.joinToString("\n"))
    }
}
