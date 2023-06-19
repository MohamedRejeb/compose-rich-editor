package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.mohamedrejeb.richeditor.model.RichTextPart
import com.mohamedrejeb.richeditor.model.RichTextStyle
import com.mohamedrejeb.richeditor.model.RichTextValue

/**
 * Adds a hyperlink to the text within the selection of the current rich text value. If the selection is collapsed,
 * the hyperlink will be inserted at the caret's position. Otherwise, the hyperlink will replace the selected text.
 *
 * @param link The URL for the hyperlink.
 * @return A new rich text value with the hyperlink added.
 */
fun RichTextValue.getLink(link: String): RichTextValue {
    // Ensure the link starts with http:// or https://
    val url =
        if (link.startsWith("https://") || link.startsWith("http://")) link else "https://$link"

    val currentText = this.textFieldValue.text
    val currentSelection = this.textFieldValue.selection
    val newRichTextPart = ArrayList<RichTextPart>()

    // Insert the URL at the caret's position if the selection is collapsed
    val newText = if (currentSelection.collapsed) {
        StringBuilder(currentText).apply {
            insert(currentSelection.start, "$url ")
        }.toString()
    } else {
        currentText
    }
    // If there are parts in the text (i.e., it's not empty)
    if (this.parts.isNotEmpty()) {
        this.parts.forEach { part ->
            // If the current selection is within this part
            if (currentSelection.isWithin(TextRange(part.fromIndex, part.toIndex))) {
                // Create a new part up to the start of the selection
                val partFirst =
                    RichTextPart(part.fromIndex, currentSelection.start - 1, part.styles)
                newRichTextPart.add(partFirst)

                // Create a hyperlink part
                val hyperlinkStyle = part.styles + RichTextStyle.Hyperlink(url)
                val hyperlinkPart: RichTextPart
                val lastPart: RichTextPart

                if (currentSelection.collapsed) {
                    hyperlinkPart = RichTextPart(
                        fromIndex = currentSelection.start,
                        toIndex = currentSelection.start + url.length - 1,
                        styles = hyperlinkStyle
                    )

                    // Create a new part after the hyperlink
                    lastPart = RichTextPart(
                        currentSelection.start + url.length,
                        part.toIndex + url.length,
                        part.styles
                    )
                } else {
                    hyperlinkPart = RichTextPart(
                        fromIndex = currentSelection.start,
                        toIndex = currentSelection.end - 1,
                        styles = hyperlinkStyle
                    )

                    // Create a new part after the hyperlink
                    lastPart = RichTextPart(currentSelection.end, part.toIndex, part.styles)
                }

                newRichTextPart.add(hyperlinkPart)
                newRichTextPart.add(lastPart)
            } else {
                newRichTextPart.add(part)
            }
        }
    } else {
        // If there are no parts in the text (i.e., it's empty)
        val hyperlinkStyle = setOf<RichTextStyle>() + RichTextStyle.Hyperlink(url)
        val hyperlinkPart: RichTextPart

        if (currentSelection.collapsed) {
            hyperlinkPart = RichTextPart(
                fromIndex = currentSelection.start,
                toIndex = currentSelection.start + url.length - 1,
                styles = hyperlinkStyle
            )
        } else {
            hyperlinkPart = RichTextPart(
                fromIndex = 0,
                toIndex = currentText.lastIndex - 1,
                styles = hyperlinkStyle
            )
        }

        newRichTextPart.add(hyperlinkPart)
    }

    val newTextFieldValue = TextFieldValue(newText, TextRange(currentSelection.min, newText.length))
    val newStyle = RichTextValueBuilder().apply {
        setTextFieldValue(newTextFieldValue)
        setParts(newRichTextPart)
        setCurrentStyles(this@getLink.currentStyles)
    }.build()
    return newStyle
}

/**
 * Checks if the current TextRange is within the given range.
 *
 * @param range The TextRange to compare against.
 * @return True if the current TextRange is within the given range, false otherwise.
 */
fun TextRange.isWithin(range: TextRange): Boolean =
    this.start >= range.start && this.end <= range.end