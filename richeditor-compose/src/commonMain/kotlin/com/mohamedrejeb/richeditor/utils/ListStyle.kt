package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextPart
import com.mohamedrejeb.richeditor.model.RichTextStyle
import com.mohamedrejeb.richeditor.model.RichTextValue

fun RichTextValue.appendListItem(lastListStyle: RichTextStyle): RichTextValue {
    // The current text of the rich text value
    val currentText = this.textFieldValue.text

    // The list item marker (bullet or number)
    val listItemMarker = when (lastListStyle) {
        is RichTextStyle.UnorderedListItem -> "• "
        is RichTextStyle.OrderedListItem -> {
            // Find the last ordered list item and increment its number
            val lastOrderedListItemNumber = this.parts
                .filter { it.styles.contains(lastListStyle) }
                .maxOfOrNull { it.styles.filterIsInstance<RichTextStyle.OrderedListItem>().first().position }
                ?: 0
            "${lastOrderedListItemNumber + 1}. "
        }
        else -> ""
    }

    // Append the list item marker to the current text
    val updatedText = "$currentText$listItemMarker"

    // Create a new RichTextPart that includes the list style.
    val listItemPart = RichTextPart(
        fromIndex = currentText.length,
        toIndex = updatedText.length - 1, // -1 because indices are 0-based
        styles = setOf(lastListStyle)
    )

    // Prepare the updated parts of the rich text value
    val updatedParts = this.parts + listItemPart

    // Create a new TextFieldValue that includes the updated text and selection.
    val updatedTextFieldValue = TextFieldValue(
        text = updatedText, selection = TextRange(updatedText.length)
    )

    // Create a new RichTextValue with the updated TextFieldValue and parts.
    return this.copy(
        textFieldValue = updatedTextFieldValue, parts = updatedParts
    )
}
