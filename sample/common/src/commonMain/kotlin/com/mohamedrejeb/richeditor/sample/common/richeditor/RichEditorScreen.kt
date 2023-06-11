package com.mohamedrejeb.richeditor.sample.common.richeditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.screen.Screen

object RichEditorScreen: Screen {

    @Composable
    override fun Content() {
        var basicRichTextValue by remember {
                    mutableStateOf("""<h1>Text</h1>
            <a href="https://www.w3schools.com">Visit W3Schools</a><br>
            <p><b>RichTextEditor</b> is a <i>composable</i> that allows you to edit <u>rich text</u> content.</p>
            <a href="https://github.com/MohamedRejeb/Compose-Rich-Editor">MDParserKit Core</a><br>
            <a href="https://music.youtube.com/search?q=summer+high">https://music.youtube.com/search?q=summer+high</a><br>
            
            <h2>An Unordered HTML List</h2>

            <ul>
              <li><a href="https://github.com/MohamedRejeb/Compose-Rich-Editor">MDParserKit Core</a></li>
              <li>Tea</li>
              <li>Milk</li>
            </ul>  

            <h2>An Ordered HTML List</h2>

            <ol>
              <li><a href="https://github.com/MohamedRejeb/Compose-Rich-Editor">MDParserKit Core</a></li>
              <li>Tea</li>
              <li><a href="https://github.com/MohamedRejeb/Compose-Rich-Editor">MDParserKit Core</a></li>
            </ol>
            """.trimIndent())
        }
        RichEditorContent(basicRichTextValue){
            basicRichTextValue = it
        }
    }

}