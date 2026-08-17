package com.mohamedrejeb.richeditor.json

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi

/**
 * Version written to the "v" field of every serialized document. Compare a stored
 * document's "v" against this to check compatibility before calling [setJson].
 */
@ExperimentalRichTextApi
public const val CURRENT_JSON_SCHEMA_VERSION: Int = 1

/** Thrown by [setJson] when JSON is structurally invalid for the rich text schema. */
@ExperimentalRichTextApi
public class MalformedRichTextJsonException internal constructor(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

/** Thrown by [setJson] when a document declares a schema version newer than this library understands. */
@ExperimentalRichTextApi
public class UnsupportedRichTextJsonVersionException internal constructor(
    public val documentVersion: Int,
) : IllegalArgumentException(
    "Document schema version $documentVersion is newer than supported version $CURRENT_JSON_SCHEMA_VERSION"
)
