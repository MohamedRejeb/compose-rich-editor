package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextPart
import com.mohamedrejeb.richeditor.model.RichTextStyle
import com.mohamedrejeb.richeditor.model.RichTextValue
import kotlin.math.max
import kotlin.math.min

internal class RichTextValueBuilder {

    private var textFieldValue: TextFieldValue = TextFieldValue()
    private val parts = mutableListOf<RichTextPart>()
    private val currentStyles = mutableSetOf<RichTextStyle>()

    fun textFieldValue(textFieldValue: TextFieldValue) = apply {
        this.textFieldValue = textFieldValue
    }

    fun parts(parts: List<RichTextPart>) = apply {
        this.parts.clear()
        this.parts.addAll(parts)
    }

    fun currentStyles(currentStyles: Set<RichTextStyle>) = apply {
        this.currentStyles.clear()
        this.currentStyles.addAll(currentStyles)
    }

    fun updateTextFieldValue(newTextFieldValue: TextFieldValue): RichTextValueBuilder {
        if (newTextFieldValue.text.length > textFieldValue.text.length) {
            handleAddingCharacters(newTextFieldValue)
        } else if (newTextFieldValue.text.length < textFieldValue.text.length) {
            handleRemovingCharacters(newTextFieldValue)
        } else {
            handleReplacingCharacters(newTextFieldValue)
        }

        val newStyle = if (!newTextFieldValue.selection.collapsed || newTextFieldValue.selection.start > 0) {
            parts
                .firstOrNull { (newTextFieldValue.selection.start - 1) in (it.fromIndex..it.toIndex) }
                ?.styles
                ?: currentStyles
        } else {
            currentStyles
        }

        collapseParts(
            textLastIndex = newTextFieldValue.text.lastIndex
        )

        return RichTextValueBuilder()
            .textFieldValue(newTextFieldValue)
            .parts(parts)
            .currentStyles(newStyle)
    }

    fun build(): RichTextValue {
        return RichTextValue(
            textFieldValue = textFieldValue,
            currentStyles = currentStyles,
            parts = parts,
        )
    }

    private fun handleAddingCharacters(
        newTextFieldValue: TextFieldValue,
    ) {
        val typedChars = newTextFieldValue.text.length - textFieldValue.text.length
        val startTypeIndex = newTextFieldValue.selection.start - typedChars

        val startRichTextPartIndex = parts.indexOfFirst {
            (startTypeIndex - 1) in it.fromIndex..it.toIndex
        }
        val endRichTextPartIndex = parts.indexOfFirst {
            startTypeIndex in it.fromIndex..it.toIndex
        }

        val startRichTextPart = parts.getOrNull(startRichTextPartIndex)
        val endRichTextPart = parts.getOrNull(endRichTextPartIndex)

        if (currentStyles == startRichTextPart?.styles) {
            parts[startRichTextPartIndex] = startRichTextPart.copy(
                toIndex = startRichTextPart.toIndex + typedChars
            )

            if (startRichTextPartIndex < parts.lastIndex) {
                moveParts(
                    mParts = parts,
                    fromIndex = startRichTextPartIndex + 1,
                    toIndex = parts.lastIndex,
                    by = typedChars,
                )
            }
        } else if (currentStyles == endRichTextPart?.styles) {
            parts[endRichTextPartIndex] = endRichTextPart.copy(
                toIndex = endRichTextPart.toIndex + typedChars
            )

            if (endRichTextPartIndex < parts.lastIndex) {
                moveParts(
                    mParts = parts,
                    fromIndex = endRichTextPartIndex + 1,
                    toIndex = parts.lastIndex,
                    by = typedChars,
                )
            }
        } else if (startRichTextPart == endRichTextPart && startRichTextPart != null) {
            parts[startRichTextPartIndex] = startRichTextPart.copy(
                toIndex = startTypeIndex - 1
            )
            parts.add(
                startRichTextPartIndex + 1, startRichTextPart.copy(
                    fromIndex = startTypeIndex + typedChars,
                    toIndex = startRichTextPart.toIndex + typedChars
                )
            )
            parts.add(
                startRichTextPartIndex + 1, RichTextPart(
                    fromIndex = startTypeIndex,
                    toIndex = startTypeIndex + typedChars - 1,
                    styles = currentStyles
                )
            )

            if ((startRichTextPartIndex + 2) < parts.lastIndex) {
                moveParts(
                    mParts = parts,
                    fromIndex = startRichTextPartIndex + 3,
                    toIndex = parts.lastIndex,
                    by = typedChars,
                )
            }
        } else if (endRichTextPart == null) {
            parts.add(
                RichTextPart(
                    fromIndex = startTypeIndex,
                    toIndex = startTypeIndex + typedChars - 1,
                    styles = currentStyles
                )
            )
        } else {
            parts.add(
                startRichTextPartIndex + 1, RichTextPart(
                    fromIndex = startTypeIndex,
                    toIndex = startTypeIndex + typedChars - 1,
                    styles = currentStyles
                )
            )

            if ((startRichTextPartIndex + 1) < parts.lastIndex) {
                moveParts(
                    mParts = parts,
                    fromIndex = startRichTextPartIndex + 2,
                    toIndex = parts.lastIndex,
                    by = typedChars,
                )
            }
        }
    }

