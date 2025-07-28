package com.example.day_together.data.model

import java.time.LocalDate
import java.util.UUID

data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val date: LocalDate,
    val isPriority: Boolean = false // D-Day 우선순위 필드, 가족기념일 우선
)