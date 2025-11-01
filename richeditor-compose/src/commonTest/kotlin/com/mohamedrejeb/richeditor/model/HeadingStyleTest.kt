package com.mohamedrejeb.richeditor.model

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals

class HeadingStyleTest {

    private val typography = Typography()

    @Test
    fun testGetSpanStyle_fontWeightIsNull() {
        // Verify that getSpanStyle always returns fontWeight = null
        assertEquals(null, HeadingStyle.Normal.getSpanStyle().fontWeight)
        assertEquals(null, HeadingStyle.H1.getSpanStyle().fontWeight)
        assertEquals(null, HeadingStyle.H2.getSpanStyle().fontWeight)
        assertEquals(null, HeadingStyle.H3.getSpanStyle().fontWeight)
        assertEquals(null, HeadingStyle.H4.getSpanStyle().fontWeight)
        assertEquals(null, HeadingStyle.H5.getSpanStyle().fontWeight)
        assertEquals(null, HeadingStyle.H6.getSpanStyle().fontWeight)
    }

    @Test
    fun testGetSpanStyle_matchesTypographyExceptFontWeight() {
        // Verify other properties match typography
        assertEquals(typography.displayLarge.toSpanStyle().copy(fontWeight = null), HeadingStyle.H1.getSpanStyle())
        assertEquals(typography.displayMedium.toSpanStyle().copy(fontWeight = null), HeadingStyle.H2.getSpanStyle())
        assertEquals(typography.displaySmall.toSpanStyle().copy(fontWeight = null), HeadingStyle.H3.getSpanStyle())
        assertEquals(typography.headlineMedium.toSpanStyle().copy(fontWeight = null), HeadingStyle.H4.getSpanStyle())
        assertEquals(typography.headlineSmall.toSpanStyle().copy(fontWeight = null), HeadingStyle.H5.getSpanStyle())
        assertEquals(typography.titleLarge.toSpanStyle().copy(fontWeight = null), HeadingStyle.H6.getSpanStyle())
        assertEquals(SpanStyle(), HeadingStyle.Normal.getSpanStyle()) // Normal should be default
    }

    @Test
    fun testGetParagraphStyle_matchesTypography() {
        // Verify paragraph styles match typography
        assertEquals(typography.displayLarge.toParagraphStyle(), HeadingStyle.H1.getParagraphStyle())
        assertEquals(typography.displayMedium.toParagraphStyle(), HeadingStyle.H2.getParagraphStyle())
        assertEquals(typography.displaySmall.toParagraphStyle(), HeadingStyle.H3.getParagraphStyle())
        assertEquals(typography.headlineMedium.toParagraphStyle(), HeadingStyle.H4.getParagraphStyle())
        assertEquals(typography.headlineSmall.toParagraphStyle(), HeadingStyle.H5.getParagraphStyle())
        assertEquals(typography.titleLarge.toParagraphStyle(), HeadingStyle.H6.getParagraphStyle())
        assertEquals(ParagraphStyle(), HeadingStyle.Normal.getParagraphStyle()) // Normal should be default
    }

    @Test
    fun testFromSpanStyle_matchesBaseHeading() {
        // Test matching base heading styles (which have fontWeight = null from getSpanStyle)
        assertEquals(HeadingStyle.H1, HeadingStyle.fromSpanStyle(HeadingStyle.H1.getSpanStyle()))
        assertEquals(HeadingStyle.H2, HeadingStyle.fromSpanStyle(HeadingStyle.H2.getSpanStyle()))
        assertEquals(HeadingStyle.H3, HeadingStyle.fromSpanStyle(HeadingStyle.H3.getSpanStyle()))
        assertEquals(HeadingStyle.H4, HeadingStyle.fromSpanStyle(HeadingStyle.H4.getSpanStyle()))
        assertEquals(HeadingStyle.H5, HeadingStyle.fromSpanStyle(HeadingStyle.H5.getSpanStyle()))
        assertEquals(HeadingStyle.H6, HeadingStyle.fromSpanStyle(HeadingStyle.H6.getSpanStyle()))
        assertEquals(HeadingStyle.Normal, HeadingStyle.fromSpanStyle(HeadingStyle.Normal.getSpanStyle()))
    }

