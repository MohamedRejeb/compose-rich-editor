import androidx.compose.ui.text.AnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals

private class CommonTest {

    @Test
    fun testPlay() {
        val html = """
            <p>Hello </p>
            <p>Koltin &copy; &amp;</p>
            <b>
        """.trimIndent()

        val annotatedStringBuilder = AnnotatedString.Builder()

        annotatedStringBuilder.addStringAnnotation("p", "hel", 0, 3)
        annotatedStringBuilder.addStringAnnotation("span", "er", 3, 5)
        annotatedStringBuilder

        assertEquals(
            annotatedStringBuilder.length,
            0
        )
    }

}