package com.mohamedrejeb.richeditor.sample.common.slack

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState

@Composable
fun SlackDemoLinkDialog(
    state: RichTextState,
    openLinkDialog: MutableState<Boolean>,
) {
    var text by remember { mutableStateOf(state.selectedLinkText.orEmpty()) }
    var link by remember { mutableStateOf(state.selectedLinkUrl.orEmpty()) }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1a1d21))
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Add link",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    openLinkDialog.value = false
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value =
            if (state.selection.collapsed)
                text
            else
                state.annotatedString.text.substring(
                    state.selection.min,
                    state.selection.max
                ),
            onValueChange = {
                text = it
            },
            label = {
                Text(
                    text = "Text",
                    color = Color.White
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White
            ),
            enabled = state.selection.collapsed && !state.isLink,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = link,
            onValueChange = {
                link = it
            },
            label = {
                Text(
                    text = "Link",
                    color = Color.White
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.End)
        ) {
            if (state.isLink) {
                OutlinedButton(
                    onClick = {
                        state.removeLink()

                        openLinkDialog.value = false
                        text = ""
                        link = ""
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color.Red
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                ) {
                    Text(
                        text = "Remove",
                        color = Color.Red
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
            }

            OutlinedButton(
                onClick = {
                    openLinkDialog.value = false
                    text = ""
                    link = ""
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
            ) {
                Text(
                    text = "Cancel",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = {
                    when {
                        state.isLink ->
                            state.updateLink(
                                url = link,
                            )

                        state.selection.collapsed ->
                            state.addLink(
                                text = text,
                                url = link
                            )

                        else ->
                            state.addLinkToSelection(
                                url = link
                            )
                    }

                    openLinkDialog.value = false
                    text = ""
                    link = ""
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007a5a),
                    contentColor = Color.White
                ),
                enabled = (text.isNotEmpty() || !state.selection.collapsed) && link.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
            ) {
                Text(
                    text = "Save",
                    color = Color.White
                )
            }
        }
    }
}