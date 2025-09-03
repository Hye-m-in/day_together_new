package com.example.day_together.data.model

/**
 * 주간 캘린더의 하루를 나타내는 데이터 모델 클래스
 */
data class WeeklyCalendarDay(
    // 날짜 (예: "2025-07-21")
    val date: String,

    // 요일 (예: "월")
    val dayOfWeek: String,

    // 해당 날짜에 포함된 이벤트 목록
    val events: List<CalendarEvent> = emptyList(),

    // 오늘 날짜인지 여부를 표시
    val isToday: Boolean = false
)