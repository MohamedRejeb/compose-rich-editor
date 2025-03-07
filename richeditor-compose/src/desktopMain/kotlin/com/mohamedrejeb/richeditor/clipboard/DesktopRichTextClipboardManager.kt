package com.mohamedrejeb.richeditor.clipboard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.awtClipboard
import com.mohamedrejeb.richeditor.model.RichTextState
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import com.mohamedrejeb.richeditor.clipboard.utils.HtmlClipboardUtils

internal actual fun createRichTextClipboardManager(
    richTextState: RichTextState,
    clipboard: Clipboard
): RichTextClipboardManager =
    DesktopRichTextClipboardManager(
        richTextState = richTextState,
        clipboard = clipboard
    )

/**
 * Desktop implementation of [RichTextClipboardManager].
 * Handles rich text clipboard operations using AWT's clipboard functionality.
 *
 * @property richTextState The [RichTextState] to be used for clipboard operations
 * @property clipboard The Compose [Clipboard] for handling clipboard operations
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class DesktopRichTextClipboardManager(
    private val richTextState: RichTextState,
    private val clipboard: Clipboard,
) : RichTextClipboardManager, Clipboard by clipboard {

    override suspend fun getClipEntry(): ClipEntry? {
        println("getClipEntry")
        try {
            val transferable = awtClipboard?.getContents(null) ?: return null
            when {
                transferable.isDataFlavorSupported(DataFlavor.fragmentHtmlFlavor) -> {
                    val rawHtmlText = transferable.getTransferData(DataFlavor.fragmentHtmlFlavor) as String
                    val plainText = transferable.getTransferData(DataFlavor.stringFlavor) as String
                    val htmlText = HtmlClipboardUtils.extractHtmlFromClipboard(rawHtmlText)
                    println("rawHtmlText: $rawHtmlText")
                    println("htmlText: $htmlText")
                    richTextState.insertHtmlAfterSelection(rawHtmlText)
                }
                transferable.isDataFlavorSupported(DataFlavor.stringFlavor) -> {
                    val plainText = transferable.getTransferData(DataFlavor.stringFlavor) as String
                    richTextState.addTextAfterSelection(plainText)
                }
                else -> null
            }
        } catch (e: Exception) {

        }

        return null
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        val html = richTextState.toHtml(richTextState.selection)
        val text = richTextState.toText(richTextState.selection)
        println("richTextState.selection: ${richTextState.selection}")
        println("selection html: $html")
        println("selection text: $text")

        val htmlSelection = object : StringSelection(html), Transferable {
            override fun getTransferDataFlavors(): Array<DataFlavor> =
                arrayOf(DataFlavor.fragmentHtmlFlavor, DataFlavor.stringFlavor)

            override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean =
                flavor in getTransferDataFlavors()

            override fun getTransferData(flavor: DataFlavor?): Any = when (flavor) {
                DataFlavor.fragmentHtmlFlavor -> html
                DataFlavor.stringFlavor -> text
                else -> throw UnsupportedOperationException("Unsupported flavor: $flavor")
            }
        }

        awtClipboard?.setContents(htmlSelection, null)
    }

    override fun getRichTextContent() {
        try {
            val transferable = awtClipboard?.getContents(null) ?: return
            when {
                transferable.isDataFlavorSupported(DataFlavor.fragmentHtmlFlavor) -> {
                    val rawHtmlText = transferable.getTransferData(DataFlavor.fragmentHtmlFlavor) as String
                    val plainText = transferable.getTransferData(DataFlavor.stringFlavor) as String
//                    RichTextClipboardManager.RichTextContent(
//                        htmlText = HtmlClipboardUtils.extractHtmlFromClipboard(rawHtmlText),
//                        plainText = plainText,
//                    )
                }
                transferable.isDataFlavorSupported(DataFlavor.stringFlavor) -> {
                    val plainText = transferable.getTransferData(DataFlavor.stringFlavor) as String
//                    RichTextClipboardManager.RichTextContent(
//                        plainText = plainText,
//                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun setRichTextContent() {
        val htmlText = ""
        val plainText = ""
//        val htmlText = content.htmlText
//        val plainText = content.plainText
//        val annotatedString = content.annotatedString

        // Set the content to system clipboard
        if (htmlText != null) {
            val formattedHtmlText = HtmlClipboardUtils.formatHtmlForClipboard(htmlText)
            val htmlSelection = object : StringSelection(plainText), Transferable {
                override fun getTransferDataFlavors(): Array<DataFlavor> =
                    arrayOf(DataFlavor.fragmentHtmlFlavor, DataFlavor.stringFlavor)

                override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean =
                    flavor in getTransferDataFlavors()

                override fun getTransferData(flavor: DataFlavor?): Any = when (flavor) {
                    DataFlavor.fragmentHtmlFlavor -> formattedHtmlText
                    DataFlavor.stringFlavor -> plainText
                    else -> throw UnsupportedOperationException("Unsupported flavor: $flavor")
                }
            }
            awtClipboard?.setContents(htmlSelection, null)
        } else {
//            awtClipboard?.setContents(StringSelection(plainText), null)
        }

        // Set the annotated string to Compose clipboard manager
//        if (annotatedString != null) {
//            awtClipboard?.setText(annotatedString)
//        }
    }

    override fun hasRichTextContent(): Boolean {
        val transferable = awtClipboard?.getContents(null)
        return transferable?.isDataFlavorSupported(DataFlavor.fragmentHtmlFlavor) == true ||
                transferable?.isDataFlavorSupported(DataFlavor.stringFlavor) == true
    }

}
