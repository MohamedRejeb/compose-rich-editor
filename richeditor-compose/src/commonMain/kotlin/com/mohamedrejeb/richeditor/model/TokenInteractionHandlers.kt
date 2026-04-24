package com.mohamedrejeb.richeditor.model

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/**
 * Handles a tap on a [RichSpanStyle.Token] span in a read-only rich-text surface
 * ([com.mohamedrejeb.richeditor.ui.BasicRichText] and its Material wrappers).
 *
 * Invoked with the [token] that was hit and the [tapOffset] in local coordinates
 * of the rich-text composable - useful for anchoring a popover at the tap point.
 *
 * Typical uses:
 * - `@mention` -> open a user card or navigate to profile.
 * - `#hashtag` or `#issueRef` -> navigate to a tag feed or open an issue.
 * - `/command` -> run a command.
 */
@ExperimentalRichTextApi
public fun interface TokenClickHandler {
    public operator fun invoke(token: RichSpanStyle.Token, tapOffset: Offset)
}

/**
 * Notifies when the [RichSpanStyle.Token] under the pointer changes in a read-only
 * rich-text surface.
 *
 * Fires on **enter** (token becomes non-null), **exit** (token becomes null), and
 * **change** (pointer moves directly from one token to an adjacent token). Does NOT
 * fire on every pointer-move event while staying over the same token - the callback
 * is cheap and the caller owns any delay / positioning policy.
 *
 * @param token the token currently under the pointer, or `null` when the pointer
 *   has left all tokens.
 * @param pointerOffset current pointer position in local coordinates - use this to
 *   anchor a preview card.
 *
 * Typical use: drive a GitHub-style preview popup for `@user` or `#issue` tokens.
 */
@ExperimentalRichTextApi
public fun interface TokenHoverHandler {
    public operator fun invoke(token: RichSpanStyle.Token?, pointerOffset: Offset)
}

/**
 * Screen-wide default for [RichSpanStyle.Token] taps. Prefer the composable
 * `onTokenClick` parameter for a specific surface; use this CompositionLocal
 * when multiple `RichText`s on one screen should share one handler.
 */
@ExperimentalRichTextApi
public val LocalTokenClickHandler: ProvidableCompositionLocal<TokenClickHandler?> =
    staticCompositionLocalOf { null }

/**
 * Screen-wide default for [RichSpanStyle.Token] hover transitions. Prefer the
 * composable `onTokenHover` parameter for a specific surface; use this
 * CompositionLocal when multiple `RichText`s on one screen should share one handler.
 */
@ExperimentalRichTextApi
public val LocalTokenHoverHandler: ProvidableCompositionLocal<TokenHoverHandler?> =
    staticCompositionLocalOf { null }
