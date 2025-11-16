package com.example.day_together

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object ChatRoomManager {

    // 내부에서만 사용하므로 private으로 변경
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    // MainActivity 등 외부에서 로그인 상태 확인용으로 사용하므로 public 유지
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    // inviteMembers 함수 (AppRepository로 기능 이동)

    // acceptInvitation 함수 (AppRepository로 기능 이동)

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