package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.type.ListMarkerStyleBehavior
import com.mohamedrejeb.richeditor.paragraph.type.ListPrefixAlignment
import com.mohamedrejeb.richeditor.paragraph.type.OrderedListStyleType
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedListStyleType

public class RichTextConfig internal constructor(
    private val updateText: () -> Unit,
) {
    public var linkColor: Color = Color.Blue
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    public var linkTextDecoration: TextDecoration = TextDecoration.Underline
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    public var codeSpanColor: Color = Color.Unspecified
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    public var codeSpanBackgroundColor: Color = Color.Transparent
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    public var codeSpanStrokeColor: Color = Color.LightGray
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    public var codeSpanCornerRadius: TextUnit = 8.sp
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    public var codeSpanStrokeWidth: TextUnit = 1.sp
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    public var codeSpanPadding: TextPaddingValues = TextPaddingValues(horizontal = 2.sp, vertical = 2.sp)
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    /**
     * The indent for ordered lists.
     */
    public var orderedListIndent: Int = DefaultListIndent
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    /**
     * The indent for unordered lists.
     */
    public var unorderedListIndent: Int = DefaultListIndent
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    /**
     * The indent for both ordered and unordered lists.
     *
     * This property is a shortcut for setting both [orderedListIndent] and [unorderedListIndent].
     * Reading it returns the shared indent while the two agree, else the last value assigned here.
     */
    public var listIndent: Int = DefaultListIndent
        get() =
            if (orderedListIndent == unorderedListIndent) orderedListIndent
            else field
        set(value) {
            field = value
            orderedListIndent = value
            unorderedListIndent = value
        }

    /**
     * The prefixes for unordered lists items.
     *
     * The prefixes are used in order if the list is nested.
     *
     * For example, if the list is nested twice, the first prefix is used for the first level,
     * the second prefix is used for the second level, and so on.
     *
     * If the list is nested more than the number of prefixes, the last prefix is used.
     *
     * The default prefixes are `•`, `◦`, and `▪`.
     */
    public var unorderedListStyleType: UnorderedListStyleType = DefaultUnorderedListStyleType
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    public var orderedListStyleType: OrderedListStyleType = DefaultOrderedListStyleType
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    /**
     * Controls how list markers ("•", "1.", etc.) inherit span styles from the
     * list item's text.
     *
     * Default is [ListMarkerStyleBehavior.InheritFromText], which keeps bold /
     * italic / color / font size on the marker but drops underline, strikethrough,
     * background, baseline shift, shadow, and geometric transforms. Matches
     * Google Docs.
     *
     * Set to [ListMarkerStyleBehavior.AlwaysDefault] to render every marker with
     * the default span style regardless of the item's content.
     */
    @ExperimentalRichTextApi
    public var listMarkerStyleBehavior: ListMarkerStyleBehavior = ListMarkerStyleBehavior.InheritFromText
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    /**
     * A style for list markers ("•", "1.", etc.) on their own, applied on top of what
     * [listMarkerStyleBehavior] gives them. Whatever it leaves unset keeps coming from that
     * behavior, so `SpanStyle(color = Color.Red)` colors the markers and keeps their size
     * in step with the text.
     *
     * It is an appearance setting of the editor, like [linkColor]: it applies to every list
     * and is not part of the document, so it is not written to HTML or Markdown.
     *
     * Default is an empty [SpanStyle], which changes nothing.
     */
    @ExperimentalRichTextApi
    public var listMarkerStyle: SpanStyle = SpanStyle()
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    /**
     * The typography of headings, per level. A level's [TextStyle] is applied on top of the
     * default typography of that level (see [HeadingStyle]), so whatever it leaves unset keeps
     * coming from the default: `TextStyle(color = Color.Red)` colors the headings of a level
     * and keeps their size and weight. Its paragraph part (line height, alignment, ...) applies
     * to the heading paragraph. Levels without an entry, and [HeadingStyle.Normal], are left
     * alone.
     *
     * It is an appearance setting of the editor, like [linkColor]: the document stores the
     * heading level only, so the styles are not written to HTML or Markdown, and a style added
     * to the heading's text or paragraph still wins over them.
     *
     * Default is an empty map, which changes nothing.
     */
    @ExperimentalRichTextApi
    public var headingTextStyles: Map<HeadingStyle, TextStyle> = emptyMap()
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    /**
     * Controls where list markers ("1.", "10.", "•", ...) sit relative to the
     * indent gutter in ordered and unordered lists.
     *
     * Default is [ListPrefixAlignment.End], which matches HTML: the marker sits
     * inside the gutter and ends at the content start, so "1." and "10." have
     * their dots aligned vertically.
     *
     * Set to [ListPrefixAlignment.Start] to make every item's marker start at
     * the same left edge instead.
     */
    @ExperimentalRichTextApi
    public var listPrefixAlignment: ListPrefixAlignment = ListPrefixAlignment.End
        set(value) {
            if (field == value) return
            field = value
            updateText()
        }

    /**
     * Whether to preserve the style when the line is empty.
     * The line can be empty when the user deletes all the characters
     * or when the user presses `enter` to create a new line.
     *
     * Default is `true`.
     */
    public var preserveStyleOnEmptyLine: Boolean = true

    /**
     * Whether to exit the list when pressing Enter on an empty list item.
     * When true, pressing Enter on an empty list item will convert it to a normal paragraph.
     * When false, pressing Enter on an empty list item will create a new list item.
     *
     * Default is `true`.
     */
    public var exitListOnEmptyItem: Boolean = true

    /**
     * Whether typing a list marker at the start of a paragraph turns it into a list:
     * `- ` or `* ` for an unordered list, a number followed by `. ` for an ordered list.
     *
     * When false the typed characters stay as text. Lists themselves are unaffected: the list
     * functions on [RichTextState] and loaded or pasted lists keep working. To remove lists
     * altogether, leave them out of [features] instead.
     *
     * Default is `true`.
     */
    public var listTypingShortcutsEnabled: Boolean = true

    /**
     * Whether a typed or pasted URL becomes a link on its own.
     *
     * When true, a word that starts with `http://`, `https://` or `www.` turns into a link once
     * a space or Enter is typed after it, and a paste whose whole text is such a URL is inserted
     * as a link. Punctuation that closes the sentence after the URL stays outside the link.
     * Text that is already a link, a code span or a token is left as it is. The link is its own
     * undo step: one undo removes it and keeps the text.
     *
     * Nothing is linked while links are left out of [features].
     *
     * Default is `true`.
     */
    @ExperimentalRichTextApi
    public var autoLinkEnabled: Boolean = true

    /**
     * Whether typing a heading marker at the start of a paragraph turns it into a heading:
     * `# ` to `###### ` set levels 1 to 6 and the marker is removed. A marker typed in a
     * list item stays as text.
     *
     * The conversion is its own undo step: one undo restores the typed marker. Nothing is
     * converted while headings are left out of [features].
     *
     * Default is `false`.
     */
    @ExperimentalRichTextApi
    public var headingTypingShortcutsEnabled: Boolean = false

    /**
     * Whether Markdown marks typed around text format it: `**bold**` or `__bold__`,
     * `*italic*` or `_italic_`, `` `code` `` and `~~strikethrough~~`. The text is formatted
     * and the marks are removed when the closing mark is typed, so `**bold*` is still text.
     * The marks must sit in one paragraph around text that does not start or end with a
     * space, an underscore mark must not touch a letter or digit outside the pair, and
     * nothing is converted inside a code span, a link or a token.
     *
     * The conversion is its own undo step: one undo restores the typed marks. What is typed
     * after the formatted text is not formatted. A mark whose feature is left out of
     * [features] stays as text.
     *
     * Default is `false`.
     */
    @ExperimentalRichTextApi
    public var inlineTypingShortcutsEnabled: Boolean = false

    /**
     * Whether the editor keeps its selection when it loses focus.
     *
     * The Compose text field collapses the selection as soon as focus moves to another
     * focusable, so a focusable toolbar button finds nothing to style. When true the editor
     * keeps the range, and its highlight, while it is unfocused.
     *
     * Default is `false`.
     */
    @ExperimentalRichTextApi
    public var preserveSelectionOnFocusLoss: Boolean = false

    /**
     * Whether copy and paste carry rich text (HTML) formatting.
     *
     * When false, paste inserts plain text only, styled by the editor's normal insertion
     * logic (inheriting from the caret context like typed text), and copy writes plain
     * text only. The web clipboard event handlers follow the same rule.
     *
     * Default is `true`.
     */
    @ExperimentalRichTextApi
    public var richClipboardEnabled: Boolean = true

    /**
     * The kinds of formatting this editor supports. Defaults to [RichTextFeature.All].
     *
     * Content entering the editor through [RichTextState.setHtml], [RichTextState.setMarkdown],
     * [RichTextState.setRichTextDocument], the HTML and Markdown inserts, or a clipboard paste
     * keeps only these features: a disallowed mark is removed and its text kept, a disallowed
     * list item or heading becomes a plain paragraph, and a disallowed image is removed. An
     * empty set makes every paste a plain text paste. Changing the set affects content that
     * enters afterwards; existing content is not re-filtered.
     */
    @ExperimentalRichTextApi
    public var features: Set<RichTextFeature> = RichTextFeature.All
}

