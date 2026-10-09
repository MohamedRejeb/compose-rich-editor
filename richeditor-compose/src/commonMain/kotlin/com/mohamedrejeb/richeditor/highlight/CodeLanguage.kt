package com.mohamedrejeb.richeditor.highlight

import androidx.compose.runtime.Immutable
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.highlight.languages.CSharpSpec
import com.mohamedrejeb.richeditor.highlight.languages.CSpec
import com.mohamedrejeb.richeditor.highlight.languages.CppSpec
import com.mohamedrejeb.richeditor.highlight.languages.DartSpec
import com.mohamedrejeb.richeditor.highlight.languages.GoSpec
import com.mohamedrejeb.richeditor.highlight.languages.JavaScriptSpec
import com.mohamedrejeb.richeditor.highlight.languages.JavaSpec
import com.mohamedrejeb.richeditor.highlight.languages.JsonSpec
import com.mohamedrejeb.richeditor.highlight.languages.KotlinSpec
import com.mohamedrejeb.richeditor.highlight.languages.PythonSpec
import com.mohamedrejeb.richeditor.highlight.languages.RustSpec
import com.mohamedrejeb.richeditor.highlight.languages.ShellSpec
import com.mohamedrejeb.richeditor.highlight.languages.SqlSpec
import com.mohamedrejeb.richeditor.highlight.languages.SwiftSpec
import com.mohamedrejeb.richeditor.highlight.languages.TypeScriptSpec
import com.mohamedrejeb.richeditor.highlight.languages.YamlSpec

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
        public val Java: CodeLanguage = CodeLanguage("java", emptyList(), listOf("java"), JavaSpec)
        public val JavaScript: CodeLanguage =
            CodeLanguage("javascript", listOf("js", "jsx", "mjs", "cjs", "node"), listOf("js", "jsx", "mjs", "cjs"), JavaScriptSpec)
        public val TypeScript: CodeLanguage = CodeLanguage("typescript", listOf("ts", "tsx"), listOf("ts", "tsx"), TypeScriptSpec)
        public val Python: CodeLanguage = CodeLanguage("python", listOf("py", "python3"), listOf("py"), PythonSpec)
        public val Swift: CodeLanguage = CodeLanguage("swift", emptyList(), listOf("swift"), SwiftSpec)
        public val Go: CodeLanguage = CodeLanguage("go", listOf("golang"), listOf("go"), GoSpec)
        public val Rust: CodeLanguage = CodeLanguage("rust", listOf("rs"), listOf("rs"), RustSpec)
        public val C: CodeLanguage = CodeLanguage("c", listOf("h"), listOf("c", "h"), CSpec)
        public val Cpp: CodeLanguage =
            CodeLanguage("cpp", listOf("c++", "cc", "cxx", "hpp"), listOf("cpp", "cc", "cxx", "hpp", "hh"), CppSpec)
        public val CSharp: CodeLanguage = CodeLanguage("csharp", listOf("cs", "c#"), listOf("cs"), CSharpSpec)
        public val Dart: CodeLanguage = CodeLanguage("dart", emptyList(), listOf("dart"), DartSpec)
        public val Shell: CodeLanguage =
            CodeLanguage("shell", listOf("sh", "bash", "zsh", "shellscript"), listOf("sh", "bash", "zsh"), ShellSpec)
        public val Sql: CodeLanguage = CodeLanguage("sql", emptyList(), listOf("sql"), SqlSpec)
        public val Json: CodeLanguage = CodeLanguage("json", emptyList(), listOf("json"), JsonSpec)
        public val Yaml: CodeLanguage = CodeLanguage("yaml", listOf("yml"), listOf("yaml", "yml"), YamlSpec)

        /** Every language the library can colour. */
        public val all: List<CodeLanguage> = listOf(
            Kotlin, Java, JavaScript, TypeScript, Python, Swift, Go, Rust, C, Cpp, CSharp, Dart, Shell, Sql, Json, Yaml,
        )

        private val byName: Map<String, CodeLanguage> =
            all.flatMap { language -> (language.aliases + language.name).map { it to language } }.toMap()

        private val byExtension: Map<String, CodeLanguage> =
            all.flatMap { language -> language.extensions.map { it to language } }.toMap()

        /**
         * The language a fenced code block or a `language-x` class names, such as "kotlin", "kt"
         * or "js". Case is ignored and only the first word counts. Null when it is not known.
         */
        public fun fromName(name: String): CodeLanguage? =
            byName[name.trim().substringBefore(' ').lowercase()]

        /** The language of a file, going by the ending of its name or path. Null when it is not known. */
        public fun fromFileName(fileName: String): CodeLanguage? =
            byExtension[fileName.substringAfterLast('/').substringAfterLast('.', "").lowercase()]
    }
}
