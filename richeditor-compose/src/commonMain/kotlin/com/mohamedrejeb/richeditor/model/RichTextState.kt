package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.mohamedrejeb.richeditor.utils.append
import com.mohamedrejeb.richeditor.utils.customMerge
import com.mohamedrejeb.richeditor.utils.isSpecifiedFieldsEquals
import com.mohamedrejeb.richeditor.utils.unmerge
import kotlin.math.max

@Composable
fun rememberRichTextState(

): RichTextState {
    return rememberSaveable(saver = RichTextState.Saver) {
        RichTextState()
    }
}

class RichTextState(

) {
    public val richParagraphStyleList = mutableStateListOf(
        RichParagraphStyle(
            key = 0,
            children = mutableStateListOf(),
        )
    )
    internal var visualTransformation: VisualTransformation by mutableStateOf(VisualTransformation.None)
    internal var textFieldValue by mutableStateOf(TextFieldValue())
    val selection get() = textFieldValue.selection
    val composition get() = textFieldValue.composition

    private var currentAppliedSpanStyle: SpanStyle by mutableStateOf(
        getSpanStyleByTextIndex(textIndex = max(0, textFieldValue.selection.min - 1))?.fullSpanStyle ?: SpanStyle()
    )

    private var toAddSpanStyle: SpanStyle by mutableStateOf(SpanStyle())
    private var toRemoveSpanStyle: SpanStyle by mutableStateOf(SpanStyle())

    private var unAppliedParagraphStyle: ParagraphStyle by mutableStateOf(ParagraphStyle())

    val currentSpanStyle: SpanStyle get() = currentAppliedSpanStyle.customMerge(toAddSpanStyle).unmerge(toRemoveSpanStyle)

    var currentParagraphStyle: ParagraphStyle by mutableStateOf(
        getParagraphStyleByTextIndex(textIndex = textFieldValue.selection.min)?.paragraphStyle ?: ParagraphStyle()
    )
        private set

    var annotatedString by mutableStateOf(AnnotatedString(text = ""))
        private set

    fun toggleSpanStyle(spanStyle: SpanStyle) {
        println("is Equals: ${currentSpanStyle.isSpecifiedFieldsEquals(spanStyle)}")
        if (currentSpanStyle.isSpecifiedFieldsEquals(spanStyle))
            removeSpanStyle(spanStyle)
        else
            addSpanStyle(spanStyle)
    }

    fun addSpanStyle(spanStyle: SpanStyle) {
        if (!currentSpanStyle.isSpecifiedFieldsEquals(spanStyle)) {
            toAddSpanStyle = toAddSpanStyle.customMerge(spanStyle)
            toRemoveSpanStyle = toRemoveSpanStyle.unmerge(spanStyle)
        }

        if (!selection.collapsed)
            handleAddingStyleToSelectedText()
    }

    fun removeSpanStyle(spanStyle: SpanStyle) {
        if (currentSpanStyle.isSpecifiedFieldsEquals(spanStyle)) {
            toRemoveSpanStyle = toRemoveSpanStyle.customMerge(spanStyle)
            toAddSpanStyle = toAddSpanStyle.unmerge(spanStyle)
        }

        if (!selection.collapsed)
            handleRemovingStyleFromSelectedText()
    }

    fun addParagraphStyle(paragraphStyle: ParagraphStyle) {
        unAppliedParagraphStyle = unAppliedParagraphStyle.merge(paragraphStyle)
        currentParagraphStyle = currentParagraphStyle.merge(unAppliedParagraphStyle)
    }

    /**
     * Handles the new text field value.
     *
     * @param newTextFieldValue the new text field value.
     */
    internal fun onTextFieldValueChange(newTextFieldValue: TextFieldValue) {
        if (newTextFieldValue.text.length > textFieldValue.text.length) {
            handleAddingCharacters(newTextFieldValue)
        } else if (newTextFieldValue.text.length < textFieldValue.text.length) {
            handleRemovingCharacters(newTextFieldValue)
        } else if (newTextFieldValue.text != textFieldValue.text) {
            handleReplacingCharacters(newTextFieldValue)
        }

        // Update text field value
        updateTextFieldValue(newTextFieldValue)
    }

    /**
     * Handles updating the text field value and all the related states such as the [annotatedString] and [visualTransformation] to reflect the new text field value.
     *
     * @param newTextFieldValue the new text field value.
     */
    private fun updateTextFieldValue(newTextFieldValue: TextFieldValue) {
        annotatedString = buildAnnotatedString {
            richParagraphStyleList.forEach { richParagraphStyle ->
                withStyle(richParagraphStyle.paragraphStyle) {
                    append(richParagraphStyle.children)
                }
            }
        }
        textFieldValue = newTextFieldValue.copy(annotatedString = annotatedString)
        visualTransformation = VisualTransformation {
            TransformedText(
                text = annotatedString,
                offsetMapping = OffsetMapping.Identity
            )
        }

        // Clear un-applied styles
        toAddSpanStyle = SpanStyle()
        toRemoveSpanStyle = SpanStyle()
        unAppliedParagraphStyle = ParagraphStyle()
        // Update current span style
        currentAppliedSpanStyle = getSpanStyleByTextIndex(textIndex = max(0, textFieldValue.selection.min - 1))
            ?.fullSpanStyle
            ?: SpanStyle()

        // Update current paragraph style
        currentParagraphStyle = getParagraphStyleByTextIndex(textIndex = max(0, textFieldValue.selection.min - 1))
            ?.paragraphStyle
            ?: ParagraphStyle()
    }

    /**
     * Handles adding characters to the text field.
     * This method will update the [richParagraphStyleList] to reflect the new changes.
     *
     * @param newTextFieldValue the new text field value.
     */
    private fun handleAddingCharacters(
        newTextFieldValue: TextFieldValue,
    ) {
        val typedCharsCount = newTextFieldValue.text.length - textFieldValue.text.length
        val typedText = newTextFieldValue.text.substring(
            startIndex = newTextFieldValue.selection.min - typedCharsCount,
            endIndex = newTextFieldValue.selection.min
        )
        val startTypeIndex = newTextFieldValue.selection.min - typedCharsCount
        val previousIndex = max(0, startTypeIndex - 1)

        val activeRichSpanStyle = getSpanStyleByTextIndex(previousIndex)

        if (activeRichSpanStyle != null) {
            val startIndex = startTypeIndex - activeRichSpanStyle.textRange.min
            val beforeText = activeRichSpanStyle.text.substring(0, startIndex)
            val afterText = activeRichSpanStyle.text.substring(startIndex)

            val activeRichSpanStyleFullSpanStyle = activeRichSpanStyle.fullSpanStyle
            val newSpanStyle = activeRichSpanStyleFullSpanStyle.customMerge(toAddSpanStyle).unmerge(toRemoveSpanStyle)


            if (
                (toAddSpanStyle == SpanStyle() && toRemoveSpanStyle == SpanStyle()) ||
                newSpanStyle == activeRichSpanStyleFullSpanStyle
            ) {
                activeRichSpanStyle.text = beforeText + typedText + afterText
            } else {
                handleUpdatingRichSpanStyle(
                    richSpanStyle = activeRichSpanStyle,
                    beforeText = beforeText,
                    middleText = typedText,
                    afterText = afterText,
                    startIndex = startTypeIndex,
                    richSpanStyleFullSpanStyle = activeRichSpanStyleFullSpanStyle,
                    newSpanStyle = newSpanStyle,
                )
            }
        } else {
            val newRichSpanStyle = RichSpanStyle(
                paragraph = richParagraphStyleList.last(),
                text = typedText,
                textRange = TextRange(startTypeIndex, startTypeIndex + typedText.length),
                spanStyle = toAddSpanStyle,
            )
            richParagraphStyleList.last().children.add(newRichSpanStyle)
        }
    }

    /**
     * Handles removing characters from the text field value.
     * This method will update the [richParagraphStyleList] to reflect the new changes.
     *
     * @param newTextFieldValue The new text field value.
     */
    private fun handleRemovingCharacters(
        newTextFieldValue: TextFieldValue
    ) {
        val removedChars = textFieldValue.text.length - newTextFieldValue.text.length
        val startRemoveIndex = newTextFieldValue.selection.min + removedChars
        val endRemoveIndex = newTextFieldValue.selection.min
        val removeRange = TextRange(endRemoveIndex, startRemoveIndex)

        val startRichSpanStyle = getSpanStyleByTextIndex(textIndex = startRemoveIndex - 1) ?: return
        val endRichSpanStyle = getSpanStyleByTextIndex(textIndex = endRemoveIndex) ?: return

        // Check deleted paragraphs
        val startParagraphIndex = richParagraphStyleList.indexOf(startRichSpanStyle.paragraph)
        val endParagraphIndex = richParagraphStyleList.indexOf(endRichSpanStyle.paragraph)
        if (endParagraphIndex < startParagraphIndex - 1) {
            richParagraphStyleList.removeRange(endParagraphIndex + 1, startParagraphIndex)
        }

        // Check deleted spans
        startRichSpanStyle.paragraph.removeTextRange(removeRange)

        if (startParagraphIndex != endParagraphIndex) {
            endRichSpanStyle.paragraph.removeTextRange(removeRange)
        }
    }

    private fun handleReplacingCharacters(
        newTextFieldValue: TextFieldValue
    ) {
        newTextFieldValue.text
    }

    private fun handleAddingStyleToSelectedText() {
        val richSpanStyleList = getSpanStyleListByTextRange(selection)

        val startSelectionIndex = selection.min
        val endSelectionIndex = selection.max

        for (i in richSpanStyleList.lastIndex downTo 0) {
            val richSpanStyle = richSpanStyleList[i]

            val beforeText = if (startSelectionIndex in richSpanStyle.textRange)
                richSpanStyle.text.substring(0, startSelectionIndex - richSpanStyle.textRange.start)
            else ""
            val middleText = richSpanStyle.text.substring(
                maxOf(startSelectionIndex - richSpanStyle.textRange.start, 0),
                minOf(endSelectionIndex - richSpanStyle.textRange.start, richSpanStyle.text.length)
            )
            val afterText = if ((endSelectionIndex - 1) in richSpanStyle.textRange)
                richSpanStyle.text.substring(endSelectionIndex - richSpanStyle.textRange.start)
            else ""

            val richSpanStyleFullSpanStyle = richSpanStyle.fullSpanStyle
            val newSpanStyle = richSpanStyleFullSpanStyle.customMerge(toAddSpanStyle).unmerge(toRemoveSpanStyle)

            val startApplyStyleIndex = maxOf(startSelectionIndex, richSpanStyle.textRange.start)

            handleUpdatingRichSpanStyle(
                richSpanStyle = richSpanStyle,
                newSpanStyle = newSpanStyle,
                startIndex = startApplyStyleIndex,
                beforeText = beforeText,
                middleText = middleText,
                afterText = afterText,
            )
        }

        updateTextFieldValue(textFieldValue)
    }

    private fun handleRemovingStyleFromSelectedText() {
        val richSpanStyleList = getSpanStyleListByTextRange(selection)

        val startSelectionIndex = selection.min
        val endSelectionIndex = selection.max

//        for (i in richSpanStyleList.lastIndex downTo richSpanStyleList.lastIndex) {
        for (i in richSpanStyleList.lastIndex downTo 0) {
            val richSpanStyle = richSpanStyleList[i]

            val beforeText = if (startSelectionIndex in richSpanStyle.textRange)
                richSpanStyle.text.substring(0, startSelectionIndex - richSpanStyle.textRange.start)
            else ""
            val middleText = richSpanStyle.text.substring(
                maxOf(startSelectionIndex - richSpanStyle.textRange.start, 0),
                minOf(endSelectionIndex - richSpanStyle.textRange.start, richSpanStyle.text.length)
            )
            val afterText = if ((endSelectionIndex - 1) in richSpanStyle.textRange)
                richSpanStyle.text.substring(endSelectionIndex - richSpanStyle.textRange.start)
            else ""

            val richSpanStyleFullSpanStyle = richSpanStyle.fullSpanStyle
            val newSpanStyle = richSpanStyleFullSpanStyle.customMerge(toAddSpanStyle).unmerge(toRemoveSpanStyle)

            val startApplyStyleIndex = maxOf(startSelectionIndex, richSpanStyle.textRange.start)

            handleUpdatingRichSpanStyle(
                richSpanStyle = richSpanStyle,
                newSpanStyle = newSpanStyle,
                startIndex = startApplyStyleIndex,
                beforeText = beforeText,
                middleText = middleText,
                afterText = afterText,
            )
        }

        updateTextFieldValue(textFieldValue)
    }

    /**
     * Apply [toAddSpanStyle] and [toRemoveSpanStyle] to a [RichSpanStyle].
     *
     * @param richSpanStyle The [RichSpanStyle] to apply the styles to.
     * @param beforeText The text before applying the styles.
     * @param middleText The text to apply the styles to.
     * @param afterText The text after applying the styles.
     * @param startIndex The start index of the text to apply the styles to.
     * @param richSpanStyleFullSpanStyle The [SpanStyle] of the [RichSpanStyle].
     * @param newSpanStyle The new [SpanStyle] to apply to the [RichSpanStyle].
     */
    private fun handleUpdatingRichSpanStyle(
        richSpanStyle: RichSpanStyle,
        beforeText: String,
        middleText: String,
        afterText: String,
        startIndex: Int,
        richSpanStyleFullSpanStyle: SpanStyle = richSpanStyle.fullSpanStyle,
        newSpanStyle: SpanStyle = richSpanStyleFullSpanStyle.customMerge(toAddSpanStyle).unmerge(toRemoveSpanStyle),
    ) {
        if (toRemoveSpanStyle == SpanStyle()) {
            handleApplyingStyleToRichSpanStyle(
                richSpanStyle = richSpanStyle,
                beforeText = beforeText,
                middleText = middleText,
                afterText = afterText,
                startIndex = startIndex,
            )
        } else {
            handleRemovingStyleFromRichSpanStyle(
                richSpanStyle = richSpanStyle,
                beforeText = beforeText,
                middleText = middleText,
                afterText = afterText,
                startIndex = startIndex,
                richSpanStyleFullSpanStyle = richSpanStyleFullSpanStyle,
                newSpanStyle = newSpanStyle,
            )
        }
    }

    /**
     * Handles applying a new [SpanStyle] to a [RichSpanStyle].
     *
     * @param richSpanStyle The [RichSpanStyle] to apply the new [SpanStyle] to.
     * @param beforeText The text before applying the styles.
     * @param middleText The text to apply the styles to.
     * @param afterText The text after applying the styles.
     * @param startIndex The start index of the text to apply the styles to.
     */
    private fun handleApplyingStyleToRichSpanStyle(
        richSpanStyle: RichSpanStyle,
        beforeText: String,
        middleText: String,
        afterText: String,
        startIndex: Int,
    ) {
        richSpanStyle.text = beforeText
        richSpanStyle.children.add(
            0,
            RichSpanStyle(
                paragraph = richSpanStyle.paragraph,
                parent = richSpanStyle,
                text = middleText,
                textRange = TextRange(
                    startIndex,
                    startIndex + middleText.length
                ),
                spanStyle = SpanStyle(textDecoration = currentSpanStyle.textDecoration).customMerge(
                    toAddSpanStyle
                ),
            )
        )
        if (afterText.isNotEmpty()) {
            richSpanStyle.children.add(
                1,
                RichSpanStyle(
                    paragraph = richSpanStyle.paragraph,
                    parent = richSpanStyle,
                    text = afterText,
                    textRange = TextRange(
                        startIndex + middleText.length,
                        startIndex + middleText.length + afterText.length
                    ),
                )
            )
        }
    }

    /**
     * Handles removing a [SpanStyle] from a [RichSpanStyle].
     *
     * @param richSpanStyle The [RichSpanStyle] to remove the [SpanStyle] from.
     * @param beforeText The text before removing the styles.
     * @param middleText The text to remove the styles from.
     * @param afterText The text after removing the styles.
     * @param startIndex The start index of the text to remove the styles from.
     * @param richSpanStyleFullSpanStyle The [SpanStyle] of the [RichSpanStyle].
     * @param newSpanStyle The new [SpanStyle] to apply to the [RichSpanStyle].
     */
    private fun handleRemovingStyleFromRichSpanStyle(
        richSpanStyle: RichSpanStyle,
        beforeText: String,
        middleText: String,
        afterText: String,
        startIndex: Int,
        richSpanStyleFullSpanStyle: SpanStyle,
        newSpanStyle: SpanStyle,
    ) {
        println("beforeText: $beforeText")
        println("middleText: $middleText")
        println("afterText: $afterText")
        println("startIndex: $startIndex")

        if (richSpanStyleFullSpanStyle == newSpanStyle) {
            println("richSpanStyleFullSpanStyle == newSpanStyle")
            return
        }

        richSpanStyle.text = beforeText
        val parentRichSpanStyle = richSpanStyle.getClosestRichSpanStyle(newSpanStyle)
        println("parentRichSpanStyle: $parentRichSpanStyle")
        val newRichSpanStyle = RichSpanStyle(
            paragraph = richSpanStyle.paragraph,
            parent = parentRichSpanStyle,
            text = middleText,
            textRange = TextRange(
                startIndex,
                startIndex + middleText.length
            ),
            spanStyle = newSpanStyle.unmerge(parentRichSpanStyle?.spanStyle),
        )
        val afterSpanStyle = RichSpanStyle(
            paragraph = richSpanStyle.paragraph,
            parent = parentRichSpanStyle,
            text = afterText,
            textRange = TextRange(
                startIndex + middleText.length,
                startIndex + middleText.length + afterText.length
            ),
            spanStyle = richSpanStyleFullSpanStyle,
        )

        val toShiftRichSpanStyleList: MutableList<RichSpanStyle> = mutableListOf()
        var previousRichSpanStyle: RichSpanStyle?
        var currentRichSpanStyle: RichSpanStyle? = richSpanStyle

        toShiftRichSpanStyleList.add(newRichSpanStyle)
        if (afterSpanStyle.text.isNotEmpty() || afterSpanStyle.children.isNotEmpty())
            toShiftRichSpanStyleList.add(afterSpanStyle)

        println("toShiftRichSpanStyleList: $toShiftRichSpanStyleList")

        while(true) {
            previousRichSpanStyle = currentRichSpanStyle
            currentRichSpanStyle = currentRichSpanStyle?.parent
            println("previousRichSpanStyle: $previousRichSpanStyle")
            println("currentRichSpanStyle: $currentRichSpanStyle")

            if (currentRichSpanStyle == null || currentRichSpanStyle == parentRichSpanStyle) {
                break
            } else {
                val index = currentRichSpanStyle.children.indexOf(previousRichSpanStyle)
                if (index in 0 until currentRichSpanStyle.children.lastIndex) {
                    ((index + 1)..currentRichSpanStyle.children.lastIndex).forEach {
                        val richSpanStyle = currentRichSpanStyle.children[it]
                        richSpanStyle.spanStyle = richSpanStyle.fullSpanStyle
                        richSpanStyle.parent = parentRichSpanStyle
                        toShiftRichSpanStyleList.add(richSpanStyle)
                    }
                    currentRichSpanStyle.children.removeRange(index + 1, currentRichSpanStyle.children.size)
                }
            }
        }

        println("toShiftRichSpanStyleList2: $toShiftRichSpanStyleList")
        println("currentRichSpanStyle: $currentRichSpanStyle")
        println("previousRichSpanStyle: $previousRichSpanStyle")

        if (parentRichSpanStyle == null || currentRichSpanStyle == null) {
            val index = richSpanStyle.paragraph.children.indexOf(previousRichSpanStyle)
            println("paragraph index: $index")
            if (index in 0 .. richSpanStyle.paragraph.children.lastIndex) {
                richSpanStyle.paragraph.children.addAll(
                    index + 1,
                    toShiftRichSpanStyleList
                )
            }
        } else {
            val index = parentRichSpanStyle.children.indexOf(previousRichSpanStyle)
            println("index: $index")
            if (index in 0 .. parentRichSpanStyle.children.lastIndex) {
                parentRichSpanStyle.children.addAll(
                    index + 1,
                    toShiftRichSpanStyleList
                )
            }
        }
    }

    /**
     * Returns the [RichParagraphStyle] that contains the given [textIndex].
     * If no [RichParagraphStyle] contains the given [textIndex], null is returned.
     *
     * @param textIndex The text index to search for.
     * @return The [RichParagraphStyle] that contains the given [textIndex], or null if no such [RichParagraphStyle] exists.
     */
    private fun getParagraphStyleByTextIndex(textIndex: Int): RichParagraphStyle? {
        var index = 0
        return richParagraphStyleList.firstOrNull { richParagraphStyle ->
            val result = richParagraphStyle.getSpanStyleByTextIndex(
                textIndex = textIndex,
                offset = index,
            )
            index = result.first
            result.second != null
        }
    }

    /**
     * Returns the [RichSpanStyle] that contains the given [textIndex].
     * If no [RichSpanStyle] contains the given [textIndex], null is returned.
     *
     * @param textIndex The text index to search for.
     * @return The [RichSpanStyle] that contains the given [textIndex], or null if no such [RichSpanStyle] exists.
     */
    private fun getSpanStyleByTextIndex(textIndex: Int): RichSpanStyle? {
        var index = 0
        richParagraphStyleList.forEach { richParagraphStyle ->
            val result = richParagraphStyle.getSpanStyleByTextIndex(
                textIndex = textIndex,
                offset = index,
            )
            if (result.second != null)
                return result.second
            else
                index = result.first
        }
        return null
    }

    private fun getSpanStyleListByTextRange(searchTextRange: TextRange): List<RichSpanStyle> {
        var index = 0
        val richSpanStyleList = mutableListOf<RichSpanStyle>()
        richParagraphStyleList.forEach { richParagraphStyle ->
            val result = richParagraphStyle.getSpanStyleListByTextRange(
                searchTextRange = searchTextRange,
                offset = index,
            )
            richSpanStyleList.addAll(result.second)
            index = result.first
        }
        return richSpanStyleList
    }

    companion object {
        val Saver: Saver<RichTextState, *> = listSaver(
            save = {
                listOf<String>()
            },
            restore = {
                RichTextState()
            }
        )
    }
}