package com.mohamedrejeb.richeditor.parser.utils

import androidx.compose.material3.Typography
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle

public enum class HeadingParagraphStyle(
    public val displayName: String,
    //val iconResId: Int,
) {

    NORMAL(
        "Normal",
        //R.drawable.format_clear_24px, //format_clear_24px.xml,
    ),
    H1(
        "H1",
        //R.drawable.format_h1_24px, //format_h1_24px.xml
    ),
    H2(
        "H2",
        //R.drawable.format_h2_24px, //format_h2_24px.xml
    ),
    H3(
        "H3",
        //R.drawable.format_h3_24px, //format_h3_24px.xml
    ),
    H4(
        "H4",
        //R.drawable.format_h4_24px, //format_h4_24px.xml
    ),
    H5(
        "H5",
        //R.drawable.format_h5_24px, //format_h5_24px.xml
    ),
    H6(
        "H6",
        //R.drawable.format_h6_24px, //format_h6_24px.xml
    );

    private val typography = Typography()

    public fun getSpanStyle(): SpanStyle {
        return this.getTextStyle().toSpanStyle()
    }

    public fun getTextStyle() : TextStyle {
        return when (this) {
            NORMAL -> TextStyle.Default
            H1 -> Typography().displayLarge
            H2 -> Typography().displayMedium
            H3 -> Typography().displaySmall
            H4 -> Typography().headlineMedium
            H5 -> Typography().headlineSmall
            H6 -> Typography().titleLarge
        }
    }

    public companion object {
        public fun fromSpanStyle(spanStyle: SpanStyle): HeadingParagraphStyle {
            return entries.find {
                val entrySpanStyle = it.getSpanStyle()
                entrySpanStyle.fontSize == spanStyle.fontSize
                        && entrySpanStyle.fontWeight == spanStyle.fontWeight
                        && entrySpanStyle.fontFamily == spanStyle.fontFamily
                        && entrySpanStyle.letterSpacing == spanStyle.letterSpacing
            } ?: NORMAL
        }
    }
}