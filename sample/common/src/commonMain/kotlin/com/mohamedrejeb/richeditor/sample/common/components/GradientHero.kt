package com.mohamedrejeb.richeditor.sample.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.sample.common.ui.theme.SampleAccents
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor

private val BoldSpan = SpanStyle(fontWeight = FontWeight.Bold)
private val ItalicSpan = SpanStyle(fontStyle = FontStyle.Italic)
private val UnderlineSpan = SpanStyle(textDecoration = TextDecoration.Underline)

/**
 * Landing hero used at the top of the home screen.
 * A gradient-painted full-bleed surface with a small editable BasicRichTextEditor so visitors
 * can immediately try the library without leaving the home screen.
 */
@Composable
fun GradientHero(
    modifier: Modifier = Modifier,
) {
    val previewState = rememberRichTextState()

    LaunchedEffect(Unit) {
        previewState.config.codeSpanColor = Color.White
        previewState.config.codeSpanBackgroundColor = Color.White.copy(alpha = 0.18f)
        previewState.config.codeSpanStrokeColor = Color.Transparent
        previewState.setHtml(
            """
            <p>Build <b>beautiful</b>, <i>expressive</i>, <u>WYSIWYG</u> editors with
            vibrant formatting and seamless <code>code</code> support.</p>
            <p>Try it - select text and format with the buttons below.</p>
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

            Spacer(Modifier.size(4.dp))

            HeroEditor(state = previewState)
        }
    }
}

@Composable
private fun HeroEditor(
    state: com.mohamedrejeb.richeditor.model.RichTextState,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HeroToolbarButton(
                icon = Icons.Outlined.FormatBold,
                isSelected = state.currentSpanStyle.fontWeight == FontWeight.Bold,
                onClick = { state.toggleSpanStyle(BoldSpan) },
            )
            HeroToolbarButton(
                icon = Icons.Outlined.FormatItalic,
                isSelected = state.currentSpanStyle.fontStyle == FontStyle.Italic,
                onClick = { state.toggleSpanStyle(ItalicSpan) },
            )
            HeroToolbarButton(
                icon = Icons.Outlined.FormatUnderlined,
                isSelected = state.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true,
                onClick = { state.toggleSpanStyle(UnderlineSpan) },
            )
            HeroToolbarButton(
                icon = Icons.Outlined.Code,
                isSelected = state.isCodeSpan,
                onClick = { state.toggleCodeSpan() },
            )
        }

        BasicRichTextEditor(
            state = state,
            textStyle = LocalTextStyle.current.copy(
                color = Color.White,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            ),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp),
        )
    }
}

@Composable
private fun HeroToolbarButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (isSelected) Color.White.copy(alpha = 0.28f) else Color.Transparent
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            // Workaround: prevent the rich editor from losing focus when a toolbar button
            // is clicked (Desktop quirk).
            .focusProperties { canFocus = false }
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}
