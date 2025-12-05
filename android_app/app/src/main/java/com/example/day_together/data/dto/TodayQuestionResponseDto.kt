package com.example.day_together.data.dto

import com.google.gson.annotations.SerializedName

    data class TodayQuestionResponseDto(
        @SerializedName("roomId")
        val roomId: String,
        @SerializedName("question")
        val question: String,
        @SerializedName("target_date")
        val targetDate: String,
        @SerializedName("category")
        val category: String? = null,
        @SerializedName("tone")
        val tone: String? = null,
        @SerializedName("timeframe")
        val timeframe: String? = null,
    )

