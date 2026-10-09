package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.isForModelText
import com.mohamedrejeb.richeditor.model.linesOf
import kotlin.math.roundToInt

/**
 * Lays [text] out and puts [action] over the top end corner of each code block in it. A code
 * block shares one text layout with the text around it, so the action is drawn over the block
 * and takes no room of its own. Without an [action], [text] is emitted as it is.
 */
@Composable
internal fun CodeBlockActionLayout(
    state: RichTextState,
    modifier: Modifier,
    action: (@Composable (code: String, language: String?) -> Unit)?,
    text: @Composable (Modifier) -> Unit,
) {
    if (action == null) {
        text(modifier)
        return
    }

    val blocks = state.renderedCodeBlocks
    Layout(
        content = {
            text(Modifier)
            blocks.forEachIndexed { index, block ->
                key(index) { Box { action(block.code, block.language) } }
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val textPlaceable = measurables.first().measure(constraints)
        val actionConstraints = Constraints(maxWidth = textPlaceable.width, maxHeight = textPlaceable.height)
        val actionPlaceables = measurables.drop(1).map { it.measure(actionConstraints) }

        layout(textPlaceable.width, textPlaceable.height) {
            textPlaceable.placeRelative(0, 0)
            val layoutResult = state.textLayoutResult
                ?.takeIf { it.isForModelText(state.annotatedString.length) }
                ?: return@layout
            actionPlaceables.forEachIndexed { index, placeable ->
                // A block that maxLines or overflow left out has no line to sit on and is not placed.
                val lines = blocks.getOrNull(index)?.let { layoutResult.linesOf(it) } ?: return@forEachIndexed
                placeable.placeRelative(
                    x = textPlaceable.width - placeable.width,
                    y = (layoutResult.getLineTop(lines.first) + CodeBlockVerticalGap.toPx()).roundToInt(),
                )
            }
        }
    }
}
