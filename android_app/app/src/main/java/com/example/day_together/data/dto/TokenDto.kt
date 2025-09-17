package com.example.day_together.data.dto

import com.google.gson.annotations.SerializedName

// 서버에 보낼 데이터 모양
data class NaverTokenRequest(
    @SerializedName("access_token") val accessToken: String
)

// 서버로부터 받을 데이터 모양
data class TokenResponse(
    @SerializedName("custom_token") val customToken: String
)