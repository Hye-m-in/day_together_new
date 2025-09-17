package com.example.day_together.data.remote

import com.example.day_together.data.dto.NaverTokenRequest
import com.example.day_together.data.dto.TokenResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/naver-login") // 서버의 상세 경로
    suspend fun naverLogin(@Body request: NaverTokenRequest): TokenResponse
}