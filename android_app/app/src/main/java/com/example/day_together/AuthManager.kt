package com.example.day_together

import android.app.Activity
import android.util.Log
import com.example.day_together.data.dto.ErrorResponse
import com.example.day_together.data.dto.GoogleTokenRequest
import com.example.day_together.data.dto.NaverTokenRequest
import com.example.day_together.data.model.User
import com.example.day_together.data.remote.ApiClient
import com.example.day_together.data.repository.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException

/**
 * 앱의 인증 관련 로직을 전담하는 싱글톤 객체
 * Firebase Authentication, Firestore(사용자 정보 저장), 네이버/구글 로그인(서버 연동)을 처리
 */
object AuthManager {

    /**
     * 네이버 로그인 SDK를 실행하여 사용자 인증을 진행
     * (이 단계에서는 네이버 측의 Access Token만 발급)
     *
     * @param activity 네이버 로그인 SDK 실행을 위한 Activity Context
     * @param onResult 로그인 결과를 반환할 콜백 (성공여부, 토큰, 에러메시지)
     */
    fun startNaverSdkLogin(
        activity: Activity,
        onResult: (success: Boolean, token: String?, errorMsg: String?) -> Unit
    ) {
        val callback = object : OAuthLoginCallback {
            override fun onSuccess() {
                // 네이버 로그인 성공 시 액세스 토큰 획득
                val token = NaverIdLoginSDK.getAccessToken()
                onResult(true, token, null)
            }

            override fun onFailure(httpStatus: Int, message: String) {
                // 통신 실패 등 SDK 내부 오류
                onResult(false, null, "통신 실패 ($httpStatus): $message")
            }

            override fun onError(errorCode: Int, message: String) {
                // 로그인 과정 중 오류 발생
                onResult(false, null, "로그인 오류 ($errorCode): $message")
            }
        }
        // 네이버 로그인 창 실행
        NaverIdLoginSDK.authenticate(activity, callback)
    }

    /**
     * 네이버 Access Token을 백엔드 서버로 보내 Firebase Custom Token으로 교환하고,
     * 이를 이용해 Firebase에 로그인
     *
     * @param accessToken 네이버 SDK로부터 받은 Access Token
     * @return AuthResult 로그인 성공 또는 실패 결과
     */
    suspend fun loginWithNaver(accessToken: String): AuthResult {
        return try {
            // 1. 백엔드 서버에 네이버 토큰 전송
            val request = NaverTokenRequest(accessToken = accessToken)
            val response = ApiClient.authService.naverLogin(request)

            // 2. 서버로부터 Firebase Custom Token 수신
            val customToken = response.customToken
            if (customToken.isBlank()) {
                return AuthResult.Failure("서버로부터 유효한 토큰을 받지 못했습니다.")
            }

            // 3. Custom Token으로 Firebase 로그인 수행
            FirebaseAuth.getInstance().signInWithCustomToken(customToken).await()
            AuthResult.Success()
        } catch (e: Exception) {
            // HTTP 400 오류 (서버에서 정의한 에러 메시지 파싱)
            if (e is HttpException && e.code() == 400) {
                val errorBody = e.response()?.errorBody()?.string()
                try {
                    val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                    val detailMessage = errorResponse.detail ?: "알 수 없는 400 오류입니다."
                    return AuthResult.Failure(detailMessage)
                } catch (jsonE: Exception) {
                    return AuthResult.Failure("서버 응답을 해석할 수 없습니다.")
                }
            }
            Log.e("AuthManager", "네이버 로그인 실패", e)
            AuthResult.Failure("서버와 통신할 수 없습니다. 네트워크 상태를 확인해주세요.")
        }
    }

