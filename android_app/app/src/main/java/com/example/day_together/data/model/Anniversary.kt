package com.example.day_together.data.model

/**
 * 기념일 정보를 담는 데이터 모델 클래스
 */
data class Anniversary(
    // 기념일 고유 ID (Firebase 데이터베이스에서 사용)
    val id: String = "",

    // 기념일 제목
    val title: String = "",

    // 날짜 (Unix Timestamp, ms 단위로 저장)
    val date: Long = 0L,

    // 기념일 종류 (기본값: 생일)
    val type: String = "BIRTHDAY"
)