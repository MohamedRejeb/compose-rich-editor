package com.mohamedrejeb.richeditor.utils

/**
 * Placeholder character used by Compose `appendInlineContent` (default
 * `alternateText`). An image span in the editor owns exactly one such
 * char in the underlying raw text so that span `textRange`s stay in
 * sync with the rendered `annotatedString` that inline content emits.
 *
 * See issue #466.
 */
internal const val InlineContentPlaceholder: String = "\uFFFD"
