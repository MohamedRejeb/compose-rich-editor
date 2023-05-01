package com.mocoding.richeditor.sample.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocoding.richeditor.model.RichTextValue
import com.mocoding.richeditor.ui.BasicRichTextEditor
import com.mocoding.richeditor.ui.OutlinedRichTextEditor
import com.mocoding.richeditor.ui.RichText
import com.mocoding.richeditor.ui.RichTextEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var basicRichTextValue by remember { mutableStateOf(RichTextValue()) }
    var richTextValue by remember { mutableStateOf(RichTextValue()) }
    var outlinedRichTextValue by remember { mutableStateOf(RichTextValue()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compose Rich Editor") },
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
                .verticalScroll(rememberScrollState())
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
                value = richTextValue,
            )
        }
    }
}