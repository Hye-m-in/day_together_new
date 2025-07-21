package com.example.day_together.ui.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.day_together.ui.theme.Day_togetherTheme

// SignUpScreen 화면을 띄워주는 액티비티
class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Day_togetherTheme {
                // SignUpScreen을 여기서 실제로 화면에 그려줌
                // NavController는 rememberNavController()로 생성해서 넘겨줌
                SignUpScreen(navController = rememberNavController())
            }
        }
    }
}