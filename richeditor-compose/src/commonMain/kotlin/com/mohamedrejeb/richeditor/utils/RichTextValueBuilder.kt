package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextPart
import com.mohamedrejeb.richeditor.model.RichTextStyle
import com.mohamedrejeb.richeditor.model.RichTextValue
import kotlin.math.max
import kotlin.math.min

/**
 * A builder for [RichTextValue]
 * @see RichTextValue
 */
internal class RichTextValueBuilder {

    private var textFieldValue: TextFieldValue = TextFieldValue()
    private val parts = mutableListOf<RichTextPart>()
    private val currentStyles = mutableSetOf<RichTextStyle>()

    /**
     * Set the text field value
     * @param textFieldValue the text field value to set
     * @return a new [RichTextValueBuilder] with the new text field value
     */
    fun setTextFieldValue(textFieldValue: TextFieldValue): RichTextValueBuilder {
        this.textFieldValue = textFieldValue
        return this
    }

    /**
     * Set the parts of the text
     * @param parts the parts to set
     * @return a new [RichTextValueBuilder] with the new parts
     * @see RichTextPart
     */
    fun setParts(parts: List<RichTextPart>): RichTextValueBuilder {
        this.parts.clear()
        this.parts.addAll(parts)
        return this
    }

    /**
     * Set the current styles
     * @param currentStyles the styles to set
     * @return a new [RichTextValueBuilder] with the new styles
     * @see RichTextStyle
     */
    fun setCurrentStyles(currentStyles: Set<RichTextStyle>): RichTextValueBuilder {
        this.currentStyles.clear()
        this.currentStyles.addAll(currentStyles)
        return this
    }

    /**
     * Add a style to the current styles
     * @param style the style to add
     * @return a new [RichTextValueBuilder] with the new style
     * @see RichTextStyle
     */
    fun addStyle(vararg style: RichTextStyle): RichTextValueBuilder {
        currentStyles.addAll(style)
        applyStylesToSelectedText(*style)
        return this
    }

    /**
     * Remove a style from the current styles
     * @param style the style to remove
     * @return a new [RichTextValueBuilder] without the style
     * @see RichTextStyle
     */
    fun removeStyle(vararg style: RichTextStyle): RichTextValueBuilder {
        currentStyles.removeAll(style.toSet())
        removeStylesFromSelectedText(*style)
        return this
    }

    /**
     * Update the current styles
     * @param newStyles the new styles
     * @return a new [RichTextValueBuilder] with the new styles
     * @see RichTextStyle
     */
    fun updateStyles(newStyles: Set<RichTextStyle>): RichTextValueBuilder {
        currentStyles.clear()
        currentStyles.addAll(newStyles)
        applyStylesToSelectedText(*newStyles.toTypedArray())
        return this
    }

    /**
     * Clear the current styles
     * @return a new [RichTextValueBuilder] with no styles
     * @see RichTextStyle
     */
    fun clearStyles(): RichTextValueBuilder {
        currentStyles.clear()
        removeAllStylesFromSelectedText()
        return this
    }

    /**
     * Add a part to the list of parts
     * @param part the part to add
     * @return a new [RichTextValueBuilder] with the new part
     * @throws IllegalArgumentException if the part is invalid
     * @see RichTextPart
     */
    fun addPart(part: RichTextPart): RichTextValueBuilder {
        if (part.fromIndex > part.toIndex) {
            throw IllegalArgumentException("fromIndex must be less than or equal to toIndex")
        }
        if (part.fromIndex < 0) {
            throw IllegalArgumentException("fromIndex must be greater than or equal to 0")
        }
        if (part.toIndex > textFieldValue.text.lastIndex) {
            throw IllegalArgumentException("toIndex must be less than or equal to the last index of the text")
        }

        parts.add(part)
        return this
    }

    /**
     * Remove a part from the list of parts
     * @param part the part to remove
     * @return a new [RichTextValueBuilder] without the part
     * @see RichTextPart
     */
    fun removePart(part: RichTextPart): RichTextValueBuilder {
        parts.remove(part)
        return this
    }

