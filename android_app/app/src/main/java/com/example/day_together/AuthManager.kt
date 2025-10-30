package com.example.day_together

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging

import com.example.day_together.data.model.User


object AuthManager {

    val db = FirebaseService.db
    val auth = FirebaseService.auth

    //회원가입
    fun registerUser(
        name: String,
        email: String,
        password: String,
        position: String,
        birthDate: String,
        isLunar: Boolean,
        onResult: (Boolean, String?) -> Unit
    ) {
        // 이메일을 소문자로 변환하여 계정 생성
        val finalEmail = email.lowercase()

        auth.createUserWithEmailAndPassword(finalEmail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val memberId = user?.uid ?: ""

                    // User 데이터 클래스를 사용하여 모든 정보 저장
                    val memberData = User(
                        uid = memberId,
                        name = name,
                        email = finalEmail, // 소문자로 변환된 이메일 저장
                        position = position,
                        birthDate = birthDate,
                        isLunar = isLunar,
                        invitedChatRoomId = null,
                        profileImageUrl = "",  // 기본값 필드 추가
                        fcmToken = null        // 기본값 필드 추가
                    )

                    db.collection("users")
                        .document(memberId)
                        .set(memberData) // User 객체를 통째로 set
                        .addOnSuccessListener {
                            // 회원가입 성공 시, 즉시 FCM 토큰 가져와서 저장
                            FirebaseMessaging.getInstance().token
                                .addOnCompleteListener { tokenTask ->
                                    if (tokenTask.isSuccessful) {
                                        val token = tokenTask.result
                                        // 방금 만든 user 문서에 fcmToken 필드 추가/업데이트
                                        db.collection("users").document(memberId)
                                            .update("fcmToken", token)
                                            .addOnSuccessListener {
                                                // 토큰 저장까지 완료 후 최종 성공 알림
                                                onResult(true, null)
                                            }
                                            .addOnFailureListener {
                                                // 토큰 저장은 실패했지만, 회원가입 자체는 성공했으므로
                                                // 일단 성공으로 처리 (다음 로그인 시 갱신됨)
                                                onResult(true, null)
                                            }
                                    } else {
                                        // 토큰 가져오기 실패. 위와 동일하게 일단 성공 처리.
                                        onResult(true, null)
                                    }
                                }

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
        // 로그인 시에도 소문자로 변환
        val finalEmail = email.lowercase()

        auth.signInWithEmailAndPassword(finalEmail, password)
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
                                // 구글 이메일도 소문자로 저장
                                val finalEmail = user.email?.lowercase() ?: ""

                                // 최초 구글 로그인 시 Firestore에 사용자 정보 저장
                                // User 데이터 클래스 사용
                                val memberData = User(
                                    uid = user.uid,
                                    name = user.displayName ?: "가족",
                                    email = finalEmail,
                                    position = "가족", // 기본값
                                    invitedChatRoomId = null,
                                    birthDate = null, // 구글 로그인은 생일 정보 없음
                                    isLunar = null,   // 구글 로그인은 생일 정보 없음
                                    profileImageUrl = user.photoUrl?.toString() ?: ""
                                )
                                userDocRef.set(memberData)
                                    .addOnSuccessListener {
                                        // 구글 최초 로그인 시에도 FCM 토큰 저장
                                        FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                                            if(tokenTask.isSuccessful) {
                                                db.collection("users").document(user.uid)
                                                    .update("fcmToken", tokenTask.result)
                                                    .addOnCompleteListener { onResult(true, null) }
                                            } else {
                                                onResult(true, null)
                                            }
                                        }

                                    }
                                    .addOnFailureListener { e -> onResult(false, e.message) }
                            } else {
                                // 기존 사용자도 로그인할 때마다 토큰 갱신
                                FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                                    if(tokenTask.isSuccessful) {
                                        db.collection("users").document(user.uid)
                                            .update("fcmToken", tokenTask.result)
                                            .addOnCompleteListener { onResult(true, null) }
                                    } else {
                                        onResult(true, null) // 토큰 갱신 실패해도 로그인은 성공
                                    }
                                }

                            }
                        }
                    } else {
                        onResult(true, null) // Firestore 확인이 안돼도 로그인 자체는 성공
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