package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CssDecoderTest {
    @Test
    fun testDecodeCssStyleMap() {
        val map = mapOf(
            "font-weight" to "bold",
            "color" to "#ff0000",
            "font-size" to "12px"
        )
        val map2 = mapOf(
            "font-weight" to "100",
            "color" to "#f00",
            "font-size" to "-12.5px"
        )

        assertEquals(
            "font-weight: bold; color: #ff0000; font-size: 12px;",
            CssDecoder.decodeCssStyleMap(map)
        )
        assertEquals(
            "font-weight: 100; color: #f00; font-size: -12.5px;",
            CssDecoder.decodeCssStyleMap(map2)
        )
    }

    @Test
    fun testDecodeColorToCss() {
        val color = Color(255, 0, 0)
        val color2 = Color(255, 0, 0, 102)
        val color3 = Color(255, 0, 0, 107)
        val color4 = Color(0, 0, 255)

        assertEquals(
            "rgba(255, 0, 0, 1.0)",
            CssDecoder.decodeColorToCss(color)
        )
        assertEquals(
            "rgba(255, 0, 0, 0.4)",
            CssDecoder.decodeColorToCss(color2)
        )
        assertEquals(
            "rgba(255, 0, 0, 0.42)",
            CssDecoder.decodeColorToCss(color3)
        )
        assertEquals(
            "rgba(0, 0, 255, 1.0)",
            CssDecoder.decodeColorToCss(color4)
        )
    }

    @Test
    fun testDecodeSizeToCss() {
        val size = 12f
        val size2 = 16f
        val size3 = 192f
        val size4 = 1.92f
        val size5 = 12.5f
        val size6 = -12f

        assertEquals(
            "12.0px",
            CssDecoder.decodeSizeToCss(size)
        )
        assertEquals(
            "16.0px",
            CssDecoder.decodeSizeToCss(size2)
        )
        assertEquals(
            "192.0px",
            CssDecoder.decodeSizeToCss(size3)
        )
        assertEquals(
            "1.92px",
            CssDecoder.decodeSizeToCss(size4)
        )
        assertEquals(
            "12.5px",
            CssDecoder.decodeSizeToCss(size5)
        )
        assertEquals(
            "-12.0px",
            CssDecoder.decodeSizeToCss(size6)
        )
    }

    @Test
    fun testDecodeTextUnitToCss() {
        val textUnit = 12.em
        val textUnit2 = 16.em
        val textUnit3 = 192.em
        val textUnit4 = 1.92.em
        val textUnit5 = 12.5.em
        val textUnit6 = 24.sp
        val textUnit7 = TextUnit.Unspecified
        val textUnit8: TextUnit? = null

        assertEquals(
            "12.0em",
            CssDecoder.decodeTextUnitToCss(textUnit)
        )
        assertEquals(
            "16.0em",
            CssDecoder.decodeTextUnitToCss(textUnit2)
        )
        assertEquals(
            "192.0em",
            CssDecoder.decodeTextUnitToCss(textUnit3)
        )
        assertEquals(
            "1.92em",
            CssDecoder.decodeTextUnitToCss(textUnit4)
        )
        assertEquals(
            "12.5em",
            CssDecoder.decodeTextUnitToCss(textUnit5)
        )
        assertEquals(
            "24.0px",
            CssDecoder.decodeTextUnitToCss(textUnit6)
        )
        assertEquals(
            null,
            CssDecoder.decodeTextUnitToCss(textUnit7)
        )
        assertEquals(
            null,
            CssDecoder.decodeTextUnitToCss(textUnit8)
        )
    }

    @Test
    fun testDecodeFontWeightToCss() {
        val fontWeight = FontWeight.Thin
        val fontWeight2 = FontWeight.ExtraLight
        val fontWeight3 = FontWeight.Light
        val fontWeight4 = FontWeight.Normal
        val fontWeight5 = FontWeight.Medium
        val fontWeight6 = FontWeight.SemiBold
        val fontWeight7 = FontWeight.Bold
        val fontWeight8 = FontWeight.ExtraBold
        val fontWeight9 = FontWeight.Black

        assertEquals(
            "100",
            CssDecoder.decodeFontWeightToCss(fontWeight)
        )
        assertEquals(
            "200",
            CssDecoder.decodeFontWeightToCss(fontWeight2)
        )
        assertEquals(
            "300",
            CssDecoder.decodeFontWeightToCss(fontWeight3)
        )
        assertEquals(
            "400",
            CssDecoder.decodeFontWeightToCss(fontWeight4)
        )
        assertEquals(
            "500",
            CssDecoder.decodeFontWeightToCss(fontWeight5)
        )
        assertEquals(
            "600",
            CssDecoder.decodeFontWeightToCss(fontWeight6)
        )
        assertEquals(
            "700",
            CssDecoder.decodeFontWeightToCss(fontWeight7)
        )
        assertEquals(
            "800",
            CssDecoder.decodeFontWeightToCss(fontWeight8)
        )
        assertEquals(
            "900",
            CssDecoder.decodeFontWeightToCss(fontWeight9)
        )
    }

    @Test
    fun testDecodeFontStyleToCss() {
        val fontStyle = FontStyle.Italic
        val fontStyle2 = FontStyle.Normal

        assertEquals(
            "italic",
            CssDecoder.decodeFontStyleToCss(fontStyle)
        )
        assertEquals(
            "normal",
            CssDecoder.decodeFontStyleToCss(fontStyle2)
        )
    }

    @Test
    fun testDecodeTextDecorationToCss() {
        val textDecoration = TextDecoration.Underline
        val textDecoration2 = TextDecoration.LineThrough
        val textDecoration3 = TextDecoration.None
        val textDecoration4 = TextDecoration.combine(
            listOf(
                TextDecoration.Underline,
                TextDecoration.LineThrough
            )
        )

        assertEquals(
            "underline",
            CssDecoder.decodeTextDecorationToCss(textDecoration)
        )
        assertEquals(
            "line-through",
            CssDecoder.decodeTextDecorationToCss(textDecoration2)
        )
        assertEquals(
            "none",
            CssDecoder.decodeTextDecorationToCss(textDecoration3)
        )
        assertEquals(
            "underline line-through",
            CssDecoder.decodeTextDecorationToCss(textDecoration4)
        )
    }

    @Test
    fun testDecodeBaselineShiftToCss() {
        val baselineShift = BaselineShift.Subscript
        val baselineShift2 = BaselineShift.Superscript
        val baselineShift3 = BaselineShift.None
        val baselineShift4 = BaselineShift(.6f)

        assertEquals(
            "sub",
            CssDecoder.decodeBaselineShiftToCss(baselineShift)
        )
        assertEquals(
            "super",
            CssDecoder.decodeBaselineShiftToCss(baselineShift2)
        )
        assertEquals(
            "baseline",
            CssDecoder.decodeBaselineShiftToCss(baselineShift3)
        )
        assertEquals(
            "60%",
            CssDecoder.decodeBaselineShiftToCss(baselineShift4)
        )
    }

    @Test
    fun testDecodeTextShadowToCss() {
        val textShadow = Shadow(
            offset = Offset(1f, 1f),
            blurRadius = 1f,
            color = Color(0, 0, 0)
        )
        val textShadow2 = Shadow(
            offset = Offset(1f, 1f),
            blurRadius = 2f,
            color = Color(0, 0, 0)
        )
        val textShadow3 = Shadow(
            offset = Offset(1f, 1f),
            blurRadius = 2f,
            color = Color(255, 204, 0)
        )

        assertEquals(
            "1.0px 1.0px 1.0px rgba(0, 0, 0, 1.0)",
            CssDecoder.decodeTextShadowToCss(textShadow)
        )
        assertEquals(
            "1.0px 1.0px 2.0px rgba(0, 0, 0, 1.0)",
            CssDecoder.decodeTextShadowToCss(textShadow2)
        )
        assertEquals(
            "1.0px 1.0px 2.0px rgba(255, 204, 0, 1.0)",
            CssDecoder.decodeTextShadowToCss(textShadow3)
        )
    }

    @Test
    fun testDecodeTextAlignToCss() {
        val textAlign = TextAlign.Left
        val textAlign2 = TextAlign.Right
        val textAlign3 = TextAlign.Center
        val textAlign4 = TextAlign.Justify
        val textAlign5 = TextAlign.Start
        val textAlign6 = TextAlign.End
        val textAlign7: TextAlign? = null

        assertEquals(
            "left",
            CssDecoder.decodeTextAlignToCss(textAlign)
        )
        assertEquals(
            "right",
            CssDecoder.decodeTextAlignToCss(textAlign2)
        )
        assertEquals(
            "center",
            CssDecoder.decodeTextAlignToCss(textAlign3)
        )
        assertEquals(
            "justify",
            CssDecoder.decodeTextAlignToCss(textAlign4)
        )
        assertEquals(
            "left",
            CssDecoder.decodeTextAlignToCss(textAlign5, TextDirection.Ltr)
        )
        assertEquals(
            "right",
            CssDecoder.decodeTextAlignToCss(textAlign5, TextDirection.Rtl)
        )
        assertEquals(
            "right",
            CssDecoder.decodeTextAlignToCss(textAlign6, TextDirection.Ltr)
        )
        assertEquals(
            "left",
            CssDecoder.decodeTextAlignToCss(textAlign6, TextDirection.Rtl)
        )
        assertEquals(
            null,
            CssDecoder.decodeTextAlignToCss(textAlign7)
        )
    }

    @Test
    fun testDecodeTextDirectionToCss() {
        val textDirection = TextDirection.Ltr
        val textDirection2 = TextDirection.Rtl
        val textDirection3: TextDirection? = null

        assertEquals(
            "ltr",
            CssDecoder.decodeTextDirectionToCss(textDirection)
        )
        assertEquals(
            "rtl",
            CssDecoder.decodeTextDirectionToCss(textDirection2)
        )
        assertEquals(
            null,
            CssDecoder.decodeTextDirectionToCss(textDirection3)
        )
    }
}