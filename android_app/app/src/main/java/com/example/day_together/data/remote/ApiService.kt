package com.example.day_together.data.remote


import com.example.day_together.data.dto.GoogleTokenRequest
import com.example.day_together.data.dto.NaverTokenRequest
import com.example.day_together.data.dto.TokenResponse
import retrofit2.http.Body
import retrofit2.http.POST

// 인증(로그인) 관련 API 인터페이스

interface AuthService {

    @POST("/naver-login")
    suspend fun naverLogin(@Body request: NaverTokenRequest): TokenResponse

    @POST("/google-login")
    suspend fun googleLogin(@Body request: GoogleTokenRequest): TokenResponse
}