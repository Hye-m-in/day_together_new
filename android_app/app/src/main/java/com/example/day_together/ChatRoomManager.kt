package com.example.day_together

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration

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

                if (finalUids.isEmpty()) {
                    onComplete(false, "초대한 사용자 정보를 찾을 수 없습니다.")
                    return@addOnSuccessListener
                }

                val chatRoomRef = db.collection("chatRooms").document(chatRoomId)
                val batch = db.batch()

                finalUids.forEach { uid ->
                    val invitationRef = db.collection("users")
                        .document(uid)
                        .collection("invitations")
                        .document(chatRoomId)

                    batch.set(
                        invitationRef, mapOf(
                            "chatRoomId" to chatRoomId,
                            "status" to "pending",
                            "invitedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // 기존 채팅방 문서에 초대된 사용자 ID 목록만 업데이트
                batch.update(
                    chatRoomRef,
                    "invitedUsers",
                    FieldValue.arrayUnion(*finalUids.toTypedArray())
                )

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
            batch.update(userInvitationRef, "status", "accepted")
            batch.update(chatRoomRef, "members", FieldValue.arrayUnion(uid))
            batch.update(userRef, "invitedChatRoomId", chatRoomId)
        }.addOnSuccessListener {
            // [추가됨] 초대 수락에 성공하면, 캘린더에 자신의 생일 일정을 자동으로 등록합니다.
            CalendarManager.registerBirthday(chatRoomId, uid)

            onComplete(true, null)
        }.addOnFailureListener { e ->
            Log.e("ChatRoomManager", "초대 수락 실패: ${e.message}", e)
            onComplete(false, "수락 처리 실패: ${e.message}")
        }
    }

    // 실시간 초대 감지 리스너 함수
    fun listenForInvitations(
        userId: String,
        onInvitationReceived: (String?) -> Unit
    ): ListenerRegistration {
        return db.collection("users").document(userId).collection("invitations")
            .whereEqualTo("status", "pending")
            .limit(1)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("ChatRoomManager", "Invitation listen failed.", error)
                    onInvitationReceived(null)
                    return@addSnapshotListener
                }

                val pendingInvitationId = snapshots?.documents?.firstOrNull()?.id
                onInvitationReceived(pendingInvitationId)
            }
    }
}