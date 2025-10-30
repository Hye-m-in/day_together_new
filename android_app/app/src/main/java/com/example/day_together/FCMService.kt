package com.example.day_together

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions

object FCMService {

    private val functions = FirebaseFunctions.getInstance()

    fun sendInviteNotification(toUserId: String, fromUserId: String) {
        val db = FirebaseService.db

        // 1. 초대한 사람(from)의 이름을 먼저 조회
        db.collection("users").document(fromUserId).get()
            .addOnSuccessListener { fromUserDoc ->
                val fromUserName = fromUserDoc.getString("name") ?: "가족" // 이름이 없으면 '가족'으로 표시

                // 2. 초대받는 사람(to)의 토큰 조회
                db.collection("users").document(toUserId).get()
                    .addOnSuccessListener { toUserDoc ->
                        val token = toUserDoc.getString("fcmToken")
                        if (!token.isNullOrBlank()) {

                            val title = "채팅방 초대"
                            // 3. UID 대신 조회한 이름 사용
                            val body = "$fromUserName 님이 회원님을 채팅방에 초대했습니다."

                            val data = hashMapOf(
                                "token" to token,
                                "title" to title,
                                "body" to body
                            )


                            functions
                                .getHttpsCallable("sendFamilyInvites")
                                .call(data)
                                .addOnSuccessListener {
                                    Log.d("FCM", "Cloud Function (sendFamilyInvites) 호출 성공")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FCM", "Cloud Function (sendFamilyInvites) 호출 실패: ${e.message}")
                                }
                        } else {
                            Log.e("FCM", "fcmToken이 비어 있음 (To: $toUserId)")
                        }
                    }
                    .addOnFailureListener {
                        Log.e("FCM", "초대받는 사람(to) 토큰 조회 실패: ${it.message}")
                    }
            }
            .addOnFailureListener {
                Log.e("FCM", "초대한 사람(from) 이름 조회 실패: ${it.message}")
            }
    }
}