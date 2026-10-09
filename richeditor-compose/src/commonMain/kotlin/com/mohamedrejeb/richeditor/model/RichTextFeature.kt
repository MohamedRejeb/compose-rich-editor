package com.mohamedrejeb.richeditor.model

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/**
 * A kind of formatting the editor can hold. [RichTextConfig.features] lists the kinds an
 * editor supports; everything else is stripped from content entering it and ignored by the
 * formatting mutators. Each entry corresponds to one
 * [com.mohamedrejeb.richeditor.document.RichTextSpanMark] or one block attribute of a
 * [com.mohamedrejeb.richeditor.document.RichTextBlock].
 */
@ExperimentalRichTextApi
public enum class RichTextFeature {
    /** Font weight 700, `<b>` and `<strong>`. */
    Bold,

    /** Italic font style. */
    Italic,

    /** Underline text decoration. */
    Underline,

    /** Line-through text decoration. */
    Strikethrough,

    /** Text color. */
    TextColor,

    /** Background color behind the text. */
    Highlight,

    /** An explicit font size. */
    FontSize,

    /** Any explicit font weight other than 700, which is [Bold]. */
    FontWeight,

    /** Letter spacing. */
    LetterSpacing,

    /** Subscript and superscript. */
    BaselineShift,

    /** Text shadow. */
    Shadow,

    /** Links, [RichSpanStyle.Link]. */
    Link,

    /** Inline code, [RichSpanStyle.Code]. */
    CodeSpan,

    /** Inline images, [RichSpanStyle.Image]. The only feature whose content has no text form. */
    Image,

    /** Mention-like tokens, [RichSpanStyle.Token]. */
    Token,

    /** Every app-defined [RichSpanStyle]. */
    CustomSpanStyle,

    /** Ordered list items. */
    OrderedList,

    /** Unordered list items. */
    UnorderedList,

    /** Heading levels 1 to 6. */
    Heading,

    /** Multi-line code blocks. */
    CodeBlock,

    /** Paragraph alignment, direction, first-line indent and line height. */
    ParagraphStyle;

    public companion object {
        /** Every feature, the default of [RichTextConfig.features]. */
        public val All: Set<RichTextFeature> = entries.toSet()
    }
}
