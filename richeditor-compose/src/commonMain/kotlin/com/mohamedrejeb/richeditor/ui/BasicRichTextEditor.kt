package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key.Companion.Backspace
import androidx.compose.ui.input.key.Key.Companion.Enter
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextStyle
import com.mohamedrejeb.richeditor.model.RichTextValue
import com.mohamedrejeb.richeditor.utils.addListItem

/**
 * Basic composable that enables users to edit rich text via hardware or software keyboard, but provides no decorations like hint or placeholder.
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state represented by [RichTextValue]. [RichTextValue] contains the text entered by user, as well as selection, cursor, text composition information and rich text information. Please check [RichTextValue] for the description of its contents.
 * It is crucial that the value provided in the onValueChange is fed back into BasicRichTextEditor in order to have the final state of the text being displayed.
 *
 * BasicRichTextEditor is a wrapper around [BasicTextField] and it accepts all the parameters that [BasicTextField] accepts.
 *
 * This composable provides basic rich text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder. A design system based implementation such as
 * Material Design Filled text field is typically what is needed to cover most of the needs. This
 * composable is designed to be used when a custom implementation for different design system is
 * needed.
 *
 * @param value The [RichTextValue] to be shown in the
 * [BasicRichTextEditor].
 * @param onValueChange Called when the input service updates the values in [RichTextValue].
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicRichTextEditor]. When `false`, the text
 * field will be neither editable nor focusable, the input of the text field will not be selectable
 * @param readOnly controls the editable state of the [BasicRichTextEditor]. When `true`, the text
 * field can not be modified, however, a user can focus it and copy text from it. Read-only text
 * fields are usually used to display pre-filled forms that user can not edit
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param keyboardOptions software keyboard options that contains configuration such as
 * [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * [KeyboardOptions.imeAction].
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling
 * text field instead of wrapping onto multiple lines. The keyboard will be informed to not show
 * the return key as the [ImeAction]. [maxLines] and [minLines] are ignored as both are
 * automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw a cursor or selection around the text.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this TextField. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this TextField in different [Interaction]s.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 * provided, there will be no cursor drawn
 * @param decorationBox Composable lambda that allows to add decorations around text field, such
 * as icon, placeholder, helper messages or similar, and automatically increase the hit target area
 * of the text field. To allow you to control the placement of the inner text field relative to your
 * decorations, the text field implementation will pass in a framework-controlled composable
 * parameter "innerTextField" to the decorationBox lambda you provide. You must call
 * innerTextField exactly once.
 *
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BasicRichTextEditor(
    value: RichTextValue,
    onValueChange: (RichTextValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions: KeyboardActions = KeyboardActions(onDone = {
        handleEnterKeyPress(
            value,
            onValueChange
        )
    }),
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    BasicTextField(
        value = value.textFieldValue,
        onValueChange = {
            if(it.text.length<value.textFieldValue.text.length){
                handleDelete(value, onValueChange)
            }
            else {
                onValueChange(
                    value.updateTextFieldValue(it)
                )
            }
        },
        modifier = modifier.onKeyEvent { event ->
            if (event.key.keyCode == Enter.keyCode) {
                handleEnterKeyPress(value, onValueChange)
            }
            if (event.key.keyCode == Backspace.keyCode) {
                // Check if there is any text selected in the editor
                return@onKeyEvent handleDelete(value, onValueChange)
            }
            return@onKeyEvent false
        },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = value.visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox,
    )
}

private fun handleDelete(
    value: RichTextValue,
    onValueChange: (RichTextValue) -> Unit
): Boolean {
    if (value.textFieldValue.selection.start != value.textFieldValue.selection.end) {
        // If there is a selection, delete the selected text and update the value accordingly
        val updatedValue = value.updateTextFieldValue(
            value.textFieldValue.copy(
                text = value.textFieldValue.text.removeRange(
                    value.textFieldValue.selection.start,
                    value.textFieldValue.selection.end
                )
            )
        )
        onValueChange(updatedValue)
        return true
    } else {
        // If there is no selection, delete the character before the cursor and update the value accordingly
        val cursorPosition = value.textFieldValue.selection.start
        if (cursorPosition > 0 && cursorPosition <= value.textFieldValue.text.length) {
            val updatedValue = value.updateTextFieldValue(
                value.textFieldValue.copy(
                    text = value.textFieldValue.text.removeRange(
                        cursorPosition - 1,
                        cursorPosition
                    )
                )
            )

            // Decrease the toIndex of the last part if the character before the cursor was removed
            val updatedParts = updatedValue.parts.toMutableList()
            val lastPart = updatedParts.lastOrNull()
            if (lastPart != null && cursorPosition - 1 == lastPart.toIndex) {
                // If the last part's fromIndex equals toIndex, remove the style
                if (lastPart.fromIndex == lastPart.toIndex - 1) {
                    updatedParts[updatedParts.lastIndex] =
                        lastPart.copy(styles = emptySet())
                } else {
                    updatedParts[updatedParts.lastIndex] =
                        lastPart.copy(toIndex = lastPart.toIndex - 1)
                }
                onValueChange(updatedValue.copy(parts = updatedParts))
            } else {
                onValueChange(updatedValue)
            }
            return true
        }
    }
    return false
}



private fun handleEnterKeyPress(
    value: RichTextValue,
    onValueChange: (RichTextValue) -> Unit
) {
    // Check if the last style is a list style
    val lastListStyle = value.parts.asReversed().find {
        it.styles.any { style ->
            style is RichTextStyle.UnorderedListItem || style is RichTextStyle.OrderedListItem
        }
    }?.styles?.lastOrNull {
        it is RichTextStyle.UnorderedListItem || it is RichTextStyle.OrderedListItem
    }

    if (lastListStyle == null) {
        // If it doesn't, just add a new line
        onValueChange(
            value.updateTextFieldValue(
                TextFieldValue(
                    text = value.textFieldValue.text + "\n",
                    selection = TextRange(value.textFieldValue.text.length + 1)
                )
            )
        )
    } else {
        // Append a new list item based on the last list style
        val updatedValue = value.addListItem()

        // Call the onValueChange callback with the updated value
        onValueChange(updatedValue.updateTextFieldValue(updatedValue.textFieldValue))
    }
}

