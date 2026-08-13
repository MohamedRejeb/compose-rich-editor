package com.mohamedrejeb.richeditor.json

import androidx.compose.ui.text.style.TextDirection
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextBlockType
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Regression pins for the pre-merge code review of the JSON codec: every input the decoder
 * accepts must re-encode without crashing, and every rejection must surface as
 * [MalformedRichTextJsonException], never as a raw [IllegalArgumentException] subtype leak.
 */
class JsonCodecReviewRegressionTest {

    private fun blockJson(spans: String): String =
        """{"v":1,"blocks":[{"id":"b0","type":"paragraph","text":"ab","spans":[$spans]}]}"""

    @Test
    fun `non-finite float values are malformed`() {
        assertFailsWith<MalformedRichTextJsonException> {
            RichTextDocumentCodec.decodeFromString(blockJson("""{"k":"baseline-shift","r":[0,0],"value":"NaN"}"""))
        }
        assertFailsWith<MalformedRichTextJsonException> {
            RichTextDocumentCodec.decodeFromString(
                blockJson("""{"k":"font-size","r":[0,0],"value":"Infinity","unit":"sp"}""")
            )
        }
        assertFailsWith<MalformedRichTextJsonException> {
            RichTextDocumentCodec.decodeFromString(
                blockJson("""{"k":"shadow","r":[0,0],"argb":"FF000000","x":"NaN","y":0,"blur":0}""")
            )
        }
    }

    @Test
    fun `font weight outside 1 to 1000 is malformed`() {
        assertFailsWith<MalformedRichTextJsonException> {
            RichTextDocumentCodec.decodeFromString(blockJson("""{"k":"font-weight","r":[0,0],"value":5000}"""))
        }
        assertFailsWith<MalformedRichTextJsonException> {
            RichTextDocumentCodec.decodeFromString(blockJson("""{"k":"font-weight","r":[0,0],"value":0}"""))
        }
    }

    @Test
    fun `negative list indent is malformed, not a raw exception`() {
        val failure = assertFailsWith<MalformedRichTextJsonException> {
            RichTextDocumentCodec.decodeFromString(
                """{"v":1,"blocks":[{"id":"b0","type":"list-item","ordered":true,"indent":-1,"text":"x","spans":[]}]}"""
            )
        }
        assertTrue(failure.message.orEmpty().contains("indent"))
    }

    @Test
    fun `negative ordered start decodes successfully`() {
        val doc = RichTextDocumentCodec.decodeFromString(
            """{"v":1,"blocks":[{"id":"b0","type":"list-item","ordered":true,"indent":0,"start":-5,"text":"x","spans":[]}]}"""
        )
        assertEquals(
            RichTextBlockType.ListItem(ordered = true, indent = 0, startNumber = -5),
            doc.blocks.single().type,
        )
    }

    @Test
    fun `argb must be exactly 8 hex digits`() {
        listOf("FF0000", "-FF00000", "1FFFFFFFF", "GG000000", "").forEach { bad ->
            assertFailsWith<MalformedRichTextJsonException>("accepted argb \"$bad\"") {
                RichTextDocumentCodec.decodeFromString(blockJson("""{"k":"color","r":[0,0],"argb":"$bad"}"""))
            }
        }
    }

    @Test
    fun `encode masks argb to 32 bits`() {
        val json = RichTextDocumentCodec.encodeToString(
            RichTextDocument(
                blocks = listOf(
                    RichTextBlock(
                        text = "x",
                        spans = listOf(RichTextSpanMark.TextColor(range = 0..0, argb = -1L)),
                    ),
                ),
            ),
        )
        assertTrue(json.contains("\"argb\":\"FFFFFFFF\""), json)
        RichTextDocumentCodec.decodeFromString(json)
    }

    @Test
    fun `unknown mark with invalid rawJson still encodes`() {
        val json = RichTextDocumentCodec.encodeToString(
            RichTextDocument(
                blocks = listOf(
                    RichTextBlock(
                        text = "x",
                        spans = listOf(
                            RichTextSpanMark.Unknown(range = 0..0, kind = "sparkle", rawJson = "not json"),
                        ),
                    ),
                ),
            ),
        )
        assertTrue(json.contains("\"k\":\"sparkle\""), json)
        RichTextDocumentCodec.decodeFromString(json)
    }

    @Test
    fun `content-or directions round-trip`() {
        val doc = RichTextDocument(
            blocks = listOf(RichTextBlock(text = "x", textDirection = TextDirection.ContentOrLtr)),
        )
        val json = RichTextDocumentCodec.encodeToString(doc)
        assertTrue(json.contains("\"dir\":\"content-or-ltr\""), json)
        assertEquals(doc, RichTextDocumentCodec.decodeFromString(json))
    }
}
