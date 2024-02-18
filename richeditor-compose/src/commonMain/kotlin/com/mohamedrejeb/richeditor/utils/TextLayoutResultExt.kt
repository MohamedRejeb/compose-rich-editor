package com.mohamedrejeb.richeditor.utils

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import kotlin.math.max
import kotlin.math.min

/**
 * Reads bounds for multiple lines. This can be removed once an
 * [official API](https://issuetracker.google.com/u/1/issues/237289433) is released.
 *
 * When [flattenForFullParagraphs] is available, the bounds for one or multiple
 * entire paragraphs is returned instead of separate lines if [startOffset]
 * and [endOffset] represent the extreme ends of those paragraph.
 *
 * @param startOffset the start offset of the range to read bounds for.
 * @param endOffset the end offset of the range to read bounds for.
 * @param flattenForFullParagraphs whether to return bounds for entire paragraphs instead of separate lines.
 * @return the list of bounds for the given range.
 */
fun TextLayoutResult.getBoundingBoxes(
    startOffset: Int,
    endOffset: Int,
    flattenForFullParagraphs: Boolean = false
): List<Rect> {
    if (startOffset == endOffset)
        return emptyList()

    if (startOffset < 0 || endOffset > layoutInput.text.length)
        return emptyList()

    val startLineNum = getLineForOffset(min(startOffset, endOffset))
    val endLineNum = getLineForOffset(max(startOffset, endOffset))

    if (flattenForFullParagraphs) {
        val isFullParagraph = (startLineNum != endLineNum)
                && getLineStart(startLineNum) == startOffset
                && multiParagraph.getLineEnd(endLineNum, visibleEnd = true) == endOffset

        if (isFullParagraph) {
            return listOf(
                Rect(
                    top = getLineTop(startLineNum),
                    bottom = getLineBottom(endLineNum),
                    left = 0f,
                    right = size.width.toFloat()
                )
            )
        }
    }

    // Compose UI does not offer any API for reading paragraph direction for an entire line.
    // So this code assumes that all paragraphs in the text will have the same direction.
    // It also assumes that this paragraph does not contain bi-directional text.
    val isLtr = multiParagraph.getParagraphDirection(offset = layoutInput.text.lastIndex) == ResolvedTextDirection.Ltr

    return fastMapRange(startLineNum, endLineNum) { lineNum ->
        Rect(
            top = getLineTop(lineNum),
            bottom = getLineBottom(lineNum),
            left = if (lineNum == startLineNum) {
                getHorizontalPosition(startOffset, usePrimaryDirection = isLtr)
            } else {
                getLineLeft(lineNum)
            },
            right = if (lineNum == endLineNum) {
                getHorizontalPosition(endOffset, usePrimaryDirection = isLtr)
            } else {
                getLineRight(lineNum)
            }
        )
    }
}