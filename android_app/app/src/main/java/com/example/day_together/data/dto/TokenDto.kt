package com.example.day_together.data.dto

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    // 서버가 "detail" 이라는 이름으로 오류 메시지를 보내므로, @SerializedName 사용
    @SerializedName("detail") val detail: String?
)

// 서버에 보낼 데이터 모양
data class NaverTokenRequest(
    @SerializedName("access_token") val accessToken: String
)

// 서버로부터 받을 데이터 모양
data class TokenResponse(
    @SerializedName("custom_token") val customToken: String
)