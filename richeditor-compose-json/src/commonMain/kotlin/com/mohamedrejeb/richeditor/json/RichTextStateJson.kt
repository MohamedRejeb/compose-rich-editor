package com.mohamedrejeb.richeditor.json

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState

/**
 * Serializes the editor content to versioned rich text JSON (schema v1). Custom span styles
 * are carried when a matching descriptor on [RichTextState.spanStyleRegistry] opts into the
 * JSON format.
 */
@ExperimentalRichTextApi
public fun RichTextState.toJson(): String =
    RichTextDocumentCodec.encodeToString(toRichTextDocument(), spanStyleRegistry)

/**
 * Replaces the editor content from rich text JSON produced by [toJson]. Unknown mark kinds
 * survive document decoding but are not applied to the editor state.
 *
 * @throws MalformedRichTextJsonException on structurally invalid input
 * @throws UnsupportedRichTextJsonVersionException when the document is from a newer schema
 */
@ExperimentalRichTextApi
public fun RichTextState.setJson(json: String): RichTextState =
    setRichTextDocument(RichTextDocumentCodec.decodeFromString(json, spanStyleRegistry))
