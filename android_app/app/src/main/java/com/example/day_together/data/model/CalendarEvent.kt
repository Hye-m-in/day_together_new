package com.example.day_together.data.model

import java.time.LocalDate
import java.util.UUID

/**
 * 캘린더에 표시될 단일 이벤트를 나타내는 데이터 모델
 */
data class CalendarEvent(
    // 이벤트 고유 ID (Firestore 문서 ID와 다를 수 있음)
    val id: String = UUID.randomUUID().toString(),

    // 이벤트 제목 (예: 생일, 결혼기념일, 회의 등)
    val title: String = "",

    // 이벤트 날짜 (앱 내부에서는 LocalDate로 사용)
    val date: LocalDate = LocalDate.now(),

    // 이벤트 유형 (예: "birthday", "anniversary", "general")
    val type: String = "general",

    // 오늘 날짜와 일치 여부 (UI에서 강조 표시용)
    val isToday: Boolean = false
) {
    companion object {
        /**
         * Firestore 저장용 변환
         * LocalDate는 String으로 변환해서 저장
         */
        fun toMap(event: CalendarEvent): Map<String, Any> {
            return mapOf(
                "id" to event.id,
                "title" to event.title,
                "date" to event.date.toString(), // "2025-08-23" 형식
                "type" to event.type,
                "isToday" to event.isToday
            )
        }

        /**
         * Firestore에서 가져온 데이터를 CalendarEvent로 변환
         */
        fun fromMap(data: Map<String, Any>): CalendarEvent {
            return CalendarEvent(
                id = data["id"] as? String ?: UUID.randomUUID().toString(),
                title = data["title"] as? String ?: "",
                date = LocalDate.parse(data["date"] as String),
                type = data["type"] as? String ?: "general",
                isToday = data["isToday"] as? Boolean ?: false
            )
        }
    }
}