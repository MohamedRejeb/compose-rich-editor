package com.mohamedrejeb.richeditor.json

import com.mohamedrejeb.richeditor.document.RichTextSpanMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RichTextDocumentCodecDecodeTest {

    @Test
    fun `decodes the golden document`() {
        val doc = RichTextDocumentCodec.decodeFromString(
            """{"v":1,"blocks":[{"id":"b0","type":"heading","level":1,"text":"Title","spans":[]}]}"""
        )
        assertEquals(1, doc.blocks.single().headingLevel)
        assertEquals("Title", doc.blocks.single().text)
    }

    @Test
    fun `block ids are ignored`() {
        val a = RichTextDocumentCodec.decodeFromString(
            """{"v":1,"blocks":[{"id":"x","type":"paragraph","text":"a","spans":[]}]}"""
        )
        val b = RichTextDocumentCodec.decodeFromString(
            """{"v":1,"blocks":[{"id":"y","type":"paragraph","text":"a","spans":[]}]}"""
        )
        assertEquals(a, b)
    }

    @Test
    fun `unknown mark kind is preserved`() {
        val doc = RichTextDocumentCodec.decodeFromString(
            """{"v":1,"blocks":[{"id":"b0","type":"paragraph","text":"ab","spans":[{"k":"sparkle","r":[0,1],"level":9}]}]}"""
        )
        val unknown = doc.blocks.single().spans.single() as RichTextSpanMark.Unknown
        assertEquals("sparkle", unknown.kind)
        assertEquals(0..1, unknown.range)
    }

    @Test
    fun `newer version is rejected`() {
        assertFailsWith<UnsupportedRichTextJsonVersionException> {
            RichTextDocumentCodec.decodeFromString("""{"v":2,"blocks":[]}""")
        }
    }

    @Test
    fun `missing or invalid version is malformed`() {
        assertFailsWith<MalformedRichTextJsonException> {
            RichTextDocumentCodec.decodeFromString("""{"blocks":[]}""")
        }
        assertFailsWith<MalformedRichTextJsonException> {
            RichTextDocumentCodec.decodeFromString("""{"v":0,"blocks":[]}""")
        }
        assertFailsWith<MalformedRichTextJsonException> {
            RichTextDocumentCodec.decodeFromString("not json")
        }
        assertFailsWith<MalformedRichTextJsonException> {
            RichTextDocumentCodec.decodeFromString("""{"v":1}""")
        }
        assertFailsWith<MalformedRichTextJsonException> {
            RichTextDocumentCodec.decodeFromString(
                """{"v":1,"blocks":[{"id":"b0","type":"paragraph","text":"a","spans":[{"k":"bold","r":[0,5]}]}]}"""
            )
        }
    }
}
