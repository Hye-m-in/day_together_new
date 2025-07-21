package com.example.day_together.ui.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.day_together.ui.theme.Day_togetherTheme

/**
 * 계정 찾기 화면을 띄워주는 액티비티 -> 실제 UI 내용은 FindAccountScreen.kt 파일에 존재
 *
 * 컴포저블 함수(= 가구 설계도, 화면에 어떻게 보일 것인지 설명)
 * 1. 목적 : ui 설명 및 화면 그리기
 * 2. 어노테이션(=꼬리표 또는 주석) : @Composable 필수
 * 3. 값 반환 없음
 * 4. 다른 @Composable 함수 안에서만 호출 가능
 * 5. 핵심 역할 : 어떻게 보일 것인가?
 */
class FindAccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 이 액티비티의 전체 내용을 Compose UI로 채운다고 설정
        setContent {
            // 앱 공통 테마(색상, 폰트 등) 적용
            Day_togetherTheme {
                // 실제 UI를 그리는 FindAccountScreen 컴포저블을 여기서 불러옴
                FindAccountScreen()
            }
        }
    }
}