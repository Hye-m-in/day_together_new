package com.example.day_together.data.model

import java.time.LocalDate
import java.util.UUID

data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val date: LocalDate // 홈 화면에서 사용하는 날짜 정보 저장

)