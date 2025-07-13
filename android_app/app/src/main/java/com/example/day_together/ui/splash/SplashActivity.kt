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
import com.example.day_together.ui.splash.SplashScreen

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen {
                checkUserStateAndNavigate()
            }
        }
    }

    private fun checkUserStateAndNavigate() {
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val onboardingCompleted = sharedPrefs.getBoolean("onboarding_completed", false)

        val nextActivity = if (AuthManager.isUserLoggedIn()) {
            MainActivity::class.java
        } else {
            OnboardingActivity::class.java
        }

        startActivity(Intent(this, nextActivity))
        finish()
    }
}