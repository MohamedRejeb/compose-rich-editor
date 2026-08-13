package com.mohamedrejeb.richeditor.json

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every mark kind must survive a codec round-trip and emit its documented "k" value. */
class RichTextDocumentCodecMarkKindsTest {

    private val sp20 = TextUnit(20f, TextUnitType.Sp)
    private val em2 = TextUnit(2f, TextUnitType.Em)

    private val kinds: List<Pair<String, RichTextSpanMark>> = listOf(
        "bold" to RichTextSpanMark.Bold(range = 0..3),
        "italic" to RichTextSpanMark.Italic(range = 0..3),
        "underline" to RichTextSpanMark.Underline(range = 0..3),
        "strike" to RichTextSpanMark.Strikethrough(range = 0..3),
        "code" to RichTextSpanMark.CodeSpan(range = 0..3),
        "link" to RichTextSpanMark.Link(range = 0..3, url = "https://example.com"),
        "color" to RichTextSpanMark.TextColor(range = 0..3, argb = 0xFF112233),
        "highlight" to RichTextSpanMark.Highlight(range = 0..3, argb = 0xFFFFFF00),
        "font-size" to RichTextSpanMark.FontSize(range = 0..3, size = sp20),
        "font-size" to RichTextSpanMark.FontSize(range = 0..3, size = em2),
        "font-weight" to RichTextSpanMark.FontWeight(range = 0..3, weight = 200),
        "letter-spacing" to RichTextSpanMark.LetterSpacing(range = 0..3, size = sp20),
        "baseline-shift" to RichTextSpanMark.BaselineShift(range = 0..3, multiplier = -0.5f),
        "shadow" to RichTextSpanMark.Shadow(range = 0..3, argb = 0xFF000000, offsetX = 1f, offsetY = 2f, blurRadius = 3f),
        "image" to RichTextSpanMark.Image(range = 0..0, url = "https://e.com/i.png"),
        "image" to RichTextSpanMark.Image(range = 0..0, url = "https://e.com/i.png", width = sp20, height = sp20, description = "alt text"),
        "token" to RichTextSpanMark.Token(range = 0..3, trigger = "@", id = "u1", label = "user"),
        "sparkle" to RichTextSpanMark.Unknown(range = 0..3, kind = "sparkle", rawJson = """{"k":"sparkle","r":[0,3],"level":9}"""),
    )

    @Test
    fun `every mark kind round-trips through the codec`() {
        kinds.forEach { (kind, mark) ->
            val doc = RichTextDocument(
                blocks = listOf(RichTextBlock(text = "abcd", spans = listOf(mark))),
            )
            val json = RichTextDocumentCodec.encodeToString(doc)
            assertTrue(json.contains("\"k\":\"$kind\""), "Missing kind $kind in: $json")
            assertEquals(doc, RichTextDocumentCodec.decodeFromString(json), "Round-trip failed for $kind")
        }
    }
}
