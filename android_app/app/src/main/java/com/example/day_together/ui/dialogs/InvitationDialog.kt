package com.example.day_together.ui.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun InvitationDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("초대 도착") },
        text = { Text("가족 채팅방에 초대받았습니다. 수락하시겠습니까?") },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("수락")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("거절")
            }
        }
    )
}

