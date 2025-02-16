package com.mohamedrejeb.richeditor.paragraph.type

import kotlin.test.Test
import kotlin.test.assertEquals

class OrderedListStyleTypeTest {
    @Test
    fun testDecimalFormat() {
        val styleType = OrderedListStyleType.Decimal
        assertEquals("1", styleType.format(1, 1))
        assertEquals("10", styleType.format(10, 1))
        assertEquals("100", styleType.format(100, 1))
        assertEquals("0", styleType.format(0, 1))
        assertEquals("-1", styleType.format(-1, 1))
    }

    @Test
    fun testLowerAlphaFormat() {
        val styleType = OrderedListStyleType.LowerAlpha
        assertEquals("a", styleType.format(1, 1))
        assertEquals("b", styleType.format(2, 1))
        assertEquals("z", styleType.format(26, 1))
        assertEquals("aa", styleType.format(27, 1))
        assertEquals("ab", styleType.format(28, 1))
        assertEquals("a", styleType.format(0, 1))
    }

    @Test
    fun testUpperAlphaFormat() {
        val styleType = OrderedListStyleType.UpperAlpha
        assertEquals("A", styleType.format(1, 1))
        assertEquals("B", styleType.format(2, 1))
        assertEquals("Z", styleType.format(26, 1))
        assertEquals("AA", styleType.format(27, 1))
        assertEquals("AB", styleType.format(28, 1))
        assertEquals("A", styleType.format(0, 1))
    }

    @Test
    fun testLowerRomanFormat() {
        val styleType = OrderedListStyleType.LowerRoman
        assertEquals("i", styleType.format(1, 1))
        assertEquals("ii", styleType.format(2, 1))
        assertEquals("iii", styleType.format(3, 1))
        assertEquals("iv", styleType.format(4, 1))
        assertEquals("v", styleType.format(5, 1))
        assertEquals("vi", styleType.format(6, 1))
        assertEquals("vii", styleType.format(7, 1))
        assertEquals("viii", styleType.format(8, 1))
        assertEquals("ix", styleType.format(9, 1))
        assertEquals("x", styleType.format(10, 1))
        assertEquals("i", styleType.format(0, 1))
    }

    @Test
    fun testUpperRomanFormat() {
        val styleType = OrderedListStyleType.UpperRoman
        assertEquals("I", styleType.format(1, 1))
        assertEquals("II", styleType.format(2, 1))
        assertEquals("III", styleType.format(3, 1))
        assertEquals("IV", styleType.format(4, 1))
        assertEquals("V", styleType.format(5, 1))
        assertEquals("VI", styleType.format(6, 1))
        assertEquals("VII", styleType.format(7, 1))
        assertEquals("VIII", styleType.format(8, 1))
        assertEquals("IX", styleType.format(9, 1))
        assertEquals("X", styleType.format(10, 1))
        assertEquals("I", styleType.format(0, 1))
    }

    @Test
    fun testDefaultSuffix() {
        val styleTypes = listOf(
            OrderedListStyleType.Decimal,
            OrderedListStyleType.LowerAlpha,
            OrderedListStyleType.UpperAlpha,
            OrderedListStyleType.LowerRoman,
            OrderedListStyleType.UpperRoman
        )

        styleTypes.forEach { styleType ->
            assertEquals(". ", styleType.getSuffix(1))
        }
    }

    @Test
    fun testMultipleEmptyStyles() {
        val styleType = OrderedListStyleType.Multiple()
        // Should default to Decimal behavior
        assertEquals("1", styleType.format(1, 1))
        assertEquals("2", styleType.format(2, 2))
        assertEquals(". ", styleType.getSuffix(1))
    }

    @Test
    fun testMultipleSingleStyle() {
        val styleType = OrderedListStyleType.Multiple(OrderedListStyleType.UpperAlpha)
        // Should use UpperAlpha for all levels
        assertEquals("A", styleType.format(1, 1))
        assertEquals("B", styleType.format(2, 2))
        assertEquals("C", styleType.format(3, 3))
        assertEquals(". ", styleType.getSuffix(1))
    }

    @Test
    fun testMultipleStyles() {
        val styleType = OrderedListStyleType.Multiple(
            OrderedListStyleType.UpperAlpha,
            OrderedListStyleType.LowerAlpha,
            OrderedListStyleType.Decimal
        )
        // Should use different styles based on nested level
        assertEquals("A", styleType.format(1, 1)) // First level: UpperAlpha
        assertEquals("a", styleType.format(1, 2)) // Second level: LowerAlpha
        assertEquals("1", styleType.format(1, 3)) // Third level: Decimal
        assertEquals("1", styleType.format(1, 4)) // Beyond styles: use last style (Decimal)
    }

    @Test
    fun testMultipleStylesNestedLevelBounds() {
        val styleType = OrderedListStyleType.Multiple(
            OrderedListStyleType.UpperAlpha,
            OrderedListStyleType.LowerAlpha
        )
        // Test nested level bounds
        assertEquals("A", styleType.format(1, 0)) // Level <= 0 should use first style
        assertEquals("A", styleType.format(1, 1)) // First level
        assertEquals("a", styleType.format(1, 2)) // Second level
        assertEquals("a", styleType.format(1, 3)) // Beyond styles should use last style
    }
}
