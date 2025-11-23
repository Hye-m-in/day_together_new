package com.example.day_together.data.model

import com.google.firebase.firestore.PropertyName

// 앱 사용자의 정보를 담는 데이터 모델 -> Firestore의 문서(Document)와 1:1로 매핑

data class User(
    // 사용자의 고유 식별자 (Firebase Auth의 UID, Firestore에는 'member_id'로 저장됨)
    @get:PropertyName("member_id") @set:PropertyName("member_id")
    var uid: String = "",

    // 사용자 이름 (닉네임)
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",

    // 사용자 이메일 주소
    @get:PropertyName("email") @set:PropertyName("email")
    var email: String = "",

    // 생년월일 (YYYYMMDD 형식)
    @get:PropertyName("birthDate") @set:PropertyName("birthDate")
    var birthDate: String? = null,

    // 가족 내 역할 또는 호칭 (예: "아빠", "딸")
    @get:PropertyName("position") @set:PropertyName("position")
    var position: String = "",

    // 프로필 이미지 URL
    @get:PropertyName("profileImageUrl") @set:PropertyName("profileImageUrl")
    var profileImageUrl: String = "",

    // 푸시 알림을 위한 FCM 토큰
    @get:PropertyName("fcmToken") @set:PropertyName("fcmToken")
    var fcmToken: String? = null,

    // 현재 초대받거나 소속된 채팅방의 ID
    // (초대 수락 시 AppRepository에서 이 필드를 업데이트함)
    @get:PropertyName("invitedChatRoomId") @set:PropertyName("invitedChatRoomId")
    var invitedChatRoomId: String? = null
)