package com.example.day_together

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.functions.FirebaseFunctions
import java.util.*

object ChatRoomManager {

    val db = FirebaseService.db
    val auth = FirebaseService.auth

    //가족 초대
    fun inviteMembers(
        chatRoomId: String,
        inviterUserId: String,
        invitedUserId: List<String>,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val resolveTasks = invitedUserId.map { input ->
            if (input.contains("@")) {
                db.collection("users").whereEqualTo("email", input).limit(1).get()
                    .continueWith { task -> task.result?.documents?.firstOrNull()?.id }
            } else {
                Tasks.forResult(input)
            }
        }

        Tasks.whenAllSuccess<String>(resolveTasks)
            .addOnSuccessListener { resolvedUids ->
                val finalUids = resolvedUids.filterNotNull()

                if(finalUids.isEmpty()){
                    onComplete(false, "초대한 사용자 정보를 찾을 수 없습니다.")
                    return@addOnSuccessListener
                }

                val chatRoomRef = db.collection("chatRooms").document(chatRoomId)

                // 1. 먼저 chatRoom 문서를 생성 (덮어쓰기 방지 위해 set 대신 set with merge 사용 가능)
                val batch = db.batch()

                finalUids.forEach { uid ->
                    // 각 사용자에게 invitations 문서 추가
                    val invitationRef = db.collection("users")
                        .document(uid)
                        .collection("invitations")
                        .document(chatRoomId)

                    batch.set(invitationRef, mapOf(
                        "chatRoomId" to chatRoomId,
                        "status" to "pending",
                        "invitedAt" to FieldValue.serverTimestamp()
                    ))
                }

                batch.set(chatRoomRef, mapOf(
                    "chatRoomId" to chatRoomId,
                    "members" to listOf(inviterUserId), // 초대한 사람은 바로 참여
                    "invitedUsers" to finalUids,
                    "createdAt" to FieldValue.serverTimestamp()
                ))

                // 채팅방에 초대한 유저 목록 업데이트
                // chatRoom 문서에 초대한 유저들을 invitedUsers 필드에 추가
                batch.update(chatRoomRef, "invitedUsers", FieldValue.arrayUnion(*finalUids.toTypedArray()))

                batch.commit()
                    .addOnSuccessListener {
                        finalUids.forEach { uid ->
                            FCMService.sendInviteNotification(uid, inviterUserId)
                        }
                        onComplete(true, null)
                    }
            }
            .addOnFailureListener { e ->
                onComplete(false, "사용자 확인 실패: ${e.message}")
            }
    }

    // 초대 수락
    fun acceptInvitation(chatRoomId: String, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return onComplete(false, "로그인 필요")
        val uid = user.uid

        val userInvitationRef = db.collection("users")
            .document(uid)
            .collection("invitations")
            .document(chatRoomId)
        val chatRoomRef = db.collection("chatRooms").document(chatRoomId)
        val userRef = db.collection("users").document(uid)

        db.runBatch { batch ->
            // 초대 수락 상태 기록
            batch.update(userInvitationRef, "status", "accepted")
            // members 필드에 현재 유저 추가
            batch.update(chatRoomRef, "members", FieldValue.arrayUnion(uid))
            // invitedCharRoomId 필드 갱신(기존 null)
            batch.update(userRef, "invitedChatRoomId", chatRoomId)
        }.addOnSuccessListener {
            onComplete(true, null)
        }.addOnFailureListener { e ->
            Log.e("ChatRoomManager", "초대 수락 실패: ${e.message}", e)
            onComplete(false, "수락 처리 실패: ${e.message}")
        }
    }
}
