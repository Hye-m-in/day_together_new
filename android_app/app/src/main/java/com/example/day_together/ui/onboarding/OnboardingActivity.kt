package com.example.day_together.ui.onboarding

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.example.day_together.ui.theme.Day_togetherTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState


class OnboardingActivity : ComponentActivity() {
    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val pagerState = rememberPagerState()

            // 로그인 화면에 도달하면 온보딩 완료 상태 저장
            LaunchedEffect(pagerState.currentPage) {
                // 0,1: 온보딩, 2: 로그인
                if (pagerState.currentPage == 2) {
                    val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
                }
            }

            Day_togetherTheme {
                OnboardingScreen(navController = navController, pagerState = pagerState)
            }
        }
    }
}