/** Whether a paste may carry formatting: rich clipboard on and at least one feature allowed. */
@OptIn(ExperimentalRichTextApi::class)
internal val RichTextConfig.richPasteEnabled: Boolean
    get() = richClipboardEnabled && features.isNotEmpty()

/** The [RichTextConfig.headingTextStyles] entry that restyles [headingStyle], if any. */
@OptIn(ExperimentalRichTextApi::class)
internal fun RichTextConfig.headingTextStyleFor(headingStyle: HeadingStyle): TextStyle? =
    if (headingStyle == HeadingStyle.Normal) null else headingTextStyles[headingStyle]

/**
 * The span style a heading restyled by [RichTextConfig.headingTextStyles] renders with: the
 * level's default typography with the configured style on top. Null when the level is not
 * restyled.
 */
internal fun RichTextConfig.headingSpanStyleFor(headingStyle: HeadingStyle): SpanStyle? =
    headingTextStyleFor(headingStyle)?.let { headingStyle.defaultSpanStyle.merge(it.toSpanStyle()) }

internal const val DefaultListIndent = 38

internal val DefaultUnorderedListStyleType =
    UnorderedListStyleType.from("•", "◦", "▪")

internal val DefaultOrderedListStyleType: OrderedListStyleType =
    OrderedListStyleType.Multiple(
        OrderedListStyleType.Decimal,
        OrderedListStyleType.LowerRoman,
        OrderedListStyleType.LowerAlpha,
    )
