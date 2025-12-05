package com.example.day_together.data.remote

import com.example.day_together.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import com.example.day_together.data.remote.QuestionService

// Retrofit 클라이언트를 싱글톤으로 관리하는 객체

object ApiClient {

    // 1. 로그 인터셉터
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // 요청/응답의 모든 내용을 로그로 확인
    }

    // 2. OkHttpClient (로그 인터셉터 탑재)
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // 3. Retrofit 인스턴스 (lazy를 사용해 실제 사용 시점에 1번만 생성)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_SERVER_URL) // 하드코딩 대신 상수 사용 (http://10.0.2.2:8000/)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 4. API 서비스 인스턴스 (lazy 사용)
    // AuthService 인터페이스를 구현한 인스턴스를 생성
    val authService: AuthService by lazy {
        retrofit.create(AuthService::class.java)
    }
    
    //질문 관련
    val questionService: QuestionService by lazy {
        retrofit.create(QuestionService::class.java)
    }
}