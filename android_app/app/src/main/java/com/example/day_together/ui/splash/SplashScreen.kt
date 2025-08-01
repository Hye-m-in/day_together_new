package com.example.day_together.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.day_together.ui.theme.Day_togetherTheme
import kotlinx.coroutines.delay

// 스플래시 화면이 표시될 시간을 2000밀리초(2초)로 정의합니다.
const val SPLASH_TIMEOUT = 2000L

/**
 * 스플래시 화면의 UI를 정의하는 컴포저블 함수
 * 이제 이 함수는 NavController의 일부로 동작 -> 화면 이동 결정은 AppNavigation에서 이루어짐
 *
 * @param onTimeout 스플래시 화면 표시 시간이 끝난 후 호출될 콜백 함수
 * AppNavigation에게 다음 화면으로 이동할 시간임을 알림
 */
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    Day_togetherTheme {
        // 화면 전체를 채우는 Column을 생성하고, 내용을 수직/수평 중앙에 배치
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 앱 로고 텍스트를 표시
            Text(
                text = "하루\n함께",
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayLarge
            )
        }
    }

    // LaunchedEffect는 이 컴포저블이 처음 화면에 표시될 때(Composition) 딱 한 번만 실행되는 코드 처리
    // 키 값으로 Unit을 사용하여 재실행되지 않도록
    LaunchedEffect(Unit) {
        // 정의된 시간(SPLASH_TIMEOUT)만큼 기다림
        delay(SPLASH_TIMEOUT)
        // 시간이 다 되면, AppNavigation으로부터 전달받은 onTimeout 콜백 함수 호출
        onTimeout()
    }
}
