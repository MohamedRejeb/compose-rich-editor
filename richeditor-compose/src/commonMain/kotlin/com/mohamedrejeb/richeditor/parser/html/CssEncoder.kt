package com.mohamedrejeb.richeditor.parser.html

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

internal object CssEncoder {

    /**
     * Parses the given CSS style string into a map of style name to style value.
     *
     * @param cssStyle the CSS style string to parse.
     * @return the parsed CSS style map.
     */
    internal fun parseCssStyle(cssStyle: String): Map<String, String> {
        return cssStyle
            .split(";")
            .map { it.split(":") }
            .filter { it.size == 2 }
            .associate { it[0].trim() to it[1].trim() }
    }

    /**
     * Converts the given CSS style map into a [SpanStyle].
     *
     * @param cssStyleMap the CSS style map to convert.
     * @return the converted [SpanStyle].
     */
    internal fun parseCssStyleMapToSpanStyle(cssStyleMap: Map<String, String>): SpanStyle {
        return SpanStyle(
            color = cssStyleMap["color"]?.let{ parseCssColor(it) } ?: Color.Unspecified,
            fontSize = cssStyleMap["font-size"]?.let { parseCssSize(it)?.sp } ?: TextUnit.Unspecified,
            fontWeight = cssStyleMap["font-weight"]?.let { parseCssFontWeight(it) },
            fontStyle = cssStyleMap["font-style"]?.let { parseCssFontStyle(it) },
            letterSpacing = cssStyleMap["letter-spacing"]?.let { parseCssSize(it)?.sp } ?: TextUnit.Unspecified,
            baselineShift = cssStyleMap["baseline-shift"]?.let { parseCssBaselineShift(it) },
            background = cssStyleMap["background"]?.let { parseCssColor(it) }
                ?: cssStyleMap["background-color"]?.let { parseCssColor(it) }
                ?: Color.Unspecified,
            textDecoration = cssStyleMap["text-decoration"]?.let { parseCssTextDecoration(it) },
            shadow = cssStyleMap["text-shadow"]?.let { parseCssTextShadow(it) },
        )
    }

    /**
     * Converts the given CSS style map into a [SpanStyle].
     *
     * @param cssStyleMap the CSS style map to convert.
     * @return the converted [SpanStyle].
     */
    internal fun parseCssStyleMapToSpanStyleSet(cssStyleMap: Map<String, String>): Set<SpanStyle> {
        val spanStyleSet = mutableSetOf<SpanStyle>()

        cssStyleMap["color"]?.let{ string ->
            parseCssColor(string)?.let {
                spanStyleSet.add(
                    SpanStyle(color = it)
                )
            }
        }
        cssStyleMap["font-size"]?.let{ string ->
            parseCssSize(string)?.sp?.let {
                spanStyleSet.add(
                    SpanStyle(fontSize = it)
                )
            }
        }
        cssStyleMap["font-weight"]?.let{ string ->
            parseCssFontWeight(string)?.let {
                spanStyleSet.add(
                    SpanStyle(fontWeight = it)
                )
            }
        }
        cssStyleMap["font-style"]?.let{ string ->
            parseCssFontStyle(string)?.let {
                spanStyleSet.add(
                    SpanStyle(fontStyle = it)
                )
            }
        }
        cssStyleMap["letter-spacing"]?.let{ string ->
            parseCssSize(string)?.sp?.let {
                spanStyleSet.add(
                    SpanStyle(letterSpacing = it)
                )
            }
        }
        cssStyleMap["baseline-shift"]?.let{ string ->
            parseCssBaselineShift(string)?.let {
                spanStyleSet.add(
                    SpanStyle(baselineShift = it)
                )
            }
        }
        cssStyleMap["background"]?.let{ string ->
            parseCssColor(string)?.let {
                spanStyleSet.add(
                    SpanStyle(background = it)
                )
            }
        }
            ?: cssStyleMap["background-color"]?.let{ string ->
                parseCssColor(string)?.let {
                    spanStyleSet.add(
                        SpanStyle(background = it)
                    )
                }
            }
        cssStyleMap["text-decoration"]?.let{ string ->
            parseCssTextDecoration(string)?.let {
                spanStyleSet.add(
                    SpanStyle(textDecoration = it)
                )
            }
        }
        cssStyleMap["text-shadow"]?.let{ string ->
            parseCssTextShadow(string)?.let {
                spanStyleSet.add(
                    SpanStyle(shadow = it)
                )
            }
        }

        return spanStyleSet
    }

