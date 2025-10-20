package com.example.day_together.data.model

import com.google.firebase.Timestamp
import java.util.Date
import java.util.UUID

/**
 * 캘린더 이벤트를 위한 단일 데이터 모델
 */
data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String? = null,
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp? = null,
    val creatorId: String = "",
    val creatorName: String = "",
    val type: String = "general",
    val isPriority: Boolean = false, // D-Day 우선순위 설정을 위한 필드

    // D-Day 스위치를 켠 시간을 저장하기 위한 필드
    val prioritySetAt: Timestamp? = null
)