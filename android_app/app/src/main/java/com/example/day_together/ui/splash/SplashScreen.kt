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
import androidx.compose.ui.tooling.preview.Preview
import com.example.day_together.ui.theme.Day_togetherTheme
import kotlinx.coroutines.delay

const val SPLASH_TIMEOUT = 2000L // 스플래시 화면 지속 시간 2초

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    Day_togetherTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "하루\n함께",
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayLarge
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(SPLASH_TIMEOUT)
        onTimeout()
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun SplashScreenPreview() {
    Day_togetherTheme {
        SplashScreen(onTimeout = {})
    }
}