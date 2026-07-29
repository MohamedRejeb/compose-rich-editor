package com.mohamedrejeb.richeditor.json.internal

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.mohamedrejeb.richeditor.json.MalformedRichTextJsonException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

internal fun Long.toArgbHex(): String =
    toString(16).uppercase().padStart(8, '0')

internal fun String.parseArgbHex(): Long =
    toLongOrNull(16) ?: throw MalformedRichTextJsonException("Invalid argb hex value: $this")

internal fun TextUnit.toJsonObject(): JsonObject = buildJsonObject {
    put("value", value.toDouble())
    put("unit", if (type == TextUnitType.Em) "em" else "sp")
}

internal fun textUnitFromJson(json: JsonObject): TextUnit {
    val value = (json["value"] as? JsonPrimitive)?.doubleOrNull
        ?: throw MalformedRichTextJsonException("Missing \"value\" in unit object")
    val unit = (json["unit"] as? JsonPrimitive)?.contentOrNull
        ?: throw MalformedRichTextJsonException("Missing \"unit\" in unit object")
    val type = when (unit) {
        "sp" -> TextUnitType.Sp
        "em" -> TextUnitType.Em
        else -> throw MalformedRichTextJsonException("Unknown unit type: $unit")
    }
    return TextUnit(value.toFloat(), type)
}

internal fun TextAlign.toJsonName(): String? = when (this) {
    TextAlign.Left -> "left"
    TextAlign.Right -> "right"
    TextAlign.Center -> "center"
    TextAlign.Justify -> "justify"
    TextAlign.Start -> "start"
    TextAlign.End -> "end"
    else -> null
}

internal fun textAlignFromJson(name: String): TextAlign = when (name) {
    "left" -> TextAlign.Left
    "right" -> TextAlign.Right
    "center" -> TextAlign.Center
    "justify" -> TextAlign.Justify
    "start" -> TextAlign.Start
    "end" -> TextAlign.End
    else -> throw MalformedRichTextJsonException("Unknown align value: $name")
}

internal fun TextDirection.toJsonName(): String? = when (this) {
    TextDirection.Ltr -> "ltr"
    TextDirection.Rtl -> "rtl"
    TextDirection.Content -> "content"
    else -> null
}

internal fun textDirectionFromJson(name: String): TextDirection = when (name) {
    "ltr" -> TextDirection.Ltr
    "rtl" -> TextDirection.Rtl
    "content" -> TextDirection.Content
    else -> throw MalformedRichTextJsonException("Unknown dir value: $name")
}
