package com.example.day_together.data.model

/**
 * 사용자에게 제시되는 질문 데이터를 나타내는 모델.
 * 이 모델은 질문의 내용만 담고 있으며, 답변 상태는 다른 곳에서 별도로 관리
 */
data class Question(
    // 질문의 고유 ID (Firestore 문서 ID와 동일)
    val id: String = "",

    // 실제 질문 내용
    val text: String = "",

    // 질문의 카테고리 (예: "가치관", "일상")
    val category: String = ""
)