package com.example.day_together.data.model


data class WeeklyCalendarDay(
    val date: String, // 날짜
    val dayOfWeek: String, // 요일
    val events: List<CalendarEvent> = emptyList(), // 해당 날짜의 이벤트 목록
    val isToday: Boolean = false
)