package com.mohamedrejeb.richeditor.sample.common.lab

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextFeature

/**
 * Shows the editor lab on the home screen. A development aid for manual device checks, so it
 * stays off in committed code: flip it locally while testing.
 */
const val EDITOR_LAB_ENABLED: Boolean = false

/**
 * A fixed document for one family of manual checks.
 *
 * @property editorHeight the editor's height, or null to let it grow with its content. A fixed
 * height makes the editor scroll on its own, which the scroll offset checks need.
 * @property fontSizedLines whether each line takes its height from its font instead of the
 * theme's fixed line height, which the caret size checks need.
 */
internal enum class LabScenario(
    val label: String,
    val html: String,
    val editorHeight: Dp?,
    val fontSizedLines: Boolean = false,
) {
    Paragraphs(
        label = "Paragraphs",
        html = "<p>Hi</p>" +
            "<p>This second paragraph is long enough to wrap across three lines on a phone " +
            "and carries one <b>bold</b> word in the middle for the handle drag check.</p>" +
            "<p>End</p>",
        editorHeight = null,
    ),
    Scrolling(
        label = "Scrolling",
        html = (1..SCROLLING_PARAGRAPH_COUNT).joinToString(separator = "") { index ->
            val tail = if (index % LONG_LINE_INTERVAL == 0) " with a longer tail of words" else ""
            "<p>Line $index$tail</p>"
        },
        editorHeight = 140.dp,
    ),
    Lists(
        label = "Lists",
        html = "<p>Before the list</p>" +
            "<ul><li>First item</li><li>Second <b>bold</b> item</li></ul>" +
            "<ol><li>One</li><li>Two</li></ol>" +
            "<p>After the list</p>",
        editorHeight = null,
    ),
    FontSizes(
        label = "Font sizes",
        html = "<p><span style=\"font-size: 40px\">Big</span></p>" +
            "<p><span style=\"font-size: 40px\"></span></p>" +
            "<p>Small</p>",
        editorHeight = null,
        fontSizedLines = true,
    ),
}

private const val SCROLLING_PARAGRAPH_COUNT = 15

// Lines of different lengths, so the empty area after a short line can be dragged into.
private const val LONG_LINE_INTERVAL = 3

/**
 * A feature set for the editor under test, so a paste can be checked against a restriction on
 * a phone or in the browser.
 */
@OptIn(ExperimentalRichTextApi::class)
internal enum class LabFeatures(
    val label: String,
    val features: Set<RichTextFeature>,
) {
    All(label = "All features", features = RichTextFeature.All),
    Emphasis(
        label = "Bold, italic, underline",
        features = setOf(RichTextFeature.Bold, RichTextFeature.Italic, RichTextFeature.Underline),
    ),
    Plain(label = "Plain text", features = emptySet()),
}
