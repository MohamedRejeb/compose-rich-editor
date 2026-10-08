package com.mohamedrejeb.richeditor.highlight

import androidx.compose.runtime.Immutable
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.highlight.languages.KotlinSpec

/** A language the library can colour. Get one from the companion, by [fromName] or by [fromFileName]. */
@ExperimentalRichTextApi
@Immutable
public class CodeLanguage internal constructor(
    public val name: String,
    internal val aliases: List<String>,
    internal val extensions: List<String>,
    internal val spec: LanguageSpec,
) {
    override fun toString(): String = "CodeLanguage($name)"

    public companion object {
        public val Kotlin: CodeLanguage = CodeLanguage("kotlin", listOf("kt", "kts"), listOf("kt", "kts"), KotlinSpec)
    }
}