    /**
     * Update the text field value and update the rich text parts accordingly to the new text field value
     * @param newTextFieldValue the new text field value
     * @return a new [RichTextValueBuilder] with the new text field value and parts
     */
    fun updateTextFieldValue(newTextFieldValue: TextFieldValue): RichTextValueBuilder {
        // Workaround to add unordered list support, it's going to be improved,
        // But for now if it works, it works :D
        var newTextFieldValue = newTextFieldValue
        if (newTextFieldValue.text.length > textFieldValue.text.length) {
            newTextFieldValue = handleAddingCharacters(newTextFieldValue)
        } else if (newTextFieldValue.text.length < textFieldValue.text.length) {
            handleRemovingCharacters(newTextFieldValue)
        }

        updateCurrentStyles(newTextFieldValue = newTextFieldValue)

        collapseParts(textLastIndex = newTextFieldValue.text.lastIndex)

        return this
            .setTextFieldValue(newTextFieldValue)
    }

    /**
     * Builds a [RichTextValue] from the current state of the builder.
     */
    fun build(): RichTextValue {
        return RichTextValue(
            textFieldValue = textFieldValue,
            currentStyles = currentStyles,
            parts = parts,
        )
    }

    /**
     * Handles adding characters to the text field.
     * This method will update the [parts] list to reflect the new text field value.
     *
     * @param newTextFieldValue the new text field value.
     */
    private fun handleAddingCharacters(
        newTextFieldValue: TextFieldValue,
    ): TextFieldValue {
        var newTextFieldValue = newTextFieldValue
        var typedChars = newTextFieldValue.text.length - textFieldValue.text.length
        val startTypeIndex = newTextFieldValue.selection.min - typedChars

        // Workaround to add unordered list support, it's going to be changed in the future
        // when I'm going to add ordered list support but for now if it works, it works :D
        if (
            newTextFieldValue.text.getOrNull(startTypeIndex - 2) == '\n' &&
            newTextFieldValue.text.getOrNull(startTypeIndex - 1) == '-' &&
            newTextFieldValue.text.getOrNull(startTypeIndex) == ' '
        ) {
            var newText = newTextFieldValue.text
            newText = newText.removeRange(startTypeIndex - 1..startTypeIndex)
            val firstHalf = newText.substring(0..startTypeIndex-2)
            val secondHalf = newText.substring(startTypeIndex-1..newText.lastIndex)
            val unorderedListPrefix = "  •  "
            newText = firstHalf + unorderedListPrefix + secondHalf
            typedChars+=3
            newTextFieldValue = newTextFieldValue.copy(
                text = newText,
                selection = TextRange(startTypeIndex + typedChars)
            )
        }

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
                    fromIndex = startRichTextPartIndex + 2,
                    toIndex = parts.lastIndex,
                    by = typedChars,
                )
            }
        }
        return newTextFieldValue
    }

    /**
     * Handles removing characters from the text field value.
     * This method will update the [parts] list to reflect the new text field value.
     *
     * @param newTextFieldValue The new text field value.
     */
    private fun handleRemovingCharacters(
        newTextFieldValue: TextFieldValue
    ) {
        val removedChars = textFieldValue.text.length - newTextFieldValue.text.length
        val startRemoveIndex = newTextFieldValue.selection.min + removedChars
        val endRemoveIndex = newTextFieldValue.selection.min
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

    /**
     * Moves [parts] from [fromIndex] to [toIndex] by [by].
     * @param fromIndex The index to start moving from.
     * @param toIndex The index to move to.
     * @param by The amount to move by.
     */
    private fun moveParts(
        fromIndex: Int,
        toIndex: Int,
        by: Int
    ) {
        val start = max(fromIndex, 0)
        val end = min(toIndex, parts.lastIndex)
        (start..end).forEach { index ->
            parts[index] = parts[index].copy(
                fromIndex = parts[index].fromIndex + by,
                toIndex = parts[index].toIndex + by,
            )
        }
    }

    /**
     * Collapses parts to avoid overlapping parts.
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

    /**
     * Updates the current styles based on the [newTextFieldValue].
     * @param newTextFieldValue The new [TextFieldValue].
     */
    private fun updateCurrentStyles(
        newTextFieldValue: TextFieldValue
    ) {
        val newStyles = parts
            .firstOrNull {
                if (newTextFieldValue.selection.min == 0 && it.fromIndex == 0) {
                    return@firstOrNull true
                }
                (newTextFieldValue.selection.min - 1) in (it.fromIndex..it.toIndex)
            }
            ?.styles
            ?: currentStyles

        setCurrentStyles(newStyles.toSet())
    }

    /**
     * Applies [style] to the selected text.
     * @param style The [RichTextStyle] to apply.
     */
    private fun applyStylesToSelectedText(vararg style: RichTextStyle) {
        updateSelectedTextParts { part ->
            val styles = part.styles.toMutableSet()
            styles.addAll(style.toSet())

            part.copy(
                styles = styles
            )
        }
    }

    /**
     * Removes [style] from the selected text.
     * @param style The [RichTextStyle] to remove.
     */
    private fun removeStylesFromSelectedText(vararg style: RichTextStyle) {
        updateSelectedTextParts { part ->
            val styles = part.styles.toMutableSet()
            styles.removeAll(style.toSet())

            part.copy(
                styles = styles
            )
        }
    }

    /**
     * Removes all styles from the selected text.
     */
    private fun removeAllStylesFromSelectedText() {
        updateSelectedTextParts { part ->
            part.copy(
                styles = emptySet()
            )
        }
    }

    /**
     * Updates the selected text parts.
     * @param update The update function.
     */
    private fun updateSelectedTextParts(
        update: (part: RichTextPart) -> RichTextPart
    ) {
        if (textFieldValue.selection.collapsed) {
            return
        }

        val fromIndex = textFieldValue.selection.min
        val toIndex = textFieldValue.selection.max

        val selectedParts = parts.filter { part ->
            part.fromIndex < toIndex && part.toIndex >= fromIndex
        }

        selectedParts.forEach { part ->
            val index = parts.indexOf(part)
            if (index !in parts.indices) return@forEach

            if (part.fromIndex < fromIndex && part.toIndex >= toIndex) {
                parts[index] = part.copy(
                    toIndex = fromIndex - 1
                )
                parts.add(
                    index + 1,
                    update(
                        part.copy(
                            fromIndex = fromIndex,
                            toIndex = toIndex - 1
                        )
                    )
                )
                parts.add(
                    index + 2,
                    part.copy(
                        fromIndex = toIndex,
                    )
                )
            } else if (part.fromIndex < fromIndex) {
                parts[index] = part.copy(
                    toIndex = fromIndex - 1
                )
                parts.add(
                    index + 1,
                    update(
                        part.copy(
                            fromIndex = fromIndex,
                        )
                    )
                )
            } else if (part.toIndex >= toIndex) {
                parts[index] = update(
                    part.copy(
                        toIndex = toIndex - 1
                    )
                )
                parts.add(
                    index + 1,
                    part.copy(
                        fromIndex = toIndex,
                    )
                )
            } else {
                parts[index] = update(part)
            }
        }
    }

    companion object {
        /**
         * Creates a [RichTextValueBuilder] from a [String].
         */
        fun from(text: String): RichTextValueBuilder {
            return RichTextValueBuilder()
                .setTextFieldValue(TextFieldValue(text = text))
        }

        /**
         * Creates a [RichTextValueBuilder] from a [TextFieldValue].
         */
        fun from(textFieldValue: TextFieldValue): RichTextValueBuilder {
            return RichTextValueBuilder()
                .setTextFieldValue(textFieldValue)
        }

        /**
         * Creates a [RichTextValueBuilder] from a [RichTextValue].
         */
        fun from(richTextValue: RichTextValue): RichTextValueBuilder {
            return RichTextValueBuilder()
                .setTextFieldValue(richTextValue.textFieldValue)
                .setParts(richTextValue.parts)
                .setCurrentStyles(richTextValue.currentStyles)
        }
    }
}