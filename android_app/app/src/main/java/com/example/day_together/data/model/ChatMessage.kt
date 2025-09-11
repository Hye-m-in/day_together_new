package com.example.day_together.data.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * 채팅 메시지 한 개를 나타내는 데이터 모델
 * Firestore의 필드명과 변수명을 매핑하기 위해 @PropertyName 어노테이션 사용
 */
data class ChatMessage(
    // 메시지 내용
    @get:PropertyName("content") @set:PropertyName("content")
    var content: String = "",

    // 메시지를 보낸 사람의 이름
    @get:PropertyName("sender") @set:PropertyName("sender")
    var sender: String = "",

    // 메시지를 보낸 시간
    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Date = Date(),

    // 메세지 유형
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String = "",
)