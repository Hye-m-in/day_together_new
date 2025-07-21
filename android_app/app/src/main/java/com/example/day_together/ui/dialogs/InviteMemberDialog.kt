package com.example.day_together.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.day_together.ui.theme.Day_togetherTheme
import com.example.day_together.ui.theme.GothicA1
import com.example.day_together.ui.theme.TextPrimary
import com.example.day_together.ui.theme.InviteDialogSurface
import com.example.day_together.ui.theme.InviteDialogButtonEnabled
import com.example.day_together.ui.theme.InviteDialogButtonDisabled
import com.example.day_together.ui.theme.InviteDialogButtonContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteMemberDialog(
    onDismissRequest: () -> Unit,
    onInviteClick: (email: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    val isInviteButtonEnabled = email.isNotBlank()

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = InviteDialogSurface,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(
                modifier = Modifier.padding(all = 20.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "멤버 초대",
                        style = TextStyle(
                            fontFamily = GothicA1,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = TextPrimary.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "이메일 초대",
                    style = TextStyle(
                        fontFamily = GothicA1,
                        fontSize = 14.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("이메일 주소를 입력해주세요", style = TextStyle(fontSize = 14.sp, color = TextPrimary.copy(alpha = 0.5f))) },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, fontFamily = GothicA1),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InviteDialogButtonEnabled,
                        unfocusedBorderColor = TextPrimary.copy(alpha = 0.3f),
                        cursorColor = InviteDialogButtonEnabled,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isInviteButtonEnabled) {
                            onInviteClick(email)
                        }
                    },
                    enabled = isInviteButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InviteDialogButtonEnabled,
                        contentColor = InviteDialogButtonContent,
                        disabledContainerColor = InviteDialogButtonDisabled,
                        disabledContentColor = InviteDialogButtonContent.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        "초대하기",
                        style = TextStyle(
                            fontFamily = GothicA1,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}

@Preview(name = "InviteMemberDialog - Enabled", showBackground = true)
@Composable
fun InviteMemberDialogEnabledPreview() {
    Day_togetherTheme {
        var email by remember { mutableStateOf("test@example.com") }
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = InviteDialogSurface,
                modifier = Modifier.widthIn(max = 340.dp)
            ) {
                Column(
                    modifier = Modifier.padding(all = 20.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "멤버 초대",
                            style = TextStyle(
                                fontFamily = GothicA1,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            ),
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        IconButton(
                            onClick = {},
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "닫기",
                                tint = TextPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "이메일 초대",
                        style = TextStyle(
                            fontFamily = GothicA1,
                            fontSize = 14.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("이메일 주소를 입력해주세요", style = TextStyle(fontSize = 14.sp, color = TextPrimary.copy(alpha = 0.5f))) },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, fontFamily = GothicA1),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = InviteDialogButtonEnabled,
                            unfocusedBorderColor = TextPrimary.copy(alpha = 0.3f),
                            cursorColor = InviteDialogButtonEnabled,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {},
                        enabled = email.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InviteDialogButtonEnabled,
                            contentColor = InviteDialogButtonContent,
                            disabledContainerColor = InviteDialogButtonDisabled,
                            disabledContentColor = InviteDialogButtonContent.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(
                            "초대하기",
                            style = TextStyle(
                                fontFamily = GothicA1,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "InviteMemberDialog - Disabled", showBackground = true)
@Composable
fun InviteMemberDialogDisabledStatePreview() {
    Day_togetherTheme {
        var email by remember { mutableStateOf("") }
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = InviteDialogSurface,
                modifier = Modifier.widthIn(max = 340.dp)
            ) {
                Column(
                    modifier = Modifier.padding(all = 20.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "멤버 초대",
                            style = TextStyle(
                                fontFamily = GothicA1,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            ),
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                        IconButton(
                            onClick = {},
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "닫기",
                                tint = TextPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "이메일 초대",
                        style = TextStyle(
                            fontFamily = GothicA1,
                            fontSize = 14.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("이메일 주소를 입력해주세요", style = TextStyle(fontSize = 14.sp, color = TextPrimary.copy(alpha = 0.5f))) },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, fontFamily = GothicA1),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = InviteDialogButtonEnabled,
                            unfocusedBorderColor = TextPrimary.copy(alpha = 0.3f),
                            cursorColor = InviteDialogButtonEnabled,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {},
                        enabled = email.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InviteDialogButtonEnabled,
                            contentColor = InviteDialogButtonContent,
                            disabledContainerColor = InviteDialogButtonDisabled,
                            disabledContentColor = InviteDialogButtonContent.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(
                            "초대하기",
                            style = TextStyle(
                                fontFamily = GothicA1,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}