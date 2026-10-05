package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.DefaultListIndent
import com.mohamedrejeb.richeditor.model.RichSpan
import com.mohamedrejeb.richeditor.model.RichTextConfig
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

/**
 * A list item with a checked state. Its marker is a ballot box character, so it is part of
 * the text like the bullet of an [UnorderedList], and it is indented like one.
 */
@OptIn(ExperimentalRichTextApi::class)
internal class TaskList private constructor(
    initialChecked: Boolean,
    initialIndent: Int,
    startTextWidth: TextUnit = 0.sp,
    initialLevel: Int = 1,
    initialPrefixAlignment: ListPrefixAlignment = ListPrefixAlignment.End,
): ParagraphType, ConfigurableStartTextWidth, ConfigurableListLevel {

    constructor(
        checked: Boolean = false,
        initialLevel: Int = 1,
    ): this(
        initialChecked = checked,
        initialIndent = DefaultListIndent,
        initialLevel = initialLevel,
    )

    constructor(
        config: RichTextConfig,
        checked: Boolean = false,
        initialLevel: Int = 1,
    ): this(
        initialChecked = checked,
        initialIndent = config.unorderedListIndent,
        initialLevel = initialLevel,
        initialPrefixAlignment = config.listPrefixAlignment,
    )

    val checked: Boolean = initialChecked

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

        if (config.listPrefixAlignment != prefixAlignment) {
            prefixAlignment = config.listPrefixAlignment
        }

        return style
    }

    private fun getNewParagraphStyle(): ParagraphStyle {
        val base = (indent * level).toFloat()
        val prefix = startTextWidth.value
        // Same geometry as UnorderedList: End keeps the marker in the indent gutter,
        // Start puts it at the indent origin.
        val textIndent =
            if (prefixAlignment == ListPrefixAlignment.End)
                TextIndent(firstLine = (base - prefix).coerceAtLeast(0f).sp, restLine = base.sp)
            else
                TextIndent(firstLine = base.sp, restLine = (base + prefix).sp)

        return ParagraphStyle(textIndent = textIndent)
    }

    override var startRichSpan: RichSpan =
        (if (checked) CheckedMarker else UncheckedMarker).let { text ->
            RichSpan(
                paragraph = RichParagraph(type = this),
                text = text,
                textRange = TextRange(0, text.length),
            )
        }

    /** This item with the given checked state. The measured marker width is not carried over. */
    fun withChecked(checked: Boolean): TaskList =
        TaskList(
            initialChecked = checked,
            initialIndent = indent,
            initialLevel = level,
            initialPrefixAlignment = prefixAlignment,
        )

    override fun getNextParagraphType(): ParagraphType =
        TaskList(
            initialChecked = false,
            initialIndent = indent,
            startTextWidth = if (checked) 0.sp else startTextWidth,
            initialLevel = level,
            initialPrefixAlignment = prefixAlignment,
        )

    override fun copy(): ParagraphType =
        TaskList(
            initialChecked = checked,
            initialIndent = indent,
            startTextWidth = startTextWidth,
            initialLevel = level,
            initialPrefixAlignment = prefixAlignment,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TaskList) return false

        if (checked != other.checked) return false
        if (indent != other.indent) return false
        if (startTextWidth != other.startTextWidth) return false
        if (level != other.level) return false
        if (prefixAlignment != other.prefixAlignment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checked.hashCode()
        result = 31 * result + indent
        result = 31 * result + startTextWidth.hashCode()
        result = 31 * result + level
        result = 31 * result + prefixAlignment.hashCode()
        return result
    }

    companion object {
        // The two markers have the same length, so checking an item never shifts the text.
        const val UncheckedMarker = "☐ "
        const val CheckedMarker = "☑ "
    }
}
