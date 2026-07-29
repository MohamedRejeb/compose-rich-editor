package com.mohamedrejeb.richeditor.json

import androidx.compose.ui.text.style.TextAlign
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextBlockType
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextDocumentCodecRoundTripTest {

    @Test
    fun `document to json to document is the identity`() {
        val doc = RichTextDocument(
            blocks = listOf(
                RichTextBlock(text = "Title", headingLevel = 2),
                RichTextBlock(
                    text = "body ￼",
                    textAlign = TextAlign.End,
                    spans = listOf(
                        RichTextSpanMark.Italic(range = 0..3),
                        RichTextSpanMark.Image(range = 5..5, url = "https://e.com/i.png"),
                        RichTextSpanMark.Unknown(
                            range = 0..0,
                            kind = "sparkle",
                            rawJson = """{"k":"sparkle","r":[0,0],"level":9}""",
                        ),
                    ),
                ),
                RichTextBlock(
                    text = "item",
                    type = RichTextBlockType.ListItem(ordered = true, indent = 1, startNumber = 4),
                ),
            ),
        )
        assertEquals(doc, RichTextDocumentCodec.decodeFromString(RichTextDocumentCodec.encodeToString(doc)))
    }

    @Test
    fun `encode is idempotent through decode including unknown marks`() {
        val original =
            """{"v":1,"blocks":[{"id":"b0","type":"paragraph","text":"ab","spans":[{"k":"sparkle","r":[0,1],"level":9}]}]}"""
        val once = RichTextDocumentCodec.encodeToString(RichTextDocumentCodec.decodeFromString(original))
        val twice = RichTextDocumentCodec.encodeToString(RichTextDocumentCodec.decodeFromString(once))
        assertEquals(once, twice)
        assertEquals(original, once)
    }
}
