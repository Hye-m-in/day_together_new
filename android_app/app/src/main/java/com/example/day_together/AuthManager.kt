package com.example.day_together

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.messaging.FirebaseMessaging

object AuthManager {

    val db = FirebaseService.db
    val auth = FirebaseService.auth

    //회원가입
    fun registerUser(
        name: String,
        email: String,
        password: String,
        position: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val memberId = user?.uid ?: ""

                    val memberData = hashMapOf(
                        "member_id" to memberId,
                        "name" to name,
                        "email" to email,
                        "position" to position,
                        "invitedChatRoomId" to null
                    )

                    db.collection("users")
                        .document(memberId)
                        .set(memberData)
                        .addOnSuccessListener {
                            onResult(true, null)
                        }
                        .addOnFailureListener { e ->
                            onResult(false, e.message)
                        }
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    //로그인
    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val currentUser = FirebaseService.auth.currentUser
                    if (currentUser != null) {
                        FirebaseMessaging.getInstance().token
                            .addOnCompleteListener { tokenTask ->
                                if (tokenTask.isSuccessful) {
                                    val token = tokenTask.result
                                    FirebaseService.db.collection("users")
                                        .document(currentUser.uid)
                                        .update("fcmToken", token)
                                }
                            }
                    }
                    onResult(true, null)
                } else {
                    onResult(false, getFriendlyErrorMessage(task.exception))
                }
            }
    }

    //로그아웃
    fun logoutUser() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // 에러 유형 메세지 매핑
    fun getFriendlyErrorMessage(e: Exception?): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> "이메일 또는 비밀번호가 잘못되었습니다."
            is FirebaseAuthInvalidUserException -> "존재하지 않는 사용자입니다."
            is FirebaseAuthUserCollisionException -> "이미 존재하는 계정입니다."
            else -> "문제가 발생했습니다. 다시 시도해주세요."
        }
    }
}
