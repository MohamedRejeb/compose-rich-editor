package com.mohamedrejeb.richeditor.compose.spellcheck

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextConfig
import com.mohamedrejeb.richeditor.utils.getBoundingBoxes

@OptIn(ExperimentalRichTextApi::class)
public object SpellCheck: RichSpanStyle {
    override val spanStyle: (RichTextConfig) -> SpanStyle = {
        SpanStyle()
    }

    override fun DrawScope.drawCustomStyle(
        layoutResult: TextLayoutResult,
        textRange: TextRange,
        richTextConfig: RichTextConfig,
        topPadding: Float,
        startPadding: Float,
    ) {
        val path = Path()
        val strokeColor = Color.Red
        val boxes = layoutResult.getBoundingBoxes(
            startOffset = textRange.start,
            endOffset = textRange.end,
            flattenForFullParagraphs = true,
        )

        boxes.fastForEach { box ->
            path.moveTo(box.left + startPadding, box.bottom + topPadding)
            path.lineTo(box.right + startPadding, box.bottom + topPadding)

            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                )
            )
        }
    }

    override val acceptNewTextInTheEdges: Boolean = false
}
