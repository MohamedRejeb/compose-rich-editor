package com.mohamedrejeb.richeditor.paragraph.type

import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class OrderedListIndentTest {

    private fun config(
        indent: Int,
        alignment: ListPrefixAlignment = ListPrefixAlignment.End,
    ): RichTextConfig =
        RichTextConfig(updateText = {}).apply {
            orderedListIndent = indent
            unorderedListIndent = indent
            listPrefixAlignment = alignment
        }

    @Test
    fun indentLargerThanPrefixAlignsDots() {
        val config = config(indent = 38)
        val list = OrderedList(number = 1, config = config, startTextWidth = 20.sp)

        val textIndent = list.getStyle(config).textIndent
        assertNotNull(textIndent)

        // 38 >= 20: classic hanging indent - prefix sits in the gutter, dots align.
        assertEquals(18f, textIndent.firstLine.value)
        assertEquals(38f, textIndent.restLine.value)
        assertTrue(textIndent.firstLine.value < textIndent.restLine.value)
    }

    @Test
    fun indentSmallerThanPrefixClampsFirstLineToZero() {
        val config = config(indent = 10)
        val list = OrderedList(number = 1, config = config, startTextWidth = 50.sp)

        val textIndent = list.getStyle(config).textIndent
        assertNotNull(textIndent)

        // base (10) < prefix (50): the natural base - prefix would be -40, which would
        // clip the marker. Clamp firstLine at 0 instead of flipping formulas - keeps
        // every item in the list on the same layout regardless of per-item marker width.
        assertEquals(0f, textIndent.firstLine.value)
        assertEquals(10f, textIndent.restLine.value)
        assertTrue(textIndent.firstLine.value >= 0f)
    }

    @Test
    fun zeroIndentKeepsPrefixVisible() {
        val config = config(indent = 0)
        val list = OrderedList(number = 1, config = config, startTextWidth = 20.sp)

        val textIndent = list.getStyle(config).textIndent
        assertNotNull(textIndent)

        // Edge case: indent = 0. firstLine clamps to 0; restLine stays at the base.
        assertEquals(0f, textIndent.firstLine.value)
        assertEquals(0f, textIndent.restLine.value)
    }

    @Test
    fun deeperLevelGrowsTheGutterAndRestoresDotAlignment() {
        val config = config(indent = 10)
        // Level 2: base (20) < prefix (50) → firstLine clamps to 0.
        val listLevel2 = OrderedList(number = 1, config = config, startTextWidth = 50.sp, initialLevel = 2)
        val level2Indent = listLevel2.getStyle(config).textIndent!!
        assertEquals(0f, level2Indent.firstLine.value)
        assertEquals(20f, level2Indent.restLine.value)

        // At level 6, base (60) >= prefix (50) → the dot-aligned layout is restored naturally.
        val listLevel6 = OrderedList(number = 1, config = config, startTextWidth = 50.sp, initialLevel = 6)
        val level6Indent = listLevel6.getStyle(config).textIndent!!
        assertEquals(10f, level6Indent.firstLine.value)
        assertEquals(60f, level6Indent.restLine.value)
    }

    @Test
    fun endAlignmentAppliesConsistentFormulaRegardlessOfMarkerWidth() {
        // Two items in the "same list" (same indent config) with different marker widths
        // (e.g. "1." vs "viii."). Both must use the End formula so they render
        // consistently; the old fallback would switch one to Start and make the list
        // visually uneven.
        val config = config(indent = 30)
        val narrowMarker = OrderedList(number = 1, config = config, startTextWidth = 10.sp)
        val wideMarker = OrderedList(number = 8, config = config, startTextWidth = 40.sp)

        val narrowIndent = narrowMarker.getStyle(config).textIndent!!
        val wideIndent = wideMarker.getStyle(config).textIndent!!

        // Both items use the End formula with restLine = base.
        assertEquals(30f, narrowIndent.restLine.value)
        assertEquals(30f, wideIndent.restLine.value)

        // Narrow marker fits in the gutter; wide marker clamps firstLine to 0.
        assertEquals(20f, narrowIndent.firstLine.value)
        assertEquals(0f, wideIndent.firstLine.value)
    }

    @Test
    fun startAlignmentForcesUniformLeftEdgeEvenWhenGutterFits() {
        val config = config(indent = 38, alignment = ListPrefixAlignment.Start)
        val list = OrderedList(number = 1, config = config, startTextWidth = 20.sp)

        val textIndent = list.getStyle(config).textIndent
        assertNotNull(textIndent)

        // Start alignment ignores the "gutter fits" case: prefix starts at the indent origin.
        assertEquals(38f, textIndent.firstLine.value)
        assertEquals(58f, textIndent.restLine.value)
    }

    @Test
    fun changingAlignmentOnConfigUpdatesExistingList() {
        val config = config(indent = 38, alignment = ListPrefixAlignment.End)
        val list = OrderedList(number = 1, config = config, startTextWidth = 20.sp)

        // Starts in End (classic dot-aligned).
        val endIndent = list.getStyle(config).textIndent!!
        assertEquals(18f, endIndent.firstLine.value)
        assertEquals(38f, endIndent.restLine.value)

        // Flip the config at runtime; the existing list picks it up on the next getStyle.
        config.listPrefixAlignment = ListPrefixAlignment.Start
        val startIndent = list.getStyle(config).textIndent!!
        assertEquals(38f, startIndent.firstLine.value)
        assertEquals(58f, startIndent.restLine.value)
    }

    @Test
    fun unorderedListRespectsAlignment() {
        val endConfig = config(indent = 38, alignment = ListPrefixAlignment.End)
        val endList = UnorderedList(config = endConfig).apply { startTextWidth = 12.sp }
        val endIndent = endList.getStyle(endConfig).textIndent!!
        assertEquals(26f, endIndent.firstLine.value)
        assertEquals(38f, endIndent.restLine.value)

        val startConfig = config(indent = 38, alignment = ListPrefixAlignment.Start)
        val startList = UnorderedList(config = startConfig).apply { startTextWidth = 12.sp }
        val startIndent = startList.getStyle(startConfig).textIndent!!
        assertEquals(38f, startIndent.firstLine.value)
        assertEquals(50f, startIndent.restLine.value)
    }
}
