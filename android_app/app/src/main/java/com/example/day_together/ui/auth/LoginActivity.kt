package com.example.day_together.ui.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.day_together.ui.theme.Day_togetherTheme

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Day_togetherTheme {

                LoginScreen(navController = rememberNavController())
            }
        }
    }
}