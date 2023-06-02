package com.mohamedrejeb.richeditor.sample.common.richeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
            <a href="https://github.com/DAKSHSEMWAL/Compose-Rich-Editor">MDParserKit Core</a><br>
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
            <a href="https://github.com/DAKSHSEMWAL/Compose-Rich-Editor">MDParserKit Core</a><br>
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
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()).clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = null,
                        onClick = { keyboardController?.hide() }
                    )
            ) {
                // BasicRichTextEditor
                Text(
                    text = "BasicRichTextEditor:",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                RichTextStyleRow(
                    modifier = Modifier.fillMaxWidth(),
                    value = basicRichTextValue,
                    onValueChanged = {
                        basicRichTextValue = it
                    },
                )

                BasicRichTextEditor(
                    modifier = Modifier.fillMaxWidth(),
                    value = basicRichTextValue,
                    onValueChange = {
                        basicRichTextValue = it
                    },
                )

                Divider(modifier = Modifier.padding(vertical = 20.dp))

                // RichTextEditor
                Text(
                    text = "RichTextEditor:",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                RichTextStyleRow(
                    modifier = Modifier.fillMaxWidth(),
                    value = richTextValue,
                    onValueChanged = {
                        richTextValue = it
                    },
                )

                RichTextEditor(
                    modifier = Modifier.fillMaxWidth(),
                    value = richTextValue,
                    onValueChange = {
                        richTextValue = it
                    },
                )

                Divider(modifier = Modifier.padding(vertical = 20.dp))

                // OutlinedRichTextEditor
                Text(
                    text = "OutlinedRichTextEditor:",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                RichTextStyleRow(
                    modifier = Modifier.fillMaxWidth(),
                    value = outlinedRichTextValue,
                    onValueChanged = {
                        outlinedRichTextValue = it
                    },
                )

                OutlinedRichTextEditor(
                    modifier = Modifier.fillMaxWidth(),
                    value = outlinedRichTextValue,
                    onValueChange = {
                        outlinedRichTextValue = it
                    },
                )

                Divider(modifier = Modifier.padding(vertical = 20.dp))

                // RichText
                Text(
                    text = "RichText:",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(8.dp))

                RichText(
                    modifier = Modifier.fillMaxWidth(),
                    richText = richTextValue,
                )
            }
        }
    }
}