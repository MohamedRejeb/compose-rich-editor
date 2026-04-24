package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.DefaultListIndent
import com.mohamedrejeb.richeditor.model.DefaultOrderedListStyleType
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextConfig
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

@OptIn(ExperimentalRichTextApi::class)
internal class OrderedList private constructor(
    number: Int,
    initialIndent: Int = DefaultListIndent,
    startTextWidth: TextUnit = 0.sp,
    initialLevel: Int = 1,
    initialStyleType: OrderedListStyleType = DefaultOrderedListStyleType,
    initialPrefixAlignment: ListPrefixAlignment = ListPrefixAlignment.End,
    /**
     * The start number for the first item in this list group.
     * Defaults to 1. When > 1, the HTML output includes `start="N"` on the `<ol>` tag.
     */
    val startFrom: Int = 1,
) : ParagraphType, ConfigurableStartTextWidth, ConfigurableListLevel {

    constructor(
        number: Int,
        initialLevel: Int = 1,
        startFrom: Int = 1,
    ) : this(
        number = number,
        initialIndent = DefaultListIndent,
        initialLevel = initialLevel,
        startFrom = startFrom,
    )

    constructor(
        number: Int,
        config: RichTextConfig,
        startTextWidth: TextUnit = 0.sp,
        initialLevel: Int = 1,
        startFrom: Int = 1,
    ) : this(
        number = number,
        initialIndent = config.orderedListIndent,
        startTextWidth = startTextWidth,
        initialLevel = initialLevel,
        initialStyleType = config.orderedListStyleType,
        initialPrefixAlignment = config.listPrefixAlignment,
        startFrom = startFrom,
    )

    var number = number
        set(value) {
            field = value
            startRichSpan = getNewStartRichSpan(startRichSpan.textRange)
        }

    override var startTextWidth: TextUnit = startTextWidth
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    private var indent = initialIndent
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    override var level = initialLevel
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    private var styleType = initialStyleType
        set(value) {
            field = value
            startRichSpan = getNewStartRichSpan(startRichSpan.textRange)
        }

    private var prefixAlignment = initialPrefixAlignment
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    private var style: ParagraphStyle =
        getNewParagraphStyle()

    override fun getStyle(config: RichTextConfig): ParagraphStyle {
        if (config.orderedListIndent != indent) {
            indent = config.orderedListIndent
        }

        if (config.orderedListStyleType != styleType) {
            styleType = config.orderedListStyleType
        }

        if (config.listPrefixAlignment != prefixAlignment) {
            prefixAlignment = config.listPrefixAlignment
        }

        return style
    }

    private fun getNewParagraphStyle(): ParagraphStyle {
        val base = (indent * level).toFloat()
        val prefix = startTextWidth.value
        // End: HTML-style alignment — prefix lives in the indent "gutter" and dots align
        // vertically, but fall back to Start when the indent is too small so the prefix
        // doesn't get clipped off the left edge.
        // Start: every item's prefix starts at the indent origin, giving a uniform left edge.
        val useEnd = prefixAlignment == ListPrefixAlignment.End && base >= prefix
        val textIndent =
            if (useEnd)
                TextIndent(firstLine = (base - prefix).sp, restLine = base.sp)
            else
                TextIndent(firstLine = base.sp, restLine = (base + prefix).sp)

        return ParagraphStyle(textIndent = textIndent)
    }

    override var startRichSpan: RichSpan =
        getNewStartRichSpan()

    @OptIn(ExperimentalRichTextApi::class)
    private fun getNewStartRichSpan(textRange: TextRange = TextRange(0)): RichSpan {
        val text = styleType.format(number, level) + styleType.getSuffix(level)

        return RichSpan(
            paragraph = RichParagraph(type = this),
            text = text,
            textRange = TextRange(
                textRange.min,
                textRange.min + text.length
            )
        )
    }

    override fun getNextParagraphType(): ParagraphType =
        OrderedList(
            number = number + 1,
            initialIndent = indent,
            startTextWidth = startTextWidth,
            initialLevel = level,
            initialStyleType = styleType,
            initialPrefixAlignment = prefixAlignment,
        )

    override fun copy(): ParagraphType =
        OrderedList(
            number = number,
            initialIndent = indent,
            startTextWidth = startTextWidth,
            initialLevel = level,
            initialStyleType = styleType,
            initialPrefixAlignment = prefixAlignment,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrderedList) return false

        if (number != other.number) return false
        if (indent != other.indent) return false
        if (startTextWidth != other.startTextWidth) return false
        if (level != other.level) return false
        if (styleType != other.styleType) return false
        if (prefixAlignment != other.prefixAlignment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indent
        result = 31 * result + number
        result = 31 * result + indent
        result = 31 * result + startTextWidth.hashCode()
        result = 31 * result + level
        result = 31 * result + styleType.hashCode()
        result = 31 * result + prefixAlignment.hashCode()
        return result
    }
}
