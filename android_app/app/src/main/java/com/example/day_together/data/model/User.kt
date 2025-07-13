package com.example.day_together.data.model

import com.google.firebase.firestore.PropertyName

data class User(
    // AuthManager에서 사용하는 사용자의 고유 ID
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",

    // AuthManager에서 사용하는 이름
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",

    // AuthManager에서 사용하는 이메일
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",

    // SignUpActivity에서 받는 가족 내 역할
    @get:PropertyName("position") @set:PropertyName("position")
    var position: String = "",

    // 로그인 시 업데이트 되는 푸시 알림용 토큰
    @get:PropertyName("fcmToken") @set:PropertyName("fcmToken")
    var fcmToken: String? = null,

    // ChatRoomManager에서 사용하는 초대된 채팅방 ID
    @get:PropertyName("invitedChatRoomId") @set:PropertyName("invitedChatRoomId")
    var invitedChatRoomId: String? = null
)