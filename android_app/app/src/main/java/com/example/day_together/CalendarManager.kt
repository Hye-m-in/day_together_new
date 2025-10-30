package com.example.day_together

import java.util.Date
import android.util.Log
import android.icu.util.Calendar
import android.icu.util.ChineseCalendar
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
import java.time.format.DateTimeFormatter

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
            // 'birthDate' 필드를 읽음 (YYYYMMDD 형식)
            val birthdayStr = doc.getString("birthDate")
            val isLunar = doc.getBoolean("isLunar") ?: false

            // YYYYMMDD 형식인지 확인
            if (birthdayStr.isNullOrBlank() || birthdayStr.length != 8) {
                Log.w("CalendarManager", "$name 님의 생일 정보가 없거나 형식이(YYYYMMDD) 맞지 않습니다.")
                return@addOnSuccessListener
            }

            // YYYYMMDD 형식용 포맷터
            val birthDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

            try {
                // "YYYYMMDD" 형식의 문자열을 LocalDate로 변환
                val birthDate = LocalDate.parse(birthdayStr, birthDateFormatter)
                val today = LocalDate.now()
                var thisYearBirthday: LocalDate
                val title: String

                // 음력/양력 변환 로직 (HomeViewModel과 동일)
                if (isLunar) {
                    val lunarCal = ChineseCalendar()
                    lunarCal.clear()
                    lunarCal.set(Calendar.YEAR, today.year)
                    val lunarMonth = birthDate.monthValue - 1
                    var lunarDay = birthDate.dayOfMonth
                    lunarCal.set(ChineseCalendar.MONTH, lunarMonth)

                    val maxDay = lunarCal.getActualMaximum(ChineseCalendar.DAY_OF_MONTH)
                    if (lunarDay > maxDay) {
                        lunarDay = maxDay
                    }
                    lunarCal.set(ChineseCalendar.DAY_OF_MONTH, lunarDay)

                    lunarCal.get(Calendar.YEAR) // 재계산
                    thisYearBirthday = lunarCal.time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

                    if (thisYearBirthday.isBefore(today.minusDays(30))) {
                        lunarCal.clear()
                        lunarCal.set(Calendar.YEAR, today.year + 1)
                        lunarCal.set(ChineseCalendar.MONTH, lunarMonth)
                        val nextYearMaxDay = lunarCal.getActualMaximum(ChineseCalendar.DAY_OF_MONTH)
                        lunarDay = birthDate.dayOfMonth
                        if (lunarDay > nextYearMaxDay) {
                            lunarDay = nextYearMaxDay
                        }
                        lunarCal.set(ChineseCalendar.DAY_OF_MONTH, lunarDay)
                        lunarCal.get(Calendar.YEAR)
                        thisYearBirthday = lunarCal.time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    title = "$name 님의 생일 (음력)"
                } else {
                    // 양력
                    thisYearBirthday = birthDate.withYear(today.year)
                    if (thisYearBirthday.isBefore(today.minusDays(1))) {
                        thisYearBirthday = thisYearBirthday.plusYears(1)
                    }
                    title = "$name 님의 생일 (양력)"
                }



                // LocalDate를 Timestamp로 변환
                val birthdayTimestamp = Timestamp(
                    Date.from(thisYearBirthday.atStartOfDay(ZoneId.systemDefault()).toInstant())
                )

                val birthdayEvent = CalendarEvent(
                    id = "birthday_${userId}", // 고유 ID
                    title = title, // [수정]
                    startTime = birthdayTimestamp,
                    creatorId = "SYSTEM_BIRTHDAY",
                    creatorName = "가족 캘린더",
                    type = "BIRTHDAY",
                    description = if (isLunar) "음력 생일" else "양력 생일"
                )

                // 생성된 생일 이벤트를 Firestore에 저장
                // addEvent는 suspend 함수이므로 코루틴 안에서 호출해야 함
                CoroutineScope(Dispatchers.IO).launch {
                    // SetOptions.merge()를 사용해 덮어쓰기 (매년 갱신 가능하도록)
                    db.collection("chatRooms").document(chatRoomId).collection(EVENTS_COLLECTION)
                        .document(birthdayEvent.id)
                        .set(birthdayEvent, SetOptions.merge()) // set + merge
                        .await()
                    Log.d("CalendarManager", "$name 님의 생일 일정 등록/업데이트 성공")
                }

            } catch (e: Exception) {
                Log.e("CalendarManager", "생일 날짜 형식 변환 실패: $birthdayStr", e)
            }
        }
    }
}