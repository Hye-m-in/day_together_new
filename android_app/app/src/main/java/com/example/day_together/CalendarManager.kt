package com.example.day_together

import java.util.Date
import android.util.Log
import com.example.day_together.data.model.CalendarEvent
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId

/**
 * 캘린더 관련 모든 Firestore 작업을 처리하는 객체 
 */
object CalendarManager {

    val db = FirebaseService.db
    // 여러 곳에서 사용될 컬렉션 이름을 상수로 만들어 오타 방지
    private const val EVENTS_COLLECTION = "events"

    /**
     * 새로운 일정을 추가하는 함수 (suspend 키워드로 비동기 처리 명시)
     */
    suspend fun addEvent(chatRoomId: String, event: CalendarEvent) {
        try {
            // chatRooms/{chatRoomId}/events/{eventId} 경로에 데이터 저장
            db.collection("chatRooms").document(chatRoomId).collection(EVENTS_COLLECTION)
                .document(event.id) // CalendarEvent 생성 시 만들어진 고유 ID를 문서 ID로 사용
                .set(event)
                .await() // 작업이 끝날 때까지 기다림
            Log.d("CalendarManager", "일정 추가 성공: ${event.title}")
        } catch (e: Exception) {
            Log.e("CalendarManager", "일정 추가 실패", e)
        }
    }

    /**
     * 기존 일정을 수정하는 함수
     */
    suspend fun updateEvent(chatRoomId: String, event: CalendarEvent) {
        // Firestore의 set은 문서를 덮어쓰므로, addEvent와 동일한 로직으로 수정 가능
        addEvent(chatRoomId, event)
    }

    /**
     * ID를 이용해 일정을 삭제하는 함수
     */
    suspend fun deleteEvent(chatRoomId: String, eventId: String) {
        try {
            db.collection("chatRooms").document(chatRoomId).collection(EVENTS_COLLECTION)
                .document(eventId)
                .delete()
                .await()
            Log.d("CalendarManager", "일정 삭제 성공: $eventId")
        } catch (e: Exception) {
            Log.e("CalendarManager", "일정 삭제 실패", e)
        }
    }

    /**
     * [실시간 공유 핵심 기능]
     * 특정 채팅방의 일정 데이터 변경을 실시간으로 감지하고,
     * 변경될 때마다 onEventsUpdated 콜백 함수를 호출함
     */
    fun listenForEvents(
        chatRoomId: String,
        onEventsUpdated: (List<CalendarEvent>) -> Unit
    ): ListenerRegistration { // 리스너를 나중에 제거할 수 있도록 등록 객체 반환
        return db.collection("chatRooms").document(chatRoomId).collection(EVENTS_COLLECTION)
            .orderBy("startTime") // 시간순으로 정렬
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("CalendarManager", "실시간 일정 감지 실패", error)
                    onEventsUpdated(emptyList())
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    // Firestore 문서 목록을 CalendarEvent 객체 목록으로 자동 변환
                    val events = snapshots.mapNotNull { it.toObject<CalendarEvent>() }
                    onEventsUpdated(events) // 변경된 최신 일정 목록 전체를 전달
                }
            }
    }

    /**
     * 사용자의 생일 정보를 가져와 캘린더에 자동으로 등록하는 함수
     */
    fun registerBirthday(chatRoomId: String, userId: String) {
        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            val name = doc.getString("name") ?: return@addOnSuccessListener
            // Firestore에 'birthday' 필드가 'YYYY-MM-DD' 형식으로 저장되어 있다고 가정
            val birthdayStr = doc.getString("birthday") ?: return@addOnSuccessListener

            try {
                // "YYYY-MM-DD" 형식의 문자열을 LocalDate로 변환
                val birthDate = LocalDate.parse(birthdayStr)
                // 올해 생일 날짜로 변경
                val thisYearBirthday = birthDate.withYear(LocalDate.now().year)

                // LocalDate를 Timestamp로 변환
                val birthdayTimestamp = Timestamp(
                    Date.from(thisYearBirthday.atStartOfDay(ZoneId.systemDefault()).toInstant())
                )

                val birthdayEvent = CalendarEvent(
                    title = "$name 님의 생일",
                    startTime = birthdayTimestamp,
                    creatorId = "system", // 시스템이 자동으로 생성했음을 표시
                    creatorName = "시스템",
                    type = "birthday"
                )

                // 생성된 생일 이벤트를 Firestore에 저장
                // addEvent는 suspend 함수이므로 코루틴 안에서 호출해야 함
                CoroutineScope(Dispatchers.IO).launch {
                    addEvent(chatRoomId, birthdayEvent)
                }

            } catch (e: Exception) {
                Log.e("CalendarManager", "생일 날짜 형식 변환 실패: $birthdayStr", e)
            }
        }
    }
}