package com.example.day_together

import android.app.Application
import com.example.day_together.BuildConfig
import com.navercorp.nid.NaverIdLoginSDK

/**
 * 앱 전체에서 한 번만 실행되는 Application 클래스
 * AndroidManifest.xml 의 android:name=".DayTogetherApp" 과 연결됨
 * 네이버 SDK 자동 초기화를 담당
 */
class DayTogetherApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // BuildConfig 클래스에서 값을 가져와 SDK 초기화
        NaverIdLoginSDK.initialize(
            context = this,
            clientId = BuildConfig.NAVER_CLIENT_ID,
            clientSecret = BuildConfig.NAVER_CLIENT_SECRET,
            clientName = BuildConfig.NAVER_CLIENT_NAME
        )
    }
}