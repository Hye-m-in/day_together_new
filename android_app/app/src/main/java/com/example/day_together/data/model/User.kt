package com.example.day_together.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String? = null, // 이메일 선택적
    val birthDate: String? = null, // "YYYY-MM-DD" 형식
    val familyRole: String? = null, // 예: "아빠", "엄마", "첫째"

)