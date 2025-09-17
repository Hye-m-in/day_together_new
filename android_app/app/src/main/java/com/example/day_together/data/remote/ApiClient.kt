package com.example.day_together.data.remote

import com.example.day_together.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // 1. 로그를 찍기 위한 로깅 인터셉터 생성
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // 요청/응답의 모든 내용을 보여줌
    }

    // 2. 로깅 인터셉터를 탑재한 OkHttpClient 생성
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // 3. Retrofit 인스턴스가 위에서 만든 OkHttpClient를 사용하도록 설정
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_SERVER_URL)
        .client(okHttpClient) // client 설정 추가
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: ApiService = retrofit.create(ApiService::class.java)
}