    /**
     * Parses a CSS color string and returns a [Color] or `null` if the color string could not be parsed.
     *
     * @param cssColor the CSS color string to parse.
     * @return the parsed [Color] or `null` if the color string could not be parsed.
     */
    internal fun parseCssColor(cssColor: String): Color? {
        val rgbRegex = Regex("""rgb\((\d+), (\d+), (\d+)\)""")
        val rgbaRegex = Regex("""rgba\((\d+), (\d+), (\d+), ([\d.]+)\)""")
        val hexRegex = Regex("""#?([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})""")

        // Check for rgb() format
        val rgbMatchResult = rgbRegex.find(cssColor)
        if (rgbMatchResult != null && rgbMatchResult.groupValues.size == 4) {
            val r = rgbMatchResult.groupValues[1].toInt()
            val g = rgbMatchResult.groupValues[2].toInt()
            val b = rgbMatchResult.groupValues[3].toInt()
            return Color(r, g, b)
        }

        // Check for rgba() format
        val rgbaMatchResult = rgbaRegex.find(cssColor)
        if (rgbaMatchResult != null && rgbaMatchResult.groupValues.size == 5) {
            val r = rgbaMatchResult.groupValues[1].toInt()
            val g = rgbaMatchResult.groupValues[2].toInt()
            val b = rgbaMatchResult.groupValues[3].toInt()
            val a = (rgbaMatchResult.groupValues[4].toFloat() * 255).toInt()
            return Color(r, g, b, a)
        }

        // Check for hexadecimal format
        val hexMatchResult = hexRegex.find(cssColor)
        if (hexMatchResult != null && hexMatchResult.groupValues.size == 2) {
            val hexValue = hexMatchResult.groupValues[1]

            // Expand shorthand hex value if necessary
            val expandedHex = if (hexValue.length == 3) {
                "${hexValue[0]}${hexValue[0]}${hexValue[1]}${hexValue[1]}${hexValue[2]}${hexValue[2]}"
            } else {
                hexValue
            }

            // Parse the individual color components
            val r = expandedHex.substring(0, 2).toInt(16)
            val g = expandedHex.substring(2, 4).toInt(16)
            val b = expandedHex.substring(4, 6).toInt(16)
            return Color(r, g, b)
        }

        // Handle named colors
        val namedColor = cssColor.lowercase()

        return cssColorMap[namedColor]
    }

    /**
     * Parses a CSS size string and returns a [Float] or `null` if the size string could not be parsed.
     *
     * @param cssSize the CSS size string to parse.
     * @return the parsed [Float] or `null` if the size string could not be parsed.
     */
    internal fun parseCssSize(cssSize: String): Float? {
        if (cssSize == "0") return 0f
        val sizeRegex = Regex("""([-]?\d+(\.\d+)?)\s*(px|pt|em|rem|%)""")
        val sizeMatchResult = sizeRegex.find(cssSize)

        if (sizeMatchResult != null && sizeMatchResult.groupValues.size == 4) {
            val value = sizeMatchResult.groupValues[1].toFloat()
            val unit = sizeMatchResult.groupValues[3]
            return when (unit) {
                "px" -> value
                "pt" -> value * 1.333f
                "em" -> value * 16
                "rem" -> value * 16
                "%" -> value * 16 / 100f
                else -> null
            }
        }
        return null
    }

