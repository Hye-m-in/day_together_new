package com.example.day_together

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.day_together.ui.theme.Day_togetherTheme
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Day_togetherTheme {
                val context = this@MainActivity
                val invitedChatRoomId = remember { mutableStateOf<String?>(null) }

                // Firestore에서 초대 여부 확인
                LaunchedEffect(Unit) {
                    checkInvitationAndSetState(invitedChatRoomId)
                }

                Scaffold {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                val userId = AuthManager.getCurrentUserId()
                                if (userId != null) {
                                    FirebaseService.db.collection("members").document(userId).get()
                                        .addOnSuccessListener { doc ->
                                            val invitedChatRoomId = doc.getString("invitedChatRoomId")
                                            if (invitedChatRoomId != null) {
                                                val intent = Intent(this@MainActivity, InvitationActivity::class.java)
                                                intent.putExtra("chatRoomId", invitedChatRoomId)
                                                startActivity(intent)
                                            } else {
                                                // 이미 채팅방에 참여 중이라면 ChatActivity로
                                                val intent = Intent(this@MainActivity, ChatActivity::class.java)
                                                startActivity(intent)
                                            }
                                        }
                                }
                            }
                        ) {
                            Text("채팅하러 가기")
                        }

                    }

                    // 초대 다이얼로그 표시
                    invitedChatRoomId.value?.let { chatRoomId ->
                        InvitationDialog(
                            onAccept = {
                                ChatRoomManager.acceptInvitation(chatRoomId) { success, message ->
                                    if (success) {
                                        val intent = Intent(context, ChatActivity::class.java)
                                        intent.putExtra("chatRoomId", chatRoomId)
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            message ?: "입장 실패",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                invitedChatRoomId.value = null
                            },
                            onDismiss = {
                                invitedChatRoomId.value = null
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkInvitationAndSetState(state: MutableState<String?>) {
        val userId = AuthManager.getCurrentUserId() ?: return

        Firebase.firestore.collection("members").document(userId).get()
            .addOnSuccessListener { document ->
                val invitedChatRoomId = document.getString("invitedChatRoomId")
                if (!invitedChatRoomId.isNullOrEmpty()) {
                    state.value = invitedChatRoomId
                }
            }
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
