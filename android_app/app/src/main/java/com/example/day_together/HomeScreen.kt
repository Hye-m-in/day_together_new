package com.example.day_together

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.day_together.ChatRoomManager.auth
import com.example.day_together.ChatRoomManager.db

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(
    invitedChatRoomId: MutableState<String?>,
    onAcceptInvitation: (String) -> Unit,
    onDismissInvitation: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseService.db
    val auth = FirebaseService.auth
    val currentUserId = auth.currentUser?.uid

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Button(onClick = {
                if (currentUserId != null) {
                    // 사용자의 초대 상태가 있는지 확인
                    db.collection("users").document(currentUserId).get()
                        .addOnSuccessListener { doc ->
                            val invitedId = doc.getString("invitedChatRoomId")
                            if (invitedId != null) {
                                // 초대 상태가 있는 경우 InvitationActivity로
                                goToChatOrInvitation(context, invitedId)
                            } else {
                                Toast.makeText(context, "참여 중인 채팅방이 없습니다", Toast.LENGTH_SHORT).show()
                                val intent = Intent(context, ChatActivity::class.java)
                                context.startActivity(intent)
                            }
                        }
                }
            }) {
                Text("채팅하러 가기")
            }

            Spacer(modifier = Modifier.height(16.dp))


            Text(
                text = "로그아웃",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable {
                        AuthManager.logoutUser()
                        val loginIntent = Intent(context, LoginActivity::class.java)
                        loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(loginIntent) }
            )

            // 초대 다이얼로그 표시
            invitedChatRoomId.value?.let { chatRoomId ->
                InvitationDialog(
                    onAccept = { onAcceptInvitation(chatRoomId) },
                    onDismiss = onDismissInvitation
                )
            }
        }
    }
}

// 채팅방 상태에 따라 Chat 또는 Invitation으로 이동
fun goToChatOrInvitation(context: Context, chatRoomId: String) {
    val uid = auth.currentUser?.uid ?: return

    val invitationRef = db.collection("users")
        .document(uid)
        .collection("invitations")
        .document(chatRoomId)

    invitationRef.get()
        .addOnSuccessListener { document ->
            val status = document.getString("status")
            if (status == "accepted") {
                // 수락된 경우: 채팅 화면으로 이동
                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra("chatRoomId", chatRoomId)
                context.startActivity(intent)
            } else {
                // 아직 수락하지 않은 경우: 초대 수락 화면으로 이동
                val intent = Intent(context, InvitationActivity::class.java)
                intent.putExtra("chatRoomId", chatRoomId)
                context.startActivity(intent)
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "초대 정보 조회 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}
@Composable
fun InvitationDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("초대 도착") },
        text = { Text("가족 채팅방에 초대받았습니다. 입장하시겠습니까?") },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("입장하기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("나중에")
            }
        }
    )
}

