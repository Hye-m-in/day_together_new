package com.example.day_together.data.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

data class FamilyGroup(
    // ChatActivity에서 사용하는, 채팅방에 참여가 수락된 멤버들의 uid 목록
    @get:PropertyName("members") @set:PropertyName("members")
    var members: List<String> = listOf(),

    // ChatRoomManager에서 사용하는, 초대되었지만 아직 수락은 안 한 멤버들의 uid 목록
    @get:PropertyName("invitedUsers") @set:PropertyName("invitedUsers")
    var invitedUsers: List<String> = listOf(),

    // ChatActivity에서 사용하는 채팅방 생성 시간
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Date = Date()
)