package com.mohamedrejeb.richeditor.ui

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mohamedrejeb.richeditor.clipboard.createRichTextClipboardManager
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.CoroutineScope

/**
 * Basic composable that enables users to edit rich text via hardware or software keyboard, but provides no decorations like hint or placeholder.
 * Whenever the user edits the texe.
 *
 * BasicRichTextEditor is a wrapper around [BasicTextField] and it accepts all the parameters that [BasicTextField] accepts.
 *
 * This composable provides basic rich text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder. A design system based implementation such as
 * Material Design Filled text field is typically what is needed to cover most of the needs. This
 * composable is designed to be used when a custom implementation for different design system is
 * needed.
 *
 * @param state [RichTextState] that holds the state of the [BasicRichTextEditor].
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicRichTextEditor]. When `false`, the text
 * field will be neither editable nor focusable, the input of the text field will not be selectable
 * @param readOnly controls the editable state of the [BasicRichTextEditor]. When `true`, the text
 * field can not be modified, however, a user can focus it and copy text from it. Read-only text
 * fields are usually used to display pre-filled forms that user can not edit
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param keyboardOptions software keyboard options that contains configuration such as
 * [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * [KeyboardOptions.imeAction].
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling
 * text field instead of wrapping onto multiple lines. The keyboard will be informed to not show
 * the return key as the [ImeAction]. [maxLines] and [minLines] are ignored as both are
 * automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param maxLength the maximum length of the text field. If the text is longer than this value,
 * it will be ignored. The default value of this parameter is [Int.MAX_VALUE].
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw a cursor or selection around the text.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this TextField. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this TextField in different [Interaction]s.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 * provided, there will be no cursor drawn
 * @param decorationBox Composable lambda that allows to add decorations around text field, such
 * as icon, placeholder, helper messages or similar, and automatically increase the hit target area
 * of the text field. To allow you to control the placement of the inner text field relative to your
 * decorations, the text field implementation will pass in a framework-controlled composable
 * parameter "innerTextField" to the decorationBox lambda you provide. You must call
 * innerTextField exactly once.
 *
 */
@Composable
public fun BasicRichTextEditor(
    state: RichTextState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    maxLength: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    BasicRichTextEditor(
        state = state,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        maxLength = maxLength,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox,
        contentPadding = PaddingValues()
    )
}

/**
 * Basic composable that enables users to edit rich text via hardware or software keyboard, but provides no decorations like hint or placeholder.
 * Whenever the user edits the texe.
 *
 * BasicRichTextEditor is a wrapper around [BasicTextField] and it accepts all the parameters that [BasicTextField] accepts.
 *
 * This composable provides basic rich text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder. A design system based implementation such as
 * Material Design Filled text field is typically what is needed to cover most of the needs. This
 * composable is designed to be used when a custom implementation for different design system is
 * needed.
 *
 * @param state [RichTextState] that holds the state of the [BasicRichTextEditor].
 * @param modifier optional [Modifier] for this text field.
 * @param enabled controls the enabled state of the [BasicRichTextEditor]. When `false`, the text
 * field will be neither editable nor focusable, the input of the text field will not be selectable
 * @param readOnly controls the editable state of the [BasicRichTextEditor]. When `true`, the text
 * field can not be modified, however, a user can focus it and copy text from it. Read-only text
 * fields are usually used to display pre-filled forms that user can not edit
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param keyboardOptions software keyboard options that contains configuration such as
 * [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * [KeyboardOptions.imeAction].
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling
 * text field instead of wrapping onto multiple lines. The keyboard will be informed to not show
 * the return key as the [ImeAction]. [maxLines] and [minLines] are ignored as both are
 * automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param maxLength the maximum length of the text field. If the text is longer than this value,
 * it will be ignored. The default value of this parameter is [Int.MAX_VALUE].
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw a cursor or selection around the text.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this TextField. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this TextField in different [Interaction]s.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 * provided, there will be no cursor drawn
 * @param decorationBox Composable lambda that allows to add decorations around text field, such
 * as icon, placeholder, helper messages or similar, and automatically increase the hit target area
 * of the text field. To allow you to control the placement of the inner text field relative to your
 * decorations, the text field implementation will pass in a framework-controlled composable
 * parameter "innerTextField" to the decorationBox lambda you provide. You must call
 * innerTextField exactly once.
 *
 */
@Composable
public fun BasicRichTextEditor(
    state: RichTextState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    singleParagraph: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    maxLength: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
    contentPadding: PaddingValues
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val clipboard = LocalClipboard.current
    val richClipboardManager = remember(state, clipboard) {
        createRichTextClipboardManager(
            richTextState = state,
            clipboard = clipboard
        )
    }

    LaunchedEffect(singleParagraph) {
        state.singleParagraphMode = singleParagraph
    }

    if (!singleParagraph) {
        // Workaround for Android to fix a bug in BasicTextField where it doesn't select the correct text
        // when the text contains multiple paragraphs.
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Press) {
                    val pressPosition = interaction.pressPosition
                    val topPadding = with(density) { contentPadding.calculateTopPadding().toPx() }
                    val startPadding = with(density) { contentPadding.calculateStartPadding(layoutDirection).toPx() }

                    adjustTextIndicatorOffset(
                        pressPosition = pressPosition,
                        state = state,
                        topPadding = topPadding,
                        startPadding = startPadding,
                    )
                }
            }
        }
    }

    CompositionLocalProvider(LocalClipboard provides richClipboardManager) {
        BasicTextField(
            value = state.textFieldValue,
            onValueChange = {
                if (readOnly) return@BasicTextField
                if (it.text.length > maxLength) return@BasicTextField

                state.onTextFieldValueChange(it)
            },
            modifier = modifier
                .onPreviewKeyEvent { event ->
                    if (readOnly)
                        return@onPreviewKeyEvent false

                    state.onPreviewKeyEvent(event)
                }
                .drawRichSpanStyle(
                    richTextState = state,
                    topPadding = with(density) { contentPadding.calculateTopPadding().toPx() },
                    startPadding = with(density) { contentPadding.calculateStartPadding(layoutDirection).toPx() },
                )
                .then(
                    if (!readOnly)
                        Modifier
                    else
                        Modifier.focusProperties { canFocus = false }
                )
                .then(
                    if (singleParagraph)
                        Modifier
                    else
                        Modifier
                            // Workaround for Desktop to fix a bug in BasicTextField where it doesn't select the correct text
                            // when the text contains multiple paragraphs.
                            .adjustTextIndicatorOffset(
                                state = state,
                                contentPadding = contentPadding,
                                density = density,
                                layoutDirection = layoutDirection,
                                scope = rememberCoroutineScope()
                            )
                ),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            visualTransformation = state.visualTransformation,
            onTextLayout = {
                state.onTextLayout(
                    textLayoutResult = it,
                    density = density,
                )
                onTextLayout(it)
            },
            interactionSource = interactionSource,
            cursorBrush = cursorBrush,
            decorationBox = decorationBox,
        )
    }
}

internal expect fun Modifier.adjustTextIndicatorOffset(
    state: RichTextState,
    contentPadding: PaddingValues,
    density: Density,
    layoutDirection: LayoutDirection,
    scope: CoroutineScope,
): Modifier

internal suspend fun adjustTextIndicatorOffset(
    pressPosition: Offset,
    state: RichTextState,
    topPadding: Float,
    startPadding: Float,
) {
    state.adjustSelectionAndRegisterPressPosition(
        pressPosition = Offset(
            x = pressPosition.x - startPadding,
            y = pressPosition.y - topPadding
        ),
    )
}

public typealias RichTextChangedListener = (RichTextState) -> Unit
