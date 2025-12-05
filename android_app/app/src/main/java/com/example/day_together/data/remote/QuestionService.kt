package com.example.day_together.data.remote

import com.example.day_together.data.dto.TodayQuestionResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST

interface QuestionService{
    /*@GET("chat-rooms/{roomId}/today-question")
    suspend fun getTodayQuestion(
        @Path("roomId") roomId: String
    ): TodayQuestionResponseDto*/

    // (옵션) 오늘 질문을 채팅방에 발행 + 질문 반환
    @POST("chat-rooms/{roomId}/publish-today-question")
    suspend fun publishTodayQuestion(
        @Path("roomId") roomId: String
    ): TodayQuestionResponseDto

}