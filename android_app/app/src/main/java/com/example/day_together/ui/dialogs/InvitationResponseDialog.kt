package com.example.day_together.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.day_together.ui.theme.*

/**
 * 멤버 초대 응답 다이얼로그(알림창)
 *
 * 1) Dialog, surface 설정 -> surface(컨테이너 컴포저블) : 배경색, 모양, 그림자 등 한꺼번에 지정해 주는 박스 역할
 * 2) 헤더 구성 (타이틀 + 닫기 버튼)
 * 3) 초대 메시지 표시
 * 4) 거절/수락 버튼 배치
 */
@Composable
fun InvitationResponseDialog(
    inviterName: String,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onDismissRequest: () -> Unit
) {
    // 1) Dialog 열기
    Dialog(onDismissRequest = onDismissRequest) {
        // 1-1) 다이얼로그 배경 Surface
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ResponseDialogSurface,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 2) 헤더 영역: 타이틀 + 닫기 버튼
                Box(modifier = Modifier.fillMaxWidth()) {
                    // 2-1) 타이틀 텍스트
                    Text(
                        text = "멤버 초대",
                        style = TextStyle(
                            fontFamily = GothicA1,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = ResponseDialogButtonContent
                        ),
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    // 2-2) 닫기
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = ResponseDialogButtonContent.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3) 초대 메시지
                Text(
                    text = "$inviterName 님이 회원님을 초대했어요",
                    style = TextStyle(
                        fontFamily = GothicA1,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )

                // 4) 거절 버튼
                Button(
                    onClick = onReject,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResponseDialogButtonBackground,
                        contentColor = ResponseDialogButtonContent
                    ),
                    border = BorderStroke(1.dp, ResponseDialogButtonBorder)
                ) {
                    Text(
                        text = "거절",
                        style = TextStyle(
                            fontFamily = GothicA1,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4-1) 수락 버튼
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResponseDialogButtonBackground,
                        contentColor = ResponseDialogButtonContent
                    ),
                    border = BorderStroke(1.dp, ResponseDialogButtonBorder)
                ) {
                    Text(
                        text = "수락",
                        style = TextStyle(
                            fontFamily = GothicA1,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}

