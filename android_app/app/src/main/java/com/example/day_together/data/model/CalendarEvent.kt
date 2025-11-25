package com.example.day_together.data.model

import com.google.firebase.Timestamp

import java.util.UUID

/**
 * 캘린더 이벤트를 위한 단일 데이터 모델
 */
data class CalendarEvent(

    // 이벤트 고유 식별자(Firestore 문서 ID)
    // 기본값 : UUID 자동 생성
    val id: String = UUID.randomUUID().toString(),
    // 이벤트 제목(예: "엄마 생일", "병원 예약"
    val title: String = "",

    // 이벤트 일정 날짜 (항상 "yyyy-MM-dd" 형식으로 저장)
    val date: String = "",

    // 이벤트 생성자 UID (Firebase Auth에서 발급되는 사용자 고유 ID)
    val creatorId: String = "",

    // 생성자 이름 또는 호칭 (예: "아빠", "엄마")
    val creatorName: String = "",

    // 가족 생일 일정 등록 시 필요(개인 일정 등록시에는 사용 안함)
    // 이벤트 유형, 설명
    val type: String = "general",
    val description: String? = null,

    // Firebase가 isPriority 필드를 인식하도록 @field:JvmField 추가
    @field:JvmField
    val isPriority: Boolean = false, // D-Day 우선순위 설정을 위한 필드

    // D-Day 스위치를 켠 시간을 저장하기 위한 필드
    val prioritySetAt: Timestamp? = null,



)