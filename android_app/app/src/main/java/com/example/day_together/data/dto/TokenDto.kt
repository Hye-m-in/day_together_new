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

/**
 * 구글 로그인: 클라이언트(앱) → 서버로 전달하는 요청 바디
 *
 * - idToken: Android에서 GoogleSignIn 후 얻은 ID 토큰(Null/Blank 금지)
 * - 서버는 이 토큰의 유효성(aud/iss/exp/iat 등) 검증 후,Firebase Admin SDK로 커스텀 토큰(custom_token)을 생성해 TokenResponse로 반환
 
 * - @SerializedName("id_token")는 서버가 snake_case로 받을 때 필드명이 정확히 일치하도록 하기 위함
 * - 서버가 camelCase("idToken")를 요구한다면 이 어노테이션을 변경해야 함
 */
data class GoogleTokenRequest(
    @SerializedName("id_token") val idToken: String
)