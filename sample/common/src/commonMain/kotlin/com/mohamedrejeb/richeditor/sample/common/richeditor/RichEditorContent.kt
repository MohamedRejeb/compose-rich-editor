package com.mohamedrejeb.richeditor.sample.common.richeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.richeditor.model.RichTextValue
import com.mohamedrejeb.richeditor.sample.common.components.RichTextStyleRow
import com.mohamedrejeb.richeditor.sample.common.ui.theme.ComposeRichEditorTheme
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun RichEditorContent() {
    val navigator = LocalNavigator.currentOrThrow
    val keyboardController = LocalSoftwareKeyboardController.current
    var basicRichTextValue by remember {
        mutableStateOf(
            RichTextValue.from(
                """<h1>Text</h1>
            <a href="https://www.w3schools.com">Visit W3Schools</a><br>
            <p><b>RichTextEditor</b> is a <i>composable</i> that allows you to edit <u>rich text</u> content.</p>
            <a href="https://www.jetbrains.com/lp/compose-multiplatform/">Compose Multiplatform</a><br>
            
            <h2>An Unordered HTML List</h2>

            <ul>
              <li>Coffee</li>
              <li>Tea</li>
              <li>Milk</li>
            </ul>  

            <h2>An Ordered HTML List</h2>

            <ol>
              <li>Coffee</li>
              <li>Tea</li>
              <li>Milk</li>
            </ol> 
            """.trimIndent()
            )
        )
    }
    var richTextValue by remember {
        mutableStateOf(
            RichTextValue.from(
                """<h1>Text</h1>
            <a href="https://www.w3schools.com">Visit W3Schools</a><br>
            <p><b>RichTextEditor</b> is a <i>composable</i> that allows you to edit <u>rich text</u> content.</p>
             <a href="https://www.jetbrains.com/lp/compose-multiplatform/">Compose Multiplatform</a><br>
             <h2>An Unordered HTML List</h2>

            <ul>
              <li>Coffee</li>
              <li>Tea</li>
              <li>Milk</li>
            </ul>  

            <h2>An Ordered HTML List</h2>

            <ol>
              <li>Coffee</li>
              <li>Tea</li>
              <li>Milk</li>
            </ol
            """.trimIndent()
            )
        )
    }
    var outlinedRichTextValue by remember { mutableStateOf(RichTextValue()) }

    ComposeRichEditorTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Compose Rich Editor") },
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.pop() }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxSize()
        ) { paddingValue ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValue)
                    .verticalScroll(rememberScrollState()).clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = null,
                        onClick = { keyboardController?.hide() }
                    )
            ) {
                // BasicRichTextEditor
                Text(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    text = "BasicRichTextEditor:",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                RichTextStyleRow(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    value = basicRichTextValue,
                    onValueChanged = {
                        basicRichTextValue = it
                    },
                )

                BasicRichTextEditor(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    value = basicRichTextValue,
                    onValueChange = {
                        basicRichTextValue = it
                    },
                )

                Divider(modifier = Modifier.padding(vertical = 20.dp))

                // RichTextEditor
                Text(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    text = "RichTextEditor:",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                RichTextStyleRow(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    value = richTextValue,
                    onValueChanged = {
                        richTextValue = it
                    },
                )

                RichTextEditor(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    value = richTextValue,
                    onValueChange = {
                        richTextValue = it
                    },
                )

                Divider(modifier = Modifier.padding(vertical = 20.dp))

                // OutlinedRichTextEditor
                Text(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    text = "OutlinedRichTextEditor:",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                RichTextStyleRow(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    value = outlinedRichTextValue,
                    onValueChanged = {
                        outlinedRichTextValue = it
                    },
                )

                OutlinedRichTextEditor(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    value = outlinedRichTextValue,
                    onValueChange = {
                        outlinedRichTextValue = it
                    },
                )

                Divider(modifier = Modifier.padding(vertical = 20.dp))

                // RichText
                Text(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    text = "RichText:",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                RichText(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    richText = richTextValue,
                )
            }
        }
    }
}