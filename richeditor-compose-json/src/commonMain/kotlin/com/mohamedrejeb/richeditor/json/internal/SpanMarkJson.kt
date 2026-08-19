@file:OptIn(com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi::class)

package com.mohamedrejeb.richeditor.json.internal

import androidx.compose.ui.unit.isSpecified
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import com.mohamedrejeb.richeditor.json.MalformedRichTextJsonException
import com.mohamedrejeb.richeditor.model.RichSpanStyleRegistry
import com.mohamedrejeb.richeditor.model.RichTextFormat
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

/**
 * Encodes [mark], or returns null for a custom mark whose style has no registered
 * descriptor opted into the JSON format (the mark is then skipped).
 */
internal fun encodeMark(mark: RichTextSpanMark, registry: RichSpanStyleRegistry): JsonObject? {
    if (mark is RichTextSpanMark.Custom) {
        val descriptor = registry.findForStyle(mark.style, RichTextFormat.Json) ?: return null
        return buildJsonObject {
            put("k", descriptor.kind)
            putRange(mark.range)
            put(
                "attrs",
                buildJsonObject {
                    descriptor.encode(mark.style).forEach { (key, value) -> put(key, value) }
                },
            )
        }
    }
    if (mark is RichTextSpanMark.Unknown) {
        // Re-emit the preserved payload verbatim, but keep k and r authoritative. rawJson
        // is a public, unvalidated field, so a payload that is not a JSON object degrades
        // to a bare mark instead of crashing the encode.
        val raw = runCatching { Json.parseToJsonElement(mark.rawJson).jsonObject }.getOrNull()
        return buildJsonObject {
            put("k", mark.kind)
            putRange(mark.range)
            raw?.forEach { (key, value) -> if (key != "k" && key != "r") put(key, value) }
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

internal fun decodeMark(json: JsonObject, registry: RichSpanStyleRegistry): RichTextSpanMark {
    val kind = (json["k"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        ?: throw MalformedRichTextJsonException("Span mark is missing its \"k\" field")
    val range = decodeRange(json)
    return when (kind) {
        "bold" -> RichTextSpanMark.Bold(range)
        "italic" -> RichTextSpanMark.Italic(range)
        "underline" -> RichTextSpanMark.Underline(range)
        "strike" -> RichTextSpanMark.Strikethrough(range)
        "code" -> RichTextSpanMark.CodeSpan(range)
        "link" -> RichTextSpanMark.Link(range, url = json.requireString("url", kind))
        "color" -> RichTextSpanMark.TextColor(range, argb = json.requireString("argb", kind).parseArgbHex())
        "highlight" -> RichTextSpanMark.Highlight(range, argb = json.requireString("argb", kind).parseArgbHex())
        "font-size" -> RichTextSpanMark.FontSize(range, size = textUnitFromJson(json))
        "font-weight" -> {
            val weight = json.requireInt("value", kind)
            if (weight !in 1..1000) {
                throw MalformedRichTextJsonException("font-weight value must be in 1..1000, was $weight")
            }
            RichTextSpanMark.FontWeight(range, weight = weight)
        }
        "letter-spacing" -> RichTextSpanMark.LetterSpacing(range, size = textUnitFromJson(json))
        "baseline-shift" -> RichTextSpanMark.BaselineShift(range, multiplier = json.requireFloat("value", kind))
        "shadow" -> RichTextSpanMark.Shadow(
            range = range,
            argb = json.requireString("argb", kind).parseArgbHex(),
            offsetX = json.requireFloat("x", kind),
            offsetY = json.requireFloat("y", kind),
            blurRadius = json.requireFloat("blur", kind),
        )
        "image" -> RichTextSpanMark.Image(
            range = range,
            url = json.requireString("url", kind),
            width = (json["width"] as? JsonObject)?.let(::textUnitFromJson)
                ?: androidx.compose.ui.unit.TextUnit.Unspecified,
            height = (json["height"] as? JsonObject)?.let(::textUnitFromJson)
                ?: androidx.compose.ui.unit.TextUnit.Unspecified,
            description = (json["alt"] as? JsonPrimitive)?.takeIf { it.isString }?.content,
        )
        "token" -> RichTextSpanMark.Token(
            range = range,
            trigger = json.requireString("trigger", kind),
            id = json.requireString("id", kind),
            label = json.requireString("label", kind),
        )
        else -> decodeCustomMark(json, kind, range, registry)
            ?: RichTextSpanMark.Unknown(
                range = range,
                kind = kind,
                rawJson = Json.encodeToString(JsonObject.serializer(), json),
            )
    }
}

/**
 * Decodes a registered custom kind to a [RichTextSpanMark.Custom], or returns null so the
 * caller degrades to [RichTextSpanMark.Unknown] (unregistered kind, JSON format opted out,
 * an attrs shape this version cannot read, or the descriptor's decoder declined the
 * attributes). Only a throwing decoder is fatal: that is an app bug, not foreign data.
 */
private fun decodeCustomMark(
    json: JsonObject,
    kind: String,
    range: IntRange,
    registry: RichSpanStyleRegistry,
): RichTextSpanMark? {
    val descriptor = registry.find(kind, RichTextFormat.Json) ?: return null
    val attributes = (json["attrs"] as? JsonObject).orEmpty().mapValues { (_, value) ->
        // A non-primitive value means a newer producer extended the attrs shape; degrade
        // to Unknown (preserving the raw payload) instead of failing the whole load.
        (value as? JsonPrimitive)?.content ?: return null
    }
    val style = try {
        descriptor.decode(attributes)
    } catch (e: MalformedRichTextJsonException) {
        throw e
    } catch (e: Exception) {
        throw MalformedRichTextJsonException("Decoder for custom mark kind \"$kind\" failed", e)
    }
    return style?.let { RichTextSpanMark.Custom(range = range, style = it) }
}

private fun decodeRange(json: JsonObject): IntRange {
    val array = json["r"] as? JsonArray
        ?: throw MalformedRichTextJsonException("Span mark is missing its \"r\" range")
    if (array.size != 2) {
        throw MalformedRichTextJsonException("Span range must have exactly two elements, had ${array.size}")
    }
    val first = (array[0] as? JsonPrimitive)?.content?.toIntOrNull()
    val last = (array[1] as? JsonPrimitive)?.content?.toIntOrNull()
    if (first == null || last == null || first > last) {
        throw MalformedRichTextJsonException("Invalid span range: $array")
    }
    return first..last
}

private fun JsonObject.requireString(key: String, kind: String): String =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
        ?: throw MalformedRichTextJsonException("Mark \"$kind\" is missing its \"$key\" field")

private fun JsonObject.requireInt(key: String, kind: String): Int =
    (this[key] as? JsonPrimitive)?.content?.toIntOrNull()
        ?: throw MalformedRichTextJsonException("Mark \"$kind\" is missing its \"$key\" field")

private fun JsonObject.requireFloat(key: String, kind: String): Float =
    (this[key] as? JsonPrimitive)?.content?.toFloatOrNull()?.takeIf { it.isFinite() }
        ?: throw MalformedRichTextJsonException(
            "Mark \"$kind\" is missing a finite \"$key\" field"
        )

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
    is RichTextSpanMark.Custom ->
        error("Unreachable: encodeMark handles Custom marks before kindName is consulted")
}
