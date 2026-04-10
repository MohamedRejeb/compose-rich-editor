package com.mohamedrejeb.richeditor.clipboard.utils

/**
 * Utility class for handling HTML clipboard operations.
 * Provides methods for formatting and parsing HTML content for clipboard operations.
 */
internal object HtmlClipboardUtils {
    /**
     * HTML header for clipboard content
     */
    private const val HTML_HEADER = "Version:0.9\n" +
        "StartHTML:00000000\n" +
        "EndHTML:00000000\n" +
        "StartFragment:00000000\n" +
        "EndFragment:00000000\n" +
        "StartSelection:00000000\n" +
        "EndSelection:00000000"

    /**
     * Fragment markers for HTML content
     */
    private const val FRAGMENT_START = "<!--StartFragment-->"
    private const val FRAGMENT_END = "<!--EndFragment-->"

    /**
     * Formats HTML content for clipboard by adding necessary headers and markers
     *
     * @param htmlContent The HTML content to be formatted
     * @return Formatted HTML string ready for clipboard
     */
    fun formatHtmlForClipboard(htmlContent: String): String {
        val htmlWithMarkers = buildString {
            appendLine("<html><body>")
            appendLine(FRAGMENT_START)
            appendLine(htmlContent)
            appendLine(FRAGMENT_END)
            appendLine("</body></html>")
        }

        val startHtml = HTML_HEADER.length + 1
        val startFragment = startHtml + htmlWithMarkers.indexOf(FRAGMENT_START)
        val endFragment = startHtml + htmlWithMarkers.indexOf(FRAGMENT_END) + FRAGMENT_END.length
        val endHtml = startHtml + htmlWithMarkers.length

        val header = HTML_HEADER
            .replace("StartHTML:00000000", "StartHTML:%.8d".format(startHtml))
            .replace("EndHTML:00000000", "EndHTML:%.8d".format(endHtml))
            .replace("StartFragment:00000000", "StartFragment:%.8d".format(startFragment))
            .replace("EndFragment:00000000", "EndFragment:%.8d".format(endFragment))
            .replace("StartSelection:00000000", "StartSelection:%.8d".format(startFragment))
            .replace("EndSelection:00000000", "EndSelection:%.8d".format(endFragment))

        return "$header\n$htmlWithMarkers"
    }

    /**
     * Extracts HTML content from clipboard format by removing headers and markers
     *
     * @param clipboardHtml The HTML content from clipboard
     * @return Clean HTML content without clipboard-specific formatting
     */
    fun extractHtmlFromClipboard(clipboardHtml: String): String {
        val startMarkerIndex = clipboardHtml.indexOf(FRAGMENT_START)
        val endMarkerIndex = clipboardHtml.indexOf(FRAGMENT_END)

        return if (startMarkerIndex != -1 && endMarkerIndex != -1) {
            clipboardHtml.substring(
                startMarkerIndex + FRAGMENT_START.length,
                endMarkerIndex
            ).trim()
        } else {
            clipboardHtml.trim()
        }
    }
}
