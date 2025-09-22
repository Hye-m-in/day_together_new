package com.example.day_together.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory


object ApiClient {
    // 에뮬레이터에서 로컬 FastAPI에 접근할 때 사용하는 특수 주소
    private const val BASE_URL = "http://10.0.2.2:8000/"

    // HTTP 요청/응답 로그를 찍어보는 인터셉터
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttpClient에 로깅 인터셉터 연결
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    // Retrofit 인스턴스 & AuthApi 서비스 프로퍼티
    val service: AuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
//            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
            .create(AuthApi::class.java)
    }
}
