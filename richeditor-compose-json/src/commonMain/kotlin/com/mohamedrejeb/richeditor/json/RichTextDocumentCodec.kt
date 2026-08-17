@file:OptIn(com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi::class)

package com.mohamedrejeb.richeditor.json

import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.json.internal.decodeBlock
import com.mohamedrejeb.richeditor.json.internal.encodeBlock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/**
 * Serializes [RichTextDocument]s to and from versioned rich text JSON (schema v1).
 *
 * The output is canonical: the same document always produces the same string, so it is
 * safe to compare, cache, and store. Unknown mark kinds survive a decode/encode cycle
 * verbatim, keeping documents from newer or extended producers lossless.
 */
internal object RichTextDocumentCodec {

    private val json: Json = Json { prettyPrint = false }

    fun encode(document: RichTextDocument): JsonObject = buildJsonObject {
        put("v", CURRENT_JSON_SCHEMA_VERSION)
        put(
            "blocks",
            buildJsonArray {
                document.blocks.forEachIndexed { index, block -> add(encodeBlock(block, index)) }
            },
        )
    }

    fun encodeToString(document: RichTextDocument): String =
        json.encodeToString(JsonObject.serializer(), encode(document))

    /**
     * Decodes a document from its JSON string form.
     *
     * @throws MalformedRichTextJsonException when the input is not valid rich text JSON.
     * @throws UnsupportedRichTextJsonVersionException when the document declares a schema
     * version newer than [CURRENT_JSON_SCHEMA_VERSION].
     */
    fun decodeFromString(jsonString: String): RichTextDocument {
        val element = try {
            json.parseToJsonElement(jsonString)
        } catch (e: SerializationException) {
            throw MalformedRichTextJsonException("Invalid JSON", e)
        }
        val jsonObject = element as? JsonObject
            ?: throw MalformedRichTextJsonException("Root must be an object")
        return decode(jsonObject)
    }

    fun decode(json: JsonObject): RichTextDocument {
        val version = (json["v"] as? JsonPrimitive)?.intOrNull
            ?: throw MalformedRichTextJsonException("Missing or invalid \"v\" field")
        if (version < 1) throw MalformedRichTextJsonException("Invalid schema version $version")
        if (version > CURRENT_JSON_SCHEMA_VERSION) throw UnsupportedRichTextJsonVersionException(version)

        val blocks = json["blocks"] as? JsonArray
            ?: throw MalformedRichTextJsonException("Missing \"blocks\" array")
        val decoded = blocks.map { blockElement ->
            val blockObject = blockElement as? JsonObject
                ?: throw MalformedRichTextJsonException("Block must be an object")
            decodeBlock(blockObject)
        }
        return try {
            RichTextDocument(blocks = decoded.ifEmpty { RichTextDocument.empty().blocks })
        } catch (e: IllegalArgumentException) {
            throw MalformedRichTextJsonException(e.message ?: "Invalid document", e)
        }
    }
}
