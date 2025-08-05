package com.example.day_together

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

// 캘린더 일정 데이터 클래스
data class Event(
    val title: String = "",
    val date: String = "", // "@2025-08-01"
    val createdBy: String = "", // uid or "system"
    val type: String = "general", //"birthday", "anniversary" 등
    val visibility: String = "public"
)

class CalendarManager {
    val db = FirebaseService.db
    val auth = FirebaseService.auth

    // 캘린더 생성
    fun createCalendarDocument(chatRoomId: String, onComplete: (Boolean) -> Unit = {}) {
        val calendarRef = db.collection("calendar").document(chatRoomId)
        val data = mapOf(
            "createdAt" to FieldValue.serverTimestamp()
        )

        calendarRef.set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("CalendarManager", "캘린더 문서 생성 완료")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("CalendarManager", "캘린더 생성 실패: ${e.message}", e)
                onComplete(false)
            }
    }

    // 생일 일정 추가
    fun registerBirthday(chatRoomId: String, userId: String) {
        val userRef = db.collection("users").document(userId)
        userRef.get().addOnSuccessListener { doc ->
            val name = doc.getString("name") ?: return@addOnSuccessListener
            val birthday = doc.getString("birthday") ?: return@addOnSuccessListener

            val birthdayEvent = hashMapOf(
                "title" to "$name 생일",
                "date" to birthday,  // "YYYY-MM-DD"
                "createdBy" to "system",
                "type" to "birthday",
                "visibility" to "public",
                "userUid" to userId
            )

            db.collection("calendar")
                .document(chatRoomId)
                .collection("events")
                .add(birthdayEvent)
                .addOnSuccessListener {
                    Log.d("CalendarManager", "생일 일정 등록 완료")
                }
                .addOnFailureListener { e ->
                    Log.e("CalendarManager", "생일 일정 등록 실패: ${e.message}", e)
                }
        }
    }

    // 전체 멤버 생일 등록
    fun registerBirthdaysForMembers(chatRoomId: String, memberIds: List<String>){
        for (uid in memberIds) {
            registerBirthday(chatRoomId, uid)
        }
    }

}