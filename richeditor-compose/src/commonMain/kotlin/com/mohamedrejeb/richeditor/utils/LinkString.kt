package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextPart
import com.mohamedrejeb.richeditor.model.RichTextStyle
import com.mohamedrejeb.richeditor.model.RichTextValue

/**
 * Extension Function to add the link to RichTextValue
 * The function checks if the any text is selected and applies the hyperlink to it
 * other wise append the link and then add the style
 *  @see <a href="https://gist.github.com/DAKSHSEMWAL/6a7f0453a6650c9b88a4f54a98ead4d7">Get Link Usage</a>
 */
fun RichTextValue.getLink(link: String): RichTextValue {
    // The current text of the rich text value
    val currentText = this.textFieldValue.text
    // The current selection of the rich text value
    val currentSelection = this.textFieldValue.selection
    // The text that is currently selected
    val selectedText = currentText.substring(currentSelection.start, currentSelection.end)

    return if (selectedText.isNotEmpty()) {
        // If there is a selection and it matches the text, we will apply the hyperlink to the selection.

        // Create a new RichTextPart that includes the hyperlink style.
        val hyperlinkPart = RichTextPart(
            fromIndex = currentSelection.start,
            toIndex = currentSelection.end - 1,
            styles = setOf(RichTextStyle.Hyperlink(url = link))
        )

        // Prepare the updated parts of the rich text value
        val updatedParts = this.parts.toMutableList().apply {
            // Remove any parts that overlap with the current selection.
            removeAll { part -> part.fromIndex >= currentSelection.start && part.toIndex <= currentSelection.end }
            // Add the new hyperlink part.
            add(hyperlinkPart)
        }

        // Create a new RichTextValue with the updated parts.
        val updatedTextFieldValue = TextFieldValue(
            text = currentText, selection = TextRange(currentText.length + 1)
        )

        // Create a new RichTextValue with the updated TextFieldValue and parts.
        this.copy(
            textFieldValue = updatedTextFieldValue, parts = updatedParts
        )
    } else {
        // If there's no selection or it doesn't match the text, we will append the hyperlink to the end of the text.

        // Prepare the updated text
        val updatedText = "$currentText $link"

        // Create a new RichTextPart that includes the hyperlink style.
        val hyperlinkPart = RichTextPart(
            fromIndex = currentText.length + 1, // add 1 for the space
            toIndex = updatedText.length - 1, styles = setOf(RichTextStyle.Hyperlink(url = link))
        )

        // Prepare the updated parts of the rich text value
        val updatedParts = this.parts + hyperlinkPart

        // Create a new TextFieldValue that includes the updated text and selection.
        val updatedTextFieldValue = TextFieldValue(
            text = updatedText, selection = TextRange(updatedText.length)
        )

        // Create a new RichTextValue with the updated TextFieldValue and parts.
        this.copy(
            textFieldValue = updatedTextFieldValue, parts = updatedParts
        )
    }
}
