package com.example.day_together

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging

import kotlinx.coroutines.tasks.await

import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

import android.util.Log




object AuthManager {

    val db = FirebaseService.db
    val auth = FirebaseService.auth

    //회원가입
    fun registerUser(
        name: String,
        email: String,
        password: String,
        birthDate: String, // 생년월일 파라미터 추가
        position: String,
        profileImage: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid ?: ""

                    val userData = hashMapOf(
                        "user_id" to userId,
                        "name" to name,
                        "email" to email,
                        "birthDate" to birthDate, // DB에 생년월일 저장 추가
                        "position" to position,
                        "profile_image" to profileImage,
                        "invitedChatRoomId" to null
                    )

                    db.collection("users")
                        .document(userId)
                        .set(userData)
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

    // 구글 로그인을 처리하는 함수 추가
    fun signInWithGoogleCredential(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 로그인 성공 후 Firestore에 사용자 정보가 있는지 확인
                    val user = auth.currentUser
                    if (user != null) {
                        val userDocRef = db.collection("users").document(user.uid)
                        userDocRef.get().addOnSuccessListener { document ->
                            if (!document.exists()) {
                                // 최초 구글 로그인 시 Firestore에 사용자 정보 저장
                                val memberData = hashMapOf(
                                    "member_id" to user.uid,
                                    "name" to user.displayName,
                                    "email" to user.email,
                                    "position" to "가족", // 기본값
                                    "invitedChatRoomId" to null
                                    // 참고: 구글 로그인 시에는 생년월일을 바로 알 수 없으므로 추후 입력받는 로직이 필요
                                )
                                userDocRef.set(memberData)
                                    .addOnSuccessListener { onResult(true, null) }
                                    .addOnFailureListener { e -> onResult(false, e.message) }
                            } else {
                                // 기존 사용자는 바로 성공 처리
                                onResult(true, null)
                            }
                        }
                    }
                } else {
                    // 로그인 실패
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
        //실제 로그인 상태 반환
        return auth.currentUser != null
    }

    // 현재 로그인된 사용자 삭제 (Auth) + 네이버 연동 해제 포함
    suspend fun deleteCurrentUser(): Boolean {
        return try {
            val user = auth.currentUser ?: return false

            // 네이버 연동 해제 시도 (로그인 여부와 상관없이 시도하되 에러는 무시)
            try {
                val isNaverUnlinked = unlinkNaver()
                if (isNaverUnlinked) {
                    Log.d("AuthManager", "네이버 연동 해제 성공")
                }
            } catch (e: Exception) {
                Log.w("AuthManager", "네이버 연동 해제 건너뜀")
            }

            // Firebase Auth 계정 삭제
            user.delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 네이버 연동 해제 (Suspend Function)
    private suspend fun unlinkNaver(): Boolean = suspendCoroutine { cont ->
        NidOAuthLogin().callDeleteTokenApi(object : OAuthLoginCallback {
            override fun onSuccess() {
                cont.resume(true)
            }
            override fun onFailure(httpStatus: Int, message: String) {
                Log.e("AuthManager", "네이버 탈퇴 실패: $message")
                // 네이버 로그인이 안 되어 있거나 토큰이 없으면 실패로 간주되나, 앱 탈퇴 흐름을 막지 않기 위해 false 반환 후 진행
                cont.resume(false)
            }
            override fun onError(errorCode: Int, message: String) {
                onFailure(errorCode, message)
            }
        })
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