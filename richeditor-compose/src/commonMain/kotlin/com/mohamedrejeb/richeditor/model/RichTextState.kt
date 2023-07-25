package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichParagraph.Type.Companion.startText
import com.mohamedrejeb.richeditor.parser.html.RichTextStateHtmlParser
import com.mohamedrejeb.richeditor.parser.markdown.RichTextStateMarkdownParser
import com.mohamedrejeb.richeditor.utils.*
import com.mohamedrejeb.richeditor.utils.append
import com.mohamedrejeb.richeditor.utils.customMerge
import com.mohamedrejeb.richeditor.utils.isSpecifiedFieldsEquals
import com.mohamedrejeb.richeditor.utils.unmerge
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun rememberRichTextState(): RichTextState {
    return rememberSaveable(saver = RichTextState.Saver) {
        RichTextState()
    }
}

class RichTextState internal constructor(
    initialRichParagraphList: List<RichParagraph>,
) {
    constructor(): this(listOf(RichParagraph()))

    internal val richParagraphList = mutableStateListOf<RichParagraph>()
    internal var visualTransformation: VisualTransformation by mutableStateOf(VisualTransformation.None)
    internal var textFieldValue by mutableStateOf(TextFieldValue())
        private set

    /**
     * The annotated string representing the rich text.
     */
    var annotatedString by mutableStateOf(AnnotatedString(text = ""))
        private set

    /**
     * The selection of the rich text.
     */
    var selection
        get() = textFieldValue.selection
        set(value) {
            if (value.min >= 0 && value.max <= textFieldValue.text.length) {
                val newTextFieldValue = textFieldValue.copy(selection = value)
                updateTextFieldValue(newTextFieldValue)
            }
        }

    val composition get() = textFieldValue.composition

    internal var singleParagraphMode by mutableStateOf(false)

    internal var textLayoutResult: TextLayoutResult? by mutableStateOf(null)
        private set

    private var lastPressPosition: Offset? by mutableStateOf(null)

    private var currentAppliedSpanStyle: SpanStyle by mutableStateOf(
        getRichSpanByTextIndex(textIndex = selection.min - 1)?.fullSpanStyle
            ?: RichSpanStyle.DefaultSpanStyle
    )

    private var currentRichSpanStyle: RichSpanStyle by mutableStateOf(
        getRichSpanByTextIndex(textIndex = selection.min - 1)?.style
            ?: RichSpanStyle.Default
    )
        private set

    val isLink get() = currentRichSpanStyle is RichSpanStyle.Link
    val isCode get() = (
            currentRichSpanStyle is RichSpanStyle.Code ||
            toAddRichSpanStyle is RichSpanStyle.Code) &&
            toRemoveRichSpanStyle !is RichSpanStyle.Code

    private var toAddSpanStyle: SpanStyle by mutableStateOf(SpanStyle())
    private var toRemoveSpanStyle: SpanStyle by mutableStateOf(SpanStyle())

    private var toAddRichSpanStyle: RichSpanStyle by mutableStateOf(RichSpanStyle.Default)
    private var toRemoveRichSpanStyle: RichSpanStyle by mutableStateOf(RichSpanStyle.Default)

    /**
     * The current span style.
     * If the selection is collapsed, the span style is the style of the character preceding the selection.
     * If the selection is not collapsed, the span style is the style of the selection.
     */
    val currentSpanStyle: SpanStyle
        get() = currentAppliedSpanStyle.customMerge(toAddSpanStyle).unmerge(toRemoveSpanStyle)

    internal var styledRichSpanList = mutableStateListOf<RichSpan>()
        private set

    private var currentAppliedParagraphStyle: ParagraphStyle by mutableStateOf(
        getRichParagraphByTextIndex(textIndex = selection.min - 1)?.paragraphStyle
            ?: richParagraphList.firstOrNull()?.paragraphStyle
            ?: RichParagraph.DefaultParagraphStyle
    )

    private var toAddParagraphStyle: ParagraphStyle by mutableStateOf(ParagraphStyle())
    private var toRemoveParagraphStyle: ParagraphStyle by mutableStateOf(ParagraphStyle())

    /**
     * The current paragraph style.
     * If the selection is collapsed, the paragraph style is the style of the paragraph containing the selection.
     * If the selection is not collapsed, the paragraph style is the style of the selection.
     */
    val currentParagraphStyle: ParagraphStyle
        get() = currentAppliedParagraphStyle.merge(toAddParagraphStyle).unmerge(toRemoveParagraphStyle)

    private var currentRichParagraphType: RichParagraph.Type by mutableStateOf(
        getRichParagraphByTextIndex(textIndex = selection.min - 1)?.type
            ?: RichParagraph.Type.Default
    )
        private set

    val isUnorderedList get() = currentRichParagraphType is RichParagraph.Type.UnorderedList
    val isOrderedList get() = currentRichParagraphType is RichParagraph.Type.OrderedList

    internal var richTextConfig by mutableStateOf(RichTextConfig())

    init {
        updateRichParagraphList(initialRichParagraphList)
    }

    @ExperimentalRichTextApi
    fun setConfig(
        linkColor: Color = Color.Unspecified,
        linkTextDecoration: TextDecoration? = null,
        codeColor: Color = Color.Unspecified,
        codeBackgroundColor: Color = Color.Unspecified,
        codeStrokeColor: Color = Color.Unspecified,
    ) {
        richTextConfig = RichTextConfig(
            linkColor = if (linkColor.isSpecified) linkColor else richTextConfig.linkColor,
            linkTextDecoration = if (linkTextDecoration != null) linkTextDecoration else richTextConfig.linkTextDecoration,
            codeColor = if (codeColor.isSpecified) codeColor else richTextConfig.codeColor,
            codeBackgroundColor = if (codeBackgroundColor.isSpecified) codeBackgroundColor else richTextConfig.codeBackgroundColor,
            codeStrokeColor = if (codeStrokeColor.isSpecified) codeStrokeColor else richTextConfig.codeStrokeColor,
        )

        updateTextFieldValue(textFieldValue)
    }

    /**
     * Toggle the [SpanStyle]
     * If the passed span style doesn't exist in the [currentSpanStyle] it's going to be added.
     * If the passed span style already exists in the [currentSpanStyle] it's going to be removed.
     *
     * Example: You can toggle Bold FontWeight by passing:
     *
     * ```
     * SpanStyle(fontWeight = FontWeight.Bold)
     * ```
     *
     * @param spanStyle the span style that is going to be toggled.
     * Only the specified params are going to be toggled, and the non specified ones are going to be ignored.
     * @see [addSpanStyle]
     * @see [removeSpanStyle]
     */
    fun toggleSpanStyle(spanStyle: SpanStyle) {
        if (currentSpanStyle.isSpecifiedFieldsEquals(spanStyle))
            removeSpanStyle(spanStyle)
        else
            addSpanStyle(spanStyle)
    }

    /**
     * Add new [SpanStyle] to the [currentSpanStyle]
     *
     * Example: You can add Bold FontWeight by passing:
     *
     * ```
     * SpanStyle(fontWeight = FontWeight.Bold)
     * ```
     *
     * @param spanStyle the span style that is going to be added to the [currentSpanStyle].
     * Only the specified params are going to be applied, and the non specified ones are going to be ignored.
     * @see [removeSpanStyle]
     * @see [toggleSpanStyle]
     */
    fun addSpanStyle(spanStyle: SpanStyle) {
        if (!currentSpanStyle.isSpecifiedFieldsEquals(spanStyle)) {
            toAddSpanStyle = toAddSpanStyle.customMerge(spanStyle)
            toRemoveSpanStyle = toRemoveSpanStyle.unmerge(spanStyle)
        }

        if (!selection.collapsed)
            handleAddingStyleToSelectedText()
    }

    /**
     * Remove an existing [SpanStyle] from the [currentSpanStyle]
     *
     * Example: You can remove Bold FontWeight by passing:
     *
     * ```
     * SpanStyle(fontWeight = FontWeight.Bold)
     * ```
     *
     * @param spanStyle the span style that is going to be removed from the [currentSpanStyle].
     * Only the specified params are going to be removed, and the non specified ones are going to be ignored.
     * @see [addSpanStyle]
     * @see [toggleSpanStyle]
     */
    fun removeSpanStyle(spanStyle: SpanStyle) {
        if (currentSpanStyle.isSpecifiedFieldsEquals(spanStyle)) {
            toRemoveSpanStyle = toRemoveSpanStyle.customMerge(spanStyle)
            toAddSpanStyle = toAddSpanStyle.unmerge(spanStyle)
        }

        if (!selection.collapsed)
            handleRemovingStyleFromSelectedText()
    }

    fun addLink(
        text: String,
        url: String,
    ) {
        if (text.isEmpty()) return

        val paragraph = richParagraphList.firstOrNull() ?: return
        val linkStyle = RichSpanStyle.Link(
            url = url,
        )
        val linkRichSpan = RichSpan(
            text = text,
            style = linkStyle,
            paragraph = paragraph,
        )

        addRichSpanAtPosition(
            linkRichSpan,
            index = selection.min,
        )

        val beforeText = textFieldValue.text.substring(0, selection.min)
        val afterText = textFieldValue.text.substring(selection.min)
        val newText = "$beforeText$text$afterText"
        updateTextFieldValue(
            newTextFieldValue = textFieldValue.copy(
                text = newText,
                selection = TextRange(selection.min + text.length),
            )
        )
    }

    fun toggleCode() {
        if (isCode)
            removeCode()
        else
            addCode()
    }

    fun addCode() {
        if (toRemoveRichSpanStyle is RichSpanStyle.Code)
            toRemoveRichSpanStyle = RichSpanStyle.Default
        toAddRichSpanStyle = RichSpanStyle.Code()

        if (!selection.collapsed)
            handleAddingStyleToSelectedText()
    }

    fun removeCode() {
        if (toAddRichSpanStyle is RichSpanStyle.Code)
            toAddRichSpanStyle = RichSpanStyle.Default
        toRemoveRichSpanStyle = RichSpanStyle.Code()

        if (!selection.collapsed)
            handleAddingStyleToSelectedText()
    }

    /**
     * Toggle the [ParagraphStyle]
     * If the passed paragraph style doesn't exist in the [currentParagraphStyle] it's going to be added.
     * If the passed paragraph style already exists in the [currentParagraphStyle] it's going to be removed.
     *
     * Example: You can toggle TextAlign Center by passing:
     *
     * ```
     * ParagraphStyle(textAlign = TextAlign.Center)
     * ```
     *
     * @param paragraphStyle the paragraph style that is going to be toggled.
     * Only the specified params are going to be toggled, and the non specified ones are going to be ignored.
     * @see [addParagraphStyle]
     * @see [removeParagraphStyle]
     */
    fun toggleParagraphStyle(paragraphStyle: ParagraphStyle) {
        if (currentParagraphStyle.isSpecifiedFieldsEquals(paragraphStyle))
            removeParagraphStyle(paragraphStyle)
        else
            addParagraphStyle(paragraphStyle)
    }

    /**
     * Add new [ParagraphStyle] to the [currentParagraphStyle]
     *
     * Example: You can add TextAlign Center by passing:
     *
     * ```
     * ParagraphStyle(textAlign = TextAlign.Center)
     * ```
     *
     * @param paragraphStyle the paragraph style that is going to be added to the [currentParagraphStyle].
     * Only the specified params are going to be applied, and the non specified ones are going to be ignored.
     * @see [removeParagraphStyle]
     * @see [toggleParagraphStyle]
     */
    fun addParagraphStyle(paragraphStyle: ParagraphStyle) {
        if (!currentParagraphStyle.isSpecifiedFieldsEquals(paragraphStyle)) {
            // If the selection is collapsed, we add the paragraph style to the paragraph containing the selection
            if (selection.collapsed) {
                val paragraph = getRichParagraphByTextIndex(selection.min - 1) ?: return
                paragraph.paragraphStyle = paragraph.paragraphStyle.merge(paragraphStyle)
            }
            // If the selection is not collapsed, we add the paragraph style to all the paragraphs in the selection
            else {
                val paragraphs = getRichParagraphListByTextRange(selection)
                if (paragraphs.isEmpty()) return
                paragraphs.fastForEach { it.paragraphStyle = it.paragraphStyle.merge(paragraphStyle) }
            }
            // We update the annotated string to reflect the changes
            updateAnnotatedString()
            // We update the current paragraph style to reflect the changes
            updateCurrentParagraphStyle()
        }
    }

    /**
     * Remove an existing [ParagraphStyle] from the [currentParagraphStyle]
     *
     * Example: You can remove TextAlign Center by passing:
     *
     * ```
     * ParagraphStyle(textAlign = TextAlign.Center)
     * ```
     *
     * @param paragraphStyle the paragraph style that is going to be removed from the [currentParagraphStyle].
     * Only the specified params are going to be removed, and the non specified ones are going to be ignored.
     * @see [addParagraphStyle]
     * @see [toggleParagraphStyle]
     */
    fun removeParagraphStyle(paragraphStyle: ParagraphStyle) {
        if (currentParagraphStyle.isSpecifiedFieldsEquals(paragraphStyle)) {
            // If the selection is collapsed, we remove the paragraph style from the paragraph containing the selection
            if (selection.collapsed) {
                val paragraph = getRichParagraphByTextIndex(selection.min - 1) ?: return
                paragraph.paragraphStyle = paragraph.paragraphStyle.unmerge(paragraphStyle)
            }
            // If the selection is not collapsed, we remove the paragraph style from all the paragraphs in the selection
            else {
                val paragraphs = getRichParagraphListByTextRange(selection)
                if (paragraphs.isEmpty()) return
                paragraphs.fastForEach { it.paragraphStyle = it.paragraphStyle.unmerge(paragraphStyle) }
            }
            // We update the annotated string to reflect the changes
            updateAnnotatedString()
            // We update the current paragraph style to reflect the changes
            updateCurrentParagraphStyle()
        }
    }

    fun toggleUnorderedList() {
        val paragraph = getRichParagraphByTextIndex(selection.min - 1) ?: return
        if (paragraph.type is RichParagraph.Type.UnorderedList) removeUnorderedList()
        else addUnorderedList()
    }

    fun addUnorderedList() {
        val paragraph = getRichParagraphByTextIndex(selection.min - 1) ?: return

        if (paragraph.type is RichParagraph.Type.UnorderedList) return

        val newType = RichParagraph.Type.UnorderedList()

        updateParagraphType(
            paragraph = paragraph,
            newType = newType
        )
    }

    fun removeUnorderedList() {
        val paragraph = getRichParagraphByTextIndex(selection.min - 1) ?: return
        if (paragraph.type !is RichParagraph.Type.UnorderedList) return

        resetParagraphType(paragraph = paragraph)
    }

    fun toggleOrderedList() {
        val paragraph = getRichParagraphByTextIndex(selection.min - 1) ?: return
        if (paragraph.type is RichParagraph.Type.OrderedList) removeOrderedList()
        else addOrderedList()
    }

    fun addOrderedList() {
        val paragraph = getRichParagraphByTextIndex(selection.min - 1) ?: return
        if (paragraph.type is RichParagraph.Type.OrderedList) return
        val index = richParagraphList.indexOf(paragraph)
        if (index == -1) return
        val previousParagraphType = richParagraphList.getOrNull(index - 1)?.type
        val orderedListNumber =
            if (previousParagraphType is RichParagraph.Type.OrderedList)
                previousParagraphType.number + 1
            else 1

        adjustOrderedListsNumbers(
            startParagraphIndex = index + 1,
            startNumber = orderedListNumber + 1,
        )

        val newType = RichParagraph.Type.OrderedList(number = orderedListNumber)
        updateParagraphType(
            paragraph = paragraph,
            newType = newType
        )
    }

    fun removeOrderedList() {
        val paragraph = getRichParagraphByTextIndex(selection.min - 1) ?: return
        if (paragraph.type !is RichParagraph.Type.OrderedList) return
        val index = richParagraphList.indexOf(paragraph)
        if (index == -1) return

        for (i in (index + 1) .. richParagraphList.lastIndex) {
            val currentParagraphType = richParagraphList[i].type
            if (currentParagraphType !is RichParagraph.Type.OrderedList) break
            richParagraphList[i].type = RichParagraph.Type.OrderedList(number = i - index)
        }

        resetParagraphType(paragraph = paragraph)
    }

    private fun updateParagraphType(paragraph: RichParagraph, newType: RichParagraph.Type) {
        val paragraphOldStartTextLength = paragraph.type.startText.length
        val paragraphFirstChildStartIndex = paragraph.getFirstNonEmptyChild()?.textRange?.min ?: selection.min

        paragraph.type = newType

        val beforeText = textFieldValue.text.substring(0, paragraphFirstChildStartIndex - paragraphOldStartTextLength)
        val afterText = textFieldValue.text.substring(paragraphFirstChildStartIndex)
        val newSelectionMin =
            if (selection.min > paragraphFirstChildStartIndex) selection.min + newType.startText.length - paragraphOldStartTextLength
            else paragraphFirstChildStartIndex + newType.startText.length - paragraphOldStartTextLength
        val newSelectionMax =
            if (selection.max > paragraphFirstChildStartIndex) selection.max + newType.startText.length - paragraphOldStartTextLength
            else paragraphFirstChildStartIndex + newType.startText.length - paragraphOldStartTextLength
        updateTextFieldValue(
            newTextFieldValue = textFieldValue.copy(
                text = beforeText + newType.startText + afterText,
                selection = TextRange(
                    newSelectionMin,
                    newSelectionMax,
                ),
            )
        )
    }

    private fun resetParagraphType(paragraph: RichParagraph) {
        val paragraphStartIndex = paragraph.getFirstNonEmptyChild()?.textRange?.min ?: selection.min

        val oldType = paragraph.type
        paragraph.type = RichParagraph.Type.Default

        val beforeText = textFieldValue.text.substring(0, paragraphStartIndex - oldType.startText.length)
        val afterText = textFieldValue.text.substring(paragraphStartIndex)
        val newSelectionMin =
            if (selection.min > paragraphStartIndex) selection.min - oldType.startText.length
            else paragraphStartIndex - oldType.startText.length
        val newSelectionMax =
            if (selection.max > paragraphStartIndex) selection.max - oldType.startText.length
            else paragraphStartIndex - oldType.startText.length
        updateTextFieldValue(
            newTextFieldValue = textFieldValue.copy(
                text = beforeText + afterText,
                selection = TextRange(
                    newSelectionMin,
                    newSelectionMax
                ),
            )
        )
    }

    /**
     * Temporarily stores the new text field value, before it is validated.
     */
    private var tempTextFieldValue = textFieldValue

    /**
     * Handles the new text field value.
     *
     * @param newTextFieldValue the new text field value.
     */
    internal fun onTextFieldValueChange(newTextFieldValue: TextFieldValue) {
        tempTextFieldValue = newTextFieldValue

        if (tempTextFieldValue.text.length > textFieldValue.text.length)
            handleAddingCharacters()
        else if (tempTextFieldValue.text.length < textFieldValue.text.length)
            handleRemovingCharacters()
        else if (
            tempTextFieldValue.text == textFieldValue.text &&
            tempTextFieldValue.selection != textFieldValue.selection
        ) {
            val lastPressPosition = this.lastPressPosition
            if (lastPressPosition != null) {
                adjustSelection(lastPressPosition, newTextFieldValue.selection)
                return
            }
        }

        // Update text field value
        updateTextFieldValue()
    }

    /**
     * Handles updating the text field value and all the related states such as the [annotatedString] and [visualTransformation] to reflect the new text field value.
     *
     * @param newTextFieldValue the new text field value.
     */
    private fun updateTextFieldValue(newTextFieldValue: TextFieldValue = tempTextFieldValue) {
        tempTextFieldValue = newTextFieldValue

        if (!singleParagraphMode) {
            // Check for paragraphs
            checkForParagraphs()
        }

        if (
            tempTextFieldValue.text == textFieldValue.text &&
            tempTextFieldValue.selection != textFieldValue.selection
        ) {
            // Update selection
            textFieldValue = tempTextFieldValue
        } else {
            // Update the annotatedString and the textFieldValue with the new values
            updateAnnotatedString(tempTextFieldValue)
        }

        // Clear un-applied styles
        toAddSpanStyle = SpanStyle()
        toRemoveSpanStyle = SpanStyle()
        toAddRichSpanStyle = RichSpanStyle.Default
        toRemoveRichSpanStyle = RichSpanStyle.Default

        // Update current span style
        updateCurrentSpanStyle()

        // Update current paragraph style
        updateCurrentParagraphStyle()

        // Clear [tempTextFieldValue]
        tempTextFieldValue = TextFieldValue()
    }

    /**
     * Update the [annotatedString] to reflect the new changes on the [richParagraphList].
     * This method will update the [annotatedString] and the [textFieldValue] to reflect the new changes.
     * If no [newTextFieldValue] is passed, the [textFieldValue] will be used instead.
     *
     * @param newTextFieldValue the new text field value.
     * @see [textFieldValue]
     * @see [annotatedString]
     */
    private fun updateAnnotatedString(newTextFieldValue: TextFieldValue = textFieldValue) {
        val newText =
            if (singleParagraphMode) newTextFieldValue.text
            else newTextFieldValue.text.replace("\n", " ")

        val newStyledRichSpanList = mutableListOf<RichSpan>()
        annotatedString = buildAnnotatedString {
            var index = 0
            richParagraphList.fastForEachIndexed { i, richParagraph ->
                if (index > newText.length) {
                    richParagraphList.removeAt(i)
                    return@fastForEachIndexed
                }

                withStyle(richParagraph.paragraphStyle.merge(richParagraph.type.style)) {
                    append(richParagraph.type.startText)
                    val richParagraphStartTextLength = richParagraph.type.startText.length
                    richParagraph.type.startRichSpan.textRange = TextRange(index, index + richParagraphStartTextLength)
                    index += richParagraphStartTextLength
                    withStyle(RichSpanStyle.DefaultSpanStyle) {
                        index = append(
                            richSpanList = richParagraph.children,
                            startIndex = index,
                            text = newText,
                            selection = newTextFieldValue.selection,
                            onStyledRichSpan = {
                                newStyledRichSpanList.add(it)
                            },
                            richTextConfig = richTextConfig,
                        )
                        if (!singleParagraphMode) {
                            if (i != richParagraphList.lastIndex) {
                                if (index < newText.length) {
                                    append(" ")
                                    index++
                                }
                            }
                        }
                    }
                }
            }
        }
        styledRichSpanList.clear()
        textFieldValue = newTextFieldValue.copy(text = newText)
        visualTransformation = VisualTransformation { _ ->
            TransformedText(
                annotatedString,
                OffsetMapping.Identity
            )
        }
        styledRichSpanList.addAll(newStyledRichSpanList)
    }

    /**
     * Handles adding characters to the text field.
     * This method will update the [richParagraphList] to reflect the new changes.
     * This method will use the [tempTextFieldValue] to get the new characters.
     */
    private fun handleAddingCharacters() {
        val typedCharsCount = tempTextFieldValue.text.length - textFieldValue.text.length
        var startTypeIndex = tempTextFieldValue.selection.min - typedCharsCount
        val typedText = tempTextFieldValue.text.substring(
            startIndex = startTypeIndex,
            endIndex = startTypeIndex + typedCharsCount,
        )
        val previousIndex = startTypeIndex - 1

        val activeRichSpan = getRichSpanByTextIndex(previousIndex)

        if (activeRichSpan != null) {
            if (startTypeIndex < activeRichSpan.textRange.min) {
                val indexDiff = activeRichSpan.textRange.min - startTypeIndex
                val beforeTypedText = tempTextFieldValue.text.substring(
                    startIndex = 0,
                    endIndex = startTypeIndex,
                )
                val paragraphStartText = tempTextFieldValue.text.substring(
                    startIndex = startTypeIndex + typedCharsCount,
                    endIndex = activeRichSpan.textRange.min + typedCharsCount,
                )
                val afterTypedText = tempTextFieldValue.text.substring(
                    startIndex = activeRichSpan.textRange.min + typedCharsCount,
                    endIndex = tempTextFieldValue.text.length,
                )
                val newTypedText = beforeTypedText + paragraphStartText + typedText + afterTypedText
                tempTextFieldValue = tempTextFieldValue.copy(
                    text = newTypedText,
                    selection = TextRange(tempTextFieldValue.selection.min + indexDiff),
                )
            }

            startTypeIndex = max(startTypeIndex, activeRichSpan.textRange.min)
            val startIndex = max(0, startTypeIndex - activeRichSpan.textRange.min)
            val beforeText = activeRichSpan.text.substring(0, startIndex)
            val afterText = activeRichSpan.text.substring(startIndex)

            val activeRichSpanFullSpanStyle = activeRichSpan.fullSpanStyle
            val newSpanStyle = activeRichSpanFullSpanStyle.customMerge(toAddSpanStyle).unmerge(toRemoveSpanStyle)
            val newRichSpanStyle =
                if (toAddRichSpanStyle !is RichSpanStyle.Default) toAddRichSpanStyle
                else if (toRemoveRichSpanStyle::class == activeRichSpan.style::class) RichSpanStyle.Default
                else activeRichSpan.style

            if (
                (
                    toAddSpanStyle == SpanStyle() && toRemoveSpanStyle == SpanStyle() &&
                    toAddRichSpanStyle is RichSpanStyle.Default && toRemoveRichSpanStyle::class != activeRichSpan.style::class
                ) || (newSpanStyle == activeRichSpanFullSpanStyle && newRichSpanStyle::class == activeRichSpan.style::class)
            ) {
                activeRichSpan.text = beforeText + typedText + afterText
            } else {
                handleUpdatingRichSpan(
                    richSpan = activeRichSpan,
                    beforeText = beforeText,
                    middleText = typedText,
                    afterText = afterText,
                    startIndex = startTypeIndex,
                    richSpanFullSpanStyle = activeRichSpanFullSpanStyle,
                    newSpanStyle = newSpanStyle,
                )
            }
        } else {
            if (richParagraphList.isEmpty()) {
                richParagraphList.add(RichParagraph())
            }

            val newRichSpan = RichSpan(
                paragraph = richParagraphList.last(),
                text = typedText,
                textRange = TextRange(startTypeIndex, startTypeIndex + typedText.length),
                spanStyle = toAddSpanStyle,
            )
            richParagraphList.last().children.add(newRichSpan)
        }
    }

    /**
     * Handles removing characters from the text field value.
     * This method will update the [richParagraphList] to reflect the new changes.
     * This method will use the [tempTextFieldValue] to get the removed characters.
     */
    private fun handleRemovingCharacters() {
        val removedCharsCount = textFieldValue.text.length - tempTextFieldValue.text.length
        val minRemoveIndex = tempTextFieldValue.selection.min
        val maxRemoveIndex = tempTextFieldValue.selection.min + removedCharsCount
        val removeRange = TextRange(minRemoveIndex, maxRemoveIndex)

        val minRichSpan = getRichSpanByTextIndex(textIndex = minRemoveIndex, true) ?: return
        val maxRichSpan = getRichSpanByTextIndex(textIndex = maxRemoveIndex - 1, true) ?: return

        // Check deleted paragraphs
        val minParagraphIndex = richParagraphList.indexOf(minRichSpan.paragraph)
        val maxParagraphIndex = richParagraphList.indexOf(maxRichSpan.paragraph)
        if (minParagraphIndex < maxParagraphIndex - 1 && !singleParagraphMode)
            richParagraphList.removeRange(minParagraphIndex + 1, maxParagraphIndex)

        // Get the first non-empty child of the min paragraph
        val minFirstNonEmptyChild = minRichSpan.paragraph.getFirstNonEmptyChild()
        val minParagraphStartTextLength = minRichSpan.paragraph.type.startRichSpan.text.length
        val minParagraphFirstChildMinIndex = minFirstNonEmptyChild?.textRange?.min ?: minParagraphStartTextLength

        // Get the first non-empty child of the max paragraph
        val maxFirstNonEmptyChild = maxRichSpan.paragraph.getFirstNonEmptyChild()
        val maxParagraphStartTextLength = maxRichSpan.paragraph.type.startRichSpan.text.length
        val maxParagraphFirstChildMinIndex = maxFirstNonEmptyChild?.textRange?.min ?: maxParagraphStartTextLength

        if (minParagraphIndex == maxParagraphIndex && !singleParagraphMode) {
            if (minFirstNonEmptyChild == null || minFirstNonEmptyChild.text.isEmpty()) {
                if (minRichSpan.paragraph.type.startText.isEmpty()) {
                    // Remove the min paragraph if it's empty (and the max paragraph is the same)
                    richParagraphList.removeAt(minParagraphIndex)
                }
            }
        }

        // Handle Remove the min paragraph custom text
        if (minRemoveIndex < minParagraphFirstChildMinIndex) {
            handleRemoveMinParagraphStartText(
                removeIndex = minRemoveIndex,
                paragraphStartTextLength = minParagraphStartTextLength,
                paragraphFirstChildMinIndex = minParagraphFirstChildMinIndex,
            )

            minRichSpan.paragraph.type = RichParagraph.Type.Default
        }

        // Handle Remove the max paragraph custom text
        if (maxRemoveIndex < maxParagraphFirstChildMinIndex) {
            handleRemoveMaxParagraphStartText(
                minRemoveIndex = minRemoveIndex,
                maxRemoveIndex = maxRemoveIndex,
                paragraphStartTextLength = maxParagraphStartTextLength,
                paragraphFirstChildMinIndex = maxParagraphFirstChildMinIndex,
            )

            maxRichSpan.paragraph.type = RichParagraph.Type.Default
        }

        // Remove spans from the max paragraph
        maxRichSpan.paragraph.removeTextRange(removeRange, maxParagraphFirstChildMinIndex)

        if (!singleParagraphMode) {
            if (maxParagraphIndex != minParagraphIndex) {
                // Remove spans from the min paragraph
                minRichSpan.paragraph.removeTextRange(removeRange, minParagraphFirstChildMinIndex)

                if (maxRichSpan.paragraph.getFirstNonEmptyChild() == null) {
                    // Remove the max paragraph if it's empty
                    richParagraphList.remove(maxRichSpan.paragraph)
                } else if (minRichSpan.paragraph.getFirstNonEmptyChild() == null) {
                    // Set the min paragraph type to the max paragraph type
                    // Since the max paragraph is going to take the min paragraph's place
                    maxRichSpan.paragraph.type = minRichSpan.paragraph.type

                    // Remove the min paragraph if it's empty
                    richParagraphList.remove(minRichSpan.paragraph)
                } else {
                    // Merge the two paragraphs if they are not empty
                    mergeTwoRichParagraphs(
                        firstParagraph = minRichSpan.paragraph,
                        secondParagraph = maxRichSpan.paragraph,
                    )
                }
            }

            if (
                minRemoveIndex == minParagraphFirstChildMinIndex - minParagraphStartTextLength - 1
            ) {
                if (
                    minRemoveIndex == minParagraphFirstChildMinIndex - minParagraphStartTextLength - 1 &&
                    minParagraphStartTextLength > 0
                ) {
                    val beforeText = tempTextFieldValue.text.substring(
                        startIndex = 0,
                        endIndex = minRemoveIndex,
                    )
                    val afterText = tempTextFieldValue.text.substring(
                        startIndex = minRemoveIndex + 1,
                        endIndex = tempTextFieldValue.text.length,
                    )
                    tempTextFieldValue = tempTextFieldValue.copy(
                        text = beforeText + afterText,
                        selection = TextRange(tempTextFieldValue.selection.min - 1),
                    )
                }

                richParagraphList.getOrNull(minParagraphIndex - 1)?.let { previousParagraph ->
                    // Merge the two paragraphs if the line break is removed
                    mergeTwoRichParagraphs(
                        firstParagraph = previousParagraph,
                        secondParagraph = maxRichSpan.paragraph,
                    )
                }
            }
        }

        checkOrderedListsNumbers(
            startParagraphIndex = minParagraphIndex - 1,
            endParagraphIndex = minParagraphIndex + 1,
        )
    }

    private fun handleRemoveMinParagraphStartText(
        removeIndex: Int,
        paragraphStartTextLength: Int,
        paragraphFirstChildMinIndex: Int,
    ) {
        if (removeIndex < paragraphFirstChildMinIndex && paragraphStartTextLength > 0) {
            val indexDiff = paragraphStartTextLength - (paragraphFirstChildMinIndex - removeIndex)
            val beforeTextEndIndex = paragraphFirstChildMinIndex - paragraphStartTextLength

            val beforeText =
                if (beforeTextEndIndex <= 0)
                    ""
                else
                    tempTextFieldValue.text.substring(
                        startIndex = 0,
                        endIndex = beforeTextEndIndex,
                    )
            val afterText =
                if (tempTextFieldValue.text.length <= removeIndex)
                    ""
                else
                    tempTextFieldValue.text.substring(
                        startIndex = removeIndex,
                        endIndex = tempTextFieldValue.text.length,
                    )
            val newText = beforeText + afterText
            val newSelection = TextRange(removeIndex - indexDiff)

            tempTextFieldValue = tempTextFieldValue.copy(
                text = newText,
                selection = newSelection,
            )
        }
    }

    private fun handleRemoveMaxParagraphStartText(
        minRemoveIndex: Int,
        maxRemoveIndex: Int,
        paragraphStartTextLength: Int,
        paragraphFirstChildMinIndex: Int,
    ) {
        if (maxRemoveIndex < paragraphFirstChildMinIndex && paragraphStartTextLength > 0) {
            paragraphStartTextLength - (paragraphFirstChildMinIndex - maxRemoveIndex)

            val beforeText =
                if (minRemoveIndex <= 0)
                    ""
                else
                    tempTextFieldValue.text.substring(
                        startIndex = 0,
                        endIndex = minRemoveIndex,
                    )

            val afterTextStartIndex = minRemoveIndex + (paragraphFirstChildMinIndex - maxRemoveIndex)

            val afterText =
                if (tempTextFieldValue.text.length <= afterTextStartIndex)
                    ""
                else
                    tempTextFieldValue.text.substring(
                        startIndex = afterTextStartIndex,
                        endIndex = tempTextFieldValue.text.length,
                    )
            val newText = beforeText + afterText

            tempTextFieldValue = tempTextFieldValue.copy(
                text = newText,
            )
        }
    }

    private fun adjustOrderedListsNumbers(
        startParagraphIndex: Int,
        startNumber: Int,
    ) {
        var number = startNumber
        // Update the paragraph type of the paragraphs after the new paragraph
        for (i in (startParagraphIndex)..richParagraphList.lastIndex) {
            val currentParagraph = richParagraphList[i]
            val currentParagraphType = currentParagraph.type
            if (currentParagraphType is RichParagraph.Type.OrderedList) {
                currentParagraph.type = RichParagraph.Type.OrderedList(number = number)
            } else break
            number++
        }
    }

    private fun checkOrderedListsNumbers(
        startParagraphIndex: Int,
        endParagraphIndex: Int,
    ) {
        var number = 1
        val startParagraph = richParagraphList.getOrNull(startParagraphIndex)
        val startParagraphType = startParagraph?.type
        if (startParagraphType is RichParagraph.Type.OrderedList) {
            number = startParagraphType.number + 1
        }
        // Update the paragraph type of the paragraphs after the new paragraph
        for (i in (startParagraphIndex + 1)..richParagraphList.lastIndex) {
            val currentParagraph = richParagraphList[i]
            val currentParagraphType = currentParagraph.type
            if (currentParagraphType is RichParagraph.Type.OrderedList) {
                currentParagraph.type = RichParagraph.Type.OrderedList(number = number)
                number++
            }
            else if (i >= endParagraphIndex) break
            else number = 1
        }
    }

    private fun checkForParagraphs() {
        var index = tempTextFieldValue.text.lastIndex

        while (true) {
            // Search for the next paragraph
            index = tempTextFieldValue.text.lastIndexOf('\n', index)

            // If there are no more paragraphs, break
            if (index < textFieldValue.selection.min) break

            // Get the rich span style at the index to split it between two paragraphs
            val richSpan = getRichSpanByTextIndex(index)

            // If there is no rich span style at the index, continue (this should not happen)
            if (richSpan == null) {
                index--
                continue
            }

            // Get the paragraph style index of the rich span style
            val paragraphIndex = richParagraphList.indexOf(richSpan.paragraph)
            // If the paragraph index is -1, continue (this should not happen)
            if (paragraphIndex == -1) {
                index--
                continue
            }

            // Make sure the index is not less than the minimum text range of the rich span style
            // This is to make sure that the index is not in paragraph custom start text
            val sliceIndex = max(index, richSpan.textRange.min)

            // Create a new paragraph style
            val newParagraph = richSpan.paragraph.slice(
                startIndex = sliceIndex,
                richSpan = richSpan,
            )

            // Get the text before and after the slice index
            val beforeText = tempTextFieldValue.text.substring(0, sliceIndex + 1)
            val afterText = tempTextFieldValue.text.substring(sliceIndex + 1)

            // Update the text field value to include the new paragraph custom start text
            tempTextFieldValue = tempTextFieldValue.copy(
                text = beforeText + newParagraph.type.startText + afterText,
                selection = TextRange(
                    start = tempTextFieldValue.selection.start + newParagraph.type.startText.length,
                    end = tempTextFieldValue.selection.end + newParagraph.type.startText.length,
                ),
            )

            // Add the new paragraph
            richParagraphList.add(paragraphIndex + 1, newParagraph)

            // Update the paragraph type of the paragraphs after the new paragraph
            val newParagraphType = newParagraph.type
            if (newParagraphType is RichParagraph.Type.OrderedList) {
                adjustOrderedListsNumbers(
                    startParagraphIndex = paragraphIndex + 2,
                    startNumber = newParagraphType.number + 1,
                )
            }

            // Remove one from the index to continue searching for paragraphs
            index--
        }
    }

    /**
     * Handles adding the style in [toAddSpanStyle] to the selected text.
     */
    private fun handleAddingStyleToSelectedText() {
        val richSpanList = getRichSpanListByTextRange(selection)

        val startSelectionIndex = selection.min
        val endSelectionIndex = selection.max

        for (i in richSpanList.lastIndex downTo 0) {
            val richSpan = richSpanList[i]

            val beforeText = if (startSelectionIndex in richSpan.textRange)
                richSpan.text.substring(0, startSelectionIndex - richSpan.textRange.start)
            else ""
            val middleText = richSpan.text.substring(
                maxOf(startSelectionIndex - richSpan.textRange.start, 0),
                minOf(endSelectionIndex - richSpan.textRange.start, richSpan.text.length)
            )
            val afterText = if ((endSelectionIndex - 1) in richSpan.textRange)
                richSpan.text.substring(endSelectionIndex - richSpan.textRange.start)
            else ""

            val richSpanFullSpanStyle = richSpan.fullSpanStyle
            val newSpanStyle = richSpanFullSpanStyle.customMerge(toAddSpanStyle).unmerge(toRemoveSpanStyle)

            val startApplyStyleIndex = maxOf(startSelectionIndex, richSpan.textRange.start)

            handleUpdatingRichSpan(
                richSpan = richSpan,
                startIndex = startApplyStyleIndex,
                beforeText = beforeText,
                middleText = middleText,
                afterText = afterText,
                newSpanStyle = newSpanStyle,

            )
        }

        updateTextFieldValue(textFieldValue)
    }

    /**
     * Handles removing the style in [toRemoveSpanStyle] from the selected text.
     */
    private fun handleRemovingStyleFromSelectedText() {
        // Get the rich span list of the selected text
        val selectedRichSpanList = getRichSpanListByTextRange(selection)

        val startSelectionIndex = selection.min
        val endSelectionIndex = selection.max

        // Loop through the rich span list
        for (i in selectedRichSpanList.lastIndex downTo 0) {
            val richSpan = selectedRichSpanList[i]

            // Get the text before, during, and after the selected text
            val beforeText = if (startSelectionIndex in richSpan.textRange)
                richSpan.text.substring(0, startSelectionIndex - richSpan.textRange.start)
            else ""
            val middleText = richSpan.text.substring(
                maxOf(startSelectionIndex - richSpan.textRange.start, 0),
                minOf(endSelectionIndex - richSpan.textRange.start, richSpan.text.length)
            )
            val afterText = if ((endSelectionIndex - 1) in richSpan.textRange)
                richSpan.text.substring(endSelectionIndex - richSpan.textRange.start)
            else ""

            val richSpanFullSpanStyle = richSpan.fullSpanStyle
            val newSpanStyle = richSpanFullSpanStyle.customMerge(toAddSpanStyle).unmerge(toRemoveSpanStyle)

            val startApplyStyleIndex = maxOf(startSelectionIndex, richSpan.textRange.start)

            handleUpdatingRichSpan(
                richSpan = richSpan,
                startIndex = startApplyStyleIndex,
                beforeText = beforeText,
                middleText = middleText,
                afterText = afterText,
                newSpanStyle = newSpanStyle,
            )
        }

        updateTextFieldValue(textFieldValue)
    }

    /**
     * Apply [toAddSpanStyle] and [toRemoveSpanStyle] to a [RichSpan].
     *
     * @param richSpan The [RichSpan] to apply the styles to.
     * @param beforeText The text before applying the styles.
     * @param middleText The text to apply the styles to.
     * @param afterText The text after applying the styles.
     * @param startIndex The start index of the text to apply the styles to.
     * @param richSpanFullSpanStyle The [SpanStyle] of the [RichSpan].
     * @param newSpanStyle The new [SpanStyle] to apply to the [RichSpan].
     */
    private fun handleUpdatingRichSpan(
        richSpan: RichSpan,
        beforeText: String,
        middleText: String,
        afterText: String,
        startIndex: Int,
        richSpanFullSpanStyle: SpanStyle = richSpan.fullSpanStyle,
        newSpanStyle: SpanStyle = richSpanFullSpanStyle.customMerge(toAddSpanStyle).unmerge(toRemoveSpanStyle),
        newRichSpanStyle: RichSpanStyle =
            if (toAddRichSpanStyle !is RichSpanStyle.Default) toAddRichSpanStyle
            else if (toRemoveRichSpanStyle::class == richSpan.style::class) RichSpanStyle.Default
            else richSpan.style,
    ) {
        if (richSpanFullSpanStyle == newSpanStyle && newRichSpanStyle::class == richSpan.style::class) return

        if (
            (toRemoveSpanStyle == SpanStyle() ||
            !richSpanFullSpanStyle.isSpecifiedFieldsEquals(toRemoveSpanStyle)) &&
            (toRemoveRichSpanStyle is RichSpanStyle.Default || newRichSpanStyle::class == richSpan.style::class)
        ) {
            handleApplyingStyleToRichSpan(
                richSpan = richSpan,
                beforeText = beforeText,
                middleText = middleText,
                afterText = afterText,
                startIndex = startIndex,
            )
        } else {
            handleRemovingStyleFromRichSpan(
                richSpan = richSpan,
                beforeText = beforeText,
                middleText = middleText,
                afterText = afterText,
                startIndex = startIndex,
                richSpanFullSpanStyle = richSpanFullSpanStyle,
                newSpanStyle = newSpanStyle,
                newRichSpanStyle = newRichSpanStyle,
            )
        }
    }

    /**
     * Handles applying a new [SpanStyle] to a [RichSpan].
     *
     * @param richSpan The [RichSpan] to apply the new [SpanStyle] to.
     * @param beforeText The text before applying the styles.
     * @param middleText The text to apply the styles to.
     * @param afterText The text after applying the styles.
     * @param startIndex The start index of the text to apply the styles to.
     */
    private fun handleApplyingStyleToRichSpan(
        richSpan: RichSpan,
        beforeText: String,
        middleText: String,
        afterText: String,
        startIndex: Int,
    ) {
        val fullSpanStyle = richSpan.fullSpanStyle

        // Simplify the richSpan tree if possible, by avoiding creating a new RichSpan.
        if (
            beforeText.isEmpty() &&
            afterText.isEmpty() &&
            richSpan.children.isEmpty()
        ) {
            richSpan.text = middleText
            richSpan.spanStyle = richSpan.spanStyle.copy(
                textDecoration = fullSpanStyle.textDecoration
            ).customMerge(toAddSpanStyle)
            richSpan.style = toAddRichSpanStyle
            return
        }

        richSpan.text = beforeText
        val newRichSpan = RichSpan(
            paragraph = richSpan.paragraph,
            parent = richSpan,
            text = middleText,
            textRange = TextRange(
                startIndex,
                startIndex + middleText.length
            ),
            spanStyle = SpanStyle(
                textDecoration = fullSpanStyle.textDecoration
            ).customMerge(toAddSpanStyle),
            style = toAddRichSpanStyle,
        )

        if (middleText.isNotEmpty()) {
            richSpan.children.add(
                0,
                newRichSpan
            )
        }
        if (afterText.isNotEmpty()) {
            richSpan.children.add(
                1,
                RichSpan(
                    paragraph = richSpan.paragraph,
                    parent = richSpan,
                    text = afterText,
                    textRange = TextRange(
                        startIndex + middleText.length,
                        startIndex + middleText.length + afterText.length
                    ),
                )
            )
        } else {
            val firstRichSpan = richSpan.children.firstOrNull()
            val secondRichSpan = richSpan.children.getOrNull(1)
            if (
                firstRichSpan != null &&
                secondRichSpan != null &&
                firstRichSpan.spanStyle == secondRichSpan.spanStyle
            ) {
                firstRichSpan.text += secondRichSpan.text
                firstRichSpan.children.addAll(secondRichSpan.children)
                richSpan.children.removeAt(1)
            }

            if (
                firstRichSpan != null &&
                richSpan.text.isEmpty() &&
                richSpan.children.size == 1
            ) {
                richSpan.text = firstRichSpan.text
                richSpan.spanStyle = richSpan.spanStyle.customMerge(firstRichSpan.spanStyle)
                richSpan.children.clear()
                richSpan.children.addAll(firstRichSpan.children)
            }
        }
    }

    /**
     * Handles removing a [SpanStyle] from a [RichSpan].
     *
     * @param richSpan The [RichSpan] to remove the [SpanStyle] from.
     * @param beforeText The text before removing the styles.
     * @param middleText The text to remove the styles from.
     * @param afterText The text after removing the styles.
     * @param startIndex The start index of the text to remove the styles from.
     * @param richSpanFullSpanStyle The [SpanStyle] of the [RichSpan].
     * @param newSpanStyle The new [SpanStyle] to apply to the [RichSpan].
     */
    private fun handleRemovingStyleFromRichSpan(
        richSpan: RichSpan,
        beforeText: String,
        middleText: String,
        afterText: String,
        startIndex: Int,
        richSpanFullSpanStyle: SpanStyle,
        newSpanStyle: SpanStyle,
        newRichSpanStyle: RichSpanStyle,
    ) {
        richSpan.text = beforeText
        val parentRichSpan = richSpan.getClosestRichSpan(newSpanStyle, newRichSpanStyle)
        val newRichSpan = RichSpan(
            paragraph = richSpan.paragraph,
            parent = parentRichSpan,
            text = middleText,
            textRange = TextRange(
                startIndex,
                startIndex + middleText.length
            ),
            spanStyle = newSpanStyle.unmerge(parentRichSpan?.spanStyle),
            style = newRichSpanStyle,
        )
        val afterRichSpan = RichSpan(
            paragraph = richSpan.paragraph,
            parent = parentRichSpan,
            text = afterText,
            textRange = TextRange(
                startIndex + middleText.length,
                startIndex + middleText.length + afterText.length
            ),
            spanStyle = richSpanFullSpanStyle,
            style = richSpan.style,
        )

        val toShiftRichSpanList: MutableList<RichSpan> = mutableListOf()
        var previousRichSpan: RichSpan?
        var currentRichSpan: RichSpan? = richSpan

        toShiftRichSpanList.add(newRichSpan)
        if (afterRichSpan.text.isNotEmpty())
            toShiftRichSpanList.add(afterRichSpan)

        while (true) {
            previousRichSpan = currentRichSpan
            currentRichSpan = currentRichSpan?.parent

            if (currentRichSpan == null || currentRichSpan == parentRichSpan) {
                break
            } else {
                val index = currentRichSpan.children.indexOf(previousRichSpan)
                if (index in 0 until currentRichSpan.children.lastIndex) {
                    val currentRichSpanFullSpanStyle = currentRichSpan.fullSpanStyle
                    ((index + 1)..currentRichSpan.children.lastIndex).forEach {
                        val childRichSpan = currentRichSpan.children[it]

                        // Ignore shifting empty RichSpan.
                        if (childRichSpan.text.isEmpty() && childRichSpan.children.isEmpty()) {
                            return@forEach
                        }

                        // Merge RichSpan span style with parent RichSpan span style.
                        childRichSpan.spanStyle = currentRichSpanFullSpanStyle.merge(childRichSpan.spanStyle)

                        // Lookup for RichSpan with the same span style and merge them to optimize the RichSpan tree.
                        val lastChild = toShiftRichSpanList.lastOrNull()
                        if (lastChild != null && lastChild.spanStyle == childRichSpan.spanStyle) {
                            if (lastChild.children.isEmpty()) {
                                lastChild.text += childRichSpan.text
                                lastChild.children.addAll(childRichSpan.children)
                            }
                            else {
                                lastChild.children.add(childRichSpan)
                                childRichSpan.parent = lastChild
                                childRichSpan.spanStyle = RichSpanStyle.DefaultSpanStyle
                                for (i in childRichSpan.children.lastIndex downTo 0) {
                                    val child = childRichSpan.children[i]
                                    child.parent = lastChild
                                    childRichSpan.children.removeAt(i)
                                    lastChild.children.add(child)
                                }
                            }
                        } else {
                            childRichSpan.parent = parentRichSpan
                            toShiftRichSpanList.add(childRichSpan)
                        }
                    }

                    // Remove shifted RichSpan from parent RichSpan.
                    currentRichSpan.children.removeRange(index + 1, currentRichSpan.children.size)

                    // Remove empty RichSpan.
                    if (previousRichSpan?.isEmpty() == true) {
                        currentRichSpan.children.removeAt(index)
                    }
                }
            }
        }

        if (parentRichSpan == null || currentRichSpan == null) {
            val index = richSpan.paragraph.children.indexOf(previousRichSpan)
            if (index in 0..richSpan.paragraph.children.lastIndex) {
                richSpan.paragraph.children.addAll(
                    index + 1,
                    toShiftRichSpanList
                )
            }

            // Remove empty RichSpan.
            if (previousRichSpan?.isEmpty() == true) {
                richSpan.paragraph.children.removeAt(index)
            }
        } else {
            val index = parentRichSpan.children.indexOf(previousRichSpan)
            if (index in 0..parentRichSpan.children.lastIndex) {
                parentRichSpan.children.addAll(
                    index + 1,
                    toShiftRichSpanList
                )
            }

            // Remove empty RichSpan.
            if (previousRichSpan?.isEmpty() == true) {
                parentRichSpan.children.removeAt(index)
            }
        }

        if (richSpan.text.isEmpty() && richSpan.children.isEmpty()) {
            val parent = richSpan.parent
            if (parent != null) {
                parent.children.remove(richSpan)
            } else {
                richSpan.paragraph.children.remove(richSpan)
            }
        }
    }

    private fun addRichSpanAtPosition(
        vararg richSpan: RichSpan,
        index: Int,
    ) {
        val previousIndex = index - 1

        val activeRichSpan = getRichSpanByTextIndex(previousIndex)

        if (activeRichSpan != null) {
            val startIndex = max(0, index - activeRichSpan.textRange.min)
            val beforeText = activeRichSpan.text.substring(0, startIndex)
            val afterText = activeRichSpan.text.substring(startIndex)

            // Simplify the richSpan tree if possible, by avoiding creating a new RichSpan.
            if (
                beforeText.isEmpty() &&
                afterText.isEmpty() &&
                activeRichSpan.children.isEmpty() &&
                richSpan.size == 1
            ) {
                activeRichSpan.text = richSpan.first().text
                activeRichSpan.style = richSpan.first().style
                return
            }

            activeRichSpan.text = beforeText

            var addedTextLength = 0
            for (i in richSpan.lastIndex downTo 0) {
                val newRichSpan = richSpan[i]
                newRichSpan.paragraph = activeRichSpan.paragraph
                newRichSpan.parent = activeRichSpan
                activeRichSpan.children.add(
                    0,
                    newRichSpan
                )
                addedTextLength += newRichSpan.text.length
            }
            if (afterText.isNotEmpty()) {
                activeRichSpan.children.add(
                    1,
                    RichSpan(
                        paragraph = activeRichSpan.paragraph,
                        parent = activeRichSpan,
                        text = afterText,
                        textRange = TextRange(
                            index + addedTextLength,
                            index + addedTextLength + afterText.length
                        ),
                    )
                )
            } else {
                val firstRichSpan = activeRichSpan.children.firstOrNull()
                val secondRichSpan = activeRichSpan.children.getOrNull(1)
                if (
                    firstRichSpan != null &&
                    secondRichSpan != null &&
                    firstRichSpan.spanStyle == secondRichSpan.spanStyle
                ) {
                    firstRichSpan.text += secondRichSpan.text
                    firstRichSpan.children.addAll(secondRichSpan.children)
                    activeRichSpan.children.removeAt(1)
                }

                if (
                    firstRichSpan != null &&
                    activeRichSpan.text.isEmpty() &&
                    activeRichSpan.children.size == 1 &&
                    richSpan.size == 1
                ) {
                    activeRichSpan.text = firstRichSpan.text
                    activeRichSpan.spanStyle = richSpan.first().spanStyle.customMerge(firstRichSpan.spanStyle)
                    activeRichSpan.children.clear()
                    activeRichSpan.children.addAll(firstRichSpan.children)
                }
            }
        } else {
            richParagraphList.last().children.addAll(richSpan)
        }
    }

    private fun getToShiftRichSpanList(
        startRichSpan: RichSpan,
        endRichSpan: RichSpan?,
    ): List<RichSpan> {
        val toShiftRichSpanList: MutableList<RichSpan> = mutableListOf()
        var previousRichSpan: RichSpan?
        var currentRichSpan: RichSpan? = startRichSpan

        while (true) {
            previousRichSpan = currentRichSpan
            currentRichSpan = currentRichSpan?.parent

            if (currentRichSpan == null || currentRichSpan == endRichSpan) {
                break
            } else {
                val index = currentRichSpan.children.indexOf(previousRichSpan)
                if (index in 0 until currentRichSpan.children.lastIndex) {
                    ((index + 1)..currentRichSpan.children.lastIndex).forEach {
                        val richSpan = currentRichSpan.children[it]
                        richSpan.spanStyle = richSpan.fullSpanStyle
                        richSpan.parent = endRichSpan
                        toShiftRichSpanList.add(richSpan)
                    }
                    currentRichSpan.children.removeRange(index + 1, currentRichSpan.children.size)
                }
            }
        }

        return toShiftRichSpanList
    }

    /**
     * Slice [RichParagraph] by [startIndex] and [richSpan] that contains [startIndex].
     * The passed [RichParagraph] will be modified, containing only the text before [startIndex].
     * And the new [RichParagraph] will be returned, containing the text after [startIndex].
     *
     * @param startIndex The start index of the slice.
     * @param richSpan The [RichSpan] that contains [startIndex].
     * @return The new [RichParagraph].
     */
    private fun RichParagraph.slice(
        startIndex: Int,
        richSpan: RichSpan,
    ): RichParagraph {
        val newRichParagraph = RichParagraph(
            paragraphStyle = paragraphStyle,
            type = type.nextParagraphType,
        )

        var previousRichSpan: RichSpan
        var currentRichSpan: RichSpan = richSpan

        val textStartIndex = if (startIndex == type.startRichSpan.textRange.min)
            startIndex - richSpan.textRange.min + type.startRichSpan.text.length
        else
            startIndex - richSpan.textRange.min


        newRichParagraph.type.startRichSpan.paragraph = newRichParagraph
        newRichParagraph.type.startRichSpan.textRange = TextRange(
            0,
            newRichParagraph.type.startRichSpan.text.length
        )

        val beforeText = if (textStartIndex > 0) richSpan.text.substring(0, textStartIndex) else "" // + ' '
        val afterText = richSpan.text.substring(textStartIndex + 1)

        richSpan.text = beforeText
        richSpan.textRange = TextRange(
            richSpan.textRange.min,
            richSpan.textRange.min + beforeText.length
        )

        val newRichSpan = RichSpan(
            paragraph = newRichParagraph,
            parent = null,
            text = afterText,
            textRange = TextRange(
                startIndex,
                startIndex + afterText.length
            ),
            spanStyle = richSpan.fullSpanStyle,
        )

        newRichParagraph.children.add(newRichSpan)

        for (i in richSpan.children.lastIndex downTo 0) {
            val childRichSpan = richSpan.children[i]
            richSpan.children.removeAt(i)
            childRichSpan.parent = newRichSpan
            childRichSpan.paragraph = newRichParagraph
            newRichSpan.children.add(childRichSpan)
        }

        while (true) {
            previousRichSpan = currentRichSpan
            currentRichSpan = currentRichSpan.parent ?: break

            val index = currentRichSpan.children.indexOf(previousRichSpan)
            if (index in 0 until currentRichSpan.children.lastIndex) {
                ((index + 1)..currentRichSpan.children.lastIndex).forEach {
                    val childRichSpan = currentRichSpan.children[it]
                    childRichSpan.spanStyle = childRichSpan.fullSpanStyle
                    childRichSpan.parent = null
                    childRichSpan.paragraph = newRichParagraph
                    newRichParagraph.children.add(childRichSpan)
                }
                currentRichSpan.children.removeRange(index + 1, currentRichSpan.children.size)
            }
        }

        val index = richSpan.paragraph.children.indexOf(previousRichSpan)
        if (index in 0 until richSpan.paragraph.children.lastIndex) {
            ((index + 1)..richSpan.paragraph.children.lastIndex).forEach {
                val childRichSpan = richSpan.paragraph.children[it]
                childRichSpan.spanStyle = childRichSpan.fullSpanStyle
                childRichSpan.parent = null
                childRichSpan.paragraph = newRichParagraph
                newRichParagraph.children.add(childRichSpan)
            }
            richSpan.paragraph.children.removeRange(index + 1, richSpan.paragraph.children.size)
        }

        return newRichParagraph
    }

    /**
     * Slice [RichSpan] by [startIndex] and [richSpan] that contains [startIndex].
     * The passed [RichSpan] will be modified, containing only the text before [startIndex].
     * And the new [RichSpan] will be returned, containing the text after [startIndex].
     *
     * @param startIndex The start index of the slice.
     * @param richSpan The [RichSpan] that contains [startIndex].
     * @return The new [RichSpan].
     */
    private fun RichSpan.slice(
        startIndex: Int,
        richSpan: RichSpan,
    ): RichSpan {
        val newRichSpan = RichSpan(
            paragraph = richSpan.paragraph,
        )

        var previousRichSpan: RichSpan
        var currentRichSpan: RichSpan = richSpan

        val textStartIndex = startIndex - richSpan.textRange.min
        val beforeText = if (textStartIndex > 0) richSpan.text.substring(0, textStartIndex) else "" // + ' '
        val afterText = richSpan.text.substring(textStartIndex + 1)

        richSpan.text = beforeText
        richSpan.textRange = TextRange(
            richSpan.textRange.min,
            richSpan.textRange.min + beforeText.length
        )

        val afterRichSpan = RichSpan(
            paragraph = richSpan.paragraph,
            parent = newRichSpan,
            text = afterText,
            textRange = TextRange(
                startIndex,
                startIndex + afterText.length
            ),
            spanStyle = richSpan.fullSpanStyle,
        )

        newRichSpan.children.add(afterRichSpan)

        for (i in richSpan.children.lastIndex downTo 0) {
            val childRichSpan = richSpan.children[i]
            richSpan.children.removeAt(i)
            childRichSpan.parent = afterRichSpan
            afterRichSpan.children.add(childRichSpan)
        }

        while (true) {
            previousRichSpan = currentRichSpan
            currentRichSpan = currentRichSpan.parent ?: break

            val index = currentRichSpan.children.indexOf(previousRichSpan)
            if (index in 0 until currentRichSpan.children.lastIndex) {
                ((index + 1)..currentRichSpan.children.lastIndex).forEach {
                    val childRichSpan = currentRichSpan.children[it]
                    childRichSpan.spanStyle = childRichSpan.fullSpanStyle
                    childRichSpan.parent = null
                    newRichSpan.children.add(childRichSpan)
                }
                currentRichSpan.children.removeRange(index + 1, currentRichSpan.children.size)
            }
        }

        val index = richSpan.paragraph.children.indexOf(previousRichSpan)
        if (index in 0 until richSpan.paragraph.children.lastIndex) {
            ((index + 1)..richSpan.paragraph.children.lastIndex).forEach {
                val childRichSpan = richSpan.paragraph.children[it]
                childRichSpan.spanStyle = childRichSpan.fullSpanStyle
                childRichSpan.parent = null
                newRichSpan.children.add(childRichSpan)
            }
            richSpan.paragraph.children.removeRange(index + 1, richSpan.paragraph.children.size)
        }

        return newRichSpan
    }

    /**
     * Merges two [RichParagraph]s into one.
     * The [firstParagraph] will be modified, containing the text of both [firstParagraph] and [secondParagraph].
     * And the [secondParagraph] will be removed.
     *
     * @param firstParagraph The first [RichParagraph].
     * @param secondParagraph The second [RichParagraph].
     */
    private fun mergeTwoRichParagraphs(
        firstParagraph: RichParagraph,
        secondParagraph: RichParagraph,
    ) {
        // Update the children paragraph of the second paragraph to the first paragraph.
        secondParagraph.updateChildrenParagraph(firstParagraph)

        // Add the children of the second paragraph to the first paragraph.
        firstParagraph.children.addAll(secondParagraph.children)

        // Remove the second paragraph from the rich paragraph list.
        richParagraphList.remove(secondParagraph)
    }

    /**
     * Updates the [currentAppliedSpanStyle] to the [SpanStyle] that should be applied to the current selection.
     */
    private fun updateCurrentSpanStyle() {
        if (selection.collapsed) {
            val richSpan = getRichSpanByTextIndex(textIndex = selection.min - 1)

            currentRichSpanStyle = richSpan
                ?.style
                ?: RichSpanStyle.Default
            currentAppliedSpanStyle = richSpan
                ?.fullSpanStyle
                ?: RichSpanStyle.DefaultSpanStyle
        }
        else {
            val richSpanList = getRichSpanListByTextRange(selection)

            currentRichSpanStyle = richSpanList
                .getCommonRichStyle()
                ?: RichSpanStyle.Default
            currentAppliedSpanStyle = getRichSpanListByTextRange(selection)
                .getCommonStyle()
                ?: RichSpanStyle.DefaultSpanStyle
        }
    }

    /**
     * Updates the [currentAppliedParagraphStyle] to the [ParagraphStyle] that should be applied to the current selection.
     */
    private fun updateCurrentParagraphStyle() {
        currentRichParagraphType
        if (selection.collapsed) {
            val richParagraph = getRichParagraphByTextIndex(selection.min - 1)

            currentRichParagraphType = richParagraph?.type
                ?: richParagraphList.firstOrNull()?.type
                ?: RichParagraph.Type.Default
            currentAppliedParagraphStyle = richParagraph?.paragraphStyle
                ?: richParagraphList.firstOrNull()?.paragraphStyle
                ?: RichParagraph.DefaultParagraphStyle
        }
        else {
            val richParagraphList = getRichParagraphListByTextRange(selection)

            currentRichParagraphType = richParagraphList
                .getCommonType()
                ?: RichParagraph.Type.Default
            currentAppliedParagraphStyle = richParagraphList
                .getCommonStyle()
                ?: ParagraphStyle()
        }
    }

    internal fun onTextLayout(textLayoutResult: TextLayoutResult) {
        this.textLayoutResult = textLayoutResult
    }

    internal fun getLinkByOffset(offset: Offset): String? {
        val richSpan = getRichSpanByOffset(offset)
        val style = richSpan?.style
        return if (style is RichSpanStyle.Link) style.url
            else null
    }

    internal fun isLink(offset: Offset): Boolean {
        val richSpan = getRichSpanByOffset(offset)
        return richSpan?.style is RichSpanStyle.Link
    }

    private fun getRichSpanByOffset(offset: Offset): RichSpan? {
        this.textLayoutResult?.let { textLayoutResult ->
            val position = textLayoutResult.getOffsetForPosition(offset)
            return getRichSpanByTextIndex(position, true)
        }
        return null
    }

    /**
     * Adjusts the [selection] to the [pressPosition].
     * This is a workaround for the [TextField] that the [selection] is not always correct when you have multiple lines.
     *
     * @param pressPosition The press position.
     */
    internal suspend fun adjustSelectionAndRegisterPressPosition(
        pressPosition: Offset,
    ) {
        adjustSelection(pressPosition)
        registerLastPressPosition(pressPosition)
    }

    /**
     * Adjusts the [selection] to the [pressPosition].
     * This is a workaround for the [TextField] that the [selection] is not always correct when you have multiple lines.
     *
     * @param pressPosition The press position.
     * @param newSelection The new selection.
     */
    private fun adjustSelection(
        pressPosition: Offset,
        newSelection: TextRange? = null,
    ) {
        val selection = newSelection ?: this.selection
        var pressX = pressPosition.x
        var pressY = pressPosition.y
        val textLayoutResult = this.textLayoutResult ?: return
        var index = 0
        for (i in 0 until textLayoutResult.lineCount) {
            index = i
            val start = textLayoutResult.getLineStart(i)
            val top = textLayoutResult.getLineTop(i)

            if (i == 0) {
                if (start > 0f) pressX += start
                if (top > 0f) pressY += top
            }

            if (i == 0 && top > pressY) break
            if (top > pressY) {
                index = i - 1
                break
            }
        }
        val selectedParagraph = richParagraphList.getOrNull(index) ?: return
        val nextParagraph = richParagraphList.getOrNull(index + 1)
        val nextParagraphStart = nextParagraph?.children?.firstOrNull()?.textRange?.min?.minus(nextParagraph.type.startText.length)
        if (
            selection.collapsed &&
            selection.min == nextParagraphStart
        )
            updateTextFieldValue(
                textFieldValue.copy(
                    selection = TextRange(selection.min - 1, selection.min - 1)
                )
            )
        else if (
            selection.collapsed &&
            index == richParagraphList.lastIndex &&
            selectedParagraph.isEmpty() &&
            selection.min == selectedParagraph.getFirstNonEmptyChild()?.textRange?.min?.minus(1)
        )
            updateTextFieldValue(
                textFieldValue.copy(
                    selection = TextRange(selection.min + 1, selection.min + 1)
                )
            )
        else if (newSelection != null)
            updateTextFieldValue(
                textFieldValue.copy(
                    selection = newSelection
                )
            )
    }

    private suspend fun registerLastPressPosition(pressPosition: Offset) {
        lastPressPosition = pressPosition
        delay(100)
        lastPressPosition = null
    }

    /**
     * Returns the [RichParagraph] that contains the given [textIndex].
     * If no [RichParagraph] contains the given [textIndex], null is returned.
     *
     * @param textIndex The text index to search for.
     * @return The [RichParagraph] that contains the given [textIndex], or null if no such [RichParagraph] exists.
     */
    private fun getRichParagraphByTextIndex(textIndex: Int): RichParagraph? {
        if (singleParagraphMode) return richParagraphList.firstOrNull()
        if (textIndex < 0) return richParagraphList.firstOrNull()

        var index = 0
        var paragraphIndex = -1
        return richParagraphList.fastFirstOrNull { richParagraphStyle ->
            paragraphIndex++
            val result = richParagraphStyle.getRichSpanByTextIndex(
                paragraphIndex = paragraphIndex,
                textIndex = textIndex,
                offset = index,
            )
            index = result.first
            result.second != null
        }
    }

    /**
     * Returns a list of [RichParagraph]s that contains at least a part of the given [searchTextRange].
     * If no [RichParagraph] contains at least a part of the given [searchTextRange], an empty list is returned.
     *
     * @param searchTextRange The [TextRange] to search for.
     * @return A list of [RichParagraph]s that contains a part of the given [searchTextRange],
     * or an empty list if no such [RichParagraph] exists.
     */
    private fun getRichParagraphListByTextRange(searchTextRange: TextRange): List<RichParagraph> {
        if (singleParagraphMode) return richParagraphList.toList()

        var index = 0
        val richParagraphList = mutableListOf<RichParagraph>()
        this.richParagraphList.fastForEachIndexed { paragraphIndex, richParagraphStyle ->
            val result = richParagraphStyle.getRichSpanListByTextRange(
                paragraphIndex = paragraphIndex,
                searchTextRange = searchTextRange,
                offset = index,
            )
            if (result.second.isNotEmpty())
                richParagraphList.add(richParagraphStyle)
            index = result.first
        }
        return richParagraphList
    }

    /**
     * Returns the [RichSpan] that contains the given [textIndex].
     * If no [RichSpan] contains the given [textIndex], null is returned.
     *
     * @param textIndex The text index to search for.
     * @return The [RichSpan] that contains the given [textIndex], or null if no such [RichSpan] exists.
     */
    private fun getRichSpanByTextIndex(
        textIndex: Int,
        ignoreCustomFiltering: Boolean = false,
    ): RichSpan? {
        // If the text index is equal or less than 0, we can return the first non-empty child of the first paragraph.
        if (textIndex <= 0) {
            val firstParagraph = richParagraphList.firstOrNull() ?: return null

            return firstParagraph.getFirstNonEmptyChild(firstParagraph.type.startText.length)
        }

        var index = 0
        richParagraphList.fastForEachIndexed { paragraphIndex, richParagraph ->
            val result = richParagraph.getRichSpanByTextIndex(
                paragraphIndex = paragraphIndex,
                textIndex = textIndex,
                offset = index,
                ignoreCustomFiltering = ignoreCustomFiltering,
            )
            if (result.second != null)
                return result.second
            else
                index = result.first
        }
        return null
    }

    /**
     * Returns a list of [RichSpan]s that contains at least a part of the given [searchTextRange].
     * If no [RichSpan] contains at least a part of the given [searchTextRange], an empty list is returned.
     *
     * @param searchTextRange The [TextRange] to search for.
     * @return A list of [RichSpan]s that contains a part of the given [searchTextRange], or an empty list if no such [RichSpan] exists.
     */
    private fun getRichSpanListByTextRange(searchTextRange: TextRange): List<RichSpan> {
        var index = 0
        val richSpanList = mutableListOf<RichSpan>()
        richParagraphList.fastForEachIndexed { paragraphIndex, richParagraphStyle ->
            val result = richParagraphStyle.getRichSpanListByTextRange(
                paragraphIndex = paragraphIndex,
                searchTextRange = searchTextRange,
                offset = index,
            )
            richSpanList.addAll(result.second)
            index = result.first
        }
        return richSpanList
    }

    /**
     * Returns a copy of this [RichTextState].
     * It can be used to create a snapshot of the current state.
     *
     * @return A copy of this [RichTextState].
     */
    fun copy(): RichTextState {
        val richParagraphList = richParagraphList.map { it.copy() }
        val richTextState = RichTextState(richParagraphList)
        richTextState.updateTextFieldValue(textFieldValue)
        richTextState.setConfig(
            linkColor = richTextConfig.linkColor,
            linkTextDecoration = richTextConfig.linkTextDecoration,
            codeColor = richTextConfig.codeColor,
            codeBackgroundColor = richTextConfig.codeBackgroundColor,
            codeStrokeColor = richTextConfig.codeStrokeColor,
        )
        return richTextState
    }

    /**
     * Updates the [RichTextState] with the given [text].
     *
     * @param text The text to update the [RichTextState] with.
     */
    fun setText(text: String) {
        val richParagraphList = listOf(RichParagraph())
        val richSpan = RichSpan(
            text = text,
            paragraph = richParagraphList.first(),
        )
        richParagraphList.first().children.add(richSpan)
        updateRichParagraphList(richParagraphList)
    }

    /**
     * Updates the [RichTextState] with the given [html].
     *
     * @param html The html to update the [RichTextState] with.
     */
    fun setHtml(html: String) {
        val richParagraphList = RichTextStateHtmlParser.encode(html).richParagraphList
        updateRichParagraphList(richParagraphList)
    }

    /**
     * Updates the [RichTextState] with the given [markdown].
     *
     * @param markdown The markdown to update the [RichTextState] with.
     */
    fun setMarkdown(markdown: String) {
        val richParagraphList = RichTextStateMarkdownParser.encode(markdown).richParagraphList
        updateRichParagraphList(richParagraphList)
    }

    /**
     * Updates the [RichTextState] with the given [newRichParagraphList].
     * The [RichTextState] will be updated with the given [newRichParagraphList] and the [annotatedString] will be updated.
     *
     * @param newRichParagraphList The [RichParagraph]s to update the [RichTextState] with.
     */
    internal fun updateRichParagraphList(newRichParagraphList: List<RichParagraph>) {
        richParagraphList.clear()
        richParagraphList.addAll(newRichParagraphList)

        if (richParagraphList.isEmpty())
            richParagraphList.add(RichParagraph())

        val newStyledRichSpanList = mutableListOf<RichSpan>()
        annotatedString = buildAnnotatedString {
            var index = 0
            richParagraphList.fastForEachIndexed { i, richParagraphStyle ->
                withStyle(richParagraphStyle.paragraphStyle.merge(richParagraphStyle.type.style)) {
                    append(richParagraphStyle.type.startText)
                    val richParagraphStartTextLength = richParagraphStyle.type.startText.length
                    richParagraphStyle.type.startRichSpan.textRange = TextRange(index, index + richParagraphStartTextLength)
                    index += richParagraphStartTextLength
                    withStyle(RichSpanStyle.DefaultSpanStyle) {
                        index = append(
                            richSpanList = richParagraphStyle.children,
                            startIndex = index,
                            onStyledRichSpan = {
                                newStyledRichSpanList.add(it)
                            },
                            richTextConfig = richTextConfig,
                        )
                        if (!singleParagraphMode) {
                            if (i != richParagraphList.lastIndex) {
                                append(" ")
                                index++
                            }
                        }
                    }
                }
            }
        }

        styledRichSpanList.clear()
        textFieldValue = TextFieldValue(
            text = annotatedString.text,
            selection = TextRange(annotatedString.text.length),
        )
        visualTransformation = VisualTransformation { _ ->
            TransformedText(
                annotatedString,
                OffsetMapping.Identity
            )
        }
        styledRichSpanList.addAll(newStyledRichSpanList)

        // Update current span style
        updateCurrentSpanStyle()

        // Update current paragraph style
        updateCurrentParagraphStyle()

        // Check paragraphs type
        checkParagraphsType()
    }

    private fun checkParagraphsType() {
        var orderedListNumber = 0
        richParagraphList.fastForEachIndexed { _, richParagraph ->
            if (richParagraph.type is RichParagraph.Type.OrderedList) {
                orderedListNumber++
                richParagraph.type = RichParagraph.Type.OrderedList(orderedListNumber)
            } else {
                orderedListNumber = 0
            }
        }
    }

    /**
     * Decodes the [RichTextState] to a html string.
     *
     * @return The html string.
     */
    fun toHtml(): String {
        return RichTextStateHtmlParser.decode(this)
    }

    /**
     * Decodes the [RichTextState] to a markdown string.
     *
     * @return The html string.
     */
    fun toMarkdown(): String {
        return RichTextStateMarkdownParser.decode(this)
    }

    /**
     * Clears the [RichTextState] and sets the [TextFieldValue] to an empty value.
     */
    fun clear() {
        richParagraphList.clear()
        richParagraphList.add(RichParagraph())
        updateTextFieldValue(TextFieldValue())
    }

    companion object {
        val Saver: Saver<RichTextState, *> = listSaver(
            save = {
                listOf(
                    it.toHtml(),
                    it.selection.start.toString(),
                    it.selection.end.toString(),
                )
            },
            restore = {
                val html = it[0]
                val selectionStart = it[1].toInt()
                val selectionEnd = it[2].toInt()
                val selection = TextRange(selectionStart, selectionEnd)
                val richTextState = RichTextState()
                richTextState.setHtml(html)
                richTextState.updateTextFieldValue(
                    richTextState.textFieldValue.copy(
                        selection = selection
                    )
                )
                richTextState
            }
        )
    }
}