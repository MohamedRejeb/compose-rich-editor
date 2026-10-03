package com.mohamedrejeb.richeditor.model

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.document.RichTextDocumentDecoder
import com.mohamedrejeb.richeditor.document.RichTextDocumentEncoder
import com.mohamedrejeb.richeditor.paragraph.RichParagraph

/**
 * The parsed [paragraphs] with everything outside [RichTextConfig.features] stripped, ready
 * for insertion or loading. The HTML and Markdown loads and inserts pass through here, and a
 * recognized paste reaches it through [RichTextState.insertHtml]; a document load filters the
 * document itself instead. Returns [paragraphs] untouched when every feature is allowed, so
 * the default path never pays the round trip.
 */
@OptIn(ExperimentalRichTextApi::class)
internal fun RichTextState.admit(paragraphs: List<RichParagraph>): List<RichParagraph> {
    val features = config.features
    if (features.containsAll(RichTextFeature.entries)) return paragraphs
    val document = RichTextDocumentEncoder.encode(paragraphs).restrictedTo(features)
    return RichTextDocumentDecoder.decode(document)
}
