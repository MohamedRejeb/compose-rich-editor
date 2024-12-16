package com.mohamedrejeb.richeditor.compose.spellcheck

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import kotlin.math.PI
import kotlin.math.sin

/**
 * RichSpanStyle that draws a Spell Check style red squiggle below the Spanned text.
 */
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

        val amplitude = 1.5.dp.toPx() // Height of the wave
        val frequency = 0.15f // Controls how many waves appear

        boxes.fastForEach { box ->
            path.moveTo(box.left + startPadding, box.bottom + topPadding)

            // Create the sine wave path
            for (x in 0..box.width.toInt()) {
                val xPos = box.left + startPadding + x
                val yPos = box.bottom + topPadding +
                        (amplitude * sin(x * frequency * 2 * PI)).toFloat()

                if (x == 0) {
                    path.moveTo(xPos, yPos)
                } else {
                    path.lineTo(xPos, yPos)
                }
            }

            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(
                    width = 1.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }

    override val acceptNewTextInTheEdges: Boolean = false
}