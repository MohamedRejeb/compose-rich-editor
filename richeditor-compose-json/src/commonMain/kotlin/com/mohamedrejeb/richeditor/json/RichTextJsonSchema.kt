package com.mohamedrejeb.richeditor.json

/** Version written to the "v" field of every serialized document. */
public const val CURRENT_JSON_SCHEMA_VERSION: Int = 1

/** Thrown when JSON is structurally invalid for the rich text schema. */
public class MalformedRichTextJsonException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

/** Thrown when a document declares a schema version newer than this library understands. */
public class UnsupportedRichTextJsonVersionException(
    public val documentVersion: Int,
) : IllegalArgumentException(
    "Document schema version $documentVersion is newer than supported version $CURRENT_JSON_SCHEMA_VERSION"
)