    /**
     * Parses a CSS font weight string and returns a [FontWeight] or `null` if the font weight string could not be parsed.
     *
     * @param cssFontWeight the CSS font weight string to parse.
     * @return the parsed [FontWeight] or `null` if the font weight string could not be parsed.
     */
    internal fun parseCssFontWeight(cssFontWeight: String): FontWeight? {
        return when (cssFontWeight.lowercase()) {
            "100", "lighter" -> FontWeight.Thin
            "200" -> FontWeight.ExtraLight
            "300" -> FontWeight.Light
            "400", "normal" -> FontWeight.Normal
            "500", "medium" -> FontWeight.Medium
            "600", "semibold" -> FontWeight.SemiBold
            "700", "bold" -> FontWeight.Bold
            "800", "extrabold" -> FontWeight.ExtraBold
            "900", "black", "bolder" -> FontWeight.Black
            else -> null
        }
    }

    /**
     * Parses a CSS font style string and returns a [FontStyle] or `null` if the font style string could not be parsed.
     *
     * @param cssFontStyle the CSS font style string to parse.
     * @return the parsed [FontStyle] or `null` if the font style string could not be parsed.
     */
    internal fun parseCssFontStyle(cssFontStyle: String): FontStyle? {
        return when (cssFontStyle) {
            "normal" -> FontStyle.Normal
            "italic", "oblique" -> FontStyle.Italic
            else -> null
        }
    }

    /**
     * Parses a CSS text decoration string and returns a [TextDecoration] or `null` if the text decoration string could not be parsed.
     * Multiple text decorations can be combined by separating them with a space.
     * For example: "underline line-through" or "line-through underline".
     *
     * @param cssTextDecoration the CSS text decoration string to parse.
     * @return the parsed [TextDecoration] or `null` if the text decoration string could not be parsed.
     */
    internal fun parseCssTextDecoration(cssTextDecoration: String): TextDecoration? {
        return when (cssTextDecoration) {
            "none" -> null
            "underline" -> TextDecoration.Underline
            "overline" -> null // Not supported in ComposeUI
            "line-through" -> TextDecoration.LineThrough
            "underline line-through", "line-through underline" -> TextDecoration.combine(
                listOf(TextDecoration.Underline, TextDecoration.LineThrough)
            )
            else -> null
        }
    }

    internal fun parseCssBaselineShift(cssBaselineShift: String): BaselineShift? {
        val shiftValue = cssBaselineShift.trim().removeSuffix("%").toFloatOrNull()
        return if (shiftValue != null) {
            BaselineShift(shiftValue / 100f)
        } else {
            when (cssBaselineShift) {
                "sub" -> BaselineShift.Subscript
                "super" -> BaselineShift.Superscript
                "baseline" -> BaselineShift.None
                else -> null
            }
        }
    }

    /**
     * Parses a CSS text shadow string and returns a [Shadow] or `null` if the text shadow string could not be parsed.
     *
     * @param cssTextShadow the CSS text shadow string to parse.
     * @return the parsed [Shadow] or `null` if the text shadow string could not be parsed.
     */
    internal fun parseCssTextShadow(cssTextShadow: String): Shadow? {
        val values = cssTextShadow.split(" ").filter { it.isNotBlank() }
        if (values.isEmpty()) return null

        return when (values.size) {
            4 -> {
                var offset = 1
                var color = parseCssColor(values[0])
                if (color == null) {
                    color = parseCssColor(values[3]) ?: return null
                    offset = 0
                }
                val offsetX = parseCssSize(values[offset]) ?: return null
                val offsetY = parseCssSize(values[offset + 1]) ?: return null
                val blurRadius = parseCssSize(values[offset + 2]) ?: return null

                Shadow(
                    color = color,
                    offset = Offset(offsetX, offsetY),
                    blurRadius = blurRadius
                )
            }
            3 -> {
                var offset = 1
                var color = parseCssColor(values[0])
                if (color == null) {
                    color = parseCssColor(values[2]) ?: return null
                    offset = 0
                }
                val offsetX = parseCssSize(values[offset]) ?: return null
                val offsetY = parseCssSize(values[offset + 1]) ?: return null

                Shadow(
                    color = color,
                    offset = Offset(offsetX, offsetY),
                    blurRadius = 0f
                )
            }
            else -> null
        }
    }

