package com.example.day_together.data.model

import java.time.LocalDate
import java.util.UUID

/**
 * 캘린더에 표시될 단일 이벤트를 나타내는 데이터 모델
 */
data class CalendarEvent(
    // 이벤트 고유 ID (자동 생성)
    val id: String = UUID.randomUUID().toString(),

    // 이벤트 내용 또는 설명
    val description: String,

    // 이벤트가 속한 날짜
    val date: LocalDate,

    // 중요 이벤트 여부 (D-Day, 기념일 등 우선 표시를 위해 사용)
    val isPriority: Boolean = false
)