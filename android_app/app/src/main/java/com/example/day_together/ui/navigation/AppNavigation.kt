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

object AppDestinations {
    const val SPLASH_ROUTE = "splash"
    const val ONBOARDING_ROUTE = "onboarding"
    const val LOGIN_ROUTE = "login"
    const val MAIN_ROUTE = "main_graph"
    const val SIGNUP_ROUTE = "signup"
    const val FIND_ACCOUNT_ROUTE = "find_account"
    const val EDIT_PROFILE_ROUTE = "edit_profile"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val isFirstLaunch by remember { mutableStateOf(true) }
    Log.d("AppNavigation", "isFirstLaunch 값: $isFirstLaunch")

    val startDestination = AppDestinations.MAIN_ROUTE
    Log.d("AppNavigation", "임시 UI 개발 모드: 시작 지점 = $startDestination")

    NavHost(navController = navController, startDestination = startDestination) {
        composable(AppDestinations.SPLASH_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.SPLASH_ROUTE}")
            SplashScreen(
                onTimeout = {
                    navController.navigate(AppDestinations.MAIN_ROUTE) {
                        popUpTo(AppDestinations.SPLASH_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        composable(AppDestinations.ONBOARDING_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.ONBOARDING_ROUTE}")
            OnboardingScreen(navController = navController)
        }

        composable(AppDestinations.LOGIN_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.LOGIN_ROUTE}")
            LoginScreen(navController = navController, fromOnboarding = false)
        }

        composable(AppDestinations.SIGNUP_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.SIGNUP_ROUTE}")
            SignUpScreen(navController = navController)
        }

        composable(AppDestinations.FIND_ACCOUNT_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.FIND_ACCOUNT_ROUTE}")
            FindAccountScreen(navController = navController)
        }

        composable(AppDestinations.MAIN_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.MAIN_ROUTE}")
            HomeScreen(appNavController = navController)
        }

        composable(AppDestinations.EDIT_PROFILE_ROUTE) {
            Log.d("AppNavigation", "Current Route: ${AppDestinations.EDIT_PROFILE_ROUTE}")
            EditProfileScreen(navController = navController)
        }
    }
}