package com.mohamedrejeb.richeditor.parser

import com.mohamedrejeb.richeditor.model.RichTextState

internal interface RichTextStateParser<T> {

    fun encode(input: T): RichTextState

    fun decode(richTextState: RichTextState): T

}