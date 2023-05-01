package com.mocoding.richeditor.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlin.math.max
import kotlin.math.min

@Immutable
data class RichTextValue internal constructor(
    val textFieldValue: TextFieldValue,
    val currentStyles: Set<RichTextStyle> = emptySet(),
    val parts: List<RichTextPart> = emptyList(),
) {

    val visualTransformation
        get() = VisualTransformation {
            TransformedText(
                text = annotatedString,
                offsetMapping = OffsetMapping.Identity
            )
        }

    val annotatedString
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
        parts: List<RichTextPart> = emptyList(),
    ) : this(
        textFieldValue = TextFieldValue(text = text),
        currentStyles = currentStyles,
        parts = parts,
    )

    fun addStyle(vararg style: RichTextStyle): RichTextValue {
       val mStyles = currentStyles.toMutableSet()
        mStyles.addAll(style.toSet())
        return copy(currentStyles = mStyles)
    }

    fun removeStyle(vararg style: RichTextStyle): RichTextValue {
        val mStyles = currentStyles.toMutableSet()
        mStyles.removeAll(style.toSet())
        return copy(currentStyles = mStyles)
    }

    fun toggleStyle(style: RichTextStyle): RichTextValue {
        return if (currentStyles.contains(style)) {
            removeStyle(style)
        } else {
            addStyle(style)
        }
    }

    fun updateStyles(newStyles: Set<RichTextStyle>): RichTextValue {
        return copy(currentStyles = newStyles)
    }

    fun updateTextFieldValue(newTextFieldValue: TextFieldValue): RichTextValue {
        val newParts = parts.toMutableList()

        if (newTextFieldValue.text.length > textFieldValue.text.length) {
            val typedChars = newTextFieldValue.text.length - textFieldValue.text.length
            val startTypeIndex = newTextFieldValue.selection.start - typedChars

            val startRichTextPartIndex = newParts.indexOfFirst {
                (startTypeIndex - 1) in it.fromIndex..it.toIndex
            }
            val endRichTextPartIndex = newParts.indexOfFirst {
                startTypeIndex in it.fromIndex..it.toIndex
            }

            val startRichTextPart = newParts.getOrNull(startRichTextPartIndex)
            val endRichTextPart = newParts.getOrNull(endRichTextPartIndex)

            if (currentStyles == startRichTextPart?.styles) {
                newParts[startRichTextPartIndex] = startRichTextPart.copy(
                    toIndex = startRichTextPart.toIndex + typedChars
                )

                if (startRichTextPartIndex < newParts.lastIndex) {
                    moveParts(
                        mParts = newParts,
                        fromIndex = startRichTextPartIndex + 1,
                        toIndex = newParts.lastIndex,
                        by = typedChars,
                    )
                }
            } else if (currentStyles == endRichTextPart?.styles) {
                newParts[endRichTextPartIndex] = endRichTextPart.copy(
                    toIndex = endRichTextPart.toIndex + typedChars
                )

                if (endRichTextPartIndex < newParts.lastIndex) {
                    moveParts(
                        mParts = newParts,
                        fromIndex = endRichTextPartIndex + 1,
                        toIndex = newParts.lastIndex,
                        by = typedChars,
                    )
                }
            } else if (startRichTextPart == endRichTextPart && startRichTextPart != null) {
                newParts[startRichTextPartIndex] = startRichTextPart.copy(
                    toIndex = startTypeIndex - 1
                )
                newParts.add(
                    startRichTextPartIndex + 1, startRichTextPart.copy(
                        fromIndex = startTypeIndex + typedChars,
                        toIndex = startRichTextPart.toIndex + typedChars
                    )
                )
                newParts.add(
                    startRichTextPartIndex + 1, RichTextPart(
                        fromIndex = startTypeIndex,
                        toIndex = startTypeIndex + typedChars - 1,
                        styles = currentStyles
                    )
                )

                if ((startRichTextPartIndex + 2) < newParts.lastIndex) {
                    moveParts(
                        mParts = newParts,
                        fromIndex = startRichTextPartIndex + 3,
                        toIndex = newParts.lastIndex,
                        by = typedChars,
                    )
                }
            } else if (endRichTextPart == null) {
                newParts.add(
                    RichTextPart(
                        fromIndex = startTypeIndex,
                        toIndex = startTypeIndex + typedChars - 1,
                        styles = currentStyles
                    )
                )
            } else {
                newParts.add(
                    startRichTextPartIndex + 1, RichTextPart(
                        fromIndex = startTypeIndex,
                        toIndex = startTypeIndex + typedChars - 1,
                        styles = currentStyles
                    )
                )

                if ((startRichTextPartIndex + 1) < newParts.lastIndex) {
                    moveParts(
                        mParts = newParts,
                        fromIndex = startRichTextPartIndex + 2,
                        toIndex = newParts.lastIndex,
                        by = typedChars,
                    )
                }
            }
        } else if (newTextFieldValue.text.length < textFieldValue.text.length) {
            val removedChars = textFieldValue.text.length - newTextFieldValue.text.length
            val startRemoveIndex = newTextFieldValue.selection.start + removedChars
            val endRemoveIndex = newTextFieldValue.selection.start
            val removeRange = endRemoveIndex until startRemoveIndex

            val removedIndexes = mutableSetOf<Int>()

            parts.forEachIndexed { index, part ->
                if (removeRange.last < part.fromIndex) {
                    // Example: L|orem| ipsum *dolor* sit amet.
                    newParts[index] = part.copy(
                        fromIndex = part.fromIndex - removedChars,
                        toIndex = part.toIndex - removedChars
                    )
                } else if (removeRange.first <= part.fromIndex && removeRange.last >= part.toIndex) {
                    // Example: Lorem| ipsum *dolor* si|t amet.
                    newParts[index] = part.copy(
                        fromIndex = 0,
                        toIndex = 0
                    )
                    removedIndexes.add(index)
                } else if (removeRange.first <= part.fromIndex && removeRange.last < part.toIndex) {
                    // Example: Lorem| ipsum *dol|or* sit amet.
                    newParts[index] = part.copy(
                        fromIndex = max(0, removeRange.first),
                        toIndex = min(newTextFieldValue.text.length, part.toIndex - removedChars)
                    )
                } else if (removeRange.first > part.fromIndex && removeRange.last <= part.toIndex) {
                    // Example: Lorem ipsum *d|olo|r* sit amet.
                    newParts[index] = part.copy(
                        toIndex = part.toIndex - removedChars
                    )
                } else if (removeRange.first > part.fromIndex && removeRange.first < part.toIndex && removeRange.last > part.toIndex) {
                    // Example: Lorem ipsum *dol|or* si|t amet.
                    newParts[index] = part.copy(
                        toIndex = removeRange.first
                    )
                }
            }

            removedIndexes.reversed().forEach { newParts.removeAt(it) }
        }

        val newStyle = newParts.firstOrNull {
            (newTextFieldValue.selection.start - 1) in (it.fromIndex..it.toIndex)
        }?.styles ?: emptySet()

        collapseParts(
            mParts = newParts,
            textLength = newTextFieldValue.text.length
        )

        println("newParts: $newParts")

        return copy(
            textFieldValue = newTextFieldValue,
            currentStyles = newStyle,
            parts = newParts,
        )
    }

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