package com.mohamedrejeb.richeditor.json

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.model.RichSpanStyleRegistry

/** Codec entry points with an empty registry, for tests not exercising custom styles. */
@OptIn(ExperimentalRichTextApi::class)
internal fun codecEncode(document: RichTextDocument): String =
    RichTextDocumentCodec.encodeToString(document, RichSpanStyleRegistry())

@OptIn(ExperimentalRichTextApi::class)
internal fun codecDecode(json: String): RichTextDocument =
    RichTextDocumentCodec.decodeFromString(json, RichSpanStyleRegistry())
