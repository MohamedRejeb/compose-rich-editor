package com.mohamedrejeb.richeditor.parser

import com.mohamedrejeb.richeditor.model.RichTextValue

internal interface RichTextParser<T> {

    fun encode(input: T): RichTextValue

    fun decode(richTextValue: RichTextValue): T

}