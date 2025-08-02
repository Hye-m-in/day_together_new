package com.example.day_together

import android.util.Log
import com.example.day_together.FirebaseService
import com.example.day_together.data.model.TokenRequest
import com.example.day_together.data.model.TokenResponse
import com.example.day_together.network.ApiClient
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



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


    // (2) 구글 로그인 ID 토큰 → 커스텀 토큰 → Firebase 인증
    /**fun loginWithGoogleIdToken(idToken: String, onResult: (Boolean, String?) -> Unit) {
        Log.d("GOOGLE_LOGIN", "loginWithGoogleIdToken 호출, idToken=$idToken")
        ApiClient.service.googleLogin(TokenRequest(idToken = idToken))
            .enqueue(object : Callback<TokenResponse> {
                override fun onResponse(
                    call: Call<TokenResponse>,
                    response: Response<TokenResponse>
                ) {
                    if (response.isSuccessful) {
                        val customToken = response.body()?.custom_token
                        if (customToken != null) {
                            Log.d("GOOGLE_LOGIN", "CustomToken 받음: $customToken")
                            auth.signInWithCustomToken(customToken)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d("GOOGLE_LOGIN", "Firebase 커스텀 토큰 로그인 성공")
                                        onResult(true, null)
                                    } else {
                                        Log.e("GOOGLE_LOGIN", "Firebase 로그인 실패", task.exception)
                                        onResult(false, task.exception?.message)
                                    }
                                }
                        } else {
                            Log.e("GOOGLE_LOGIN", "Custom token이 null")
                            onResult(false, "Custom token이 없습니다.")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("GOOGLE_LOGIN", "서버 오류: code=${response.code()}, body=$errorBody")
                        onResult(false, "서버 오류: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                    Log.e("GOOGLE_LOGIN", "Retrofit 호출 실패", t)
                    onResult(false, t.message)
                }
            })
    }*/

    // (3)네이버 로그인
    fun handleNaverToken(
        accessToken: String,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        Log.d("NAVER_LOGIN", "handleNaverToken 호출, token=$accessToken")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.service.naverLogin(TokenRequest(accessToken = accessToken))
                val customToken = response.custom_token

                if (customToken != null) {
                    auth.signInWithCustomToken(customToken)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("NAVER_LOGIN", "Firebase 커스텀 토큰 로그인 성공")
                                onResult(true, null)
                            } else {
                                Log.e("NAVER_LOGIN", "Firebase 로그인 실패", task.exception)
                                onResult(false, task.exception?.message)
                            }
                        }
                } else {
                    Log.e("NAVER_LOGIN", "Custom token이 null")
                    onResult(false, "Custom token이 없습니다.")
                }
            } catch (e: Exception) {
                Log.e("NAVER_LOGIN", "예외 발생", e)
                onResult(false, e.message)
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