    @Test
    fun testFromSpanStyle_matchesBaseHeadingWithBold() {
        // Test matching base heading styles when the input SpanStyle has FontWeight.Bold
        // The fromSpanStyle logic should ignore the base heading's null fontWeight
        assertEquals(HeadingStyle.H1, HeadingStyle.fromSpanStyle(HeadingStyle.H1.getSpanStyle().copy(fontWeight = FontWeight.Bold)))
        assertEquals(HeadingStyle.H2, HeadingStyle.fromSpanStyle(HeadingStyle.H2.getSpanStyle().copy(fontWeight = FontWeight.Bold)))
        assertEquals(HeadingStyle.H3, HeadingStyle.fromSpanStyle(HeadingStyle.H3.getSpanStyle().copy(fontWeight = FontWeight.Bold)))
        assertEquals(HeadingStyle.H4, HeadingStyle.fromSpanStyle(HeadingStyle.H4.getSpanStyle().copy(fontWeight = FontWeight.Bold)))
        assertEquals(HeadingStyle.H5, HeadingStyle.fromSpanStyle(HeadingStyle.H5.getSpanStyle().copy(fontWeight = FontWeight.Bold)))
        assertEquals(HeadingStyle.H6, HeadingStyle.fromSpanStyle(HeadingStyle.H6.getSpanStyle().copy(fontWeight = FontWeight.Bold)))
        // Normal paragraph with bold should still be Normal
        assertEquals(HeadingStyle.Normal, HeadingStyle.fromSpanStyle(HeadingStyle.Normal.getSpanStyle().copy(fontWeight = FontWeight.Bold)))
    }

    @Test
    fun testFromSpanStyle_noMatchReturnsNormal() {
        // Test SpanStyles that don't match any heading
        assertEquals(HeadingStyle.Normal, HeadingStyle.fromSpanStyle(SpanStyle()))
        assertEquals(HeadingStyle.Normal, HeadingStyle.fromSpanStyle(SpanStyle(fontSize = 10.sp))) // Different size
        assertEquals(HeadingStyle.Normal, HeadingStyle.fromSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))) // Only bold
    }

    @Test
    fun testFromParagraphStyle_matchesBaseHeading() {
        // Test matching base paragraph styles
        assertEquals(HeadingStyle.H1, HeadingStyle.fromParagraphStyle(HeadingStyle.H1.getParagraphStyle()))
        assertEquals(HeadingStyle.H2, HeadingStyle.fromParagraphStyle(HeadingStyle.H2.getParagraphStyle()))
        assertEquals(HeadingStyle.H3, HeadingStyle.fromParagraphStyle(HeadingStyle.H3.getParagraphStyle()))
        assertEquals(HeadingStyle.H4, HeadingStyle.fromParagraphStyle(HeadingStyle.H4.getParagraphStyle()))
        assertEquals(HeadingStyle.H5, HeadingStyle.fromParagraphStyle(HeadingStyle.H5.getParagraphStyle()))
        assertEquals(HeadingStyle.H6, HeadingStyle.fromParagraphStyle(HeadingStyle.H6.getParagraphStyle()))
        assertEquals(HeadingStyle.Normal, HeadingStyle.fromParagraphStyle(HeadingStyle.Normal.getParagraphStyle()))
    }

    @Test
    fun testFromParagraphStyle_noMatchReturnsNormal() {
        // Test ParagraphStyles that don't match any heading
        assertEquals(HeadingStyle.Normal, HeadingStyle.fromParagraphStyle(ParagraphStyle()))
        assertEquals(HeadingStyle.Normal, HeadingStyle.fromParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))) // Different alignment
    }
}
