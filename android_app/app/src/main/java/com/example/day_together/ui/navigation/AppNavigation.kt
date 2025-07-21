package com.example.day_together.navigation

import com.example.day_together.ui.settings.EditProfileScreen
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.day_together.ui.auth.FindAccountScreen
import com.example.day_together.ui.auth.LoginScreen
import com.example.day_together.ui.auth.SignUpScreen
import com.example.day_together.ui.home.HomeScreen
import com.example.day_together.ui.onboarding.OnboardingScreen
import com.example.day_together.ui.splash.SplashScreen
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState

/**
 * 앱의 전체 화면 경로를 정의하는 객체
 */
object AppDestinations {
    const val SPLASH_ROUTE = "splash"
    const val ONBOARDING_ROUTE = "onboarding"
    const val LOGIN_ROUTE = "login"
    const val MAIN_ROUTE = "main_graph"
    const val SIGNUP_ROUTE = "signup"
    const val FIND_ACCOUNT_ROUTE = "find_account"
    const val EDIT_PROFILE_ROUTE = "edit_profile"
}

/**
 * 앱의 전체 화면 전환(내비게이션)을 관리하는 메인 컴포저블
 */
@OptIn(ExperimentalPagerApi::class)
@Composable
fun AppNavigation() {
    // NavController: 화면 이동을 담당하는 컨트롤러
    val navController = rememberNavController()
    val isFirstLaunch by remember { mutableStateOf(true) }
    Log.d("AppNavigation", "isFirstLaunch 값: $isFirstLaunch")

    // 앱 시작 시 보여줄 첫 화면을 설정
    val startDestination = AppDestinations.MAIN_ROUTE
    Log.d("AppNavigation", "임시 UI 개발 모드: 시작 지점 = $startDestination")

    // NavHost: 내비게이션 경로에 따라 어떤 화면을 보여줄지 결정하는 컨테이너
    NavHost(navController = navController, startDestination = startDestination) {
        // 스플래시 화면 경로
        composable(AppDestinations.SPLASH_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.SPLASH_ROUTE}")
            SplashScreen(
                onTimeout = {
                    // 일정 시간 후 메인 화면으로 이동
                    navController.navigate(AppDestinations.MAIN_ROUTE) {
                        // 스플래시 화면은 뒤로가기로 돌아올 수 없도록 스택에서 제거
                        popUpTo(AppDestinations.SPLASH_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        // 온보딩 화면 경로
        composable(AppDestinations.ONBOARDING_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.ONBOARDING_ROUTE}")
            val pagerState = rememberPagerState()

            OnboardingScreen(navController = navController, pagerState = pagerState)
        }

        // 로그인 화면 경로
        composable(AppDestinations.LOGIN_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.LOGIN_ROUTE}")

            LoginScreen(fromOnboarding = false)
        }

        // 회원가입 화면 경로
        composable(AppDestinations.SIGNUP_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.SIGNUP_ROUTE}")

            SignUpScreen(navController = navController)
        }

        // 계정 찾기 화면 경로
        composable(AppDestinations.FIND_ACCOUNT_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.FIND_ACCOUNT_ROUTE}")
            FindAccountScreen()
        }

        // 메인 화면(홈) 경로
        composable(AppDestinations.MAIN_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.MAIN_ROUTE}")
            HomeScreen(appNavController = navController)
        }

        // 프로필 수정 화면 경로
        composable(AppDestinations.EDIT_PROFILE_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.EDIT_PROFILE_ROUTE}")
            EditProfileScreen(navController = navController)
        }
    }
}