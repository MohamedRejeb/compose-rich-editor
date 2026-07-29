package com.mohamedrejeb.richeditor.json.internal

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextBlockType
import com.mohamedrejeb.richeditor.json.MalformedRichTextJsonException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun decodeBlock(json: JsonObject): RichTextBlock {
    val typeName = (json["type"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        ?: throw MalformedRichTextJsonException("Block is missing its \"type\" field")
    val text = (json["text"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        ?: throw MalformedRichTextJsonException("Block is missing its \"text\" field")
    val spans = (json["spans"] as? JsonArray)?.map { element ->
        val markObject = element as? JsonObject
            ?: throw MalformedRichTextJsonException("Span mark must be an object")
        decodeMark(markObject)
    } ?: emptyList()

    val (type, headingLevel) = when (typeName) {
        "paragraph" -> RichTextBlockType.Paragraph to 0
        "heading" -> RichTextBlockType.Paragraph to json.requireBlockInt("level")
        "list-item" -> {
            val ordered = (json["ordered"] as? JsonPrimitive)?.booleanOrNull
                ?: throw MalformedRichTextJsonException("list-item block is missing its \"ordered\" field")
            RichTextBlockType.ListItem(
                ordered = ordered,
                indent = (json["indent"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                startNumber = if (ordered) (json["start"] as? JsonPrimitive)?.content?.toIntOrNull() else null,
            ) to ((json["heading"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0)
        }
        else -> throw MalformedRichTextJsonException("Unknown block type: $typeName")
    }

    return try {
        RichTextBlock(
            text = text,
            type = type,
            spans = spans,
            headingLevel = headingLevel,
            textAlign = (json["align"] as? JsonPrimitive)?.takeIf { it.isString }
                ?.let { textAlignFromJson(it.content) } ?: TextAlign.Unspecified,
            textDirection = (json["dir"] as? JsonPrimitive)?.takeIf { it.isString }
                ?.let { textDirectionFromJson(it.content) } ?: TextDirection.Unspecified,
            lineHeight = (json["lineHeight"] as? JsonObject)?.let(::textUnitFromJson) ?: TextUnit.Unspecified,
            textIndent = (json["textIndent"] as? JsonObject)?.let { indent ->
                TextIndent(
                    firstLine = (indent["firstLine"] as? JsonObject)?.let(::textUnitFromJson) ?: TextUnit.Unspecified,
                    restLine = (indent["restLine"] as? JsonObject)?.let(::textUnitFromJson) ?: TextUnit.Unspecified,
                )
            },
            isLineBreak = (json["br"] as? JsonPrimitive)?.booleanOrNull == true,
        )
    } catch (e: IllegalArgumentException) {
        if (e is MalformedRichTextJsonException) throw e
        throw MalformedRichTextJsonException(e.message ?: "Invalid block", e)
    }
}

private fun JsonObject.requireBlockInt(key: String): Int =
    (this[key] as? JsonPrimitive)?.content?.toIntOrNull()
        ?: throw MalformedRichTextJsonException("Block is missing its \"$key\" field")

internal fun encodeBlock(block: RichTextBlock, index: Int): JsonObject = buildJsonObject {
    put("id", "b$index")
    val type = block.type
    when {
        type is RichTextBlockType.ListItem -> {
            put("type", "list-item")
            put("ordered", type.ordered)
            put("indent", type.indent)
            type.startNumber?.let { put("start", it) }
            if (block.headingLevel > 0) put("heading", block.headingLevel)
        }
        block.headingLevel > 0 -> {
            put("type", "heading")
            put("level", block.headingLevel)
        }
        else -> put("type", "paragraph")
    }
    block.textAlign.toJsonName()?.let { put("align", it) }
    block.textDirection.toJsonName()?.let { put("dir", it) }
    if (block.lineHeight.isSpecified) put("lineHeight", block.lineHeight.toJsonObject())
    block.textIndent?.let { indent ->
        put(
            "textIndent",
            buildJsonObject {
                if (indent.firstLine.isSpecified) put("firstLine", indent.firstLine.toJsonObject())
                if (indent.restLine.isSpecified) put("restLine", indent.restLine.toJsonObject())
            },
        )
    }
    if (block.isLineBreak) put("br", true)
    put("text", block.text)
    put("spans", buildJsonArray { block.spans.forEach { add(encodeMark(it)) } })
}
