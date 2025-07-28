package com.example.day_together.data.model

data class Question(
    val id: String = "", // Firestore 문서 ID
    val text: String = "",
    val category: String = "", // 질문 카테고리
    // 답변 상태는 가족 그룹 정보나 채팅방 정보에서 관리
)