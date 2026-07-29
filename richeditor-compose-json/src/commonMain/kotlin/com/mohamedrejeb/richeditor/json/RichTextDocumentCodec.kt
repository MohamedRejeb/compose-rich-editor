package com.mohamedrejeb.richeditor.json

import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.json.internal.encodeBlock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Serializes [RichTextDocument]s to and from versioned rich text JSON (schema v1).
 *
 * The output is canonical: the same document always produces the same string, so it is
 * safe to compare, cache, and store. Unknown mark kinds survive a decode/encode cycle
 * verbatim, keeping documents from newer or extended producers lossless.
 */
public object RichTextDocumentCodec {

    private val json: Json = Json { prettyPrint = false }

    public fun encode(document: RichTextDocument): JsonObject = buildJsonObject {
        put("v", CURRENT_JSON_SCHEMA_VERSION)
        put(
            "blocks",
            buildJsonArray {
                document.blocks.forEachIndexed { index, block -> add(encodeBlock(block, index)) }
            },
        )
    }

    public fun encodeToString(document: RichTextDocument): String =
        json.encodeToString(JsonObject.serializer(), encode(document))
}
