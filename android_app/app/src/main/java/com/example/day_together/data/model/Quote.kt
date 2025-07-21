package com.example.day_together.data.model

/**
 * 앱에 표시될 명언(인용구)을 나타내는 데이터 모델
 */

data class Quote(
    // 명언의 고유 ID
    val id: String = "",

    // 명언 내용
    val text: String = "",

)