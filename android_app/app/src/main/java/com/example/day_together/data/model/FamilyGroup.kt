package com.example.day_together.data.model

import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * 가족(채팅 그룹)을 나타내는 데이터 모델
 * Firestore의 필드명과 변수명을 매핑하기 위해 @PropertyName 어노테이션 사용
 *
 * 어노테이션(Annotation) -> 코드에 붙이는 주석 또는 꼬리표(tag)
 * 1. 컴파일러에게 정보 제공
 * 2. 빌드 시점에 코드 자동 생성
 * 3. 실행 시점에 정보 제공
 *
 * 예시) @PropertyName("members") 어노테이션
 * members 변수를 Firebase Firestore 데이터베이스에 저장하거나 읽어올 때,
 * 변수 이름이 바뀌더라도 데이터베이스 필드 이름은 반드시 "members" 라는 이름으로 사용
 */

data class FamilyGroup(
    // 그룹에 참여가 확정된 멤버들의 고유 ID(uid) 목록
    @get:PropertyName("members") @set:PropertyName("members")
    var members: List<String> = listOf(),

    // 그룹에 초대되었지만 아직 수락하지 않은 멤버들의 uid 목록
    @get:PropertyName("invitedUsers") @set:PropertyName("invitedUsers")
    var invitedUsers: List<String> = listOf(),

    // 그룹이 생성된 시간
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Date = Date()
)