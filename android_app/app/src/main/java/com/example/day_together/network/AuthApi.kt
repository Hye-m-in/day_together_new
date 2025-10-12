package com.example.day_together.network

import com.example.day_together.data.model.TokenRequest
import com.example.day_together.data.model.TokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    /**
     * FastAPI 서버의 /google-login 엔드포인트 호출
     * @param request idToken을 담은 Request body
     * @return custom_token을 반환하는 Response
     */
    @POST("/google-login")
    suspend fun googleLogin(@Body request: TokenRequest): TokenResponse

    @POST("/naver-login")
    suspend fun naverLogin(@Body request: TokenRequest): TokenResponse
}
