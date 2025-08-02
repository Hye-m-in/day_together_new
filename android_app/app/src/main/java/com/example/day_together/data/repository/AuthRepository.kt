//FakeRepository대신 Retrofit 호출만 담당하는 클래스
package com.example.day_together.data.repository

import com.example.day_together.data.model.TokenRequest
import com.example.day_together.data.model.TokenResponse
import com.example.day_together.network.ApiClient


class AuthRepository {
    suspend fun loginWithGoogle(idToken: String): TokenResponse {
        return ApiClient.service.googleLogin(TokenRequest(idToken = idToken))
    }

    suspend fun loginWithNaver(accessToken: String): TokenResponse {
        return ApiClient.service.naverLogin(TokenRequest(accessToken = accessToken))
    }

}