package com.mohamedrejeb.richeditor.sample.common.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.richeditor.model.RichTextValue
import com.mohamedrejeb.richeditor.sample.common.htmleditor.HtmlEditorScreen
import com.mohamedrejeb.richeditor.sample.common.richeditor.RichEditorScreen
import com.mohamedrejeb.richeditor.ui.material3.RichText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent() {
    val navigator = LocalNavigator.currentOrThrow

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
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValue)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(20.dp))

            RichText(
                richText = RichTextValue.from("<u>Welcome</u> to <b>Compose Rich Editor Demo</b>"),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(20.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(40.dp)
            ) {
                Button(
                    onClick = {
                        navigator.push(RichEditorScreen)
                    }
                ) {
                    Text("Rich Text Editor Demo")
                }

                Button(
                    onClick = {
                        navigator.push(HtmlEditorScreen)
                    },
                ) {
                    Text("HTML Editor Demo")
                }
            }

        }
    }
}