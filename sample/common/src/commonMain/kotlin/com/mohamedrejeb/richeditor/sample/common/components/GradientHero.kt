package com.mohamedrejeb.richeditor.sample.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.ui.theme.SampleAccents
import com.mohamedrejeb.richeditor.ui.material3.RichText

/**
 * Landing hero used at the top of the home screen.
 * A gradient-painted full-bleed surface holding an inline RichText preview that demonstrates
 * the library's formatting in a single glance.
 */
@Composable
fun GradientHero(
    modifier: Modifier = Modifier,
) {
    val previewState = rememberRichTextState()

    LaunchedEffect(Unit) {
        previewState.setHtml(
            """
            <p>Build <b>beautiful</b>, <i>expressive</i>, <u>WYSIWYG</u> editors with
            <span style="color: #FDE68A">vibrant</span> formatting and seamless
            <span style="background: rgba(255,255,255,0.18); padding: 0 4px; border-radius: 4px">code</span>
            support.</p>
            """.trimIndent()
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        SampleAccents.Indigo,
                        SampleAccents.Violet,
                        SampleAccents.Magenta,
                    ),
                ),
            )
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "COMPOSE RICH EDITOR",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Text(
                text = "WYSIWYG rich text\nfor every Compose target.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Text(
                text = "Android · iOS · Desktop · Web. One editor, one state model, every platform.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )

            Spacer(Modifier.size(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(16.dp),
            ) {
                RichText(
                    state = previewState,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
