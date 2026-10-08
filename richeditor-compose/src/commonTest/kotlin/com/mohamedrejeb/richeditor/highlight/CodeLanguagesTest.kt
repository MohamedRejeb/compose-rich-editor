package com.mohamedrejeb.richeditor.highlight

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.highlight.CodeTokenKind.Annotation
import com.mohamedrejeb.richeditor.highlight.CodeTokenKind.Comment
import com.mohamedrejeb.richeditor.highlight.CodeTokenKind.Keyword
import com.mohamedrejeb.richeditor.highlight.CodeTokenKind.Number
import com.mohamedrejeb.richeditor.highlight.CodeTokenKind.String
import com.mohamedrejeb.richeditor.highlight.languages.SqlLineComment
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalRichTextApi::class)
class CodeLanguagesTest {

    @Test
    fun java() = assertEquals(
        listOf("@Override" to Annotation, "public" to Keyword, "int" to Keyword, "return" to Keyword, "1" to Number, "// x" to Comment),
        tokenTexts("@Override public int a() { return 1; } // x", CodeLanguage.Java),
    )

    @Test
    fun javascript() = assertEquals(
        listOf("const" to Keyword, "`x\ny`" to String, "'z'" to String, "/* c */" to Comment),
        tokenTexts("const a = `x\ny` + 'z' /* c */", CodeLanguage.JavaScript),
    )

    @Test
    fun typescript() = assertEquals(
        listOf("interface" to Keyword, "readonly" to Keyword, "string" to Keyword),
        tokenTexts("interface A { readonly b: string }", CodeLanguage.TypeScript),
    )

    @Test
    fun python() = assertEquals(
        listOf("@cache" to Annotation, "def" to Keyword, "\"\"\"doc\nmore\"\"\"" to String, "return" to Keyword, "None" to Keyword, "# c" to Comment),
        tokenTexts("@cache\ndef a():\n    \"\"\"doc\nmore\"\"\"\n    return None # c", CodeLanguage.Python),
    )

    @Test
    fun swift() = assertEquals(
        listOf("@MainActor" to Annotation, "func" to Keyword, "let" to Keyword, "/* a /* b */ */" to Comment),
        tokenTexts("@MainActor func a() { let b = c } /* a /* b */ */", CodeLanguage.Swift),
    )

    @Test
    fun go() = assertEquals(
        listOf("func" to Keyword, "`raw\\`" to String, "nil" to Keyword),
        tokenTexts("func a() { b := `raw\\`; _ = nil }", CodeLanguage.Go),
    )

    @Test
    fun `rust lifetimes are not strings`() = assertEquals(
        listOf("fn" to Keyword, "str" to Keyword, "\"s\"" to String),
        tokenTexts("fn a<'a>(b: &'a str) { \"s\" }", CodeLanguage.Rust),
    )

    @Test
    fun c() = assertEquals(
        listOf("#include" to Annotation, "int" to Keyword, "return" to Keyword, "0" to Number),
        tokenTexts("#include <stdio.h>\nint main() { return 0; }", CodeLanguage.C),
    )

    @Test
    fun cpp() = assertEquals(
        listOf("class" to Keyword, "public" to Keyword, "virtual" to Keyword, "void" to Keyword, "nullptr" to Keyword),
        tokenTexts("class A { public: virtual void b() { c = nullptr; } }", CodeLanguage.Cpp),
    )

    @Test
    fun csharp() = assertEquals(
        listOf("namespace" to Keyword, "var" to Keyword, "await" to Keyword),
        tokenTexts("namespace A { var b = await c; }", CodeLanguage.CSharp),
    )

    @Test
    fun dart() = assertEquals(
        listOf("@override" to Annotation, "final" to Keyword, "'''a\nb'''" to String),
        tokenTexts("@override final a = '''a\nb'''", CodeLanguage.Dart),
    )

    @Test
    fun `shell comments need a leading space`() = assertEquals(
        listOf("if" to Keyword, "\"\$x\"" to String, "then" to Keyword, "fi" to Keyword, "# done" to Comment),
        tokenTexts("if [ \$# = \"\$x\" ]; then a; fi # done", CodeLanguage.Shell),
    )

    @Test
    fun `sql keywords ignore case`() = assertEquals(
        listOf("select" to Keyword, "FROM" to Keyword, "Where" to Keyword, "'x'" to String, "${SqlLineComment} note" to Comment),
        tokenTexts("select a FROM b Where c = 'x' ${SqlLineComment} note", CodeLanguage.Sql),
    )

    @Test
    fun json() = assertEquals(
        listOf("\"a\"" to String, "1.5" to Number, "\"b\"" to String, "true" to Keyword, "null" to Keyword),
        tokenTexts("{\"a\": 1.5, \"b\": [true, null]}", CodeLanguage.Json),
    )

    @Test
    fun yaml() = assertEquals(
        listOf("\"b#c\"" to String, "# note" to Comment, "true" to Keyword),
        tokenTexts("a: \"b#c\" # note\nd: true", CodeLanguage.Yaml),
    )

    @Test
    fun `yaml comments need a leading space`() = assertEquals(
        listOf("# note" to Comment, "true" to Keyword),
        tokenTexts("a: b#c # note\nd: true", CodeLanguage.Yaml),
    )
}
