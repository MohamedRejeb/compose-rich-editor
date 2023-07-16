package com.mohamedrejeb.richeditor.sample.common.slack

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlackDemoLinkDialog(
    state: RichTextState,
    text: MutableState<String>,
    link: MutableState<String>,
    openLinkDialog: MutableState<Boolean>,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(.4f))
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .width(300.dp)
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
                value = text.value,
                onValueChange = {
                    text.value = it
                },
                label = {
                    Text(
                        text = "Text",
                        color = Color.White
                    )
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = link.value,
                onValueChange = {
                    link.value = it
                },
                label = {
                    Text(
                        text = "Link",
                        color = Color.White
                    )
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
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
                OutlinedButton(
                    onClick = {
                        openLinkDialog.value = false
                        text.value = ""
                        link.value = ""
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
                        state.addLink(
                            text = text.value,
                            url = link.value
                        )
                        openLinkDialog.value = false
                        text.value = ""
                        link.value = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007a5a),
                        contentColor = Color.White
                    ),
                    enabled = text.value.isNotEmpty() && link.value.isNotEmpty(),
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
}