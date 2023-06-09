package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextPart
import com.mohamedrejeb.richeditor.model.RichTextStyle
import com.mohamedrejeb.richeditor.model.RichTextValue


fun RichTextValue.addListItem(): RichTextValue {
    // Get the last part
    val lastPart = this.parts.lastOrNull()

    // The updated parts
    val updatedParts = this.parts.toMutableList()
    var updateText = this.textFieldValue.text
    val cursorPosition = this.textFieldValue.selection.start

    // Find the current part based on cursor position
    val currentPart = updatedParts.find { cursorPosition in it.fromIndex..it.toIndex }

    // Check whether the last part has a list style
    val lastListStyle = lastPart?.styles?.find { it is RichTextStyle.UnorderedListItem || it is RichTextStyle.OrderedListItem }
    val lastHyperlinkStyle = lastPart?.styles?.find { it is RichTextStyle.Hyperlink }
    when (lastListStyle) {
        is RichTextStyle.UnorderedListItem -> {
            // If the last style is an UnorderedListItem, add a new one
            if(lastHyperlinkStyle!=null){
                updateText.removeSuffix(" ")
            }
            updateText +="\n• "
            val newPart = RichTextPart(
                fromIndex = this.textFieldValue.text.length,
                toIndex = updateText.length - 1,
                styles = setOf(RichTextStyle.UnorderedListItem)
            )
            updatedParts.add(newPart)
        }

        is RichTextStyle.OrderedListItem -> {
            // If the last style is an OrderedListItem, increment the position and add a new one
            val newPosition = lastListStyle.position + 1
            if(lastHyperlinkStyle!=null){
                updateText = updateText.removeSuffix(" ")
            }
            updateText += "\n$newPosition. "
            val newPart = RichTextPart(
                fromIndex = this.textFieldValue.text.length,
                toIndex = updateText.length - 1,
                styles = setOf(RichTextStyle.OrderedListItem(newPosition))
            )

            updatedParts.add(newPart)
        }
    }

    // Create a new TextFieldValue that includes the updated text and selection.
    val updatedTextFieldValue = TextFieldValue(
        text = updateText, selection = TextRange(updateText.length)
    )
    // Create a new RichTextValue with the updated parts
    return this.copy(textFieldValue = updatedTextFieldValue, parts = updatedParts)
}
