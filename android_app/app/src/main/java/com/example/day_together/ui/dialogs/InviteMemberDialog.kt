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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.day_together.ui.theme.*

/**
 * 멤버초대 다이얼로그(알림창)
 *
 * 1) 상태 변수 선언 (email)
 * 2) 버튼 활성화 조건 계산
 * 3) Dialog 및 Surface 설정 -> surface(컨테이너 컴포저블) : 배경색, 모양, 그림자 등 한꺼번에 지정해 주는 박스 역할
 * 4) 헤더 컴포저블
 *    4-1) 타이틀 텍스트
 *    4-2) 닫기 버튼
 * 5) 설명 텍스트
 * 6) 이메일 입력 필드
 * 7) 초대하기 버튼
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteMemberDialog(
    onDismissRequest: () -> Unit,
    onInviteClick: (email: String) -> Unit
) {
    // 1) 이메일 상태
    var email by remember { mutableStateOf("") }

    // 2) 이메일이 비어있지 않을 때만 버튼 활성화
    val isInviteButtonEnabled = email.isNotBlank()

    // 3) 다이얼로그 오픈
    Dialog(onDismissRequest = onDismissRequest) {
        // 3-1) 배경 Surface
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = InviteDialogSurface,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // 4) 헤더: 타이틀 + 닫기
                Box(modifier = Modifier.fillMaxWidth()) {
                    // 4-1) "멤버 초대" 타이틀
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
                    // 4-2) 우측 닫기 버튼
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

                // 5) 설명 텍스트
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

                // 6) 이메일 입력 필드
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "이메일 주소를 입력해주세요",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = TextPrimary.copy(alpha = 0.5f)
                            )
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = TextPrimary,
                        fontFamily = GothicA1
                    ),
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

                // 7) 초대하기 버튼
                Button(
                    onClick = { if (isInviteButtonEnabled) onInviteClick(email) },
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

