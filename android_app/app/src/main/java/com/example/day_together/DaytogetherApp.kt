
package com.example.day_together

import android.app.Application
import com.navercorp.nid.NaverIdLoginSDK

/**
 * 앱 전체에서 한 번만 실행되는 Application 클래스
 * - AndroidManifest.xml 의 android:name=".DayTogetherApp" 과 연결됨
 * - 네이버 SDK 자동 초기화를 담당
 */
class DayTogetherApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 네이버 SDK 초기화 (values/strings.xml 에 정의된 값 가져오기)
        NaverIdLoginSDK.initialize(
            context = this,
            clientId = getString(R.string.naver_client_id),
            clientSecret = getString(R.string.naver_client_secret),
            clientName = getString(R.string.app_name)
        )
    }
}
