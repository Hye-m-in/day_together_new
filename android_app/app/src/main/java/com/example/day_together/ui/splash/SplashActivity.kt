package com.example.day_together.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.day_together.AuthManager
import com.example.day_together.MainActivity
import com.example.day_together.ui.onboarding.OnboardingActivity


@SuppressLint("CustomSplashScreen") 
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent로 Composable UI 화면에 표시
        setContent {
            // SplashScreen Composable을 호출 + 시간초과 시 실행될 콜백 함수를 전달함
            SplashScreen {
                checkUserStateAndNavigate()
            }
        }
    }

    // 사용자 로그인 여부 확인하고 적절한 화면으로 이동
    private fun checkUserStateAndNavigate() {
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)


        // 사용자의 로그인 상태를 확인하여 다음으로 이동할 액티비티 결정
        val nextActivity = if (AuthManager.isUserLoggedIn()) {
            MainActivity::class.java // 로그인 상태이면 메인 화면으로
        } else {
            OnboardingActivity::class.java // 로그아웃 상태이면 온보딩 화면으로
        }

        // 결정된 액티비티로 화면을 전환
        startActivity(Intent(this, nextActivity))
        // 현재 스플래시 화면은 종료하여 사용자가 뒤로 가기 버튼을 눌렀을 때 다시 보이지 않도록 설정
        finish()
    }
}