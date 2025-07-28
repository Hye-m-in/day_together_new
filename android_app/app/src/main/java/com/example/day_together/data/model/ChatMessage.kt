package com.example.day_together.data.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

data class ChatMessage(
    // ChatActivity에서 사용하는 메시지 내용
    @get:PropertyName("content") @set:PropertyName("content")
    var content: String = "",

    // ChatActivity에서 보내는 사람의 '이름(name)'을 저장하고 있음
    @get:PropertyName("sender") @set:PropertyName("sender")
    var sender: String = "",

    // ChatActivity에서 사용하는 메시지 전송 시간
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Date = Date()
)