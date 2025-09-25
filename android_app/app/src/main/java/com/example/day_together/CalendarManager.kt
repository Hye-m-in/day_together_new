package com.example.day_together

import android.util.Log
import com.example.day_together.data.model.CalendarEvent
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

    // 캘린더 생성
    fun createCalendarDocument(chatRoomId: String, onComplete: (Boolean) -> Unit = {}) {
        // 경로 통일: chatRooms/{chatRoomId}/calendar_meta 하위에 메타 문서 생성
        val calendarMetaRef = db.collection("chatRooms")
            .document(chatRoomId)
            .collection("calendar_meta")
            .document("meta")

        val data = mapOf(
            "createdAt" to FieldValue.serverTimestamp()
        )

        calendarMetaRef.set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("CalendarManager", "캘린더 메타 문서 생성 완료")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("CalendarManager", "캘린더 메타 생성 실패: ${e.message}", e)
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

            // 경로 통일: chatRooms/{chatRoomId}/calendar
            db.collection("chatRooms")
                .document(chatRoomId)
                .collection("calendar")
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

    // 이벤트 추가
    suspend fun addEvent(chatRoomId: String, event: CalendarEvent) {
        try {
            // 경로 통일: chatRooms/{chatRoomId}/calendar
            // 문서 ID를 event.id로 고정 저장 (업데이트/삭제 용이)
            db.collection("chatRooms")
                .document(chatRoomId)
                .collection("calendar")
                .document(event.id) // event.id를 문서 ID로 사용
                .set(CalendarEvent.toMap(event))
                .await()
        } catch (e: Exception) {
            Log.e("CalendarManager", "Failed to add event", e)
        }
    }

    // 이벤트 전체 조회
    suspend fun getEvents(chatRoomId: String): List<CalendarEvent> {
        return try {
            // 경로 통일: chatRooms/{chatRoomId}/calendar
            val snapshot = db.collection("chatRooms")
                .document(chatRoomId)
                .collection("calendar")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data?.let { CalendarEvent.fromMap(it) }
            }
        } catch (e: Exception) {
            Log.e("CalendarManager", "Failed to fetch events", e)
            emptyList()
        }
    }

    // 이벤트 수정
    suspend fun updateEvent(chatRoomId: String, event: CalendarEvent) {
        requireNotNull(event.id) { "event.id는 null일 수 없습니다." }

        // 경로 통일 + 직렬화 일관성: toMap 사용
        db.collection("chatRooms")
            .document(chatRoomId)
            .collection("calendar")
            .document(event.id!!)
            .set(CalendarEvent.toMap(event))
            .await()
    }

    // 이벤트 삭제
    suspend fun deleteEvent(chatRoomId: String, eventId: String) {
        try {
            // 경로 통일: chatRooms/{chatRoomId}/calendar
            db.collection("chatRooms")
                .document(chatRoomId)
                .collection("calendar")
                .document(eventId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("CalendarManager", "Failed to delete event", e)
        }
    }

    fun saveEvent(chatRoomId: String, event: CalendarEvent) {
        // 간단 저장용 -> 필요하면 CalendarEvent.toMap(event)로 통일 가능
        val eventMap = mapOf(
            "title" to event.title,
            "date" to event.date.toString(),  // "2025-08-23" 형식으로 변환
            "type" to event.type
        )

        // 경로 통일: chatRooms/{chatRoomId}/calendar
        db.collection("chatRooms")
            .document(chatRoomId)
            .collection("calendar")
            .add(eventMap)
    }
}
