package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * CSS written by software running in an Arabic or Persian locale can carry numbers in that
 * locale's digits ("font-size: ١٥px"). The parser reads them as the numbers they are.
 */
class CssNonAsciiDigitsTest {

    private fun spanStyleOf(css: String) =
        CssEncoder.parseCssStyleMapToSpanStyle(CssEncoder.parseCssStyle(css))

    @Test
    fun `arabic indic digits in a font size are read`() {
        assertEquals(15.sp, spanStyleOf("font-size: ١٥px").fontSize)
    }

    @Test
    fun `persian digits in a font size are read`() {
        assertEquals(15.sp, spanStyleOf("font-size: ۱۵px").fontSize)
    }

    @Test
    fun `the arabic decimal separator is read as a decimal point`() {
        assertEquals(1.5.em, spanStyleOf("font-size: ١٫٥em").fontSize)
    }

    @Test
    fun `the arabic percent sign is read as a percent sign`() {
        assertEquals(1.5.em, spanStyleOf("font-size: ١٥٠٪").fontSize)
    }

    @Test
    fun `non ASCII digits in a color are read`() {
        assertEquals(Color(255, 0, 0), spanStyleOf("color: rgb(٢٥٥, ٠, ٠)").color)
    }

    @Test
    fun `non ASCII digits in a font weight are read`() {
        assertEquals(FontWeight.Bold, spanStyleOf("font-weight: ٧٠٠").fontWeight)
    }

    @Test
    fun `non ASCII digits in a letter spacing are read`() {
        assertEquals(2.sp, spanStyleOf("letter-spacing: ٢px").letterSpacing)
    }

    @Test
    fun `values without non ASCII digits are left as written`() {
        assertEquals(
            mapOf("font-size" to "12.5px", "color" to "#ff0000", "font-weight" to "bold"),
            CssEncoder.parseCssStyle("font-size: 12.5px; color: #ff0000; font-weight: bold"),
        )
    }

    @Test
    fun `html with an arabic indic font size loads with that size`() {
        val state = RichTextState()

        state.setHtml("""<p><span style="font-size: ١٥px">text</span></p>""")

        assertEquals("text", state.toText())
        assertEquals(15.sp, state.richParagraphList.first().children.first().spanStyle.fontSize)
    }
}
