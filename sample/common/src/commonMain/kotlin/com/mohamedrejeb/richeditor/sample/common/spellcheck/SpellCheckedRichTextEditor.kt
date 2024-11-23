package com.mohamedrejeb.richeditor.sample.common.spellcheck

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.mohamedrejeb.richeditor.ui.RichSpanClickListener
import com.mohamedrejeb.richeditor.utils.WordSegment

@Composable
fun SpellCheckedRichTextEditor(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    maxLength: Int = Int.MAX_VALUE,
    onRichSpanClick: RichSpanClickListener? = null,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),
    spellCheckState: SpellCheckState = rememberSpellCheckState(null),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
) {
    var spellCheckWord by remember { mutableStateOf<WordSegment?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var menuPosition by remember { mutableStateOf(Offset.Zero) }

    fun clearSpellCheck() {
        spellCheckWord = null
        expanded = false
        menuPosition = Offset.Zero
    }

    Box(modifier = modifier) {
        BasicRichTextEditor(
            modifier = Modifier.fillMaxSize(),
            enabled = enabled,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            maxLength = maxLength,
            onTextLayout = onTextLayout,
            interactionSource = interactionSource,
            state = spellCheckState.richTextState,
            cursorBrush = cursorBrush,
            onRichSpanClick = { span, range, click ->
                val correction = spellCheckState.handleSpanClick(span, range, click)
                if (correction != null) {
                    spellCheckWord = correction
                    menuPosition = click
                    expanded = true
                } else {
                    onRichSpanClick?.invoke(span, range, click)
                }
            },
            decorationBox = decorationBox,
        )

        SpellCheckDropdown(
            spellCheckWord,
            menuPosition,
            spellCheckState,
            dismiss = ::clearSpellCheck,
            correctSpelling = { segment, correction ->
                spellCheckState.correctSpelling(segment, correction)
                clearSpellCheck()
            }
        )
    }
}