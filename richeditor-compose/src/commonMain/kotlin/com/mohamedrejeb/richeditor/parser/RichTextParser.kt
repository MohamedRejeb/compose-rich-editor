package com.mohamedrejeb.richeditor.parser

import com.mohamedrejeb.richeditor.model.RichTextValue

interface RichTextParser {

    fun encode(input: String): RichTextValue

    fun decode(input: RichTextValue): String

}