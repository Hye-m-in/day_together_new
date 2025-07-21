
package com.example.day_together.ui.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.day_together.ui.theme.Day_togetherTheme

/**
 * 로그인 화면을 띄워주는 액티비티
 * 실제 UI 내용은 LoginScreen.kt 파일에 잇음
 */
class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 이 액티비티의 전체 내용을 Compose UI로 채운다고 설정
        setContent {
            // 앱 공통 테마(색상, 폰트 등) 적용
            Day_togetherTheme {
                // 실제 UI를 그리는 LoginScreen 컴포저블을 여기서 불러옴
                LoginScreen()
            }
        }
    }
}
