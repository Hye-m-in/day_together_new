package com.example.day_together

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions

object FCMService {

    private val functions = FirebaseFunctions.getInstance()

    fun sendInviteNotification(toUserId: String, fromUserId: String) {
        val db = FirebaseService.db

        db.collection("users").document(toUserId).get()
            .addOnSuccessListener { snapshot ->
                val token = snapshot.getString("fcmToken")
                if (!token.isNullOrBlank()) {
                    val title = "채팅방 초대"
                    val body = "$fromUserId 님이 당신을 채팅방에 초대했습니다."

                    val data = hashMapOf(
                        "token" to token,
                        "title" to title,
                        "body" to body
                    )

                    functions
                        .getHttpsCallable("sendInvitation")
                        .call(data)
                        .addOnSuccessListener {
                            Log.d("FCM", "Cloud Function 호출 성공")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FCM", "Cloud Function 호출 실패: ${e.message}")
                        }
                } else {
                    Log.e("FCM", "fcmToken이 비어 있음")
                }
            }
            .addOnFailureListener {
                Log.e("FCM", "사용자 토큰 조회 실패: ${it.message}")
            }
    }
}