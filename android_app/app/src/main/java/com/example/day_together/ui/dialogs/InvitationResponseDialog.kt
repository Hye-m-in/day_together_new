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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.day_together.ui.theme.Day_togetherTheme
import com.example.day_together.ui.theme.GothicA1
import com.example.day_together.ui.theme.TextPrimary
import com.example.day_together.ui.theme.ResponseDialogSurface
import com.example.day_together.ui.theme.ResponseDialogButtonBackground
import com.example.day_together.ui.theme.ResponseDialogButtonContent
import com.example.day_together.ui.theme.ResponseDialogButtonBorder


@Composable
fun InvitationResponseDialog(
    inviterName: String,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ResponseDialogSurface,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(all = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
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
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = ResponseDialogButtonContent.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${inviterName}님이 회원님을 초대했어요",
                    style = TextStyle(
                        fontFamily = GothicA1,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )

                Button(
                    onClick = onReject,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResponseDialogButtonBackground,
                        contentColor = ResponseDialogButtonContent
                    ),
                    border = BorderStroke(1.dp, ResponseDialogButtonBorder)
                ) {
                    Text(
                        "거절",
                        style = TextStyle(fontFamily = GothicA1, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResponseDialogButtonBackground,
                        contentColor = ResponseDialogButtonContent
                    ),
                    border = BorderStroke(1.dp, ResponseDialogButtonBorder)
                ) {
                    Text(
                        "수락",
                        style = TextStyle(fontFamily = GothicA1, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InvitationResponseDialogPreview() {
    Day_togetherTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            InvitationResponseDialog(
                inviterName = "테스트사용자",
                onAccept = {},
                onReject = {},
                onDismissRequest = {}
            )
        }
    }
}