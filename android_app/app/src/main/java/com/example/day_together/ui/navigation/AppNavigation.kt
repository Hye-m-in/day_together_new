package com.example.day_together.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.day_together.AuthManager
import com.example.day_together.MainScreen
import com.example.day_together.ui.auth.FindAccountScreen
import com.example.day_together.ui.auth.LoginScreen
import com.example.day_together.ui.auth.SignUpScreen
import com.example.day_together.ui.onboarding.OnboardingScreen
import com.example.day_together.ui.settings.EditProfileScreen
import com.example.day_together.ui.splash.SplashScreen
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState

/**
 * 앱의 전체 화면 경로(Route)를 상수로 정의하는 객체
 */
object AppDestinations {
    const val SPLASH_ROUTE = "splash"
    const val ONBOARDING_ROUTE = "onboarding"
    const val LOGIN_ROUTE = "login"
    const val MAIN_ROUTE = "main"
    const val SIGNUP_ROUTE = "signup"
    const val FIND_ACCOUNT_ROUTE = "find_account"
    const val EDIT_PROFILE_ROUTE = "edit_profile"
}

/**
 * 앱의 전체 화면 전환을 관리하는 최상위 컴포저블
 * 앱의 진입점(MainActivity)에서 이 함수를 호출
 */
@OptIn(ExperimentalPagerApi::class)
@Composable
fun AppNavigation() {
    // 앱 전체의 화면 이동을 담당하는 최상위 NavController 생성
    val navController: NavHostController = rememberNavController()
    val context = LocalContext.current

    // 앱의 시작점을 스플래시 화면으로 설정
    val startDestination = AppDestinations.SPLASH_ROUTE

    // NavHost: 내비게이션 경로와 실제 화면(Composable)을 매핑하는 컨테이너
    NavHost(navController = navController, startDestination = startDestination) {

        // 1. 스플래시 화면
        composable(AppDestinations.SPLASH_ROUTE) {
            SplashScreen(
                onTimeout = {
                    // 2초 후, 사용자의 상태를 확인하여 다음 화면 결정
                    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val onboardingCompleted = sharedPrefs.getBoolean("onboarding_completed", false)

                    val nextRoute = when {
                        // 이미 로그인 되어 있으면 -> 메인 화면으로
                        AuthManager.isUserLoggedIn() -> AppDestinations.MAIN_ROUTE
                        // 온보딩을 이미 봤다면 -> 로그인 화면으로
                        onboardingCompleted -> AppDestinations.LOGIN_ROUTE
                        // 아무것도 해당 안 되면 (첫 실행) -> 온보딩 화면으로
                        else -> AppDestinations.ONBOARDING_ROUTE
                    }

                    // 결정된 화면으로 이동하고, 스플래시 화면은 뒤로가기로 돌아올 수 없도록 스택에서 제거
                    navController.navigate(nextRoute) {
                        popUpTo(AppDestinations.SPLASH_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        // 2. 온보딩 화면
        composable(AppDestinations.ONBOARDING_ROUTE) {
            val pagerState = rememberPagerState()

            // 온보딩 마지막 페이지에 도달하면 '완료' 상태를 저장
            LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage == 2) { // 0, 1: 온보딩, 2: 시작하기 페이지
                    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
                }
            }
            OnboardingScreen(navController = navController, pagerState = pagerState)
        }

        // 3. 로그인 화면
        composable(AppDestinations.LOGIN_ROUTE) {
            LoginScreen(navController = navController)
        }

        // 4. 회원가입 화면
        composable(AppDestinations.SIGNUP_ROUTE) {
            SignUpScreen(navController = navController)
        }

        // 5. 계정 찾기 화면
        composable(AppDestinations.FIND_ACCOUNT_ROUTE) {
            FindAccountScreen(navController = navController)
        }

        // 6. 하단 바가 있는 메인 화면
        composable(AppDestinations.MAIN_ROUTE) {
            MainScreen(appNavController = navController)
        }

        // 7. 개인정보 수정 화면
        composable(AppDestinations.EDIT_PROFILE_ROUTE) {
            EditProfileScreen(navController = navController)
        }
    }
}
