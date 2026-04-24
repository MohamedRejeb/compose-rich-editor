package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.DefaultListIndent
import com.mohamedrejeb.richeditor.model.DefaultUnorderedListStyleType
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextConfig
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

@OptIn(ExperimentalRichTextApi::class)
internal class UnorderedList private constructor(
    initialIndent: Int = DefaultListIndent,
    startTextWidth: TextUnit = 0.sp,
    initialLevel: Int = 1,
    initialStyleType: UnorderedListStyleType = DefaultUnorderedListStyleType,
    initialPrefixAlignment: ListPrefixAlignment = ListPrefixAlignment.End,
): ParagraphType, ConfigurableStartTextWidth, ConfigurableListLevel {

    constructor(
        initialLevel: Int = 1,
    ): this(
        initialIndent = DefaultListIndent,
        initialLevel = initialLevel,
    )

    constructor(
        config: RichTextConfig,
        initialLevel: Int = 1,
    ): this(
        initialIndent = config.unorderedListIndent,
        initialLevel = initialLevel,
        initialStyleType = config.unorderedListStyleType,
        initialPrefixAlignment = config.listPrefixAlignment,
    )

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
            startRichSpan = getNewStartRichSpan()
        }

    private var styleType = initialStyleType
        set(value) {
            field = value
            startRichSpan = getNewStartRichSpan()
        }

    private var prefixAlignment = initialPrefixAlignment
        set(value) {
            field = value
            style = getNewParagraphStyle()
        }

    private var style: ParagraphStyle =
        getNewParagraphStyle()

    override fun getStyle(config: RichTextConfig): ParagraphStyle {
        if (config.unorderedListIndent != indent) {
            indent = config.unorderedListIndent
        }

        if (config.unorderedListStyleType != styleType) {
            styleType = config.unorderedListStyleType
        }

        if (config.listPrefixAlignment != prefixAlignment) {
            prefixAlignment = config.listPrefixAlignment
        }

        return style
    }

    private fun getNewParagraphStyle(): ParagraphStyle {
        val base = (indent * level).toFloat()
        val prefix = startTextWidth.value
        // End: HTML-style alignment — prefix lives in the indent "gutter". Clamp firstLine
        // at 0 so the prefix stays visible when the indent is smaller than the prefix;
        // clamping (instead of switching formulas) keeps the layout consistent across items.
        // Start: every item's prefix starts at the indent origin.
        val textIndent =
            if (prefixAlignment == ListPrefixAlignment.End)
                TextIndent(firstLine = (base - prefix).coerceAtLeast(0f).sp, restLine = base.sp)
            else
                TextIndent(firstLine = base.sp, restLine = (base + prefix).sp)

        return ParagraphStyle(textIndent = textIndent)
    }

    @OptIn(ExperimentalRichTextApi::class)
    override var startRichSpan: RichSpan =
        getNewStartRichSpan()

    @OptIn(ExperimentalRichTextApi::class)
    private fun getNewStartRichSpan(textRange: TextRange = TextRange(0)): RichSpan {
        val prefixIndex =
            (level - 1).coerceIn(styleType.prefixes.indices)

        val prefix = styleType.prefixes
            .getOrNull(prefixIndex)
            ?: "•"

        val text = "$prefix "

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
        UnorderedList(
            initialIndent = indent,
            startTextWidth = startTextWidth,
            initialLevel = level,
            initialStyleType = styleType,
            initialPrefixAlignment = prefixAlignment,
        )

    override fun copy(): ParagraphType =
        UnorderedList(
            initialIndent = indent,
            startTextWidth = startTextWidth,
            initialLevel = level,
            initialStyleType = styleType,
            initialPrefixAlignment = prefixAlignment,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnorderedList) return false

        if (indent != other.indent) return false
        if (startTextWidth != other.startTextWidth) return false
        if (level != other.level) return false
        if (styleType != other.styleType) return false
        if (prefixAlignment != other.prefixAlignment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = indent
        result = 31 * result + startTextWidth.hashCode()
        result = 31 * result + level
        result = 31 * result + styleType.hashCode()
        result = 31 * result + prefixAlignment.hashCode()
        return result
    }
}
