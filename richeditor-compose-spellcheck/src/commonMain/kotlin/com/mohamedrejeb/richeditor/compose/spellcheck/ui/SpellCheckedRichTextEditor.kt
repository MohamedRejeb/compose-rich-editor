package com.mohamedrejeb.richeditor.compose.spellcheck.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import com.mohamedrejeb.richeditor.compose.spellcheck.SpellCheckMenuState
import com.mohamedrejeb.richeditor.compose.spellcheck.SpellCheckState
import com.mohamedrejeb.richeditor.compose.spellcheck.SpellCheckTextContextMenuProvider
import com.mohamedrejeb.richeditor.compose.spellcheck.rememberSpellCheckState
import com.mohamedrejeb.richeditor.compose.spellcheck.utils.debounceUntilQuiescent
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.mohamedrejeb.richeditor.ui.InteractionType
import com.mohamedrejeb.richeditor.ui.RichSpanClickListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
public fun SpellCheckedRichTextEditor(
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
    val menuState by remember(spellCheckState) { mutableStateOf(SpellCheckMenuState(spellCheckState)) }

    val changesFlow = MutableStateFlow(RichTextState())
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            changesFlow.debounceUntilQuiescent(1.seconds).collect { richTextState ->
                spellCheckState.onTextChange(richTextState)
            }
        }
    }

    SpellCheckTextContextMenuProvider(
        modifier = modifier,
        spellCheckMenuState = menuState,
    ) {
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
            onRichTextChangedListener = { changesFlow.tryEmit(it) },
            onRichSpanClick = { span, range, click, type ->
                return@BasicRichTextEditor if (type == InteractionType.SecondaryClick || type == InteractionType.Tap) {
                    val correction = spellCheckState.handleSpanClick(span, range, click)
                    if (correction != null) {
                        menuState.missSpelling.value = SpellCheckMenuState.MissSpelling(correction, click)
                        true
                    } else {
                        menuState.missSpelling.value = null
                        onRichSpanClick?.invoke(span, range, click, type) ?: false
                    }
                } else {
                    menuState.missSpelling.value = null
                    false
                }
            },
            decorationBox = decorationBox,
        )
    }
}