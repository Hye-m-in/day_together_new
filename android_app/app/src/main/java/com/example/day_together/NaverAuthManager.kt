package com.example.day_together

import android.app.Activity
import android.content.Context
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback

private const val CALLBACK_URL = "your://app/callback"  // 필요 시
object NaverAuthManager {
    private val CLIENT_ID     = BuildConfig.NAVER_CLIENT_ID
    private val CLIENT_SECRET = BuildConfig.NAVER_CLIENT_SECRET
    // 네이버 개발자 센터에 등록된 애플리케이션 이름으로 바꿔주세요
    private const val CLIENT_NAME = "day_together"

    /** SDK 초기화 */
    fun initialize(context: Context) {
        // callbackUrl 파라미터 없애고 clientName을 네 번째 인자로 전달
        NaverIdLoginSDK.initialize(
            context,
            CLIENT_ID,
            CLIENT_SECRET,
            CLIENT_NAME
        )
    }


    /** 로그인 버튼 클릭 시 호출 */
    fun startLogin(
        activity: Activity,
        onResult: (success: Boolean, token: String?, errorMsg: String?) -> Unit
    ) {
        val callback = object : OAuthLoginCallback {
            override fun onSuccess() {
                val token = NaverIdLoginSDK.getAccessToken()
                onResult(true, token, null)
            }

            override fun onFailure(httpStatus: Int, message: String) {
                onResult(false, null, "통신 실패 ($httpStatus): $message")
            }

            // ❌ X: override fun onError(errorCode: OAuthErrorCode, message: String)
            // ✅ O:
            override fun onError(errorCode: Int, message: String) {
                onResult(false, null, "로그인 오류 ($errorCode): $message")
            }
        }
        NaverIdLoginSDK.authenticate(activity, callback)
    }
}

