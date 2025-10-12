package com.example.day_together.data.remote

import com.example.day_together.data.dto.NaverTokenRequest
import com.example.day_together.data.dto.TokenResponse
import com.example.day_together.data.dto.GoogleTokenRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 서버와의 통신을 담당하는 Retrofit 인터페이스
 *
 * 네이버 로그인: 네이버 SDK에서 받은 access_token을 서버로 전달
 * 구글 로그인: 구글 로그인에서 얻은 id_token을 서버로 전달
 * 서버는 각각의 토큰을 검증 후 Firebase 커스텀 토큰(custom_token)을 생성해 반환
 *
 * 주의:
 * - 경로("/naver-login", "/google-login")는 반드시 서버 라우팅과 일치해야 함
 * - 서버 응답은 TokenResponse(custom_token) 형식
 */
interface ApiService {

    @POST("/naver-login") // 네이버 로그인용 서버 엔드포인트
    suspend fun naverLogin(@Body request: NaverTokenRequest): TokenResponse

    @POST("/google-login") // 구글 로그인용 서버 엔드포인트
    suspend fun googleLogin(@Body request: GoogleTokenRequest): TokenResponse
}
