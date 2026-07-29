package com.mohamedrejeb.richeditor.json.internal

import androidx.compose.ui.unit.isSpecified
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextBlockType
import kotlinx.serialization.json.add
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
