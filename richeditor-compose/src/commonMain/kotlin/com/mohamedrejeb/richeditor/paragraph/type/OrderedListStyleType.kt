package com.mohamedrejeb.richeditor.paragraph.type

/**
 * Interface for customizing the style of ordered list markers.
 *
 * This interface allows you to define how numbers in ordered lists should be formatted.
 * Several predefined implementations are provided in the companion object:
 * - [OrderedListStyleType.Decimal] for decimal numbers (1, 2, 3, ...)
 * - [OrderedListStyleType.LowerAlpha] for lowercase letters (a, b, c, ...)
 * - [OrderedListStyleType.UpperAlpha] for uppercase letters (A, B, C, ...)
 * - [OrderedListStyleType.LowerRoman] for lowercase Roman numerals (i, ii, iii, ...)
 * - [OrderedListStyleType.UpperRoman] for uppercase Roman numerals (I, II, III, ...)
 *
 * You can also create your own custom implementations by implementing this interface.
 *
 * Example usage:
 * ```kotlin
 * // Using a predefined style
 * richTextState.config.orderedListStyleType = OrderedListStyleType.UpperRoman
 *
 * // Creating a custom style
 * val customStyle = object : OrderedListStyleType {
 *     override fun format(number: Int) = "Item $number"
 *     override fun getSuffix() = ") "
 * }
 * ```
 */
public interface OrderedListStyleType {
    /**
     * Format the number into a string representation.
     *
     * @param number The number to format (1-based index)
     * @param nestedLevel The nested level of the list item (1-based index)
     * @return The formatted string representation of the number
     */
    public fun format(number: Int, nestedLevel: Int): String = number.toString()

    /**
     * Get the suffix to append after the formatted number.
     * Default implementation returns ". " (dot with space).
     *
     * @return The suffix string to append after the formatted number
     */
    public fun getSuffix(nestedLevel: Int): String = ". "

    /**
     * Default decimal numbers style (1, 2, 3, ...)
     */
    public object Decimal: OrderedListStyleType {
        override fun format(number: Int, nestedLevel: Int): String = number.toString()
    }

    /**
     * Lowercase letters style (a, b, c, ...)
     */
    public object LowerAlpha : OrderedListStyleType {
        override fun format(number: Int, nestedLevel: Int): String =
            formatToAlpha(
                number = number,
                base = 'a'
            )
    }

    /**
     * Uppercase letters style (A, B, C, ...)
     */
    public object UpperAlpha: OrderedListStyleType {
        override fun format(number: Int, nestedLevel: Int): String =
            formatToAlpha(
                number = number,
                base = 'A'
            )
    }

    /**
     * Lowercase Roman numerals style (i, ii, iii, ...)
     */
    public object LowerRoman: OrderedListStyleType {
        private val romanNumerals = arrayOf(
            "m" to 1000,
            "cm" to 900,
            "d" to 500,
            "cd" to 400,
            "c" to 100,
            "xc" to 90,
            "l" to 50,
            "xl" to 40,
            "x" to 10,
            "ix" to 9,
            "v" to 5,
            "iv" to 4,
            "i" to 1
        )

        override fun format(number: Int, nestedLevel: Int): String =
            formatToRomanNumber(
                number = number,
                romanNumerals = romanNumerals,
                defaultValue = "i"
            )
    }

    /**
     * Uppercase Roman numerals style (I, II, III, ...)
     */
    public object UpperRoman: OrderedListStyleType {
        private val romanNumerals = arrayOf(
            "M" to 1000,
            "CM" to 900,
            "D" to 500,
            "CD" to 400,
            "C" to 100,
            "XC" to 90,
            "L" to 50,
            "XL" to 40,
            "X" to 10,
            "IX" to 9,
            "V" to 5,
            "IV" to 4,
            "I" to 1
        )

        override fun format(number: Int, nestedLevel: Int): String =
            formatToRomanNumber(
                number = number,
                romanNumerals = romanNumerals,
                defaultValue = "I"
            )
    }

    public class Multiple(
        public vararg val styles: OrderedListStyleType,
    ) : OrderedListStyleType {
        override fun format(number: Int, nestedLevel: Int): String {
            if (styles.isEmpty())
                return Decimal.format(number, nestedLevel)

            val style = styles[(nestedLevel - 1).coerceIn(styles.indices)]
            return style.format(number, nestedLevel)
        }

        override fun getSuffix(nestedLevel: Int): String {
            if (styles.isEmpty())
                return Decimal.getSuffix(nestedLevel)

            val style = styles[(nestedLevel - 1).coerceIn(styles.indices)]
            return style.getSuffix(nestedLevel)
        }
    }

    private companion object {

        private fun formatToAlpha(
            number: Int,
            base: Char,
        ): String {
            if (number <= 0)
                return base.toString()

            val baseCode = base.code - 1
            val result = StringBuilder()
            var n = number
            while (n > 0) {
                val remainder = (n - 1) % 26 + 1
                result.insert(0, (baseCode + remainder).toChar())
                n = (n - 1) / 26
            }
            return result.toString()
        }

        private fun formatToRomanNumber(
            number: Int,
            romanNumerals: Array<Pair<String, Int>>,
            defaultValue: String,
        ): String {
            if (number <= 0)
                return defaultValue

            var n = number
            val result = StringBuilder()
            for ((roman, value) in romanNumerals) {
                while (n >= value) {
                    result.append(roman)
                    n -= value
                }
            }
            return result.toString()
        }

    }
}
