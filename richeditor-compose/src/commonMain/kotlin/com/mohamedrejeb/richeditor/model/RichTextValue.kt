package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.mohamedrejeb.richeditor.utils.RichTextValueBuilder
import kotlin.math.max
import kotlin.math.min

/**
 * A value that represents the text of a RichTextEditor
 * @param textFieldValue the [TextFieldValue] of the text field
 * @param currentStyles the current styles applied to the text
 * @param parts the parts of the text that have different styles
 * @see RichTextStyle
 * @see RichTextPart
 */
@Immutable
public data class RichTextValue internal constructor(
    internal val textFieldValue: TextFieldValue,
    val currentStyles: Set<RichTextStyle> = emptySet(),
    internal val parts: List<RichTextPart> = emptyList(),
) {

    /**
     * The [VisualTransformation] to apply to the text field
     */
    internal val visualTransformation
        get() = VisualTransformation {
            TransformedText(
                text = annotatedString,
                offsetMapping = OffsetMapping.Identity
            )
        }

    /**
     * The [AnnotatedString] representation of the text
     */
    internal val annotatedString
        get() = AnnotatedString(
            text = textFieldValue.text,
            spanStyles = parts.map { part ->
                val spanStyle = part.styles.fold(SpanStyle()) { acc, style ->
                    style.applyStyle(acc)
                }

                AnnotatedString.Range(
                    item = spanStyle,
                    start = part.fromIndex,
                    end = part.toIndex + 1,
                )
            }
        )

    constructor(
        text: String = "",
        currentStyles: Set<RichTextStyle> = emptySet(),
    ) : this(
        textFieldValue = TextFieldValue(text = text),
        currentStyles = currentStyles,
        parts = emptyList(),
    )

    /**
     * Add a style to the current styles
     * @param style the style to add
     * @return a new [RichTextValue] with the new style added
     * @see RichTextStyle
     */
    fun addStyle(vararg style: RichTextStyle): RichTextValue {
       val mStyles = currentStyles.toMutableSet()
        mStyles.addAll(style.toSet())
        return copy(currentStyles = mStyles)
    }

    /**
     * Remove a style from the current styles
     * @param style the style to remove
     * @return a new [RichTextValue] with the style removed
     * @see RichTextStyle
     */
    fun removeStyle(vararg style: RichTextStyle): RichTextValue {
        val mStyles = currentStyles.toMutableSet()
        mStyles.removeAll(style.toSet())
        return copy(currentStyles = mStyles)
    }

    /**
     * Toggle a style
     * @param style the style to toggle
     * @return a new [RichTextValue] with the style toggled
     * @see RichTextStyle
     */
    fun toggleStyle(style: RichTextStyle): RichTextValue {
        return if (currentStyles.contains(style)) {
            removeStyle(style)
        } else {
            addStyle(style)
        }
    }

    /**
     * Update the current styles
     * @param newStyles the new styles
     * @return a new [RichTextValue] with the new styles
     * @see RichTextStyle
     */
    fun updateStyles(newStyles: Set<RichTextStyle>): RichTextValue {
        return copy(currentStyles = newStyles)
    }

    /**
     * Update the text field value and update the rich text parts accordingly to the new text field value
     * @param newTextFieldValue the new text field value
     * @return a new [RichTextValue] with the new text field value
     */
    internal fun updateTextFieldValue(newTextFieldValue: TextFieldValue): RichTextValue {
        return RichTextValueBuilder
            .from(this)
            .updateTextFieldValue(newTextFieldValue)
            .build()
    }

    /**
     * Moves parts in [mParts] from [fromIndex] to [toIndex] by [by].
     * @param mParts The list of parts to move.
     * @param fromIndex The index to start moving from.
     * @param toIndex The index to move to.
     * @param by The amount to move by.
     */
    private fun moveParts(
        mParts: MutableList<RichTextPart>,
        fromIndex: Int,
        toIndex: Int,
        by: Int
    ) {
        val start = max(fromIndex, 0)
        val end = min(toIndex, mParts.lastIndex)
        (start..end).forEach { index ->
            mParts[index] = mParts[index].copy(
                fromIndex = mParts[index].fromIndex + by,
                toIndex = mParts[index].toIndex + by,
            )
        }
    }

    /**
     * Collapses parts in [mParts] to avoid overlapping parts.
     * @param mParts The list of parts to collapse.
     * @param textLength The length of the text.
     */
    private fun collapseParts(
        mParts: MutableList<RichTextPart>,
        textLength: Int
    ) {
        val startRangeMap = mutableMapOf<Int, Int>()
        val endRangeMap = mutableMapOf<Int, Int>()
        val removedIndexes = mutableSetOf<Int>()

        mParts.forEachIndexed { index, part ->
            startRangeMap[part.fromIndex] = index
            endRangeMap[part.toIndex] = index
        }

        mParts.forEachIndexed { index, part ->
            if (removedIndexes.contains(index)) {
                return@forEachIndexed
            }

            val start = part.fromIndex
            val end = part.toIndex

            if (end < start) {
                removedIndexes.add(index)
                return@forEachIndexed
            }

            if (startRangeMap.containsKey(end + 1)) {
                val otherRangeIndex = requireNotNull(startRangeMap[end + 1])
                if (mParts[otherRangeIndex].styles == part.styles) {
                    mParts[index] = part.copy(
                        toIndex = mParts[otherRangeIndex].toIndex
                    )

                    // Remove collapsed values
                    startRangeMap.remove(end + 1)
                    endRangeMap.remove(end)
                    removedIndexes.add(otherRangeIndex)
                }
            }

            if (endRangeMap.containsKey(start - 1)) {
                val otherRangeIndex = requireNotNull(endRangeMap[start - 1])
                if (mParts[otherRangeIndex].styles == part.styles) {
                    mParts[index] = part.copy(
                        fromIndex = mParts[otherRangeIndex].fromIndex
                    )

                    // Remove collapsed values
                    startRangeMap.remove(start - 1)
                    endRangeMap.remove(start - 1)
                    removedIndexes.add(otherRangeIndex)
                }
            }

            mParts[index] = mParts[index].copy(
                fromIndex = max(0, mParts[index].fromIndex),
                toIndex = min(textLength - 1, mParts[index].toIndex),
            )
        }

        removedIndexes.reversed().forEach { mParts.removeAt(it) }
    }

    companion object {
        /**
         * The default [Saver] implementation for [RichTextValue].
         */
        val Saver = Saver<RichTextValue, Any>(
            save = {
                arrayListOf(
                    with(TextFieldValue.Saver) { this@Saver.save(it.textFieldValue) },

                    )
            },
            restore = {
                @Suppress("UNCHECKED_CAST")
                val list = it as List<Any>
                RichTextValue(
                    textFieldValue = with(TextFieldValue.Saver) { restore(list[0]) }!!,
                )
            }
        )
    }
}