package com.mohamedrejeb.richeditor.highlight

import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(ExperimentalRichTextApi::class)
class CodeLanguageLookupTest {

    @Test
    fun `names and aliases resolve without regard to case`() {
        assertSame(CodeLanguage.Kotlin, CodeLanguage.fromName("kotlin"))
        assertSame(CodeLanguage.Kotlin, CodeLanguage.fromName("Kotlin"))
        assertSame(CodeLanguage.Kotlin, CodeLanguage.fromName("kt"))
        assertSame(CodeLanguage.JavaScript, CodeLanguage.fromName("js"))
        assertSame(CodeLanguage.Shell, CodeLanguage.fromName("bash"))
        assertSame(CodeLanguage.Cpp, CodeLanguage.fromName("c++"))
        assertSame(CodeLanguage.CSharp, CodeLanguage.fromName("c#"))
        assertSame(CodeLanguage.Yaml, CodeLanguage.fromName("yml"))
    }

    @Test
    fun `only the first word of a tag is the language`() {
        assertSame(CodeLanguage.Kotlin, CodeLanguage.fromName("  kotlin title=\"x\""))
    }

    @Test
    fun `unknown and empty names are null`() {
        assertNull(CodeLanguage.fromName(""))
        assertNull(CodeLanguage.fromName("   "))
        assertNull(CodeLanguage.fromName("brainfuck"))
    }

    @Test
    fun `file names resolve by their ending`() {
        assertSame(CodeLanguage.Kotlin, CodeLanguage.fromFileName("src/main/Foo.kt"))
        assertSame(CodeLanguage.Kotlin, CodeLanguage.fromFileName("build.gradle.kts"))
        assertSame(CodeLanguage.TypeScript, CodeLanguage.fromFileName("App.TSX"))
        assertSame(CodeLanguage.C, CodeLanguage.fromFileName("a.h"))
        assertNull(CodeLanguage.fromFileName("Makefile"))
        assertNull(CodeLanguage.fromFileName("dir.kt/readme"))
    }

    @Test
    fun `every language has a unique name`() {
        assertEquals(16, CodeLanguage.all.size)
        assertEquals(16, CodeLanguage.all.map { it.name }.toSet().size)
    }
}
