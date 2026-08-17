package com.mohamedrejeb.richeditor.json

/** Version written to the "v" field of every serialized document. */
internal const val CURRENT_JSON_SCHEMA_VERSION: Int = 1

/** Thrown by [setJson] when JSON is structurally invalid for the rich text schema. */
public class MalformedRichTextJsonException internal constructor(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

/** Thrown by [setJson] when a document declares a schema version newer than this library understands. */
public class UnsupportedRichTextJsonVersionException internal constructor(
    public val documentVersion: Int,
) : IllegalArgumentException(
    "Document schema version $documentVersion is newer than supported version $CURRENT_JSON_SCHEMA_VERSION"
)
