package com.example.day_together.data.model

import com.google.gson.annotations.SerializedName

data class TokenRequest(
    @SerializedName("id_token")
    val idToken: String? = null,

    @SerializedName("access_token")
    val accessToken: String? = null
)