    /**
     * Map of CSS color names to [Color]s.
     *
     * @see <a href="https://www.w3schools.com/colors/colors_names.asp">W3Schools Colors Names</a>
     */
    private val cssColorMap = mapOf(
        "aliceblue" to Color(240, 248, 255),
        "antiquewhite" to Color(250, 235, 215),
        "aqua" to Color(0, 255, 255),
        "aquamarine" to Color(127, 255, 212),
        "azure" to Color(240, 255, 255),
        "beige" to Color(245, 245, 220),
        "bisque" to Color(255, 228, 196),
        "black" to Color(0, 0, 0),
        "blanchedalmond" to Color(255, 235, 205),
        "blue" to Color(0, 0, 255),
        "blueviolet" to Color(138, 43, 226),
        "brown" to Color(165, 42, 42),
        "burlywood" to Color(222, 184, 135),
        "cadetblue" to Color(95, 158, 160),
        "chartreuse" to Color(127, 255, 0),
        "chocolate" to Color(210, 105, 30),
        "coral" to Color(255, 127, 80),
        "cornflowerblue" to Color(100, 149, 237),
        "cornsilk" to Color(255, 248, 220),
        "crimson" to Color(220, 20, 60),
        "cyan" to Color(0, 255, 255),
        "darkblue" to Color(0, 0, 139),
        "darkcyan" to Color(0, 139, 139),
        "darkgoldenrod" to Color(184, 134, 11),
        "darkgray" to Color(169, 169, 169),
        "darkgreen" to Color(0, 100, 0),
        "darkgrey" to Color(169, 169, 169),
        "darkkhaki" to Color(189, 183, 107),
        "darkmagenta" to Color(139, 0, 139),
        "darkolivegreen" to Color(85, 107, 47),
        "darkorange" to Color(255, 140, 0),
        "darkorchid" to Color(153, 50, 204),
        "darkred" to Color(139, 0, 0),
        "darksalmon" to Color(233, 150, 122),
        "darkseagreen" to Color(143, 188, 143),
        "darkslateblue" to Color(72, 61, 139),
        "darkslategray" to Color(47, 79, 79),
        "darkslategrey" to Color(47, 79, 79),
        "darkturquoise" to Color(0, 206, 209),
        "darkviolet" to Color(148, 0, 211),
        "deeppink" to Color(255, 20, 147),
        "deepskyblue" to Color(0, 191, 255),
        "dimgray" to Color(105, 105, 105),
        "dimgrey" to Color(105, 105, 105),
        "dodgerblue" to Color(30, 144, 255),
        "firebrick" to Color(178, 34, 34),
        "floralwhite" to Color(255, 250, 240),
        "forestgreen" to Color(34, 139, 34),
        "fuchsia" to Color(255, 0, 255),
        "gainsboro" to Color(220, 220, 220),
        "ghostwhite" to Color(248, 248, 255),
        "gold" to Color(255, 215, 0),
        "goldenrod" to Color(218, 165, 32),
        "gray" to Color(128, 128, 128),
        "green" to Color(0, 128, 0),
        "greenyellow" to Color(173, 255, 47),
        "grey" to Color(128, 128, 128),
        "honeydew" to Color(240, 255, 240),
        "hotpink" to Color(255, 105, 180),
        "indianred" to Color(205, 92, 92),
        "indigo" to Color(75, 0, 130),
        "ivory" to Color(255, 255, 240),
        "khaki" to Color(240, 230, 140),
        "lavender" to Color(230, 230, 250),
        "lavenderblush" to Color(255, 240, 245),
        "lawngreen" to Color(124, 252, 0),
        "lemonchiffon" to Color(255, 250, 205),
        "lightblue" to Color(173, 216, 230),
        "lightcoral" to Color(240, 128, 128),
        "lightcyan" to Color(224, 255, 255),
        "lightgoldenrodyellow" to Color(250, 250, 210),
        "lightgray" to Color(211, 211, 211),
        "lightgreen" to Color(144, 238, 144),
        "lightgrey" to Color(211, 211, 211),
        "lightpink" to Color(255, 182, 193),
        "lightsalmon" to Color(255, 160, 122),
        "lightseagreen" to Color(32, 178, 170),
        "lightskyblue" to Color(135, 206, 250),
        "lightslategray" to Color(119, 136, 153),
        "lightslategrey" to Color(119, 136, 153),
        "lightsteelblue" to Color(176, 196, 222),
        "lightyellow" to Color(255, 255, 224),
        "lime" to Color(0, 255, 0),
        "limegreen" to Color(50, 205, 50),
        "linen" to Color(250, 240, 230),
        "magenta" to Color(255, 0, 255),
        "maroon" to Color(128, 0, 0),
        "mediumaquamarine" to Color(102, 205, 170),
        "mediumblue" to Color(0, 0, 205),
        "mediumorchid" to Color(186, 85, 211),
        "mediumpurple" to Color(147, 112, 219),
        "mediumseagreen" to Color(60, 179, 113),
        "mediumslateblue" to Color(123, 104, 238),
        "mediumspringgreen" to Color(0, 250, 154),
        "mediumturquoise" to Color(72, 209, 204),
        "mediumvioletred" to Color(199, 21, 133),
        "midnightblue" to Color(25, 25, 112),
        "mintcream" to Color(245, 255, 250),
        "mistyrose" to Color(255, 228, 225),
        "moccasin" to Color(255, 228, 181),
        "navajowhite" to Color(255, 222, 173),
        "navy" to Color(0, 0, 128),
        "oldlace" to Color(253, 245, 230),
        "olive" to Color(128, 128, 0),
        "olivedrab" to Color(107, 142, 35),
        "orange" to Color(255, 165, 0),
        "orangered" to Color(255, 69, 0),
        "orchid" to Color(218, 112, 214),
        "palegoldenrod" to Color(238, 232, 170),
        "palegreen" to Color(152, 251, 152),
        "paleturquoise" to Color(175, 238, 238),
        "palevioletred" to Color(219, 112, 147),
        "papayawhip" to Color(255, 239, 213),
        "peachpuff" to Color(255, 218, 185),
        "peru" to Color(205, 133, 63),
        "pink" to Color(255, 192, 203),
        "plum" to Color(221, 160, 221),
        "powderblue" to Color(176, 224, 230),
        "purple" to Color(128, 0, 128),
        "rebeccapurple" to Color(102, 51, 153),
        "red" to Color(255, 0, 0),
        "rosybrown" to Color(188, 143, 143),
        "royalblue" to Color(65, 105, 225),
        "saddlebrown" to Color(139, 69, 19),
        "salmon" to Color(250, 128, 114),
        "sandybrown" to Color(244, 164, 96),
        "seagreen" to Color(46, 139, 87),
        "seashell" to Color(255, 245, 238),
        "sienna" to Color(160, 82, 45),
        "silver" to Color(192, 192, 192),
        "skyblue" to Color(135, 206, 235),
        "slateblue" to Color(106, 90, 205),
        "slategray" to Color(112, 128, 144),
        "slategrey" to Color(112, 128, 144),
        "snow" to Color(255, 250, 250),
        "springgreen" to Color(0, 255, 127),
        "steelblue" to Color(70, 130, 180),
        "tan" to Color(210, 180, 140),
        "teal" to Color(0, 128, 128),
        "thistle" to Color(216, 191, 216),
        "tomato" to Color(255, 99, 71),
        "turquoise" to Color(64, 224, 208),
        "violet" to Color(238, 130, 238),
        "wheat" to Color(245, 222, 179),
        "white" to Color(255, 255, 255),
        "whitesmoke" to Color(245, 245, 245),
        "yellow" to Color(255, 255, 0),
        "yellowgreen" to Color(154, 205, 50)
    )

}