    /**
     * 이메일/비밀번호로 신규 회원가입 진행
     * Firebase Auth에 계정을 생성하고, Firestore에 사용자 정보를 저장
     *
     * @param name 이름
     * @param email 이메일
     * @param password 비밀번호
     * @param position 가족 내 역할
     * @param birthDate 생년월일
     */
    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        position: String,
        birthDate: String,
    ): AuthResult {
        val finalEmail = email.lowercase() // 이메일 소문자 통일

        return try {
            // 1. Firebase Authentication에 사용자 생성
            val authResult = FirebaseAuth.getInstance().createUserWithEmailAndPassword(finalEmail, password).await()
            val user = authResult.user
            val memberId = user?.uid ?: ""

            if (memberId.isBlank()) {
                return AuthResult.Failure("사용자 ID 생성에 실패했습니다.")
            }

            // 2. Firestore에 저장할 사용자 데이터 객체 생성
            val memberData = User(
                uid = memberId,
                name = name,
                email = finalEmail,
                position = position,
                birthDate = birthDate,
                invitedChatRoomId = null,
                profileImageUrl = "",
                fcmToken = null
            )

            // 3. Firestore 'users' 컬렉션에 사용자 정보 저장
            FirebaseFirestore.getInstance().collection("users")
                .document(memberId)
                .set(memberData)
                .await()

            // 4. 가입 직후 푸시 알림을 위한 FCM 토큰 저장 시도
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                FirebaseFirestore.getInstance().collection("users").document(memberId)
                    .update("fcmToken", token)
                    .await()
            } catch (e: Exception) {
                Log.w("AuthManager", "FCM 토큰 저장 실패", e) // 토큰 저장이 실패해도 가입은 성공으로 처리
            }

            AuthResult.Success()

        } catch (e: Exception) {
            AuthResult.Failure(getFriendlyErrorMessage(e))
        }
    }

    /**
     * 일반 이메일/비밀번호 로그인을 수행
     * 로그인 성공 시 FCM 토큰을 갱신
     */
    suspend fun loginUser(email: String, password: String): AuthResult {
        val finalEmail = email.lowercase()
        return try {
            // 1. Firebase Auth 로그인
            FirebaseAuth.getInstance().signInWithEmailAndPassword(finalEmail, password).await()

            // 2. 로그인 성공 후 FCM 토큰 갱신 (기기 변경 등에 대응)
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val token = FirebaseMessaging.getInstance().token.await()
                    FirebaseFirestore.getInstance().collection("users")
                        .document(currentUser.uid)
                        .update("fcmToken", token)
                        .await()
                }
            } catch (e: Exception) {
                Log.w("AuthManager", "로그인 후 FCM 토큰 갱신 실패", e)
            }
            AuthResult.Success()
        } catch (e: Exception) {
            AuthResult.Failure(getFriendlyErrorMessage(e))
        }
    }

    /**
     * 구글 로그인 ID Token을 백엔드 서버로 전송하여 검증 후,
     * Firebase Custom Token을 받아 로그인
     */
    suspend fun loginWithGoogleViaServer(idToken: String): AuthResult {
        return try {
            // 1. 서버로 구글 ID Token 전송
            val request = GoogleTokenRequest(idToken = idToken)
            val response = ApiClient.authService.googleLogin(request)
            val customToken = response.customToken

            if (customToken.isBlank()) {
                return AuthResult.Failure("서버로부터 유효한 토큰을 받지 못했습니다.")
            }

            // 2. 발급받은 Custom Token으로 Firebase 로그인
            FirebaseAuth.getInstance().signInWithCustomToken(customToken).await()
            AuthResult.Success()
        } catch (e: Exception) {
            // 서버 에러 처리
            if (e is HttpException && e.code() == 400) {
                val errorBody = e.response()?.errorBody()?.string()
                try {
                    val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
                    val detailMessage = errorResponse.detail ?: "알 수 없는 400 오류입니다."
                    return AuthResult.Failure(detailMessage)
                } catch (jsonE: Exception) {
                    return AuthResult.Failure("서버 응답을 해석할 수 없습니다.")
                }
            }
            Log.e("AuthManager", "구글 로그인(서버) 실패", e)
            AuthResult.Failure("서버와 통신할 수 없습니다. 네트워크 상태를 확인해주세요.")
        }
    }

    /**
     * 사용자의 비밀번호를 변경
     * 보안상 중요한 작업이므로 재인증(Re-authentication) 과정을 거친 후 변경
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): AuthResult {
        val user = FirebaseAuth.getInstance().currentUser ?: return AuthResult.Failure("로그인이 필요합니다.")
        val email = user.email ?: return AuthResult.Failure("이메일 정보를 찾을 수 없습니다.")

        return try {
            // 1. 기존 비밀번호로 자격 증명(Credential) 생성
            val credential = EmailAuthProvider.getCredential(email, oldPassword)

            // 2. 사용자 재인증 (로그인한지 오래된 경우를 대비)
            user.reauthenticate(credential).await()

            // 3. 비밀번호 업데이트
            user.updatePassword(newPassword).await()
            Log.d("AuthManager", "비밀번호 변경 성공")
            AuthResult.Success()
        } catch (e: Exception) {
            Log.e("AuthManager", "비밀번호 변경 실패", e)
            val message = if (e is FirebaseAuthInvalidCredentialsException) {
                "기존 비밀번호가 일치하지 않습니다."
            } else {
                getFriendlyErrorMessage(e)
            }
            AuthResult.Failure(message)
        }
    }

    /**
     * 비밀번호 재설정 이메일 발송
     */
    suspend fun resetPassword(email: String): AuthResult {
        return try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            AuthResult.Success()
        } catch (e: Exception) {
            AuthResult.Failure(getFriendlyErrorMessage(e))
        }
    }

    // 로그아웃
    fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
    }

    /**
     * 현재 로그인된 사용자의 UID를 반환
     */
    fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    /**
     * 현재 로그인 상태인지 확인
     */
    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    /**
     * Firebase 예외를 사용자에게 보여줄 메시지로 변환
     */
    private fun getFriendlyErrorMessage(e: Exception?): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> "이메일 또는 비밀번호가 잘못되었습니다."
            is FirebaseAuthInvalidUserException -> "존재하지 않는 사용자입니다."
            is FirebaseAuthUserCollisionException -> "이미 존재하는 계정입니다."
            else -> e?.message ?: "문제가 발생했습니다. 다시 시도해주세요."
        }
    }
}