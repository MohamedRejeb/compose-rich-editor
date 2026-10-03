package com.mohamedrejeb.richeditor.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.type.ListMarkerStyleBehavior
import com.mohamedrejeb.richeditor.paragraph.type.ListPrefixAlignment
import com.mohamedrejeb.richeditor.paragraph.type.OrderedListStyleType
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedListStyleType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every rendering property on [RichTextConfig] rebuilds the editor content when set. Apps
 * assign them in composition or in a LaunchedEffect, often with the value they already hold,
 * so an assignment that changes nothing must not cost a rebuild.
 */
@OptIn(ExperimentalRichTextApi::class)
class RichTextConfigTest {

    private class Counted {
        var rebuilds = 0
        val config = RichTextConfig(updateText = { rebuilds++ })
    }

    @Test
    fun `assigning the current value does not rebuild`() {
        val counted = Counted()
        val config = counted.config

        config.linkColor = config.linkColor
        config.linkTextDecoration = config.linkTextDecoration
        config.codeSpanColor = config.codeSpanColor
        config.codeSpanBackgroundColor = config.codeSpanBackgroundColor
        config.codeSpanStrokeColor = config.codeSpanStrokeColor
        config.codeSpanCornerRadius = config.codeSpanCornerRadius
        config.codeSpanStrokeWidth = config.codeSpanStrokeWidth
        config.codeSpanPadding = config.codeSpanPadding
        config.orderedListIndent = config.orderedListIndent
        config.unorderedListIndent = config.unorderedListIndent
        config.listIndent = config.listIndent
        config.unorderedListStyleType = config.unorderedListStyleType
        config.orderedListStyleType = config.orderedListStyleType
        config.listMarkerStyleBehavior = config.listMarkerStyleBehavior
        config.listPrefixAlignment = config.listPrefixAlignment

        assertEquals(0, counted.rebuilds)
    }

    @Test
    fun `assigning an equal but distinct value does not rebuild`() {
        val counted = Counted()
        val config = counted.config

        config.linkColor = Color(0xFF0000FF)
        config.codeSpanPadding = TextPaddingValues(horizontal = 2.sp, vertical = 2.sp)
        config.unorderedListStyleType = UnorderedListStyleType.from("•", "◦", "▪")
        config.codeSpanCornerRadius = 8.sp

        assertEquals(0, counted.rebuilds)
    }

    @Test
    fun `assigning a new value rebuilds once`() {
        val counted = Counted()
        val config = counted.config

        config.linkColor = Color.Red
        config.linkTextDecoration = TextDecoration.None
        config.codeSpanColor = Color.Red
        config.codeSpanBackgroundColor = Color.Red
        config.codeSpanStrokeColor = Color.Red
        config.codeSpanCornerRadius = 1.sp
        config.codeSpanStrokeWidth = 2.sp
        config.codeSpanPadding = TextPaddingValues(horizontal = 1.sp, vertical = 1.sp)
        config.orderedListIndent = 10
        config.unorderedListIndent = 11
        config.unorderedListStyleType = UnorderedListStyleType.from("-")
        config.orderedListStyleType = OrderedListStyleType.LowerRoman
        config.listMarkerStyleBehavior = ListMarkerStyleBehavior.AlwaysDefault
        config.listPrefixAlignment = ListPrefixAlignment.Start

        assertEquals(14, counted.rebuilds)
    }

    @Test
    fun `listIndent sets both indents and rebuilds once per changed indent`() {
        val counted = Counted()
        val config = counted.config

        config.listIndent = 20

        assertEquals(20, config.orderedListIndent)
        assertEquals(20, config.unorderedListIndent)
        assertEquals(20, config.listIndent)
        assertEquals(2, counted.rebuilds)
    }

    @Test
    fun `listIndent reads the shared indent when both agree`() {
        val config = Counted().config

        config.orderedListIndent = 30
        config.unorderedListIndent = 30

        assertEquals(30, config.listIndent)
    }

    @Test
    fun `listIndent keeps its last assigned value when the indents differ`() {
        val config = Counted().config

        config.listIndent = 20
        config.orderedListIndent = 44

        assertEquals(20, config.listIndent)
        assertEquals(44, config.orderedListIndent)
        assertEquals(20, config.unorderedListIndent)
    }
}
