package com.mohamedrejeb.richeditor.json

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.mohamedrejeb.richeditor.document.RichTextBlock
import com.mohamedrejeb.richeditor.document.RichTextBlockType
import com.mohamedrejeb.richeditor.document.RichTextDocument
import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import kotlin.test.Test
import kotlin.test.assertEquals

class RichTextDocumentCodecEncodeTest {

    @Test
    fun `empty document`() {
        assertEquals(
            """{"v":1,"blocks":[{"id":"b0","type":"paragraph","text":"","spans":[]}]}""",
            RichTextDocumentCodec.encodeToString(RichTextDocument.empty()),
        )
    }

    @Test
    fun `golden document with heading, alignment, marks, and ordered list`() {
        val doc = RichTextDocument(
            blocks = listOf(
                RichTextBlock(text = "Title", headingLevel = 1),
                RichTextBlock(
                    text = "Hello bold link",
                    textAlign = TextAlign.Center,
                    spans = listOf(
                        RichTextSpanMark.Bold(range = 6..9),
                        RichTextSpanMark.Link(range = 11..14, url = "https://example.com"),
                    ),
                ),
                RichTextBlock(text = "One", type = RichTextBlockType.ListItem(ordered = true)),
            ),
        )
        assertEquals(
            """{"v":1,"blocks":[""" +
                """{"id":"b0","type":"heading","level":1,"text":"Title","spans":[]},""" +
                """{"id":"b1","type":"paragraph","align":"center","text":"Hello bold link","spans":[""" +
                """{"k":"bold","r":[6,9]},{"k":"link","r":[11,14],"url":"https://example.com"}]},""" +
                """{"id":"b2","type":"list-item","ordered":true,"indent":0,"text":"One","spans":[]}]}""",
            RichTextDocumentCodec.encodeToString(doc),
        )
    }

    @Test
    fun `color highlight and font-size marks`() {
        val doc = RichTextDocument(
            blocks = listOf(
                RichTextBlock(
                    text = "x",
                    spans = listOf(
                        RichTextSpanMark.TextColor(range = 0..0, argb = 0xFFFF0000),
                        RichTextSpanMark.Highlight(range = 0..0, argb = 0xFF00FF00),
                        RichTextSpanMark.FontSize(range = 0..0, size = TextUnit(20f, TextUnitType.Sp)),
                    ),
                ),
            ),
        )
        assertEquals(
            """{"v":1,"blocks":[{"id":"b0","type":"paragraph","text":"x","spans":[""" +
                """{"k":"color","r":[0,0],"argb":"FFFF0000"},""" +
                """{"k":"highlight","r":[0,0],"argb":"FF00FF00"},""" +
                """{"k":"font-size","r":[0,0],"value":20.0,"unit":"sp"}]}]}""",
            RichTextDocumentCodec.encodeToString(doc),
        )
    }
}
