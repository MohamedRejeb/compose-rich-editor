package com.mohamedrejeb.richeditor.json.internal

import androidx.compose.ui.unit.isSpecified
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

internal fun encodeMark(mark: RichTextSpanMark): JsonObject {
    if (mark is RichTextSpanMark.Unknown) {
        // Re-emit the preserved payload verbatim, but keep k and r authoritative.
        val raw = Json.parseToJsonElement(mark.rawJson).jsonObject
        return buildJsonObject {
            put("k", mark.kind)
            putRange(mark.range)
            raw.forEach { (key, value) -> if (key != "k" && key != "r") put(key, value) }
        }
    }
    return buildJsonObject {
        put("k", mark.kindName())
        putRange(mark.range)
        when (mark) {
            is RichTextSpanMark.Link -> put("url", mark.url)
            is RichTextSpanMark.TextColor -> put("argb", mark.argb.toArgbHex())
            is RichTextSpanMark.Highlight -> put("argb", mark.argb.toArgbHex())
            is RichTextSpanMark.FontSize -> {
                put("value", mark.size.value.toDouble())
                put("unit", mark.size.unitName())
            }
            is RichTextSpanMark.FontWeight -> put("value", mark.weight)
            is RichTextSpanMark.LetterSpacing -> {
                put("value", mark.size.value.toDouble())
                put("unit", mark.size.unitName())
            }
            is RichTextSpanMark.BaselineShift -> put("value", mark.multiplier.toDouble())
            is RichTextSpanMark.Shadow -> {
                put("argb", mark.argb.toArgbHex())
                put("x", mark.offsetX.toDouble())
                put("y", mark.offsetY.toDouble())
                put("blur", mark.blurRadius.toDouble())
            }
            is RichTextSpanMark.Image -> {
                put("url", mark.url)
                if (mark.width.isSpecified) put("width", mark.width.toJsonObject())
                if (mark.height.isSpecified) put("height", mark.height.toJsonObject())
                mark.description?.let { put("alt", it) }
            }
            is RichTextSpanMark.Token -> {
                put("trigger", mark.trigger)
                put("id", mark.id)
                put("label", mark.label)
            }
            else -> Unit
        }
    }
}

private fun JsonObjectBuilder.putRange(range: IntRange) {
    put(
        "r",
        buildJsonArray {
            add(JsonPrimitive(range.first))
            add(JsonPrimitive(range.last))
        },
    )
}

private fun androidx.compose.ui.unit.TextUnit.unitName(): String =
    if (type == androidx.compose.ui.unit.TextUnitType.Em) "em" else "sp"

internal fun RichTextSpanMark.kindName(): String = when (this) {
    is RichTextSpanMark.Bold -> "bold"
    is RichTextSpanMark.Italic -> "italic"
    is RichTextSpanMark.Underline -> "underline"
    is RichTextSpanMark.Strikethrough -> "strike"
    is RichTextSpanMark.CodeSpan -> "code"
    is RichTextSpanMark.Link -> "link"
    is RichTextSpanMark.TextColor -> "color"
    is RichTextSpanMark.Highlight -> "highlight"
    is RichTextSpanMark.FontSize -> "font-size"
    is RichTextSpanMark.FontWeight -> "font-weight"
    is RichTextSpanMark.LetterSpacing -> "letter-spacing"
    is RichTextSpanMark.BaselineShift -> "baseline-shift"
    is RichTextSpanMark.Shadow -> "shadow"
    is RichTextSpanMark.Image -> "image"
    is RichTextSpanMark.Token -> "token"
    is RichTextSpanMark.Unknown -> kind
}
