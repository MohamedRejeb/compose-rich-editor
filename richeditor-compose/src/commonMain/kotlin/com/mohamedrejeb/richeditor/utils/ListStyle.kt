package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextPart
import com.mohamedrejeb.richeditor.model.RichTextStyle
import com.mohamedrejeb.richeditor.model.RichTextValue


fun RichTextValue.addListItem(): RichTextValue {
    // Get the last style
    val lastStyle = this.parts.lastOrNull()?.styles?.lastOrNull()

    // The updated parts
    val updatedParts = this.parts.toMutableList()
    var updateText = this.textFieldValue.text
    when (lastStyle) {
        is RichTextStyle.UnorderedListItem -> {
            // If the last style is an UnorderedListItem, add a new one
            updateText += "• "
            val newPart = RichTextPart(
                fromIndex = this.textFieldValue.text.length,
                toIndex = updateText.length,  // Adjust these values as needed
                styles = setOf(RichTextStyle.UnorderedListItem)
            )

            updatedParts.add(newPart)
        }

        is RichTextStyle.OrderedListItem -> {
            // If the last style is an OrderedListItem, increment the position and add a new one
            val newPosition = lastStyle.position + 1
            updateText += "$newPosition. "
            val newPart = RichTextPart(
                fromIndex = this.textFieldValue.text.length,
                toIndex = updateText.length,  // Adjust these values as needed
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
