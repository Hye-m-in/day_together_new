package com.example.day_together

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import java.util.*

object ChatRoomManager {

    val db = FirebaseService.db
    val auth = FirebaseService.auth
    val notFoundEmails = mutableListOf<String>()

    //가족 초대
    fun inviteMembers(
        chatRoomId: String,
        inviterUserId: String,
        invitedUserId: List<String>,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val tasks = invitedUserId.map { idOrEmail ->
            if (idOrEmail.contains("@")) {
                db.collection("members").whereEqualTo("email", idOrEmail).limit(1).get()
                    .continueWith { task ->
                        task.result?.documents?.firstOrNull()?.id
                    }
            } else {
                Tasks.forResult(idOrEmail)
            }
        }

        Tasks.whenAllSuccess<String>(tasks)
            .addOnSuccessListener { resolvedUids ->
                val finalUids = resolvedUids.filterNotNull()

                if(finalUids.isEmpty()){
                    onComplete(false, "초대한 이메일 중 존재하는 사용자가 없습니다.")
                    return@addOnSuccessListener
                }

                val chatRoomRef = db.collection("chatRooms").document(chatRoomId)
                chatRoomRef.get().addOnSuccessListener { snapshot ->
                    val existingInvited = snapshot.get("invitedUsers") as? List<String> ?: emptyList()
                    val updated = (existingInvited + finalUids).distinct()

                    chatRoomRef.update("invitedUsers", updated)
                        .addOnSuccessListener {
                            val userTasks = finalUids.map { uid ->
                                db.collection("users")
                                    .document(uid)
                                    .update("invitedChatRoomId", chatRoomId)
                            }

                            Tasks.whenAllSuccess<Void>(userTasks)
                                .addOnSuccessListener {
                                    finalUids.forEach{uid ->
                                        FCMService.sendInviteNotification(uid, inviterUserId)
                                    }
                                    onComplete(true, null) }
                                .addOnFailureListener {
                                    e -> onComplete(false, "유저 업데이트 실패: ${e.message}") }
                        }
                        .addOnFailureListener {
                            e -> onComplete(false, "초대 업데이트 실패: ${e.message}") }
                }.addOnFailureListener {
                    e -> onComplete(false, "채팅방 조회 실패: ${e.message}") }
            }
            .addOnFailureListener {
                e -> onComplete(false, "사용자 확인 실패: ${e.message}") }
    }

    // 초대 수락
    fun acceptInvitation(chatRoomId: String, onComplete: (Boolean, String?) -> Unit) {
        val currentUser = auth.currentUser ?: return onComplete(false, "로그인 정보 없음")
        val userId = currentUser.uid

        val data = hashMapOf(
            "userId" to userId,
            "invitedChatRoomId" to chatRoomId
        )

        FirebaseFunctions.getInstance()
            .getHttpsCallable("acceptInvitationAndCreateRoom")
            .call(data)
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, "채팅방 생성/입장 실패: ${e.message}")
            }
    }
}