    private fun handleRemovingCharacters(
        newTextFieldValue: TextFieldValue
    ) {
        val removedChars = textFieldValue.text.length - newTextFieldValue.text.length
        val startRemoveIndex = newTextFieldValue.selection.start + removedChars
        val endRemoveIndex = newTextFieldValue.selection.start
        val removeRange = endRemoveIndex until startRemoveIndex

        val removedIndexes = mutableSetOf<Int>()

        parts.forEachIndexed { index, part ->
            if (removeRange.last < part.fromIndex) {
                // Example: L|orem| ipsum *dolor* sit amet.
                parts[index] = part.copy(
                    fromIndex = part.fromIndex - removedChars,
                    toIndex = part.toIndex - removedChars
                )
            } else if (removeRange.first <= part.fromIndex && removeRange.last >= part.toIndex) {
                // Example: Lorem| ipsum *dolor* si|t amet.
                parts[index] = part.copy(
                    fromIndex = 0,
                    toIndex = 0
                )
                removedIndexes.add(index)
            } else if (removeRange.first <= part.fromIndex && removeRange.last < part.toIndex) {
                // Example: Lorem| ipsum *dol|or* sit amet.
                parts[index] = part.copy(
                    fromIndex = max(0, removeRange.first),
                    toIndex = min(newTextFieldValue.text.length, part.toIndex - removedChars)
                )
            } else if (removeRange.first > part.fromIndex && removeRange.last <= part.toIndex) {
                // Example: Lorem ipsum *d|olo|r* sit amet.
                parts[index] = part.copy(
                    toIndex = part.toIndex - removedChars
                )
            } else if (removeRange.first > part.fromIndex && removeRange.first < part.toIndex && removeRange.last > part.toIndex) {
                // Example: Lorem ipsum *dol|or* si|t amet.
                parts[index] = part.copy(
                    toIndex = removeRange.first
                )
            }
        }

        removedIndexes.reversed().forEach { parts.removeAt(it) }
    }

    private fun handleReplacingCharacters(
        newTextFieldValue: TextFieldValue
    ) {
        if (textFieldValue.text == newTextFieldValue.text) {
            return
        }

        // Todo: Handle replacing characters.
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
     * @param textLastIndex The last index of the text, used to determine if a part is out of bounds.
     */
    private fun collapseParts(
        textLastIndex: Int
    ) {
        val startRangeMap = mutableMapOf<Int, Int>()
        val endRangeMap = mutableMapOf<Int, Int>()
        val removedIndexes = mutableSetOf<Int>()

        parts.forEachIndexed { index, part ->
            startRangeMap[part.fromIndex] = index
            endRangeMap[part.toIndex] = index
        }

        parts.forEachIndexed { index, part ->
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
                if (parts[otherRangeIndex].styles == part.styles) {
                    parts[index] = part.copy(
                        toIndex = parts[otherRangeIndex].toIndex
                    )

                    // Remove collapsed values
                    startRangeMap.remove(end + 1)
                    endRangeMap.remove(end)
                    removedIndexes.add(otherRangeIndex)
                }
            }

            if (endRangeMap.containsKey(start - 1)) {
                val otherRangeIndex = requireNotNull(endRangeMap[start - 1])
                if (parts[otherRangeIndex].styles == part.styles) {
                    parts[index] = part.copy(
                        fromIndex = parts[otherRangeIndex].fromIndex
                    )

                    // Remove collapsed values
                    startRangeMap.remove(start - 1)
                    endRangeMap.remove(start - 1)
                    removedIndexes.add(otherRangeIndex)
                }
            }

            parts[index] = parts[index].copy(
                fromIndex = max(0, parts[index].fromIndex),
                toIndex = min(textLastIndex, parts[index].toIndex),
            )
        }

        removedIndexes.reversed().forEach { parts.removeAt(it) }
    }

    companion object {
        fun from(text: String): RichTextValueBuilder {
            return RichTextValueBuilder()
                .textFieldValue(TextFieldValue(text = text))
        }

        fun from(textFieldValue: TextFieldValue): RichTextValueBuilder {
            return RichTextValueBuilder()
                .textFieldValue(textFieldValue)
        }

        fun from(richTextValue: RichTextValue): RichTextValueBuilder {
            return RichTextValueBuilder()
                .textFieldValue(richTextValue.textFieldValue)
                .parts(richTextValue.parts)
                .currentStyles(richTextValue.currentStyles)
        }
    }
}