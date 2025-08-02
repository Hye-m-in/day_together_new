
package com.example.day_together.data.model

// 설정 데이터 담는 데이터 클래스
data class UserSettings(
    val questionFrequency: String,
    val questionTime: String,
    val notificationEnabled: Boolean,
    val vibrationEnabled: Boolean
)