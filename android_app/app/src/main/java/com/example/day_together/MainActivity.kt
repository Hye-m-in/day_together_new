package com.example.day_together

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.tasks.await
import com.google.firebase.messaging.FirebaseMessaging


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
                    val userId = AuthManager.getCurrentUserId()
                    if (userId != null) {
                        val invitationSnapshot = Firebase.firestore
                            .collection("users").document(userId)
                            .collection("invitations")
                            .whereEqualTo("status", "pending")
                            .limit(1)
                            .get()
                            .await()

                        val invitation = invitationSnapshot.documents.firstOrNull()
                        val pendingRoomId = invitation?.getString("chatRoomId")

                        if(!pendingRoomId.isNullOrEmpty()){
                            invitedChatRoomId.value = pendingRoomId
                        }
                        // FCM 토큰 등록
                        registerFcmTokenToFirestore()
                    }
                }

                HomeScreen(
                    invitedChatRoomId = invitedChatRoomId,
                    onAcceptInvitation = { chatRoomId ->
                        ChatRoomManager.acceptInvitation(chatRoomId) { success, message ->
                            if (success) {
                                val intent = Intent(context, ChatActivity::class.java)
                                intent.putExtra("chatRoomId", chatRoomId)
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, message ?: "입장 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                        invitedChatRoomId.value = null
                    },
                    onDismissInvitation = {
                        invitedChatRoomId.value = null
                    }
                )
            }
        }
    }

    fun registerFcmTokenToFirestore() {
        val user = FirebaseService.auth.currentUser ?: return
        val uid = user.uid

        // FCM 토큰 가져오기
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                val token = task.result

                if (task.isSuccessful && !token.isNullOrBlank()) {
                    Log.d("FCM", "FCM 토큰: $token")

                    // users 컬렉션의 해당 사용자 문서에 fcm token필드 없데이트
                    Firebase.firestore.collection("users").document(uid)
                        .update("fcmToken", token)
                        .addOnSuccessListener {
                            Log.d("FCM", "FCM 토큰 저장 성공")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FCM", "FCM 토큰 저장 실패: ${e.message}")
                        }
                } else {
                    Log.w("FCM", "FCM 토큰 가져오기 실패", task.exception)
                }
            }
    }
}